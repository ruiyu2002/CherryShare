<template>
  <div class="mobile-picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item class="picture-item">
          <!-- 单张图片卡片 -->
          <a-card class="picture-card" hoverable @click="doClickPicture(picture)">
            <!-- 图片封面 -->
            <template #cover>
              <div class="image-wrapper">
                <div class="image-container">
                  <img
                    :alt="picture.name"
                    :src="`${picture.thumbnailUrl ?? picture.url}?${new Date().getTime()}`"
                    @load="handleImageLoad(picture)"
                    @error="handleImageError(picture)"
                  />
                </div>
              </div>
            </template>

            <!-- 图片信息 -->
            <a-card-meta :title="picture.name">
              <template #description v-if="!showOp">
                <div class="interaction-bar">
                  <!-- 点赞按钮 -->
                  <div class="action-item" @click="(e) => doLike(picture, e)">
                    <van-icon
                      class="action-icon"
                      size="20"
                      :name="picture.isLiked === 1 ? 'like' : 'like-o'"
                      :color="picture.isLiked === 1 ? '#ff6b6b' : '#94a3b8'"
                    />
                    <span class="action-count">{{ formatNumber(picture.likeCount) }}</span>
                  </div>

                  <!-- 评论按钮 -->
                  <div class="action-item" @click="(e) => doComments(picture, e)">
                    <van-icon class="action-icon" size="20" name="chat-o" color="#94a3b8" />
                    <span class="action-count">{{ formatNumber(picture.commentCount) }}</span>
                  </div>

                  <!-- 分享按钮 -->
                  <div class="action-item" @click="(e) => doShare(picture, e)">
                    <van-icon
                      class="action-icon"
                      size="20"
                      name="share"
                      :color="shareButtonColor[picture.id] || '#94a3b8'"
                    />
                    <span class="action-count">{{ formatNumber(picture.shareCount) }}</span>
                  </div>
                </div>
              </template>
            </a-card-meta>

            <!-- 操作按钮 -->
            <template v-if="showOp" #actions>
              <!-- 我的发布页面显示审核信息 -->
              <div v-if="isMyPosts" class="review-status">
                <a-button type="link" class="review-button" @click.stop="showReviewModal(picture)">
                  <template v-if="picture.reviewStatus === 0">
                    <ClockCircleOutlined class="status-icon pending" />
                    <span class="status-text">待审核</span>
                  </template>
                  <template v-else-if="picture.reviewStatus === 1">
                    <CheckCircleOutlined class="status-icon approved" />
                    <span class="status-text">已通过</span>
                  </template>
                  <template v-else-if="picture.reviewStatus === 2">
                    <CloseCircleOutlined class="status-icon rejected" />
                    <span class="status-text">已拒绝</span>
                  </template>
                </a-button>
              </div>
              <!-- 其他页面显示操作按钮 -->
              <div v-else class="operation-buttons">
                <a-button
                  v-if="canEdit"
                  type="link"
                  class="action-button edit-button"
                  @click="(e) => doEdit(picture, e)"
                >
                  <EditOutlined />
                </a-button>
                <a-button
                  type="link"
                  class="action-button search-button"
                  @click="(e) => doSearch(picture, e)"
                >
                  <SearchOutlined />
                </a-button>
                <a-button
                  v-if="canDelete"
                  type="link"
                  class="action-button delete-button"
                  @click="(e) => doDelete(picture, e)"
                >
                  <DeleteOutlined />
                </a-button>
              </div>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

    <!-- 分享模态框 -->
    <ShareModal ref="shareModalRef" :link="shareLink" />

    <!-- 评论抽屉 -->
    <a-drawer
      class="comments-drawer"
      v-model:open="visible"
      :placement="device === DEVICE_TYPE_ENUM.PC ? 'right' : 'bottom'"
      title="评论"
      :footer="false"
      @cancel="closeModal"
      :height="'80vh'"
      :width="400"
    >
      <!-- 修改这里，将点击事件限制在非输入区域 -->
      <div class="drawer-content" ref="scrollContainer" @scroll="handleScroll">
        <div class="comments-area" @click="cancelReply">
          <!-- 加载中状态 -->
          <div v-if="commentloading" class="loading-container">
            <a-spin tip="加载评论中..." />
          </div>

          <!-- 评论列表 -->
          <template v-else>
            <CommentList
              v-if="showFirstComment"
              :comments="firstcomment"
              @reply-clicked="handleReplyClick"
              @update-comments="updateComments"
            />
            <CommentList
              :comments="comments"
              @reply-clicked="handleReplyClick"
              @update-comments="updateComments"
            />
            <div v-if="isEndOfData" class="no-more-data">没有更多评论了~</div>
          </template>
        </div>
      </div>

      <!-- 评论输入区域 -->
      <div class="comment-input-wrapper">
        <!-- 回复信息提示 -->
        <div v-if="replyCommentId" class="reply-info">
          <div class="reply-text">
            <span class="reply-label">回复评论</span>
            <ArrowRightOutlined class="reply-arrow" />
          </div>
          <CloseCircleOutlined class="cancel-reply" @click="cancelReply" />
        </div>

        <div class="input-area" :class="{ 'is-replying': replyCommentId }">
          <!-- 表情按钮 -->
          <SmileOutlined
            class="emoji-trigger"
            :class="{ active: showEmojiPicker }"
            @click="toggleEmojiPicker"
          />

          <a-input
            v-model:value="newCommentContent"
            :placeholder="replyCommentId ? '写下你的回复...' : '写下你的评论...'"
            class="comment-input"
            :maxLength="200"
          >
            <template #prefix v-if="replyCommentId">
              <MessageOutlined class="reply-icon" />
            </template>
            <template #suffix>
              <span class="word-count">{{ newCommentContent.length }}/200</span>
            </template>
          </a-input>

          <a-button
            type="primary"
            class="send-button"
            :class="{ 'reply-button': replyCommentId }"
            :disabled="!newCommentContent.trim()"
            @click="addComment"
          >
            {{ replyCommentId ? '回复' : '发送' }}
          </a-button>
        </div>

        <!-- 表情选择器 -->
        <div v-if="showEmojiPicker" class="emoji-picker-container">
          <EmojiPicker
            @select="onEmojiSelect"
            :i18n="emojiI18n"
            class="custom-emoji-picker"
          />
        </div>
      </div>
    </a-drawer>

    <!-- 审核详情弹窗 -->
    <a-modal
      v-model:open="reviewModalVisible"
      :title="getReviewModalTitle(currentPicture?.reviewStatus)"
      :footer="null"
      class="review-modal"
    >
      <div class="review-detail">
        <div class="status-icon-large">
          <ClockCircleOutlined v-if="currentPicture?.reviewStatus === 0" class="pending" />
          <CheckCircleOutlined v-else-if="currentPicture?.reviewStatus === 1" class="approved" />
          <CloseCircleOutlined v-else-if="currentPicture?.reviewStatus === 2" class="rejected" />
        </div>
        <div class="review-message">
          <template v-if="currentPicture?.reviewStatus === 0">
            您的图片正在审核中，请耐心等待...
          </template>
          <template v-else-if="currentPicture?.reviewStatus === 1">
            恭喜！您的图片已通过审核
          </template>
          <template v-else-if="currentPicture?.reviewStatus === 2">
            {{ currentPicture?.reviewMessage || '抱歉，您的图片未通过审核' }}
          </template>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import ShareModal from '@/components/ShareModal.vue'
import { h, onMounted, reactive, ref, nextTick, computed } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  CloseOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ArrowRightOutlined,
  SmileOutlined,
  MessageOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController.ts'
import { throttle } from 'vant/es/lazyload/vue-lazyload/util'

import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { getDeviceType } from '@/utils/device.ts'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { doShareUsingPost } from '@/api/shareRecordController.ts'

const loginUserStore = useLoginUserStore()
const currPicture = ref<API.PictureVO>()
//回复评论id
const replyCommentId = ref<string>('')

// 添加设备类型检测
const device = ref<string>('')

onMounted(async () => {
  device.value = await getDeviceType()
  replyCommentId.value = ''
  currPicture.value = props.dataList[0]
})

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
  canEdit?: boolean
  canDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
const handleReplyClick = (commentId: string) => {
  // console.log('MobilePictureList - 回复被点击，评论 ID:', commentId)
  replyCommentId.value = commentId

  // 强制更新输入框状态
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input .ant-input') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      // 滚动到输入框位置
      inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

// 评论
// 是否可见
const visible = ref(false)

// 评论数据
const comments = ref<API.CommentsQueryRequest[]>([])

// 存储用户输入的评论内容
const newCommentContent = ref('')

// 存储第一个评论列表数据
const firstcomment = ref<API.CommentsQueryRequest[]>([])

// 控制第一个评论列表的显示与隐藏
const showFirstComment = ref(false)

// 控制加载状态
const commentloading = ref(true)

// 使用 reactive 包裹 queryRequest 使其具有响应式
const queryRequest = reactive<API.CommentsQueryRequest>({
  pictureId: 0,
  current: 1,
  pageSize: 15,
})

//删除评论刷新操作
const updateComments = async () => {
  try {
    commentloading.value = true // 显示加载状态
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data.data != null) {
      // 确保评论ID作为字符串处理
      comments.value = res.data.data.records.map((comment) => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))
      commentloading.value = false
    } else {
      comments.value = []
      isEndOfData.value = true
      commentloading.value = false
    }
  } catch (error) {
    console.error('查询评论异常', error)
    commentloading.value = false
  }
}
// 打开弹窗
const doComments = async (picture, e) => {
  currPicture.value = picture
  queryRequest.pictureId = picture.id
  e.stopPropagation()
  visible.value = true
  try {
    // 数据清理操作
    comments.value = [] // 先清空评论数据
    firstcomment.value = []
    queryRequest.current = 1
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data.data != null) {
      comments.value = res.data.data.records
      commentloading.value = false // 数据加载完成，关闭加载状态
    } else {
      isEndOfData.value = true
      commentloading.value = false // 关闭加载状态
    }
  } catch (error) {
    console.error('查询评论异常', error)
    commentloading.value = false // 关闭加载状态
  }
}

// 新增用于移动端分页的页码变量
const page = ref(1)
const isEndOfData = ref(false)
const isLoading = ref(false)

// 处理滚动事件
const handleScroll = throttle(async (e: Event) => {
  // console.log('滚动事件触发')
  if (isLoading.value || isEndOfData.value) return
  const target = e.target as HTMLElement
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 200) {
    isLoading.value = true
    try {
      page.value++
      queryRequest.current = page.value
      const res = await queryCommentUsingPost(queryRequest)
      if (res.data.code === 0 && res.data.data) {
        const newData = res.data.data.records ?? []
        comments.value = [...comments.value, ...newData]
        isEndOfData.value = newData.length === 0
        // console.log('newData:', newData)
      } else {
        message.error('获取数据失败，' + res.data.message)
      }
    } catch (error) {
      console.error('加载更多评论异常', error)
    } finally {
      isLoading.value = false
    }
  }
}, 1500)

// 关闭弹窗
const closeModal = () => {
  replyCommentId.value = ''
  visible.value = false
  // 数据清理操作
  firstcomment.value = []
  newCommentContent.value = ''
  showFirstComment.value = false
  commentloading.value = false // 关闭加载状态
}

const requestBody: API.CommentsAddRequest = {
  content: newCommentContent.value,
  parentCommentId: 0, // 这里将 parentCommentId 设为 0，可根据需求修改
  pictureId: 0,
  userId: loginUserStore.loginUser.id,
}
// 添加评论
const addComment = async () => {
  try {
    if (!newCommentContent.value.trim()) {
      message.warning('评论内容不能为空')
      return
    }

    const requestBody: API.CommentsAddRequest = {
      content: newCommentContent.value,
      // 使用字符串形式传递 parentCommentId
      parentCommentId: replyCommentId.value ? replyCommentId.value : '0',
      pictureId: currPicture.value.id,
      userId: loginUserStore.loginUser.id,
    }

    // console.log('发送评论请求体:', requestBody) // 添加日志

    const res = await addCommentUsingPost(requestBody)
    if (res.data.code === 0) {
      message.success('评论成功')
      newCommentContent.value = ''
      replyCommentId.value = ''

      // 刷新评论列表
      queryRequest.current = 1
      page.value = 1
      isEndOfData.value = false
      await updateComments()
    }
  } catch (error) {
    console.error('添加评论失败，请求体:', requestBody, '错误:', error)
    message.error('评论失败，请稍后重试')
  }
}

// 处理图片加载完成事件，根据宽高比设置行内样式
const handleImageLoad = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  const width = imgElement.width
  const ratio = picture.picScale
  if (ratio && ratio > 0.58) {
    imgElement.style.width = '100%'
    imgElement.style.height = '100%'
    imgElement.style.objectFit = 'cover'
  } else if (ratio) {
    imgElement.style.width = '100%'
    imgElement.style.height = `${width * 1.78}px`
    imgElement.style.objectFit = 'cover'
    imgElement.style.objectPosition = 'center'
  }
}

// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

const doSearch = (picture, e) => {
  e.stopPropagation()
  router.push({
    path: `/search_picture`,
    query: {
      pictureId: picture.id,
    },
  })
}

// 编辑
const doEdit = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据，使用ant-design-vue的Modal.confirm进行删除提示
const doDelete = async (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该图片吗？此操作不可撤销哦！',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await deletePictureUsingPost({ id })
      if (res.data.code === 0) {
        message.success('删除成功，数据更新可能需要一段时间')
        props.onReload?.()
      } else {
        message.error('删除失败')
      }
    },
  })
}

// 处理图片加载错误的函数
const handleImageError = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  imgElement.src = picture.url // 将图片src替换为picture.url
}

// 点赞操作
const doLike = async (picture, e) => {
  e.stopPropagation()
  const requestBody: API.LikeRequest = {
    targetId: picture.id,
    targetType: 1, // 1 表示图片类型
    isLiked: picture.isLiked !== 1
  }

  try {
    const res = await doLikeUsingPost(requestBody)
    if (res.data.code === 0) {
      // 更新前端数据
      if (requestBody.isLiked) {
        picture.likeCount++
        picture.isLiked = 1
      } else {
        picture.likeCount--
        picture.isLiked = 0
      }
    }
  } catch (error) {
    message.error('操作异常')
  }
}

// 分享操作相关变量
const shareModalRef = ref()
const shareLink = ref<string>()
// 用于存储每个分享按钮的颜色，以图片id作为键
const shareButtonColor = ref<{ [key: string]: string }>({})

// 分享
// 处理分享
const doShare = async (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()

  // 设置分享链接和图片
  shareLink.value = window.location.origin + '/picture/' + picture.id

  // 显示分享模态框
  shareModalRef.value?.openModal()

  // 调用通用分享接口
  try {
    const requestBody: API.ShareRequest = {
      targetId: picture.id,
      targetType: 1, // 1 表示图片类型
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      // 更新分享数
      picture.shareCount++
      // 更新分享按钮颜色
      shareButtonColor.value[picture.id] = '#60c3d5'
    }
  } catch (error) {
    console.error('分享失败:', error)
    message.error('分享失败')
  }
}
// 格式化数字的函数，将较大数字转换为带k、w的格式，保留两位小数
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    const wan = (num / 10000).toFixed(2)
    return `${wan}w`
  } else if (num >= 1000) {
    const qian = (num / 1000).toFixed(2)
    return `${qian}k`
  }
  return num.toString()
}

// 取消回复状态
const cancelReply = () => {
  if (replyCommentId.value) {
    replyCommentId.value = ''
  }
}

// 审核弹窗相关
const reviewModalVisible = ref(false)
const currentPicture = ref<API.PictureVO>()

const showReviewModal = (picture: API.PictureVO) => {
  currentPicture.value = picture
  reviewModalVisible.value = true
}

const getReviewModalTitle = (status?: number) => {
  switch (status) {
    case 0:
      return '审核中'
    case 1:
      return '审核通过'
    case 2:
      return '审核未通过'
    default:
      return '审核状态'
  }
}

// 表情选择器相关
const showEmojiPicker = ref(false)

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const onEmojiSelect = (emoji: string) => {
  newCommentContent.value += emoji
}
</script>

<style scoped>
.mobile-picture-list {
  padding: 4px;
  width: 100%;
  margin: 0 auto;
}

.picture-item {
  margin-bottom: 16px;
  width: 100% !important;
}

:deep(.ant-list-items) {
  width: 100%;
}

:deep(.ant-list-item) {
  width: 100% !important;
  padding: 0 !important;
}

.picture-card {
  width: 100% !important;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  :deep(.ant-card-body) {
    background: rgba(255, 255, 255, 0.8);
  }

  :deep(.ant-card-actions) {
    background: rgba(255, 255, 255, 0.8);
    border-top: 1px solid rgba(0, 0, 0, 0.05);
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 66%;
  background: #f8fafc;
  border-radius: 12px 12px 0 0;
  overflow: hidden;
}

.image-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-card:hover .image-container img {
  transform: scale(1.05);
}

.interaction-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.action-icon {
  transition: transform 0.2s ease;

  &:active {
    transform: scale(1.2);
  }
}

.action-count {
  font-size: 13px;
  color: #94a3b8;
}

.operation-buttons {
  display: flex;
  justify-content: space-around;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
  transition: all 0.3s ease;

  .anticon {
    font-size: 16px;
  }

  &:hover {
    background: rgba(0, 0, 0, 0.02);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }
}

.edit-button {
  color: #ff8e53;

  &:hover {
    color: #ff7a3d;
    background: rgba(255, 142, 83, 0.1);
  }
}

.search-button {
  color: #45b090;

  &:hover {
    color: #3a9579;
    background: rgba(69, 176, 144, 0.1);
  }
}

.delete-button {
  color: #ff6b6b;

  &:hover {
    color: #ff5252;
    background: rgba(255, 107, 107, 0.1);
  }
}

/* 评论抽屉样式 */
.comments-drawer {
  :deep(.ant-drawer-content) {
    border-radius: 0;
  }

  :deep(.ant-drawer-content-wrapper) {
    /* PC 端右侧抽屉样式 */
    @media screen and (min-width: 769px) {
      box-shadow: -4px 0 16px rgba(0, 0, 0, 0.08);
    }
  }

  /* 移动端底部抽屉样式 */
  @media screen and (max-width: 768px) {
    :deep(.ant-drawer-content) {
      border-radius: 16px 16px 0 0;
    }
  }

  :deep(.ant-drawer-header) {
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.ant-drawer-body) {
    padding: 0;
  }
}

.drawer-content {
  padding: 16px;
  overflow-y: auto;
  height: calc(100% - 120px);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.no-more-data {
  text-align: center;
  color: #94a3b8;
  padding: 16px;
  margin-bottom: 60px;
}

.comment-input-wrapper {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: white;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  z-index: 1000;

  &.is-replying {
    padding-top: 12px;
    transform: translateY(-8px);
    background: #f8fafc;
    animation: slideUp 0.3s ease;
  }
}

.reply-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff6f3;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #ffe4d9;
}

.reply-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-label {
  font-size: 13px;
  color: #ff8e53;
  font-weight: 500;
}

.reply-arrow {
  font-size: 12px;
  color: #ff8e53;
}

.cancel-reply {
  cursor: pointer;
  padding: 4px;
  color: #ff8e53;
  font-size: 16px;
  transition: all 0.3s ease;
}

.cancel-reply:hover {
  transform: rotate(90deg);
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: white;
  transition: all 0.3s ease;
}

.input-area.is-replying {
  background: #fff6f3;
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

.reply-icon {
  color: #ff8e53;
  margin-right: 4px;
}

.comment-input {
  border-radius: 18px;
}

.word-count {
  font-size: 12px;
  color: #94a3b8;
}

.send-button {
  min-width: 64px;
  height: 36px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  transition: all 0.3s ease;
}

.reply-button {
  background: linear-gradient(135deg, #ff9c6e 0%, #ff8e53 100%);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  left: 0;
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

.comments-area {
  min-height: calc(100% - 80px);
  padding-bottom: 80px;
}

.review-status {
  padding: 8px;
  text-align: center;
}

.review-message {
  margin-left: 8px;
  color: #ff4d4f;
  font-size: 12px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-status {
    padding: 4px;
  }

  .review-message {
    display: block;
    margin-top: 4px;
    margin-left: 0;
  }
}

.review-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.status-icon {
  font-size: 16px;

  &.pending {
    color: #1890ff;
  }

  &.approved {
    color: #52c41a;
  }

  &.rejected {
    color: #ff4d4f;
  }
}

.status-text {
  font-size: 14px;
}

/* 审核弹窗样式 */
.review-detail {
  text-align: center;
  padding: 24px;
}

.status-icon-large {
  font-size: 48px;
  margin-bottom: 16px;

  .pending {
    color: #1890ff;
  }

  .approved {
    color: #52c41a;
  }

  .rejected {
    color: #ff4d4f;
  }
}

.review-message {
  font-size: 16px;
  color: #1f2937;
  line-height: 1.5;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-button {
    padding: 4px 8px;
  }

  .status-icon {
    font-size: 14px;
  }

  .status-text {
    font-size: 13px;
  }

  .review-detail {
    padding: 16px;
  }

  .status-icon-large {
    font-size: 40px;
  }

  .review-message {
    font-size: 14px;
  }
}
</style>
