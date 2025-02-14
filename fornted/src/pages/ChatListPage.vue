<template>
  <div id="chatListPage">
    <!-- 背景动画 -->
    <div class="background-animation" v-if="device !== DEVICE_TYPE_ENUM.PC">
      <div class="gradient-bg" style="background: linear-gradient(45deg, rgba(82, 196, 26, 0.03), rgba(115, 209, 61, 0.05))"></div>
      <div class="moving-waves"></div>
    </div>


    <div class="decorative-elements">
      <div class="floating-circle circle-1" style="background: rgba(82, 196, 26, 0.1)"></div>
      <div class="floating-circle circle-2" style="background: rgba(115, 209, 61, 0.08)"></div>
      <div class="floating-circle circle-3" style="background: rgba(135, 208, 104, 0.06)"></div>
    </div>

    <div class="main-content" :class="{ 'pc-main-content': device === DEVICE_TYPE_ENUM.PC }" style="z-index: 10;">
      <!-- 下拉刷新区域 -->
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh" v-if="device !== DEVICE_TYPE_ENUM.PC">
        <!-- 顶部导航栏和搜索框 -->
        <div id="chatListHeader">
          <!-- 标签栏 -->
          <div class="chat-tabs">
            <div
              class="tab-item"
              :class="{ active: activeTab === 'all' }"
              @click="handleTabChange('all')"
            >
              全部
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'friend' }"
              @click="handleTabChange('friend')"
            >
              好友
              <span v-if="friendUnreadCount > 0" class="tab-badge">{{ friendUnreadCount }}</span>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'private' }"
              @click="handleTabChange('private')"
            >
              私信
              <span v-if="privateUnreadCount > 0" class="tab-badge">{{ privateUnreadCount }}</span>
            </div>
          </div>
          <!-- 搜索栏 -->
          <div class="search-bar">
            <div class="search-wrapper">
              <a-input-search
                v-model:value="searchText"
                placeholder="搜索聊天记录"
                @search="handleSearch"
                :loading="loading"
                class="search-input"
              >
                <template #enterButton>
                  <SearchOutlined style="color: #ffffff" />
                </template>
              </a-input-search>
            </div>
          </div>
        </div>

        <!-- 聊天列表 -->
        <div id="chatListContent" :class="{ 'pc-chat-list': device === DEVICE_TYPE_ENUM.PC }">
          <!-- 空状态 -->
          <div v-if="!loading && filteredChatList.length === 0" class="empty-state">
            <lottie-player
              :src="getEmptyLottieUrl()"
              background="transparent"
              speed="1"
              style="width: 240px; height: 240px;"
              loop
              autoplay
            ></lottie-player>
            <div class="empty-text">
              <h3>{{ getEmptyText() }}</h3>
              <p v-if="searchText">换个关键词试试吧</p>
            </div>
          </div>

          <a-list v-else
                  :loading="loading"
                  :data-source="filteredChatList"
                  :split="true"
                  class="chat-list"
                  :style="{ height: 'calc(93vh - 120px)', overflowY: 'auto' }"
                  @scroll="handleListScroll"
          >
            <template #renderItem="{ item }">
              <a-list-item @click="handleChatClick(item)" :class="{ unread: getUnreadCount(item) > 0 }">
                <a-list-item-meta>
                  <template #avatar>
                    <div class="avatar-wrapper">
                      <a-avatar :src="item.targetUser?.userAvatar || getDefaultAvatar(item.targetUser?.userName)" />
                      <a-badge
                        :count="getUnreadCount(item)"
                        :numberStyle="{ backgroundColor: '#73d13d' }"
                      >
                        <span v-if="getUnreadCount(item) > 0" class="unread-badge">
                          {{ getUnreadCount(item) > 99 ? '99+' : getUnreadCount(item) }}
                        </span>
                      </a-badge>
                    </div>
                  </template>
                  <template #title>
                    <span class="chat-title">
                      <!-- PC端使用tooltip，移动端使用点击事件 -->
                      <a-tooltip
                        v-if="!isMobile"
                        :title="item.isSender ? item.userChatName : item.targetUserChatName"
                        :mouseEnterDelay="0.5"
                        placement="top"
                      >
                        <span class="username-text">{{ item.isSender ? item.userChatName : item.targetUserChatName }}</span>
                      </a-tooltip>
                      <!-- 移动端点击显示 -->
                      <span
                        v-else
                        class="username-text"
                        @click.stop="handleNameClick(item, item.isSender ? item.userChatName : item.targetUserChatName)"
                      >
                        {{ item.isSender ? item.userChatName : item.targetUserChatName }}
                      </span>
                      <span
                        :class="{
                          'friend-tag': item.chatType === 1,
                          'private-tag': item.chatType === 0
                        }"
                      >
                        {{ item.chatType === 1 ? '好友' : '私信' }}
                      </span>
                      <span v-if="item.isSender" class="sender-tag">发起者</span>
                    </span>
                  </template>
                  <template #description>
                    <div class="chat-desc">
                      <span class="last-message">{{ item.lastMessage || '暂无消息' }}</span>
                      <span class="message-time">{{ formatMessageTime(item.lastMessageTime) }}</span>
                    </div>
                  </template>
                </a-list-item-meta>
                <!-- 添加更多操作按钮 -->
                <template #extra>
                  <a-dropdown
                    placement="bottomRight"
                    trigger="click"
                    @click.stop
                  >
                    <a-button
                      type="text"
                      class="more-btn"
                      @click.stop
                    >
                      <EllipsisOutlined />
                    </a-button>
                    <template #overlay>
                      <div class="action-menu">
                        <div
                          class="action-item"
                          @click.stop="showEditNameModal(item)"
                        >
                          <EditOutlined class="action-icon" />
                          <span>修改聊天名称</span>
                        </div>
                        <div
                          class="action-item danger"
                          @click.stop="showDeleteConfirm(item)"
                        >
                          <DeleteOutlined class="action-icon" />
                          <span>删除聊天</span>
                        </div>
                      </div>
                    </template>
                  </a-dropdown>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </van-pull-refresh>

      <!-- PC端内容 -->
      <template v-else>
        <div id="chatListHeader">
          <!-- 标签栏 -->
          <div class="chat-tabs">
            <div
              class="tab-item"
              :class="{ active: activeTab === 'all' }"
              @click="handleTabChange('all')"
            >
              全部
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'friend' }"
              @click="handleTabChange('friend')"
            >
              好友
              <span v-if="friendUnreadCount > 0" class="tab-badge">{{ friendUnreadCount }}</span>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'private' }"
              @click="handleTabChange('private')"
            >
              私信
              <span v-if="privateUnreadCount > 0" class="tab-badge">{{ privateUnreadCount }}</span>
            </div>
          </div>
          <!-- 搜索栏 -->
          <div class="search-bar">
            <div class="search-wrapper">
              <a-input-search
                v-model:value="searchText"
                placeholder="搜索聊天记录"
                @search="handleSearch"
                :loading="loading"
                class="search-input"
              >
                <template #enterButton>
                  <SearchOutlined style="color: #ffffff" />
                </template>
              </a-input-search>
            </div>
          </div>
        </div>

        <div id="chatListContent" class="pc-chat-list">
          <!-- 空状态 -->
          <div v-if="!loading && filteredChatList.length === 0" class="empty-state">
            <lottie-player
              :src="getEmptyLottieUrl()"
              background="transparent"
              speed="1"
              style="width: 240px; height: 240px;"
              loop
              autoplay
            ></lottie-player>
            <div class="empty-text">
              <h3>{{ getEmptyText() }}</h3>
              <p v-if="searchText">换个关键词试试吧</p>
            </div>
          </div>

          <!-- 聊天列表 -->
          <a-list v-else
                  :loading="loading"
                  :data-source="filteredChatList"
                  :split="true"
                  class="chat-list"
          >
            <template #renderItem="{ item }">
              <a-list-item @click="handleChatClick(item)" :class="{ unread: getUnreadCount(item) > 0 }">
                <a-list-item-meta>
                  <template #avatar>
                    <div class="avatar-wrapper">
                      <a-avatar :src="item.targetUser?.userAvatar || getDefaultAvatar(item.targetUser?.userName)" />
                      <a-badge
                        :count="getUnreadCount(item)"
                        :numberStyle="{ backgroundColor: '#73d13d' }"
                      >
                        <span v-if="getUnreadCount(item) > 0" class="unread-badge">
                          {{ getUnreadCount(item) > 99 ? '99+' : getUnreadCount(item) }}
                        </span>
                      </a-badge>
                    </div>
                  </template>
                  <template #title>
                    <span class="chat-title">
                      <!-- PC端使用tooltip，移动端使用点击事件 -->
                      <a-tooltip
                        v-if="!isMobile"
                        :title="item.isSender ? item.userChatName : item.targetUserChatName"
                        :mouseEnterDelay="0.5"
                        placement="top"
                      >
                        <span class="username-text">{{ item.isSender ? item.userChatName : item.targetUserChatName }}</span>
                      </a-tooltip>
                      <!-- 移动端点击显示 -->
                      <span
                        v-else
                        class="username-text"
                        @click.stop="handleNameClick(item, item.isSender ? item.userChatName : item.targetUserChatName)"
                      >
                        {{ item.isSender ? item.userChatName : item.targetUserChatName }}
                      </span>
                      <span
                        :class="{
                          'friend-tag': item.chatType === 1,
                          'private-tag': item.chatType === 0
                        }"
                      >
                        {{ item.chatType === 1 ? '好友' : '私信' }}
                      </span>
                      <span v-if="item.isSender" class="sender-tag">发起者</span>
                    </span>
                  </template>
                  <template #description>
                    <div class="chat-desc">
                      <span class="last-message">{{ item.lastMessage || '暂无消息' }}</span>
                      <span class="message-time">{{ formatMessageTime(item.lastMessageTime) }}</span>
                    </div>
                  </template>
                </a-list-item-meta>
                <!-- 添加更多操作按钮 -->
                <template #extra>
                  <a-dropdown
                    placement="bottomRight"
                    trigger="click"
                    @click.stop
                  >
                    <a-button
                      type="text"
                      class="more-btn"
                      @click.stop
                    >
                      <EllipsisOutlined />
                    </a-button>
                    <template #overlay>
                      <div class="action-menu">
                        <div
                          class="action-item"
                          @click.stop="showEditNameModal(item)"
                        >
                          <EditOutlined class="action-icon" />
                          <span>修改聊天名称</span>
                        </div>
                        <div
                          class="action-item danger"
                          @click.stop="showDeleteConfirm(item)"
                        >
                          <DeleteOutlined class="action-icon" />
                          <span>删除聊天</span>
                        </div>
                      </div>
                    </template>
                  </a-dropdown>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </div>

        <!-- PC端分页器 -->
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="searchParams.current"
            v-model:pageSize="searchParams.pageSize"
            :total="total"
            :page-size-options="['4', '12', '20', '30']"
            :show-total="(total) => `共 ${total} 条`"
            @change="handlePageChange"
            show-size-changer
            show-quick-jumper
            class="custom-pagination"
          />
        </div>
      </template>
    </div>

    <!-- 删除确认弹框 -->
    <a-modal
      v-model:open="deleteConfirmVisible"
      :title="null"
      :footer="null"
      :width="400"
      class="delete-confirm-modal"
    >
      <div class="delete-confirm-content">
        <div class="warning-icon">
          <ExclamationCircleFilled style="color: #ff6b6b;" />
        </div>
        <h3 class="confirm-title">确认删除该聊天？</h3>
        <p class="confirm-desc">
          用户名称：{{ selectedChat?.targetUser?.userName || '未设置' }}<br>
          聊天类型：{{ selectedChat?.chatType === 1 ? '好友' : '私信' }}
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">
            确认删除
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 修改名称弹窗 -->
    <a-modal
      v-model:open="editNameVisible"
      title="修改聊天名称"
      @ok="handleEditNameConfirm"
      :confirmLoading="editNameLoading"
      :class="'edit-name-modal'"
      :maskStyle="{ backgroundColor: 'rgba(0, 0, 0, 0.45)' }"
      :zIndex="1001"
    >
      <div class="edit-name-content">
        <div class="edit-icon">
          <EditOutlined style="color: #ff8e53" />
        </div>
        <a-input
          v-model:value="newChatName"
          placeholder="请输入新的聊天名称"
          :maxLength="50"
          show-count
          class="edit-input"
        />
      </div>
      <template #footer>
        <div class="edit-actions">
          <a-button class="cancel-btn" @click="editNameVisible = false">取消</a-button>
          <a-button
            class="confirm-btn"
            :loading="editNameLoading"
            @click="handleEditNameConfirm"
          >
            确认
          </a-button>
        </div>
      </template>
    </a-modal>

    <!-- 底部加载状态 -->
    <div v-if="loading" class="loading-more">
      <a-spin size="small" />
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, notification } from 'ant-design-vue'
import { SearchOutlined, EllipsisOutlined, DeleteOutlined, ExclamationCircleOutlined, ExclamationCircleFilled, EditOutlined } from '@ant-design/icons-vue'
import { listPrivateChatByPageUsingPost, deletePrivateChatUsingPost, updateChatNameUsingPost } from '@/api/privateChatController'
import { formatMessageTime } from "@/utils/dateUtils"
import { getDefaultAvatar } from '@/utils/userUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import PrivateChat = API.PrivateChat
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { getDeviceType } from '@/utils/device.ts'
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})
const loginUserStore = useLoginUserStore()
const router = useRouter()
const loading = ref(false)
const chatList = ref<PrivateChat[]>([])
const searchText = ref('')
const current = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const activeTab = ref('all')  // 当前选中的标签
const friendUnreadCount = ref(0)  // 好友未读消息总数
const privateUnreadCount = ref(0)  // 私信未读消息总数
const refreshing = ref(false)
const total = ref(0)

// 删除确认相关的状态
const deleteConfirmVisible = ref(false)
const selectedChat = ref<PrivateChat | null>(null)

// 修改名称相关
const editNameVisible = ref(false)
const editNameLoading = ref(false)
const newChatName = ref('')

// 判断是否为移动端
const isMobile = computed(() => {
  return window.innerWidth <= 768
})

// 获取未读消息数
const getUnreadCount = (chat: PrivateChat) => {
  // 如果当前登录用户是发起者，返回userUnreadCount
  if (chat.userId === loginUserStore.loginUser?.id) {
    return chat.userUnreadCount || 0
  }
  // 如果当前登录用户是目标用户，返回targetUserUnreadCount
  return chat.userUnreadCount || 0
}

// 根据当前标签过滤聊天列表
const filteredChatList = computed(() => {
  if (activeTab.value === 'all') return chatList.value
  return chatList.value.filter(chat =>
    activeTab.value === 'friend' ? chat.chatType === 1 : chat.chatType === 0
  )
})

// 获取空状态文本
const getEmptyText = () => {
  if (loading.value) return '加载中...'
  if (searchText.value) return '没有找到相关聊天'
  switch (activeTab.value) {
    case 'friend':
      return '暂无好友聊天'
    case 'private':
      return '暂无私信消息'
    default:
      return '暂无聊天记录'
  }
}

// 获取空状态动画URL
const getEmptyLottieUrl = () => {
  if (loading.value) {
    return 'https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json'  // 加载动画
  }
  if (searchText.value) {
    return 'https://assets9.lottiefiles.com/packages/lf20_swnrn2oy.json'  // 搜索空结果动画
  }
  switch (activeTab.value) {
    case 'friend':
      return 'https://assets3.lottiefiles.com/packages/lf20_hy4txm7l.json'  // 好友相关动画
    case 'private':
      return 'https://assets1.lottiefiles.com/packages/lf20_zw7jo1.json'  // 消息相关动画
    default:
      return 'https://assets3.lottiefiles.com/private_files/lf30_bn5winlb.json'  // 默认动画
  }
}

// 处理标签切换
const handleTabChange = (tab: string) => {
  activeTab.value = tab
  fetchChatList(true)
}

// 计算未读消息数
const calculateUnreadCounts = () => {
  friendUnreadCount.value = chatList.value
    .filter(chat => chat.chatType === 1)
    .reduce((sum, chat) => sum + (getUnreadCount(chat) || 0), 0)

  privateUnreadCount.value = chatList.value
    .filter(chat => chat.chatType === 0)
    .reduce((sum, chat) => sum + (getUnreadCount(chat) || 0), 0)
}

// 监听聊天列表变化，重新计算未读数
watch(() => chatList.value, calculateUnreadCounts, { deep: true })

// 修改搜索参数的定义
const searchParams = reactive({
  current: 1,
  pageSize: 7,
  searchText: '',
  chatType: undefined
})

// 修改分页处理方法
const handlePageChange = (page: number, size: number) => {
  searchParams.current = page
  searchParams.pageSize = size
  fetchChatList(true)
}

// 修改获取聊天列表的方法
const fetchChatList = async (isRefresh = false) => {
  if (loading.value) return
  try {
    loading.value = true
    if (isRefresh) {
      chatList.value = []
    }
    const res = await listPrivateChatByPageUsingPost({
      searchText: searchText.value,
      current: searchParams.current,
      pageSize: searchParams.pageSize,
      chatType: activeTab.value === 'all' ? undefined : activeTab.value === 'friend' ? 1 : 0
    })
    if (res.data.code === 0) {
      const { records, total: totalCount } = res.data.data
      chatList.value = records
      total.value = totalCount
    } else {
      message.error('获取聊天列表失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取聊天列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  fetchChatList(true)
}

// 点击聊天项
const handleChatClick = (chat: PrivateChat) => {
  if (chat.targetUser) {
    router.push({
      path: `/chat/${chat.targetUser.id}`,
      query: {
        privateChatId: chat.id,
        userName: chat.isSender ? chat.userChatName : chat.targetUserChatName,
        userAvatar: chat.targetUser.userAvatar,
        userAccount: chat.targetUser.userAccount,
        createTime: chat.targetUser.createTime,
        isSender: chat.isSender.toString()
      }
    })
  }
}

// 下拉刷新处理
const onRefresh = async () => {
  try {
    await fetchChatList(true)
  } finally {
    refreshing.value = false
  }
}

// 显示删除确认框
const showDeleteConfirm = (chat: PrivateChat) => {
  selectedChat.value = chat
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedChat.value?.id) return

  try {
    const res = await deletePrivateChatUsingPost({
      privateChatId: selectedChat.value.id
    })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 从列表中移除
      chatList.value = chatList.value.filter(item => item.id !== selectedChat.value?.id)
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
  }
}

// 显示修改名称弹窗
const showEditNameModal = (chat: PrivateChat) => {
  selectedChat.value = chat
  newChatName.value = chat.userChatName || ''
  editNameVisible.value = true
}

// 确认修改名称
const handleEditNameConfirm = async () => {
  if (!selectedChat.value?.id || !newChatName.value.trim()) return

  try {
    editNameLoading.value = true
    const res = await updateChatNameUsingPost({
      privateChatId: selectedChat.value.id,
      chatName: newChatName.value.trim()
    })

    if (res.data.code === 0) {
      message.success('修改成功')
      // 更新本地数据
      const chat = chatList.value.find(item => item.id === selectedChat.value?.id)
      if (chat) {
        chat.userChatName = newChatName.value.trim()
      }
      editNameVisible.value = false
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('修改失败：' + error.message)
  } finally {
    editNameLoading.value = false
  }
}

// 处理名称点击
const handleNameClick = (item: PrivateChat, name: string) => {
  if (name.length > 8) {
    showMobileNameToast(name)
  } else {
    handleChatClick(item)
  }
}

// 移动端显示完整用户名
const showMobileNameToast = (name: string) => {
  // 判断名称长度,小于8个字符直接返回不显示弹框
  if(name.length <= 8) return;

  notification.info({
    message: undefined,
    description: name,
    style: {
      borderRadius: '8px',
      backgroundColor: 'rgba(0, 0, 0, 0.75)',
      color: '#fff',
      padding: '12px 16px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    }
  })
}

// 处理列表滚动
const handleListScroll = () => {}

onMounted(() => {
  fetchChatList(true)
})
</script>

<style scoped>
#chatListPage {
  min-height: 93vh;
  position: relative;
  overflow: hidden;
  margin-left: -20px!important;
  margin-right: -20px !important;
  margin-top: -40px !important;
  border-radius: 24px !important;
}

/* 背景动画 */
.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
}

.moving-waves {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.15) 25%, transparent 25%),
  linear-gradient(-45deg, rgba(255, 255, 255, 0.15) 25%, transparent 25%);
  background-size: 60px 60px;
  animation: waveMove 20s linear infinite;
}

/* 装饰元素 */
.decorative-elements {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.floating-circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  animation: float 20s infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: -5s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  top: 40%;
  right: -200px;
  animation-delay: -10s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: 30%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(10px, 10px) rotate(5deg);
  }
  50% {
    transform: translate(0, 20px) rotate(0deg);
  }
  75% {
    transform: translate(-10px, 10px) rotate(-5deg);
  }
}

@keyframes waveMove {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 60px 60px;
  }
}

.main-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}
:deep(.ant-btn-primary) {
  background: #52c41a !important;
  border-color: #52c41a !important;
  height: 30px !important;
  width: 50px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  border-radius: 0 20px 20px 0 !important;
  transition: all 0.3s ease !important;

  &:hover {
    background: #52c41a !important;
    border-color: #52c41a !important;
  }

  &:active {
    transform: scale(0.98);
  }
}
#chatListHeader {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;

  .chat-tabs {
    display: flex;
    gap: 12px;
    margin-top: 6px;
    justify-content: center;
  }

  .tab-item {
    position: relative;
    padding: 8px 32px;
    border-radius: 20px;
    font-size: 14px;
    color: #64748b;
    cursor: pointer;
    transition: all 0.3s ease;
    background: rgba(255, 255, 255, 0.5);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    gap: 6px;

    &:hover {
      background: rgba(255, 255, 255, 0.8);
      color: #1a1a1a;
    }

    &.active {
      background: linear-gradient(135deg, #95de64 0%, #73d13d 100%);
      color: white;
      font-weight: 500;
      box-shadow: 0 2px 8px rgba(115, 209, 61, 0.15);
    }
  }

  .tab-badge {
    min-width: 18px;
    height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    background: #73d13d;
    color: white;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: normal;
  }

  .search-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: -12px;
  }

  .search-wrapper {
    flex: 1;
    max-width: 800px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    margin: 4px 0 8px 0;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    padding: 2px;
    transform-style: preserve-3d;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }

    :deep(.ant-input-search) {
      .ant-input {
        background: rgba(82, 196, 26, 0.05);
      }

      .ant-input-group-addon {
        .ant-btn {
          background: linear-gradient(135deg, #95de64 0%, #73d13d 100%);
          border: none;

          &:hover {
            background: linear-gradient(135deg, #73d13d 0%, #52c41a 100%);
          }
        }
      }
    }
  }

  .search-input {
    :deep(.ant-input) {
      background: transparent;
      border-radius: 20px;
      padding-left: 16px;
      border: none;
      box-shadow: none;
      height: 40px;
      font-size: 14px;

      &:focus {
        background: rgba(255, 255, 255, 0.9);
      }
    }

    :deep(.ant-input-group) {
      display: flex !important;
      background: transparent;
      border: none;
      border-radius: 20px;
      overflow: hidden;
    }
  }
}

#chatListContent {
  .chat-list {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    padding: 4px;
    transform-style: preserve-3d;
    transition: all 0.3s ease;
    position: relative;
    z-index: 2;
    scrollbar-width: thin;
    scrollbar-color: #e5e7eb transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #e5e7eb;
      border-radius: 3px;

      &:hover {
        background-color: #d1d5db;
      }
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }
  }

  .chat-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #333;

    .username-text {
      max-width: 8em;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      line-height: 1.5;
      padding: 2px 0;
    }

    .friend-tag,
    .private-tag,
    .sender-tag {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: normal;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      backdrop-filter: blur(4px);
    }

    .friend-tag {
      color: #52c41a;
      background: linear-gradient(135deg, rgba(82, 196, 26, 0.1), rgba(115, 209, 61, 0.15));
    }

    .private-tag {
      color: #1890ff;
      background: linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(54, 207, 201, 0.15));
    }

    .sender-tag {
      color: #722ed1;
      background: linear-gradient(135deg, rgba(114, 46, 209, 0.1), rgba(173, 55, 255, 0.15));
    }
  }

  .avatar-wrapper {
    position: relative;

    :deep(.ant-avatar) {
      border: 2px solid rgba(255, 255, 255, 0.8);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
    }
  }

  .chat-desc {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #666;
    font-size: 13px;
    margin-top: 4px;

    .last-message {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 12px;
    }

    .message-time {
      font-size: 12px;
      color: #94a3b8;
      background: rgba(148, 163, 184, 0.1);
      padding: 2px 6px;
      border-radius: 10px;
    }
  }

  :deep(.ant-list-item) {
    cursor: pointer;
    transition: all 0.3s;
    padding: 12px 16px;
    border-radius: 8px;
    margin: 2px 0;
    border: none !important;
    background: transparent;

    &:hover {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.9));
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    &.unread {
      background: rgba(82, 196, 26, 0.05);

      .last-message {
        color: #52c41a;
        font-weight: 500;
      }
    }
  }
}

#chatListLoadMore {
  text-align: center;
  margin-top: 20px;

  :deep(.ant-btn-link) {
    color: #666;
    font-size: 14px;

    &:hover {
      color: #ff8e53;
    }
  }
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  }
}

.empty-text {
  display: flex;
  flex-direction: column;
  gap: 8px;

  h3 {
    margin: 0;
    font-size: 18px;
    color: #333;
    font-weight: 600;
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #666;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  #chatListPage {
    margin: 0;
    border-radius: 0;
  }

  .background-animation {
    display: none;
  }

  .main-content {
    padding: 0 12px;
  }

  #chatListContent {
    .chat-list {
      background: #fff;
      box-shadow: none;
      backdrop-filter: none;
      transform: none !important;
      border-radius: 12px;
    }
  }

  #chatListHeader {
    .search-wrapper {
      margin: 8px 0;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
  }

  .more-btn {
    opacity: 1;
    width: 40px;
    height: 40px;
    right: 8px;

    .anticon {
      font-size: 24px;
    }
  }

  .action-menu {
    padding: 8px;
    z-index: 0;
  }

  .action-item {
    padding: 12px 16px;

    .action-icon {
      font-size: 20px;
    }
  }

  .empty-state {
    background: #fff;
    box-shadow: none;
    backdrop-filter: none;
    border-radius: 12px;
    padding: 32px 16px;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

/* 下拉刷新样式定制 */
:deep(.van-pull-refresh) {
  overflow: visible;
}

:deep(.van-pull-refresh__track) {
  overflow: visible;
}

:deep(.van-pull-refresh__head) {
  color: #ff8e53;
}

.more-btn {
  width: 32px;
  height: 32px;
  padding-bottom: 20px !important;
  border-radius: 16px;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  padding: 0;

  &:hover {
    color: #ff8e53;
    background: transparent;
  }

  .anticon {
    font-size: 20px;
  }
}

/* PC端样式 */
@media screen and (min-width: 769px) {
  .more-btn {
    opacity: 0;
  }

  :deep(.ant-list-item) {
    &:hover {
      .more-btn {
        opacity: 1;
      }
    }
  }
}

.action-menu {
  background: white;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  min-width: 120px;
}

.action-item {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  color: #666;

  &:hover {
    background: rgba(0, 0, 0, 0.02);
  }

  &.danger {
    color: #ff4d4f;

    &:hover {
      background: rgba(255, 77, 79, 0.1);
    }
  }

  .action-icon {
    font-size: 16px;
  }
}

:deep(.ant-list-item) {
  position: relative;

  &:hover {
    .more-btn {
      opacity: 1;
    }
  }
}

/* 删除确认弹框样式 */
:deep(.delete-confirm-modal) {
  .ant-modal-content {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-body {
    padding: 0;
  }
}

.delete-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  margin-bottom: 16px;

  .anticon {
    animation: pulse 2s infinite;
  }
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.confirm-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    color: #1a1a1a;
    border-color: #94a3b8;
    background: #f8fafc;
  }
}

.confirm-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  background: #ff6b6b;
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background: #ff5252;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .delete-confirm-content {
    padding: 24px 16px;
  }

  .warning-icon {
    font-size: 40px;
  }

  .confirm-title {
    font-size: 16px;
  }

  .confirm-desc {
    font-size: 13px;
  }

  .confirm-actions {
    gap: 8px;
  }

  .cancel-button,
  .confirm-button {
    min-width: 90px;
    height: 36px;
    font-size: 13px;
  }
}

/* 修改名称弹框样式 */
:deep(.edit-name-modal) {
  .ant-modal-content {
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-header {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;

    .ant-modal-title {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }
}

.edit-name-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.edit-icon {
  font-size: 32px;
  width: 64px;
  height: 64px;
  border-radius: 32px;
  background: rgba(255, 142, 83, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-input {
  width: 100%;

  :deep(.ant-input) {
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 14px;
    border-color: #e5e7eb;
    transition: all 0.3s ease;

    &:hover, &:focus {
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
    }
  }

  :deep(.ant-input-show-count-suffix) {
    color: #a3a3a3;
  }
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .cancel-btn {
    min-width: 80px;
    height: 36px;
    border-radius: 18px;
    border: 1px solid #e5e7eb;
    color: #64748b;
    transition: all 0.3s ease;

    &:hover {
      color: #1a1a1a;
      border-color: #94a3b8;
      background: #f8fafc;
    }
  }

  .confirm-btn {
    min-width: 80px;
    height: 36px;
    border-radius: 18px;
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    border: none;
    color: white;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(82, 196, 26, 0.2);
    }

    &:active {
      transform: translateY(1px);
    }
  }
}

/* 下拉菜单样式 */
:deep(.ant-dropdown) {
  z-index: 1000 !important; /* 确保低于弹框的1001 */
}

/* Tooltip 样式 */
:deep(.ant-tooltip) {
  .ant-tooltip-inner {
    background-color: rgba(0, 0, 0, 0.75);
    border-radius: 4px;
    padding: 6px 12px;
    font-size: 14px;
    line-height: 1.4;
    max-width: 300px;
    word-break: break-all;
  }

  .ant-tooltip-arrow-content {
    background-color: rgba(0, 0, 0, 0.75);
  }
}

.username-text {
  cursor: pointer;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: #666;
  font-size: 14px;
}

/* 优化通知弹框样式 */
:deep(.ant-notification-notice) {
  padding: 0;
  min-height: 40px;

  .ant-notification-notice-message {
    display: none;
  }

  .ant-notification-notice-description {
    margin: 0;
    font-size: 14px;
    line-height: 1.4;
  }

  .ant-notification-notice-close {
    top: 50%;
    transform: translateY(-50%);
    color: rgba(255,255,255,0.6);

    &:hover {
      color: #fff;
    }
  }
}

/* PC端特定样式 */
.pc-main-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 80px;
}

.pc-chat-list {
  height: calc(100vh - 320px) !important;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto !important;
}

/* 调整列表项样式 */
:deep(.ant-list-item) {
  padding: 16px !important;
}

/* 美化滚动条 */
.pc-chat-list::-webkit-scrollbar {
  width: 6px;
}

.pc-chat-list::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 3px;
}

.pc-chat-list::-webkit-scrollbar-track {
  background-color: transparent;
}

/* PC端分页器样式 */
.pagination-wrapper {
  margin-top: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: flex-end;
}

:deep(.custom-pagination) {
  .ant-pagination-total-text {
    color: #64748b;
    margin-right: 12px;
  }

  .ant-pagination-prev,
  .ant-pagination-next,
  .ant-pagination-item,
  .ant-pagination-jump-prev,
  .ant-pagination-jump-next {
    border-radius: 8px;
    transition: all 0.3s ease;
    margin-right: 8px;

    &:hover {
      border-color: #73d13d;
      a {
        color: #73d13d;
      }
    }
  }

  .ant-pagination-item-active {
    background: linear-gradient(135deg, #95de64 0%, #73d13d 100%);
    border: none;
    box-shadow: 0 2px 8px rgba(115, 209, 61, 0.2);

    a {
      color: white !important;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(115, 209, 61, 0.3);
    }
  }

  .ant-pagination-options {
    .ant-select {
      .ant-select-selector {
        border-radius: 8px !important;
        transition: all 0.3s ease;
        height: 32px !important;
        padding: 0 11px !important;

        .ant-select-selection-item {
          line-height: 30px !important;
          color: #64748b;
        }

        &:hover {
          border-color: #73d13d !important;
          background: #f6ffed;
        }
      }

      &.ant-select-focused .ant-select-selector {
        border-color: #73d13d !important;
        box-shadow: 0 0 0 2px rgba(115, 209, 61, 0.1) !important;
      }
    }

    .ant-pagination-options-quick-jumper {
      color: #64748b;
      margin-left: 16px;

      input {
        border-radius: 8px;
        transition: all 0.3s ease;
        height: 32px;
        width: 50px;
        text-align: center;
        margin: 0 8px;

        &:hover {
          border-color: #73d13d;
          background: #f6ffed;
        }

        &:focus {
          border-color: #73d13d;
          box-shadow: 0 0 0 2px rgba(115, 209, 61, 0.1);
        }
      }
    }
  }

  /* 下拉菜单样式 */
  :deep(.ant-select-dropdown) {
    border-radius: 8px;
    overflow: hidden;
    padding: 4px;

    .ant-select-item {
      transition: all 0.3s ease;
      padding: 8px 12px;
      border-radius: 6px;
      margin: 2px 0;

      &:hover {
        background: #f6ffed;
        color: #73d13d;
      }

      &-option-selected {
        background: #f6ffed !important;
        color: #73d13d !important;
        font-weight: 500;
      }

      &-option-active {
        background: #f6ffed !important;
        color: #73d13d !important;
      }
    }
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .pc-main-content {
    margin-bottom: 0;
  }
}
</style>
