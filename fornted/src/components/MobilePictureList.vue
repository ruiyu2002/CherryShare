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
                <img
                  :alt="picture.name"
                  :src="`${picture.thumbnailUrl ?? picture.url}?${new Date().getTime()}`"
                  @load="handleImageLoad(picture)"
                  @error="handleImageError(picture)"
                />
              </div>
            </template>

            <!-- 图片信息 -->
            <a-card-meta :title="picture.name">
              <template #description v-if="!showOp">
                <div class="interaction-bar">
                  <div class="picture-header">
                    <div class="picture-user" @click.stop="handleUserClick(picture.user)">
                      <a-avatar class="user-avatar" :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"/>
                      <span>{{ picture.user?.userName }}</span>
                    </div>
                  </div>
                  <!-- 浏览数 -->
                  <div class="action-item view-count">
                    <van-icon
                      class="action-icon"
                      size="20"
                      name="eye-o"
                      color="#94a3b8"
                    />
                    <span class="action-count">{{ formatNumber(picture.viewCount) }}</span>
                  </div>
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
                      :color="picture.isShared === 1 ? '#60c3d5' : '#94a3b8'"
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
                  type="link"
                  v-if="canEdit"
                  class="edit-button"
                  @click="(e) => doEdit(picture, e)"
                >
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button type="link" class="search-button" @click="(e) => doSearch(picture, e)">
                  <template #icon><SearchOutlined /></template>
                  搜相似
                </a-button>
                <a-button
                  type="link"
                  v-if="canDelete"
                  class="delete-button"
                  @click="(e) => doDelete(picture, e)"
                >
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </div>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

    <!-- 分享模态框 -->
    <ShareModal ref="shareModalRef" :link="shareLink" :imageUrl="shareImage" />

    <!-- 评论抽屉 -->
    <a-drawer
      class="comments-drawer"
      v-model:open="visible"
      placement="bottom"
      title="评论"
      :footer="false"
      @cancel="closeModal"
      :height="'80vh'"
    >
      <!-- 添加宠物动画 -->
      <div class="pet-animation">
        <lottie-player
          :src="currentPet.url"
          background="transparent"
          speed="1"
          style="width: 120px; height: 120px;"
          ref="petAnimation"
          loop
          autoplay
        ></lottie-player>
      </div>

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
            v-model:value="commentContent"
            :placeholder="replyCommentId ? '写下你的回复...' : '写下你的评论...'"
            class="comment-input"
            :maxLength="200"
            @pressEnter="addComment"
          >
            <template #prefix v-if="replyCommentId">
              <MessageOutlined class="reply-icon" />
            </template>
            <template #suffix>
              <span class="word-count">{{ commentContent.length }}/200</span>
            </template>
          </a-input>

          <a-button
            type="primary"
            class="send-button"
            :class="{ 'reply-button': replyCommentId }"
            :disabled="!commentContent.trim()"
            @click="addComment"
          >
            {{ replyCommentId ? '回复' : '发送' }}
          </a-button>
        </div>

        <!-- 表情选择器 -->
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
import { useRouter } from 'vue-router'
import ShareModal from '@/components/ShareModal.vue'
import { h, onMounted, onUnmounted, reactive, ref, nextTick, computed } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  CloseOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SmileOutlined,
  MessageOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController.ts'
import { throttle } from 'vant/es/lazyload/vue-lazyload/util'
// import EmojiPicker from "vue3-emoji-picker";
import EmojiPicker from '@/components/EmojiPicker.vue'
import '@lottiefiles/lottie-player'

import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { doShareUsingPost } from '@/api/shareRecordController.ts'

const loginUserStore = useLoginUserStore()
const currPicture = ref<API.PictureVO>()
//回复评论id
const replyCommentId = ref<string>('')

onMounted(async () => {
  replyCommentId.value = ''
  currPicture.value = props.dataList[0]
  // console.log(props.dataList)
})
// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}
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

  // 更新输入框占位符
  const inputEl = document.querySelector('.comment-input') as HTMLInputElement
  if (inputEl) {
    nextTick(() => {
      inputEl.focus()
      // 滚动到输入框位置
      inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
    })
  }
}

// 评论
// 是否可见
const visible = ref(false)

// 评论数据
const comments = ref<API.CommentsQueryRequest[]>([])

// 存储用户输入的评论内容
const commentContent = ref('')

// 存储第一个评论列表数据
const firstcomment = ref<API.CommentsQueryRequest[]>([])

// 控制第一个评论列表的显示与隐藏
const showFirstComment = ref(false)

// 控制加载状态
const commentloading = ref(true)

// 使用 reactive 包裹 queryRequest 使其具有响应式
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: 0,
  targetType: 1,
  current: 1,
  pageSize: 15,
})

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
  queryRequest.targetId = picture.id
  e.stopPropagation()
  visible.value = true
  // 随机切换宠物
  currentPet.value = PETS[Math.floor(Math.random() * PETS.length)]
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
  commentContent.value = ''
  showFirstComment.value = false
  commentloading.value = false // 关闭加载状态
}

// 修改评论提交函数
const addComment = async () => {
  try {
    if (!commentContent.value.trim()) {
      message.warning('评论内容不能为空')
      return
    }

    if (!currPicture.value?.id) {
      message.error('图片信息获取失败')
      return
    }

    const requestBody: API.CommentsAddRequest = {
      targetId: currPicture.value.id,
      targetType: 1,
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    }

    const res = await addCommentUsingPost(requestBody)
    if (res.data.code === 0) {
      // message.success('评论成功')
      // 播放宠物庆祝动画
      if (petAnimation.value) {
        petAnimation.value.play()
      }
      // 清空输入内容和状态
      commentContent.value = ''
      replyCommentId.value = ''
      showEmojiPicker.value = false

      // 刷新评论列表
      queryRequest.current = 1
      page.value = 1
      isEndOfData.value = false
      await updateComments()
    } else {
      message.error('评论失败：' + res.data.message)
    }
  } catch (error) {
    console.error('评论失败:', error)
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
const shareLink = ref<string>('')
const shareImage = ref('')

// 处理分享
const doShare = async (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()

  // 如果已经分享过,则执行取消分享
  if (picture.isShared === 1) {
    try {
      const requestBody: API.ShareRequest = {
        targetId: picture.id,
        targetType: 1,
        isShared: false
      }
      const res = await doShareUsingPost(requestBody)
      if (res.data.code === 0) {
        picture.shareCount--
        picture.isShared = 0
      }
    } catch (error) {
      console.error('取消分享失败:', error)
      message.error('取消分享失败')
    }
    return
  }

  // 未分享过,显示分享模态框
  shareLink.value = window.location.origin + '/picture/' + picture.id
  shareImage.value = picture.url
  shareModalRef.value?.openModal()

  // 调用分享接口
  try {
    const requestBody: API.ShareRequest = {
      targetId: picture.id,
      targetType: 1,
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      picture.shareCount++
      picture.isShared = 1
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

// 取消回复
const cancelReply = () => {
  replyCommentId.value = ''
}

// 处理返回键
const handleBackButton = () => {
  if (visible.value) {
    visible.value = false
    return true
  }
  return false
}

// 监听返回键
onMounted(() => {
  window.addEventListener('popstate', () => {
    if (handleBackButton()) {
      // 阻止默认的返回行为
      history.pushState(null, '', document.URL)
    }
  })

  // 初始化时添加一个历史记录，用于触发 popstate
  history.pushState(null, '', document.URL)
})

// 清理监听器
onUnmounted(() => {
  window.removeEventListener('popstate', handleBackButton)
})

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

// 评论相关状态
// 表情选择器相关
const showEmojiPicker = ref(false)

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji
}

// 提交评论
const submitComment = async () => {
  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }

  try {
    const res = await addCommentUsingPost({
      content: commentContent.value,
      parentCommentId: replyToId.value || '0',
      pictureId: currPicture.value?.id,
      userId: loginUserStore.loginUser.id
    })

    if (res.data.code === 0) {
      message.success('评论成功')
      commentContent.value = ''
      showEmojiPicker.value = false
      replyToId.value = ''
      await updateComments()
    } else {
      message.error('评论失败：' + res.data.message)
    }
  } catch (error) {
    console.error('评论失败:', error)
    message.error('评论失败，请稍后重试')
  }
}

// 回车提交评论
const handleEnterPress = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submitComment()
  }
}

// 处理回复评论
const handleReply = (commentId: string) => {
  replyToId.value = commentId
  // 聚焦输入框
  const input = document.querySelector('.comment-input')
  if (input) {
    (input as HTMLElement).focus()
  }
}

// 暴露方法给父组件
defineExpose({
  handleReply
})

// 表情选择器国际化配置
const emojiI18n = {
  categories: {
    recent: '最近使用',
    smileys: '表情符号',
    people: '人物',
    animals: '动物与自然',
    food: '食物与饮料',
    activities: '活动',
    travel: '旅行与地点',
    objects: '物品',
    symbols: '符号',
    flags: '旗帜'
  },
  search: '搜索表情',
  clear: '清除',
  notFound: '未找到表情'
}

// 宠物动画列表
const PETS = [
  {
    name: 'dog',
    url: 'https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json'
  },
  {
    name: 'cat',
    url: 'https://assets2.lottiefiles.com/packages/lf20_bkqn2x.json'
  },
  {
    name: 'rabbit',
    url: 'https://assets8.lottiefiles.com/packages/lf20_GofK09iPAE.json'
  },
  {
    name: 'hamster',
    url: 'https://assets4.lottiefiles.com/packages/lf20_yriifcqx.json'
  },
  {
    name: 'bird',
    url: 'https://assets3.lottiefiles.com/private_files/lf30_d5nmlcv1.json'
  },
  {
    name: 'panda',
    url: 'https://assets9.lottiefiles.com/packages/lf20_swnrn2oy.json'
  },
  {
    name: 'penguin',
    url: 'https://assets10.lottiefiles.com/packages/lf20_dw8rzsix.json'
  },
  {
    name: 'fox',
    url: 'https://assets1.lottiefiles.com/packages/lf20_zw7jo1.json'
  }
]

// 当前宠物动画
const currentPet = ref(PETS[Math.floor(Math.random() * PETS.length)])

// 宠物动画相关
const petAnimation = ref(null)
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
  overflow: hidden;
  background: #f8fafc;
  border-radius: 16px 16px 0 0;

  img {
    width: 100%;
    display: block;
    transition: transform 0.3s ease;
  }
}

.interaction-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-count {
  cursor: default;
  &:hover {
    transform: none;
    .action-icon,
    .action-count {
      color: #94a3b8 !important;
    }
  }
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;

  &:hover:not(.view-count) {
    transform: translateY(-1px);

    .action-icon,
    .action-count {
      color: #ff8e53 !important;
    }
  }
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
  padding: 0 12px;
}

.edit-button {
  color: #ff8e53;
}

.search-button {
  color: #45b090;
}

.delete-button {
  color: #ff6b6b;
}

/* 评论抽屉样式 */
.comments-drawer {
  :deep(.ant-drawer-content) {
    border-radius: 16px 16px 0 0;
    background: #f8fafc;
  }

  :deep(.ant-drawer-header) {
    padding: 16px 24px;
    border-bottom: 1px solid rgba(255, 142, 83, 0.1);
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(8px);
    position: relative;

    .ant-drawer-title {
      font-weight: 500;
      color: #1a1a1a;
    }

    /* 增加选择器优先级 */
    :deep(.ant-drawer-close) {
      position: absolute;
      top: 12px;
      right: 12px;
      left: auto !important; /* 强制覆盖默认样式 */
      color: #94a3b8;
      padding: 8px;
      transition: all 0.3s ease;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.9);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
      z-index: 1008;
      margin: 0 !important;

      &:hover {
        color: #64748b;
        transform: rotate(90deg);
        background: rgba(255, 255, 255, 0.95);
      }

      &:active {
        transform: scale(0.9) rotate(90deg);
      }
    }
  }

  :deep(.ant-drawer-body) {
    padding: 0;
  }
}

.drawer-content {
  padding: 16px;
  overflow-y: auto;
  height: calc(100vh - 200px);
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
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px;
  background: white;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 1000;
}

.input-container {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  border-radius: 20px;
  padding: 4px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.emoji-trigger {
  padding: 8px;
  color: #94a3b8;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.picture-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.picture-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  &:hover {
    color: #ff8e53;
  }
}
.emoji-trigger.active {
  color: #ff8e53;
  transform: scale(1.1);
}

.comment-input {
  flex: 1;
  background: transparent;
  border: none;
  padding: 8px 0;
}

.submit-btn {
  height: 32px;
  border-radius: 16px;
  padding: 0 16px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  font-size: 14px;
}

/* 表情选择器容器样式 */
.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px 12px 0 0;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 自定义表情选择器样式 */
:deep(.emoji-picker) {
  width: 100%;
  border: none;
  padding: 12px;

  .emoji-picker__search {
    padding: 8px;
    background: #f8fafc;
    border-radius: 8px;
    margin-bottom: 8px;
  }

  .emoji-picker__category-name {
    font-size: 13px;
    padding: 8px;
    color: #94a3b8;
    font-weight: 500;
  }
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

/* 审核弹窗样式优化 */
.review-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
  }

  :deep(.ant-modal-header) {
    border-bottom: none;
    padding: 20px 24px 0;
  }

  :deep(.ant-modal-title) {
    font-size: 18px;
    font-weight: 600;
    text-align: center;
  }

  :deep(.ant-modal-body) {
    padding: 20px 24px 24px;
  }
}

.review-detail {
  text-align: center;
  padding: 24px;
  background: #f8fafc;
  border-radius: 12px;
}

.status-icon-large {
  font-size: 48px;
  margin-bottom: 20px;

  .pending {
    color: #1890ff;
    filter: drop-shadow(0 4px 8px rgba(24, 144, 255, 0.2));
  }

  .approved {
    color: #52c41a;
    filter: drop-shadow(0 4px 8px rgba(82, 196, 26, 0.2));
  }

  .rejected {
    color: #ff4d4f;
    filter: drop-shadow(0 4px 8px rgba(255, 77, 79, 0.2));
  }
}

.review-message {
  font-size: 16px;
  color: #1f2937;
  line-height: 1.6;
  padding: 0 16px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-modal {
    :deep(.ant-modal-content) {
      border-radius: 12px;
    }

    :deep(.ant-modal-header) {
      padding: 16px 20px 0;
    }

    :deep(.ant-modal-title) {
      font-size: 16px;
    }

    :deep(.ant-modal-body) {
      padding: 16px 20px 20px;
    }
  }

  .review-detail {
    padding: 20px;
  }

  .status-icon-large {
    font-size: 40px;
    margin-bottom: 16px;
  }

  .review-message {
    font-size: 14px;
    padding: 0 12px;
  }
}

.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  z-index: 1000;
  background: white;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
}

/* 自定义表情选择器样式 */
:deep(.emoji-picker) {
  width: 100%;
  border: none;

  .emoji-picker__search {
    padding: 8px;
    background: #f8fafc;
  }

  .emoji-picker__category-name {
    font-size: 13px;
    padding: 8px;
    color: #94a3b8;
  }
}

/* 优化表情选择器样式 */
.custom-emoji-picker {
  :deep(.emoji-picker) {
    width: 100%;
    border: none;
    padding: 16px;
    background: white;
    border-radius: 12px 12px 0 0;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);

    .emoji-picker__search {
      padding: 8px;
      background: #f8fafc;
      border-radius: 8px;
      margin-bottom: 12px;
    }

    .emoji-picker__category-name {
      font-size: 13px;
      padding: 8px;
      color: #64748b;
      font-weight: 500;
    }
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

/* 输入框聚焦状态 */
:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: #ff8e53 !important;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
}

.emoji-picker-container :deep(.v3-footer){
  display: none;
}

.emoji-picker-container :deep(.v3-body){
}

/* 添加宠物动画样式 */
.pet-animation {
  position: fixed;
  right: 20px;
  bottom: 100px;
  z-index: 1000;
  pointer-events: none;
  opacity: 0.9;
  transform: scale(0.8);
  transition: all 0.3s ease;
  animation: fadeIn 0.5s ease;
}

/* 当评论抽屉打开时的动画效果 */
.comments-drawer:deep(.ant-drawer-content-wrapper) {
  .pet-animation {
    animation: bounce 1s ease infinite;
  }
}

@keyframes bounce {
  0%, 100% {
    transform: scale(0.8) translateY(0);
  }
  50% {
    transform: scale(0.8) translateY(-10px);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.6);
  }
  to {
    opacity: 0.9;
    transform: scale(0.8);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .pet-animation {
    right: 10px;
    bottom: 80px;
    transform: scale(0.6);
  }
}
</style>

