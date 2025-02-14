<template>
  <div id="pictureDetailPage" v-show="mounted" :class="{ mounted }">
    <!-- 图片已删除状态 -->
    <div v-if="isDeleted" class="deleted-state">
      <div class="deleted-content">
        <DeleteOutlined class="deleted-icon" />
        <h2>图片已删除</h2>
        <p>该图片可能已被作者删除或管理员删除</p>
        <a-button type="primary" @click="router.back()" class="back-button">
          返回上一页
        </a-button>
      </div>
    </div>

    <!-- 正常显示图片内容 -->
    <template v-else>
      <a-row :gutter="[16, 16]">
        <!-- 图片预览 -->
        <a-col :sm="24" :md="16" :xl="18" class="preview-col">
          <a-card class="preview-card" :bordered="false">
            <template #title> </template>
            <div class="image-container" :class="{ loaded: pictureLoaded }">
              <template v-if="pictureLoaded">
                <a-image :src="picture.url" class="main-image" />
              </template>
              <template v-else>
                <div class="loading-container">
                  <div class="loading-content">
                    <div class="loading-spinner">
                      <a-spin size="large">
                        <template #indicator>
                          <LoadingOutlined :style="{ fontSize: '24px', color: '#ff8e53' }" spin />
                        </template>
                      </a-spin>
                    </div>
                    <div class="loading-text">
                      <div class="primary-text">图片加载中...</div>
                      <div class="secondary-text">请稍候片刻</div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </a-card>
        </a-col>


        <!-- 图片信息区域 -->
        <a-col :sm="24" :md="8" :xl="6" class="info-col">
          <a-card class="info-card" :bordered="false">
            <!-- 信息描述列表 -->
            <!-- 添加聊天室 -->
            <div class="chat-section" v-if="showChatRoom">
              <a-button
                type="primary"
                class="chat-button"
                @click="openChatModal"
              >
                <template #icon><MessageOutlined /></template>
                <span class="chat-button-text">
              聊天室
              <a-avatar-group
                :maxCount="5"
                size="small"
                class="online-avatars"
              >
                <a-tooltip
                  v-for="user in onlineUsers"
                  :key="user.id"
                  :title="user.userName"
                >
                  <a-avatar
                    :src="user.userAvatar || getDefaultAvatar(user.userName)"
                  />
                </a-tooltip>
              </a-avatar-group>
              <span class="online-count">({{ onlineCount }}人在线)</span>
            </span>
              </a-button>
            </div>
            <a-descriptions :column="1" class="info-descriptions">
              <a-descriptions-item label="作者" class="author-item" >
                <a-space>
                  <a-avatar
                    :size="28"
                    @click="handleUserClick(picture.user)"
                    :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                  />
                  <div class="author-name">{{ picture.user?.userName }}</div>
                  <a-button
                    v-if="picture.user?.id !== loginUserStore.loginUser?.id"
                    :type="isFollowed ? 'default' : 'primary'"
                    size="small"
                    class="follow-button"
                    @click="handleFollow"
                    :loading="followLoading"
                  >
                    {{ isFollowed ? '已关注' : '关注' }}
                  </a-button>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item label="名称">
                {{ picture.name ?? '未命名' }}
              </a-descriptions-item>
              <a-descriptions-item label="简介">
                {{ picture.introduction ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="分类" v-if="canEdit">
                {{ picture.category ?? '默认' }}
              </a-descriptions-item>
              <a-descriptions-item label="标签" v-if="canEdit">
                <a-tag v-for="tag in picture.tags" :key="tag">
                  {{ tag }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="格式" v-if="device === DEVICE_TYPE_ENUM.PC">
                {{ picture.picFormat ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="宽度" v-if="device === DEVICE_TYPE_ENUM.PC">
                {{ picture.picWidth ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="高度" v-if="device === DEVICE_TYPE_ENUM.PC">
                {{ picture.picHeight ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="宽高比">
                {{ picture.picScale ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="大小">
                {{ formatSize(picture.picSize) }}
              </a-descriptions-item>
              <a-descriptions-item label="主色调">
                <a-space>
                  <div
                    v-if="picture.picColor"
                    :style="{
                    width: '66px',
                    height: '24px',
                    backgroundColor: toHexColor(picture.picColor),
                  }"
                  />
                  <div v-else>-</div>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item v-if="device !== DEVICE_TYPE_ENUM.PC">
                <div class="mobile-actions">
                  <a-space align="start">
                    <a-button type="primary" @click="doDownload" class="action-btn download-btn">
                      <DownloadOutlined />
                    </a-button>
                    <a-button
                      type="primary"
                      @click="doShare"
                      v-if="showShareButton"
                      class="action-btn share-btn"
                    >
                      <ShareAltOutlined />
                    </a-button>
                    <a-button
                      type="primary"
                      @click="doEdit"
                      v-if="canEdit"
                      class="action-btn edit-btn"
                    >
                      <EditOutlined />
                    </a-button>
                    <a-button
                      type="primary"
                      @click="showDeleteConfirm"
                      v-if="canDelete"
                      class="action-btn delete-btn"
                    >
                      <DeleteOutlined />
                    </a-button>
                  </a-space>
                </div>
              </a-descriptions-item>
            </a-descriptions>

            <!-- 操作按钮区域 -->
            <div class="action-area" v-if="device === DEVICE_TYPE_ENUM.PC">
              <a-space wrap>
                <a-button type="primary" @click="doDownload" class="action-btn download-btn">
                  <DownloadOutlined />
                </a-button>
                <a-button
                  type="primary"
                  @click="doShare"
                  v-if="showShareButton"
                  class="action-btn share-btn"
                >
                  <ShareAltOutlined />
                </a-button>
                <a-button type="primary" @click="doEdit" v-if="canEdit" class="action-btn edit-btn">
                  <EditOutlined />
                </a-button>
                <a-button
                  type="primary"
                  @click="showDeleteConfirm"
                  v-if="canDelete"
                  class="action-btn delete-btn"
                >
                  <DeleteOutlined />
                </a-button>
              </a-space>
            </div>
          </a-card>

        </a-col>
      </a-row>
    </template>
    <ShareModal ref="shareModalRef" :link="shareLink" :imageUrl="shareImage" />
    <!-- 聊天室弹框 -->
    <a-modal
      v-model:visible="showChatModal"
      title="聊天室"
      :width="800"
      :footer="null"
      :class="{ 'mobile-chat-modal': device !== DEVICE_TYPE_ENUM.PC }"
      :closable="device === DEVICE_TYPE_ENUM.PC"
      :bodyStyle="{ height: '600px', overflow: 'hidden' }"
    >
      <template #title>
        <div class="modal-header">
          <div class="header-title">
            <span class="title-text">图片讨论</span>
            <a-popover
              placement="bottomRight"
              trigger="hover"
              :overlayClassName="'online-users-popover'"
            >
              <template #content>
                <div class="online-users-list">
                  <div class="section-title">在线用户 ({{ onlineCount }})</div>
                  <div v-for="user in onlineUsers" :key="user.id" class="online-user-item">
                    <a-avatar :src="user.userAvatar || getDefaultAvatar(user.userName)" size="small" />
                    <span class="online-user-name">{{ user.userName }}</span>
                    <span class="online-status active"></span>
                  </div>
                </div>
              </template>
              <a-avatar-group
                :maxCount="5"
                class="header-avatars"
              >
                <a-tooltip
                  v-for="user in onlineUsers"
                  :key="user.id"
                  :title="user.userName"
                >
                  <a-avatar :src="user.userAvatar || getDefaultAvatar(user.userName)" />
                </a-tooltip>
              </a-avatar-group>
            </a-popover>
            <span class="online-count">({{ onlineCount }}人在线)</span>
          </div>
        </div>
      </template>

      <PictureChatRoom
        ref="chatRoomRef"
        :pictureId="props.id"
        @message="handleChatMessage"
        class="modal-chat-room"
      />
    </a-modal>
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
        <h3 class="confirm-title">确认删除该图片？</h3>
        <p class="confirm-desc">
          <span>删除后将无法恢复，是否继续？</span>
        </p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="deleteConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmDelete">
            确认删除
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch, nextTick, onUnmounted } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  ShareAltOutlined,
  LoadingOutlined,
  MessageOutlined,
  ExclamationCircleFilled,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRoute, useRouter } from 'vue-router'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { prevRoute } from '@/router'
import ShareModal from '@/components/ShareModal.vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space.ts'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'

const route = useRoute() // 获取当前路由实例
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 新增一个响应式变量用于标记图片是否加载完成，初始化为false
const pictureLoaded = ref(false)
const mounted = ref(false)
const isDeleted = computed(() => !picture.value || !picture.value.id)

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  const currentRoutePath = route.path // 获取当前路由的路径
  // console.log('当前路由路径：', currentRoutePath)
  // 获取图片详情，在获取成功后将图片加载完成标记设为true
  await fetchPictureDetail()
  await checkIsFollowed()
  pictureLoaded.value = true

  // 添加淡入动画
  setTimeout(() => {
    mounted.value = true
  }, 100)

  // 自动触发一次聊天室按钮点击
  if (loginUserStore.loginUser) {
    showChatModal.value = true
    nextTick(() => {
      showChatModal.value = false
    })
  }
})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

//是否显示分享按钮
const showShareButton = computed(() => {
  // 仅登录用户可分享
  // console.log('prevRoute.name', prevRoute)
  return prevRoute.name !== '空间详情'
})

// 判断是否显示聊天室
const showChatRoom = computed(() => {
  // 只有在以下条件都满足时才显示聊天室：
  // 1. 图片可以分享
  // 2. 图片不是来自个人空间
  return showShareButton.value && !picture.value?.spaceId
})

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({} as API.PictureVO)

const loginUserStore = useLoginUserStore()


// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      // message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    // message.error('获取图片详情失败：' + e.message)
  }
}

const router = useRouter()

// 编辑
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除图片
const deleteConfirmVisible = ref(false)

const showDeleteConfirm = () => {
  deleteConfirmVisible.value = true
}

const confirmDelete = async () => {
  try {
    const res = await deletePictureUsingPost({
      id: picture.value?.id
    })
    if (res.data.code === 0) {
      // message.success('删除成功')
      deleteConfirmVisible.value = false
      router.back()
    } else {
      // message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    // message.error('删除失败，请稍后重试')
  }
}

// 下载图片
const doDownload = () => {
  downloadImage(picture.value.url)
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()
// 分享图片
const shareImage = ref('')

// 分享
const doShare = () => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value.id}`
  // 设置分享图片，优先使用缩略图
  shareImage.value = picture.value.thumbnailUrl || picture.value.url
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}

// 计算属性添加空值检查
const pageTitle = computed(() => {
  return `${picture.value?.name || '加载中'} - 图片详情`
})

const isFollowed = ref(false)
const followLoading = ref(false)

// 检查是否已关注
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !picture.value?.user?.id) {
    return
  }
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id
    })
    if (res.data?.data) {
      isFollowed.value = res.data.data
    }
  } catch (error) {
    // console.error('检查关注状态失败:', error)
  }
}

// 处理关注/取消关注
const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  followLoading.value = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: picture.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })

    if (res.data?.code === 0) {
      isFollowed.value = !isFollowed.value
    } else {
      // message.error('操作失败')
    }
  } catch (error) {
    // console.error('关注操作失败:', error)
    // message.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 修改聊天相关的状态
const showChatModal = ref(false)
const onlineUsers = ref<any[]>([])
const onlineCount = ref(0)
const chatRoomRef = ref()

// 处理聊天消息
const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    onlineCount.value = msg.onlineCount
    onlineUsers.value = msg.onlineUsers
  }
}

// 打开聊天室弹框
const openChatModal = () => {
  if (loginUserStore.loginUser) {
    showChatModal.value = true
  } else {
    message.warning('请先登录')
  }
}

// 处理用户点击
const handleUserClick = (user) => {
  if (!user) return
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar,
      userAccount: user.userAccount,
      userProfile: user.userProfile,
      userRole: user.userRole,
      createTime: user.createTime
    }
  })
}

// 在页面卸载时断开连接
onUnmounted(() => {
  if (chatRoomRef.value) {
    chatRoomRef.value.disconnect()
  }
})

</script>

<style scoped>
#pictureDetailPage {
  margin-bottom: 16px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

#pictureDetailPage.mounted {
  opacity: 1;
  transform: translateY(0);
}

.preview-col,
.info-col {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  transform: translateY(20px);
}

#pictureDetailPage.mounted .preview-col {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.2s;
}

#pictureDetailPage.mounted .info-col {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.3s;
}

/* 图片容器动画 */
.image-container {
  opacity: 0;
  transform: scale(0.98);
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.image-container.loaded {
  opacity: 1;
  transform: scale(1);
}

/* 预览卡片样式优化 */
.preview-card {
  background: #f9f9f9;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;

  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

/* 图片样式优化 */
.main-image {
  transition: all 0.3s ease;

  &:deep(.ant-image-img) {
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
  }

  &:hover:deep(.ant-image-img) {
    transform: scale(1.02);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  }
}

/* 信息卡片样式优化 */
.info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .preview-card,
  .info-card {
    background: #262626;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  }

  .preview-card:hover,
  .info-card:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
  }

  .image-container {
    background: #1a1a1a;
  }

  .main-image:deep(.ant-image-img) {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  .username,
  .title-text {
    color: #e5e7eb;
  }

  .info-descriptions :deep(.ant-descriptions-item-label),
  .info-descriptions :deep(.ant-descriptions-item-content) {
    color: #9ca3af;
  }
}

/* 移动端适配优化 */
@media screen and (max-width: 768px) {
  #pictureDetailPage {
    margin: 0;
  }

  .preview-card,
  .info-card {
    border-radius: 0;
    box-shadow: none;
  }

  .image-container {
    padding: 0;
  }

  .main-image:deep(.ant-image-img) {
    border-radius: 0;
  }

  .deleted-state {
    margin: 16px;
    min-height: calc(100vh - 232px);
  }

  .deleted-icon {
    font-size: 40px;
  }

  .deleted-content h2 {
    font-size: 18px;
  }

  .deleted-content p {
    font-size: 13px;
  }

  .back-button {
    min-width: 110px;
    height: 36px;
  }
}

/* 预览卡片样式 */
.preview-card {
  background: #f9f9f9;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.preview-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

/* 图片容器样式优化 */
.image-container {
  position: relative;
  width: 100%;
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  padding: 16px;
  transition: all 0.3s ease;
}

/* 预览遮罩层样式优化 */
.preview-mask {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: white;
  font-size: 16px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(10px);

  &:hover {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 预览模式样式优化 */
:deep(.ant-image-preview) {
  .ant-image-preview-img {
    max-width: 90vw;
    max-height: 90vh;
    border-radius: 8px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  }

  .ant-image-preview-operations {
    background: rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(10px);
    border-radius: 8px;
    margin: 16px;
    padding: 8px 16px;
  }

  .ant-image-preview-operations-operation {
    padding: 8px;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 4px;
    }
  }
}

/* 信息卡片样式 */
.info-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

/* 描述列表样式 */
.info-descriptions :deep(.ant-descriptions-item) {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-descriptions :deep(.ant-descriptions-item-label) {
  color: #666;
  font-size: 14px;
  width: 80px;
}

.info-descriptions :deep(.ant-descriptions-item-content) {
  color: #333;
  font-size: 14px;
}

/* 作者信息样式 */
.author-item {
  margin-bottom: 8px;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* 按钮区域样式 */
.action-area {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 统一的按钮基础样式 */
.action-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  border: none !important;
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: white !important;
}

/* 下载按钮 - 蓝紫渐变 */
:deep(.download-btn.ant-btn-primary) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.35);
}

/* 分享按钮 - 蓝橙渐变 */
:deep(.share-btn.ant-btn-primary) {
  background: linear-gradient(135deg, #0ea5e9, #f59e0b) !important;
  box-shadow: 0 4px 15px rgba(14, 165, 233, 0.35);
}

/* 编辑按钮 - 绿色渐变 */
:deep(.edit-btn.ant-btn-primary) {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.35);
}

/* 删除按钮 - 红色渐变 */
:deep(.delete-btn.ant-btn-primary) {
  background: linear-gradient(135deg, #ef4444, #dc2626) !important;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35);
}

/* 图标样式 */
.action-btn :deep(.anticon) {
  font-size: 18px;
  color: white !important;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  transition: all 0.3s ease;
}

/* 按钮悬停效果 */
.action-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1) saturate(1.1);
}

.action-btn:hover :deep(.anticon) {
  transform: scale(1.1);
  filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.8));
}

/* 点击效果 */
.action-btn:active {
  transform: scale(0.95);
  filter: brightness(0.95);
}

/* 按钮发光效果 */
.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent 0%,
    rgba(255, 255, 255, 0) 20%,
    rgba(255, 255, 255, 0.4) 50%,
    rgba(255, 255, 255, 0) 80%,
    transparent 100%
  );
  animation: shine 4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

@keyframes shine {
  0% {
    left: -150%;
    opacity: 0;
  }
  10% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.3;
  }
  100% {
    left: 150%;
    opacity: 0;
  }
}

/* 让每个按钮的动画错开开始时间 */
.download-btn::before {
  animation-delay: 0s;
}

.share-btn::before {
  animation-delay: 1s;
}

.edit-btn::before {
  animation-delay: 2s;
}

.delete-btn::before {
  animation-delay: 3s;
}

/* 按钮容器样式 */
.action-area {
  display: flex;
  padding: 8px 0;
}

.action-area :deep(.ant-space) {
  gap: 12px !important;
}

/* 移除旧的 PC 端按钮样式 */
@media screen and (min-width: 769px) {
  .action-btn {
    width: 40px;
    height: 40px;
  }
}

/* 移动端样式 */
@media screen and (max-width: 768px) {
  #pictureDetailPage {
    margin: 0;
    width: 100%;
  }

  .preview-card {
    margin: 0;
    border-radius: 0;
    width: 100%;
  }

  .image-container {
    border-radius: 0;
    background: #f9f9f9;
    width: 100%;
    padding: 0;
    min-height: 300px;
    position: relative;
  }

  :deep(.ant-card-body) {
    padding: 0;
    width: 100%;
  }

  :deep(.ant-image) {
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
  }

  :deep(.ant-image-img) {
    width: 100% !important;
    height: auto !important;
    max-height: 660px;
    min-width: 100%;
    object-fit: contain;
  }

  /* 加载占位符样式优化 */
  .loading-placeholder {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f9f9f9;
    z-index: 1;

    .loading-content {
      padding: 16px;
      background: rgba(255, 255, 255, 0.8);
      backdrop-filter: blur(8px);
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }

    .loading-text {
      font-size: 13px;
      color: #64748b;
      margin: 0;
    }

    :deep(.ant-spin) {
      .ant-spin-dot {
        font-size: 24px;
        margin: auto;
      }

      .ant-spin-text {
        padding-top: 8px;
        font-size: 13px;
        color: #64748b;
      }
    }
  }

  /* 针对小图的特殊处理 */
  :deep(.ant-image-img[style*='width: 0']),
  :deep(.ant-image-img[style*='width:0']),
  :deep(.ant-image-img[style*='width: auto']),
  :deep(.ant-image-img[style*='width:auto']) {
    width: 100% !important;
    height: auto !important;
    object-fit: contain;
  }

  /* 修复信息卡片样式 */
  .info-card {
    margin: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .info-card :deep(.ant-card-body) {
    padding: 12px 16px;
  }

  /* 修复描述列表样式 */
  .info-descriptions :deep(.ant-descriptions-row) {
    display: flex;
    border-bottom: 1px solid #f0f0f0;
  }

  .info-descriptions :deep(.ant-descriptions-item) {
    padding: 8px 0;
    width: 100%;
  }

  .info-descriptions :deep(.ant-descriptions-item-container) {
    display: flex;
  }

  .info-descriptions :deep(.ant-descriptions-item-label) {
    width: 60px;
    font-size: 13px;
    color: #666;
    flex-shrink: 0;
  }

  .info-descriptions :deep(.ant-descriptions-item-content) {
    flex: 1;
    font-size: 13px;
    color: #333;
    padding-left: 8px;
  }

  /* 标签样式 */
  :deep(.ant-tag) {
    margin: 2px 4px 2px 0;
    font-size: 12px;
  }

  /* 作者信息样式 */
  .author-item :deep(.ant-space) {
    display: flex;
    align-items: center;
  }

  .author-name {
    font-size: 13px;
    margin-left: 8px;
  }

  /* 按钮区域样式 */
  .info-descriptions :deep(.ant-descriptions-item:last-child) {
    border-bottom: none;
  }

  .mobile-actions {
    display: flex;
    padding: 4px 0;
  }

  .mobile-actions :deep(.ant-space) {
    width: 100%;
    gap: 8px !important;
    justify-content: flex-start;
  }

  .mobile-actions .action-btn {
    width: 34px;
    height: 34px;
    border-radius: 6px;
  }

  .mobile-actions .action-btn :deep(.anticon) {
    font-size: 16px;
  }

  /* 移动端按钮样式细节优化 */
  .mobile-actions .action-btn:deep(.ant-btn-primary) {
    background: linear-gradient(45deg, #6366f1 0%, #8b5cf6 50%, #d946ef 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
    color: white;
  }

  .mobile-actions .action-btn:deep(.ant-btn-primary.ant-btn-background-ghost) {
    background: linear-gradient(45deg, #06b6d4 0%, #3b82f6 50%, #f97316 100%);
    border: none;
    color: white;
    box-shadow: 0 4px 15px rgba(6, 182, 212, 0.3);
  }

  .mobile-actions .action-btn:deep(.ant-btn:not(.ant-btn-primary):not(.ant-btn-dangerous)) {
    background: linear-gradient(45deg, #059669 0%, #10b981 50%, #34d399 100%);
    border: none;
    color: white;
    box-shadow: 0 4px 15px rgba(5, 150, 105, 0.3);
  }

  .mobile-actions .action-btn:deep(.ant-btn-dangerous) {
    background: linear-gradient(45deg, #dc2626 0%, #ef4444 50%, #f87171 100%);
    border: none;
    color: white;
    box-shadow: 0 4px 15px rgba(220, 38, 38, 0.3);
  }

  /* 按钮悬停效果增强 */
  .mobile-actions .action-btn:hover {
    transform: translateY(-2px) scale(1.05);
    filter: brightness(1.1) contrast(1.1);
  }

  /* 点击效果增强 */
  .mobile-actions .action-btn:active {
    transform: scale(0.95);
    filter: brightness(0.95) contrast(0.95);
  }

  /* 图标发光效果 */
  .mobile-actions .action-btn :deep(.anticon) {
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
  }

  /* 涟漪效果增强 */
  .mobile-actions .action-btn::after {
    background: rgba(255, 255, 255, 0.8);
  }

  @keyframes ripple {
    0% {
      transform: scale(0, 0);
      opacity: 0.8;
    }
    20% {
      transform: scale(25, 25);
      opacity: 0.5;
    }
    100% {
      opacity: 0;
      transform: scale(40, 40);
    }
  }

  /* 按钮基础样式 */
  .mobile-actions :deep(.action-btn.ant-btn) {
    width: 36px;
    height: 36px;
    padding: 0;
    border: none !important;
    position: relative;
    overflow: hidden;
    border-radius: 10px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    color: white !important;
  }

  /* 下载按钮 - 蓝紫渐变 */
  .mobile-actions :deep(.download-btn.ant-btn-primary) {
    background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.35);
  }

  /* 分享按钮 - 蓝橙渐变 */
  .mobile-actions :deep(.share-btn.ant-btn-primary) {
    background: linear-gradient(135deg, #0ea5e9, #f59e0b) !important;
    box-shadow: 0 4px 15px rgba(14, 165, 233, 0.35);
  }

  /* 编辑按钮 - 绿色渐变 */
  .mobile-actions :deep(.edit-btn.ant-btn-primary) {
    background: linear-gradient(135deg, #10b981, #059669) !important;
    box-shadow: 0 4px 15px rgba(16, 185, 129, 0.35);
  }

  /* 删除按钮 - 红色渐变 */
  .mobile-actions :deep(.delete-btn.ant-btn-primary) {
    background: linear-gradient(135deg, #ef4444, #dc2626) !important;
    box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35);
  }

  /* 图标样式 */
  .mobile-actions :deep(.action-btn .anticon) {
    font-size: 18px;
    color: white !important;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
    transition: all 0.3s ease;
  }

  /* 按钮悬停效果 */
  .mobile-actions :deep(.action-btn:hover) {
    transform: translateY(-2px);
    filter: brightness(1.1) saturate(1.1);
  }

  .mobile-actions :deep(.action-btn:hover .anticon) {
    transform: scale(1.1);
    filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.8));
  }

  /* 点击效果 */
  .mobile-actions :deep(.action-btn:active) {
    transform: scale(0.95);
    filter: brightness(0.95);
  }

  /* 按钮发光效果 */
  .mobile-actions :deep(.action-btn)::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    animation: shine 2s infinite;
  }

  @keyframes shine {
    0% {
      left: -100%;
    }
    50% {
      left: 100%;
    }
    100% {
      left: 100%;
    }
  }

  /* 按钮容器样式 */
  .mobile-actions {
    display: flex;
    padding: 8px 0;
  }

  .mobile-actions :deep(.ant-space) {
    gap: 12px !important;
  }

  .loading-placeholder {
    .loading-content {
      padding: 16px;
      background: rgba(255, 255, 255, 0.95);
    }

    .loading-text {
      font-size: 13px;
    }

    :deep(.ant-spin) {
      .ant-spin-dot {
        font-size: 24px;
      }

      .ant-spin-text {
        padding-top: 8px;
        font-size: 13px;
      }
    }
  }
}

/* PC端样式 */
@media screen and (min-width: 769px) {
  .image-container {
    min-height: 400px;
    max-height: 660px;
    background: #f9f9f9;
    border-radius: 4px;
    padding: 20px;
  }

  :deep(.ant-image) {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  :deep(.ant-image-img) {
    max-width: 100%;
    max-height: 620px;
    object-fit: contain;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-radius: 4px;
  }
}

/* 添加悬停效果 */
:deep(.ant-image-img:hover) {
  transform: scale(1.02);
  transition: all 0.3s ease;
}

/* PC端按钮样式 */
@media screen and (min-width: 769px) {
  .action-btn {
    width: 40px;
    height: 40px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }

  .action-btn :deep(.anticon) {
    font-size: 18px;
  }

  /* 主按钮样式 */
  .action-btn:deep(.ant-btn-primary) {
    background: linear-gradient(135deg, #ff8e53, #ff6b6b);
    border: none;
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
  }

  /* 幽灵按钮样式 */
  .action-btn:deep(.ant-btn-primary.ant-btn-background-ghost) {
    border: 1px solid #ff8e53;
    color: #ff8e53;
  }

  /* 普通按钮样式 */
  .action-btn:deep(.ant-btn:not(.ant-btn-primary):not(.ant-btn-dangerous)) {
    border: 1px solid #d9d9d9;
    color: #666;
  }

  /* 危险按钮样式 */
  .action-btn:deep(.ant-btn-dangerous) {
    border: 1px solid #ff4d4f;
    color: #ff4d4f;
  }

  /* 按钮悬停效果 */
  .action-btn:hover {
    transform: translateY(-2px);
    transition: transform 0.2s;
  }
}

/* 按钮交互效果 */
.action-btn:active {
  transform: scale(0.95);
  transition: transform 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
  transition: transform 0.2s;
}

/* 加载占位符基础样式 */
.loading-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  z-index: 1;

  .loading-content {
    padding: 24px 32px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    z-index: 2;
  }

  .loading-text {
    color: #64748b;
    font-size: 14px;
    margin: 0;
  }

  :deep(.ant-spin) {
    .ant-spin-dot {
      font-size: 24px;
      margin: auto;
    }
  }
}

@media screen and (max-width: 768px) {
  /* ... 其他移动端样式保持不变 ... */

  .image-container {
    border-radius: 0;
    background: #f9f9f9;
    width: 100%;
    padding: 0;
    min-height: 300px;
    position: relative;
  }

  :deep(.ant-image) {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  :deep(.ant-image-img) {
    width: 100% !important;
    height: auto !important;
    max-height: 90vh;
    object-fit: contain;
  }

  /* 移动端加载占位符样式调整 */
  .loading-placeholder {
    .loading-content {
      padding: 20px 24px;
      background: rgba(255, 255, 255, 0.95);
    }

    .loading-text {
      font-size: 13px;
    }

    :deep(.ant-spin) {
      .ant-spin-dot {
        font-size: 22px;
      }
    }
  }
}

/* 加载容器基础样式 */
.loading-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  z-index: 10;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  animation: fadeIn 0.3s ease;
  min-width: 200px;
}

.loading-spinner {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
  animation: pulse 2s infinite ease-in-out;
}

.loading-text {
  text-align: center;
}

.primary-text {
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.secondary-text {
  color: #64748b;
  font-size: 14px;
}

/* 移动端样式调整 */
@media screen and (max-width: 768px) {
  .loading-content {
    padding: 20px;
    background: rgba(255, 255, 255, 0.95);
    min-width: 180px;
  }

  .primary-text {
    font-size: 15px;
    color: #1a1a1a;
    font-weight: 500;
  }

  .secondary-text {
    font-size: 13px;
    color: #64748b;
    margin-top: 4px;
  }

  :deep(.ant-spin) {
    .ant-spin-dot {
      font-size: 22px !important;
    }

    .ant-spin-dot-item {
      background-color: #ff8e53 !important;
    }

    .anticon {
      font-size: 22px !important;
      color: #ff8e53 !important;
    }
  }

  .loading-spinner {
    margin-bottom: 12px;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

.loading-spinner {
  animation: pulse 2s infinite ease-in-out;
}

/* 加载动画样式 */
:deep(.ant-spin) {
  .ant-spin-dot {
    font-size: 24px !important;
  }

  .ant-spin-dot-item {
    background-color: #ff8e53;
  }

  .anticon {
    font-size: 24px;
    color: #ff8e53;
  }
}

.follow-button {
  min-width: 68px;
  height: 28px;
  border-radius: 14px;
  font-size: 13px;
  padding: 0 12px;
  transition: all 0.3s ease;
}

.follow-button.ant-btn-primary {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  box-shadow: 0 2px 6px rgba(255, 107, 107, 0.2);
}

.follow-button.ant-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.follow-button.ant-btn-default {
  border-color: #e2e8f0;
  color: #64748b;
}

.follow-button.ant-btn-default:hover {
  color: #ff8e53;
  border-color: #ff8e53;
  background: #fff6f3;
}

.chat-section {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.chat-button {
  width: 100%;
  height: 40px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.chat-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
}

.chat-button-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.online-avatars {
  margin: 0 8px;
}

.online-count {
  font-size: 13px;
  opacity: 0.9;
}

/* 弹框样式 */
.chat-modal {
  :deep(.ant-modal-content) {
    border-radius: 12px;
    overflow: hidden;
  }

  :deep(.ant-modal-header) {
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.ant-modal-body) {
    padding: 0;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-avatars {
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
  }
}

.modal-chat-room {
  height: 100%;
}

.online-users-list {
  max-height: 300px;
  overflow-y: auto;
  min-width: 200px;
  padding: 12px;
}

.online-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.02);
  }
}

.online-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 4px rgba(82, 196, 26, 0.5);
}

.section-title {
  padding: 8px;
  color: #666;
  font-size: 13px;
  font-weight: 500;
  border-left: 3px solid #ff8e53;
  margin: 8px 0;
  background: rgba(255, 142, 83, 0.05);
}

/* 标签容器样式 */
.info-descriptions :deep(.ant-descriptions-item-content) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0;
}

/* 标签样式优化 */
:deep(.ant-tag) {
  margin: 0;
  padding: 6px 12px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #0284c7;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  height: 28px;
  line-height: 1;

  &:hover {
    transform: translateY(-2px);
    background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
    box-shadow: 0 4px 12px rgba(2, 132, 199, 0.15);
  }
}

/* 不同类型标签使用不同配色 */
:deep(.ant-tag):nth-child(3n+1) {
  background: linear-gradient(135deg, #fdf4ff 0%, #fae8ff 100%);
  color: #c026d3;

  &:hover {
    background: linear-gradient(135deg, #fae8ff 0%, #f5d0fe 100%);
    box-shadow: 0 4px 12px rgba(192, 38, 211, 0.15);
  }
}

:deep(.ant-tag):nth-child(3n+2) {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  color: #16a34a;

  &:hover {
    background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
    box-shadow: 0 4px 12px rgba(22, 163, 74, 0.15);
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  :deep(.ant-tag) {
    background: linear-gradient(135deg, rgba(2, 132, 199, 0.1) 0%, rgba(2, 132, 199, 0.2) 100%);
    color: #38bdf8;

    &:hover {
      background: linear-gradient(135deg, rgba(2, 132, 199, 0.2) 0%, rgba(2, 132, 199, 0.3) 100%);
      box-shadow: 0 4px 12px rgba(2, 132, 199, 0.2);
    }
  }

  :deep(.ant-tag):nth-child(3n+1) {
    background: linear-gradient(135deg, rgba(192, 38, 211, 0.1) 0%, rgba(192, 38, 211, 0.2) 100%);
    color: #e879f9;

    &:hover {
      background: linear-gradient(135deg, rgba(192, 38, 211, 0.2) 0%, rgba(192, 38, 211, 0.3) 100%);
      box-shadow: 0 4px 12px rgba(192, 38, 211, 0.2);
    }
  }

  :deep(.ant-tag):nth-child(3n+2) {
    background: linear-gradient(135deg, rgba(22, 163, 74, 0.1) 0%, rgba(22, 163, 74, 0.2) 100%);
    color: #4ade80;

    &:hover {
      background: linear-gradient(135deg, rgba(22, 163, 74, 0.2) 0%, rgba(22, 163, 74, 0.3) 100%);
      box-shadow: 0 4px 12px rgba(22, 163, 74, 0.2);
    }
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  :deep(.ant-tag) {
    padding: 4px 10px;
    font-size: 12px;
    height: 24px;
  }

  .info-descriptions :deep(.ant-descriptions-item-content) {
    gap: 6px;
    padding: 2px 0;
  }
}

/* 移动端聊天室弹窗样式 */
:deep(.mobile-chat-modal) {
  top: 0;
  padding: 0;
  margin: 0;
  max-width: 100vw;
  width: 100vw !important;
  height: 100vh !important;
}

:deep(.mobile-chat-modal .ant-modal-content) {
  height: 100vh;
  border-radius: 0;
}

:deep(.mobile-chat-modal .ant-modal-body) {
  height: calc(100vh - 55px);
  padding: 0;
}

:deep(.mobile-chat-modal .ant-modal-header) {
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

/* 删除状态样式 */
.deleted-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 32px 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.deleted-content {
  text-align: center;
  animation: fadeIn 0.5s ease;
}

.deleted-icon {
  font-size: 48px;
  color: #ff6b6b;
  margin-bottom: 16px;
  opacity: 0.5;
}

.deleted-content h2 {
  font-size: 20px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.deleted-content p {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
}

.back-button {
  min-width: 120px;
  height: 40px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

