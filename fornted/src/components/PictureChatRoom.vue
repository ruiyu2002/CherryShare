<template>
  <div class="chat-room">
    <div class="chat-content">
      <div class="messages-container">
        <!-- 顶部提示 -->
        <div class="header-actions">
          <template v-if="!hasMore && messages.length > 0 && isAtTop">
            <div class="first-message-tip">已经到顶啦</div>
          </template>
          <template v-else-if="hasMore && isAtTop">
            <a-button
              type="link"
              size="small"
              :loading="loadingHistory"
              @click="loadHistory"
              class="load-more-btn"
            >
              <template #icon><DownOutlined /></template>
              加载更多
            </a-button>
          </template>
        </div>

        <div class="chat-messages" ref="messageContainer" @scroll="handleScroll">
          <template v-for="(msg, index) in messages" :key="msg.id">
            <div v-if="shouldShowTimestamp(msg, messages[index - 1])" class="timestamp-divider">
              {{ formatMessageDivider(msg.createTime) }}
            </div>
            <div class="message"
                 :class="{ 'message-self': msg.sender?.id === loginUser?.id }"
            >
              <div class="message-wrapper">
                <a-avatar
                  :src="msg.sender?.userAvatar"
                  class="clickable-avatar"
                  @click="handleAvatarClick(msg.sender)"
                />
                <div class="message-content">
                  <!-- 只在图片聊天室和空间聊天室显示用户昵称 -->
                  <div class="message-header" v-if="props.type !== 'private'">
                    <span class="sender-name">{{ msg.sender?.userName }}</span>
                  </div>
                  <div class="message-text" @click="replyToMessage(msg)">{{ msg.content }}</div>
                  <!-- 回复消息显示 -->
                  <div v-if="msg.replyMessage" class="reply-content">
                    <span class="reply-text">
                      <template v-if="props.type !== 'private'">
                        回复 {{ msg.replyMessage.sender?.userName }}:
                      </template>
                      <template v-else>回复:</template>
                    </span>
                    {{ msg.replyMessage.content }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="chat-input">
          <!-- 显示当前回复的消息 -->
          <div v-if="replyTo" class="reply-preview">
            <span>回复 <span v-if="props.type !== 'private'">{{ replyTo.sender?.userName }}</span>: "{{ replyTo.content }}"</span>
            <a-button type="link" size="small" @click="cancelReply">取消回复</a-button>
          </div>

          <div class="input-area">
            <!-- 表情按钮 -->
            <SmileOutlined
              class="emoji-trigger"
              :class="{ active: showEmojiPicker }"
              @click="toggleEmojiPicker"
            />

            <a-input-group compact>
              <a-input
                v-model:value="inputMessage"
                placeholder="输入消息..."
                @keyup.enter.prevent="sendMessage"
                :disabled="!connected"
                style="width: calc(100% - 120px)"
                ref="messageInput"
                @blur="handleInputBlur"
              />
              <a-button
                type="primary"
                @click.prevent="sendMessage"
                :disabled="!connected"
              >
                发送
              </a-button>
            </a-input-group>
          </div>

          <!-- 表情选择器 -->
          <div v-if="showEmojiPicker" class="emoji-picker-container">
            <EmojiPicker @select="onEmojiSelect" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import ChatWebSocket from '@/utils/chatWebSocket'
import { getDefaultAvatar } from '@/utils/userUtils'

import type { ChatMessage } from '@/types/chat'
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps<{
  pictureId?: number | string
  spaceId?: number | string
  privateChatId?: number | string
  type?: 'chat' | 'space' | 'private'
}>()

const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)

const chatWs = ref<ChatWebSocket | null>(null)
const connected = ref(false)
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const messageInput = ref<HTMLElement | null>(null)

// 分页相关
const current = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const loadingHistory = ref(false)

// 回复相关
const replyTo = ref<ChatMessage | null>(null)

// 表情相关
const showEmojiPicker = ref(false)

// 添加是否在顶部的状态
const isAtTop = ref(false)

// 定义 emit
const emit = defineEmits<{
  (e: 'update:onlineUsers', users: any[]): void
  (e: 'update:onlineCount', count: number): void
  (e: 'message', msg: any): void
}>()

// 添加在线用户相关的状态
const onlineCount = ref(0)
const onlineUsers = ref<any[]>([])

const router = useRouter()

// 切换表情选择器显示状态
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

// 选择表情
const onEmojiSelect = (emoji: string) => {
  inputMessage.value += emoji
  showEmojiPicker.value = false
}

// 加载历史消息
const loadHistory = async () => {
  if (loadingHistory.value) return
  try {
    loadingHistory.value = true
    const params = {
      ...(props.pictureId ? { pictureId: String(props.pictureId) } : {}),
      ...(props.spaceId ? { spaceId: String(props.spaceId) } : {}),
      ...(props.privateChatId ? { privateChatId: String(props.privateChatId) } : {}),
      page: current.value
    }
    chatWs.value?.sendMessage({
      type: 'loadMore',
      ...params
    })
  } finally {
    loadingHistory.value = false
  }
}

// 设置要回复的消息
const replyToMessage = (msg: ChatMessage) => {
  if (msg && msg.id) {
    replyTo.value = {
      id: String(msg.id),
      content: msg.content,
      sender: {
        id: String(msg.sender?.id),
        userName: msg.sender?.userName || '',
        userAvatar: msg.sender?.userAvatar || getDefaultAvatar(msg.sender?.userName)
      }
    }
  }
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
}

// 初始化WebSocket连接
const initWebSocket = () => {
  if (!loginUser.value) {
    message.error('请先登录')
    return
  }

  chatWs.value = new ChatWebSocket({
    pictureId: props.pictureId,
    spaceId: props.spaceId,
    privateChatId: props.privateChatId,
    type: props.type || 'chat'
  })

  // console.log('chatWs.value:', chatWs.value)

  // 注册事件处理器
  chatWs.value.on('open', () => {
    connected.value = true
    // message.success('聊天室连接成功')
    // 连接成功后，后端会自动发送历史消息
  })

  chatWs.value.on('message', (msg) => {
    if (msg.type === 'history') {
      // 处理初始历史消息
      messages.value = msg.messages
        .filter(m => m.content?.trim())
        .map(m => ({
          ...m,
          sender: m.sender ? {
            ...m.sender,
            userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
          } : null
        }))
        .reverse()
      hasMore.value = msg.messages.length === pageSize.value
      current.value++
      loadingHistory.value = false
      scrollToBottom()
    } else if (msg.type === 'moreHistory') {
      // 处理加载更多的历史消息
      const oldScrollHeight = messageContainer.value?.scrollHeight || 0
      const oldScrollTop = messageContainer.value?.scrollTop || 0

      messages.value = [...msg.messages
        .filter(m => m.content?.trim())
        .map(m => ({
          ...m,
          sender: m.sender ? {
            ...m.sender,
            userAvatar: m.sender.userAvatar || getDefaultAvatar(m.sender.userName)
          } : null
        }))
        .reverse(), ...messages.value]
      hasMore.value = msg.hasMore
      current.value++
      loadingHistory.value = false

      // 恢复滚动位置
      nextTick(() => {
        if (messageContainer.value) {
          const newScrollHeight = messageContainer.value.scrollHeight
          const heightDiff = newScrollHeight - oldScrollHeight
          messageContainer.value.scrollTop = oldScrollTop + heightDiff
        }
      })
    } else if (msg.type === 'onlineUsers') {
      // 发送在线用户信息到父组件
      emit('message', msg)
    } else {
      // 处理普通消息
      if (!messages.value.some(m => m.id === msg.id) && msg.content?.trim()) {
        const newMessage = {
          ...msg,
          sender: msg.sender ? {
            ...msg.sender,
            userAvatar: msg.sender.userAvatar || getDefaultAvatar(msg.sender.userName)
          } : null,
          createTime: msg.createTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
        }
        messages.value.push(newMessage)
        scrollToBottom()
      }
    }
  })

  chatWs.value.on('close', () => {
    connected.value = false
    // message.warning('聊天室连接已断开')
  })

  chatWs.value.on('error', () => {
    connected.value = false
    // message.error('聊天室连接失败')
    loadingHistory.value = false
  })

  // 建立连接
  chatWs.value.connect()
}

// 处理输入框失焦
const handleInputBlur = (e: Event) => {
  // 防止输入框失去焦点
  const target = e.target as HTMLInputElement
  setTimeout(() => {
    target.focus()
  }, 100)
}

// 修改发送消息函数
const sendMessage = () => {
  if (!inputMessage.value.trim() || !chatWs.value || !connected.value) {
    return
  }

  const chatMessage = {
    type: props.type === 'private' ? 1 : (props.type === 'space' ? 3 : 2),
    content: inputMessage.value.trim(),
    ...(props.pictureId ? { pictureId: props.pictureId.toString() } : {}),
    ...(props.spaceId ? { spaceId: props.spaceId.toString() } : {}),
    ...(props.privateChatId ? { privateChatId: props.privateChatId.toString() } : {}),
    senderId: loginUser.value?.id.toString(),
    createTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    replyId: replyTo.value?.id ? replyTo.value.id.toString() : null,
    rootId: replyTo.value?.rootId ? replyTo.value.rootId.toString() :
      replyTo.value?.id ? replyTo.value.id.toString() : null
  }

  const success = chatWs.value.sendMessage(chatMessage)
  if (success) {
    inputMessage.value = ''
    replyTo.value = null
    // 保持输入框焦点
    if (messageInput.value) {
      (messageInput.value as HTMLInputElement).focus()
    }
  } else {
    message.error('发送失败，请重试')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    setTimeout(() => {
      messageContainer.value!.scrollTop = messageContainer.value!.scrollHeight
    }, 50)
  }
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) {
    return ''
  }
  const date = dayjs(time)
  return date.isValid() ? date.format('YY-MM-DD') : ''
}

// 修改滚动处理函数
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  // 判断是否在顶部，允许有1px的误差
  isAtTop.value = target.scrollTop <= 1

  // 当滚动到接近顶部时自动加载更多
  if (target.scrollTop <= 30 && !loadingHistory.value && hasMore.value) {
    loadHistory()
  }
}

// 添加判断是否显示时间分割线的方法
const shouldShowTimestamp = (currentMsg: any, prevMsg: any) => {
  if (!prevMsg) return true

  const currentTime = dayjs(currentMsg.createTime)
  const prevTime = dayjs(prevMsg.createTime)

  // 如果时间差超过5分钟，显示时间分割线
  return currentTime.diff(prevTime, 'minute') >= 5
}

// 添加格式化时间分割线文本的方法
const formatMessageDivider = (time: string) => {
  const msgTime = dayjs(time)
  const now = dayjs()

  // 如果是今天的消息
  if (msgTime.isSame(now, 'day')) {
    return msgTime.format('HH:mm')
  }

  // 如果是昨天的消息
  if (msgTime.isSame(now.subtract(1, 'day'), 'day')) {
    return '昨天 ' + msgTime.format('HH:mm')
  }

  // 如果是前天的消息
  if (msgTime.isSame(now.subtract(2, 'day'), 'day')) {
    return '前天 ' + msgTime.format('HH:mm')
  }

  // 如果是今年的消息
  if (msgTime.isSame(now, 'year')) {
    return msgTime.format('MM-DD HH:mm')
  }

  // 其他情况显示完整日期
  return msgTime.format('YYYY-MM-DD HH:mm')
}

// 处理头像点击
const handleAvatarClick = (user: any) => {
  if (!user?.id) return
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName),
      userAccount: user.userAccount,
      createTime: user.createTime
    }
  })
}

// 暴露方法给父组件
defineExpose({
  initWebSocket,
  disconnect: () => {
    if (chatWs.value) {
      chatWs.value.disconnect()
    }
  }
})

onMounted(() => {
  initWebSocket()
})

onUnmounted(() => {
  if (chatWs.value) {
    chatWs.value.disconnect()
  }
  emit('update:onlineCount', 0)
  emit('update:onlineUsers', [])
})
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;
  overflow: hidden;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.messages-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
  position: relative;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  position: relative;
  z-index: 0;
}

.message {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
  width: 100%;
}

.message-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

.message-content {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  position: relative;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
  width: calc(100% - 48px);
}

.message-header {
  display: flex;
  margin-bottom: 8px;
  align-items: center;
}

.sender-name {
  font-weight: 600;
  color: #1890ff;
  font-size: 14px;
}

.message-time {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
  padding: 0 12px;
}

.message-text {
  word-break: break-all;
  cursor: pointer;
  line-height: 1.5;
  color: #333;
  transition: background-color 0.2s ease;
}

.message-text:hover {
  background: #f0f7ff;
  border-radius: 4px;
  padding: 2px 4px;
  margin: -2px -4px;
}

.reply-content {
  margin-top: 12px;
  padding: 10px;
  background: rgba(247, 249, 252, 0.8);
  border-radius: 8px;
  font-size: 13px;
  border-left: 3px solid #ff8e53;
  transition: all 0.3s ease;
}

.reply-text {
  color: #1890ff;
  margin-right: 6px;
  font-weight: 500;
}

.chat-input {
  padding: 20px;
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  margin-top: 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.reply-preview {
  padding: 12px 16px;
  background: rgb(255, 143, 83);
  border-radius: 8px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  border-left: 3px solid #ff8e53;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 美化滚动条 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-room {
    margin: 0;
    border-radius: 16px 16px 0 0;
    height: 100%;
  }

  .messages-container {
    padding: 10px;
  }

  .chat-messages {
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 10px;
  }

  .chat-input {
    padding: 16px;
  }

  .message-content {
    width: calc(100% - 44px);
  }
}

.input-area {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.emoji-trigger {
  cursor: pointer;
  font-size: 20px;
  color: #94a3b8;
  transition: all 0.3s ease;
  padding: 8px;
}

.emoji-trigger:hover,
.emoji-trigger.active {
  color: #ff8e53;
  transform: scale(1.1);
}

.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  margin-bottom: 8px;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-actions {
  padding: 8px;
  display: flex;
  justify-content: center;
  position: relative;
  z-index: 1;
  background: transparent;
}

.load-more-btn {
  font-size: 13px;
  padding: 4px 12px;
  height: 28px;
  border-radius: 14px;
  transition: all 0.3s ease;
  color: #666;
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(4px);
  width: fit-content;
  margin: 0 auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.load-more-btn:hover {
  color: #1890ff;
  transform: translateY(-1px);
}

.load-more-btn:active {
  transform: translateY(0);
}

/* 修改输入框组样式 */
:deep(.ant-input-group) {
  display: flex;
  width: 100%;
}

:deep(.ant-input) {
  flex: 1;
  border-radius: 8px 0 0 8px !important;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;

  &:hover, &:focus {
    border-color: #ff8e53;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
  }
}

:deep(.ant-btn-primary) {
  border-radius: 0 8px 8px 0;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  height: 32px;
  min-width: 80px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.25);
  }

  &:active {
    transform: translateY(0);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .input-area {
    max-width: 100%;
  }
}

/* 自己发送的消息样式 */
.message-self {
  align-items: flex-end;
}

.message-self .message-wrapper {
  flex-direction: row-reverse;
  width: 100%;
}

.message-self .message-content {
  background: rgba(255, 142, 83, 0.1);
  border-color: rgba(255, 142, 83, 0.2);
  width: calc(100% - 48px);
}

.message-self .message-header {
  flex-direction: row-reverse;
}

.message-self .sender-name {
  color: #ff8e53;
}

.message-self .message-text {
  text-align: right;
}

.message-self .reply-content {
  text-align: right;
  border-left: none;
  border-right: 3px solid #ff8e53;
}

/* 添加时间分割线样式 */
.timestamp-divider {
  text-align: center;
  margin: 8px 24px;
  color: #999;
  font-size: 12px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0.6;
  max-width: 240px;
  margin-left: auto;
  margin-right: auto;
}

.timestamp-divider::before,
.timestamp-divider::after {
  content: '';
  flex: 1;
  height: 0.5px;
  background: rgba(0, 0, 0, 0.1);
  max-width: 32px;
}

/* 优化时间戳间距 */
.timestamp-divider + .message {
  margin-top: 4px;
}

.message + .timestamp-divider {
  margin-top: 12px;
}

/* 移除原有的消息时间样式 */
.message-time {
  display: none;
}

/* 优化头像点击样式 */
.clickable-avatar {
  cursor: pointer;
  transition: all 0.3s ease;
}

.clickable-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 顶部提示样式 */
.first-message-tip {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  border-radius: 20px;
  margin: 0 auto;
  width: fit-content;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
</style>
