<template>
  <div class="waterfall-picture-list">
    <!-- 根据设备类型显示不同布局 -->
    <template v-if="!isMobile">
      <!-- PC端瀑布流布局 -->
      <div class="masonry-wrapper">
        <div class="masonry-grid">
          <!-- 使用计算后的列数据进行渲染 -->
          <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
            <div
              v-for="picture in column"
              :key="picture.id"
              class="masonry-item"
              @click="doClickPicture(picture)"
            >
              <div class="image-wrapper">
                <div
                  class="aspect-ratio-box"
                  :style="{ paddingTop: `${(1 / (picture.picScale || 1)) * 100}%` }"
                >
                  <img
                    :alt="picture.name"
                    class="masonry-image"
                    :src="picture.thumbnailUrl || picture.url"
                    @load="handleImageLoad"
                    @error="handleImageError(picture)"
                  />
                </div>
              </div>
              <div class="picture-info">
                <div class="picture-header">
                  <div class="picture-user" @click.stop="handleUserClick(picture.user)">
                    <a-avatar class="user-avatar" :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"/>
                    <span>{{ picture.user?.userName }}</span>
                  </div>
                </div>
                <div class="picture-title">{{ picture.name }}</div>
                <div class="picture-actions">
                  <div class="action-item view-count">
                    <EyeOutlined />
                    <span>{{ formatNumber(picture.viewCount) }}</span>
                  </div>
                  <div class="action-item" @click.stop="(e) => doLike(picture, e)">
                    <LikeOutlined :class="{ active: picture.isLiked === 1 }" />
                    <span>{{ formatNumber(picture.likeCount) }}</span>
                  </div>
                  <div class="action-item" @click.stop="(e) => doComments(picture, e)">
                    <CommentOutlined />
                    <span>{{ formatNumber(picture.commentCount) }}</span>
                  </div>
                  <div class="action-item" @click.stop="(e) => doShare(picture, e)">
                    <ShareAltOutlined :class="{ active: picture.isShared === 1 }" />
                    <span>{{ formatNumber(picture.shareCount) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-wrapper">
          <a-spin>
            <template #indicator>
              <LoadingOutlined style="font-size: 24px; color: #ff8e53;" spin />
            </template>
          </a-spin>
        </div>
        <!-- 没有更多数据 -->
        <div v-if="isEndOfData" class="no-more-data">
          <span>没有更多了</span>
        </div>
      </div>
    </template>

    <!-- 分享模态框 -->
    <ShareModal ref="shareModalRef" :link="shareLink" :imageUrl="shareImage" />

    <!-- 评论抽屉 -->
    <a-drawer
      class="comments-drawer"
      v-model:open="visible"
      placement="right"
      title="评论"
      :footer="false"
      @cancel="closeModal"
      :height="'80vh'"
    >
      <!-- 修改宠物动画 -->
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
      <div class="drawer-content" ref="scrollContainer" >
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
import { h, onMounted, onUnmounted, reactive, ref, nextTick, computed, watch } from 'vue'
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
  LikeOutlined,
  CommentOutlined,
  ShareAltOutlined,
  EyeOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController.ts'
import { throttle } from 'vant/es/lazyload/vue-lazyload/util'
// import EmojiPicker from "vue3-emoji-picker";
import EmojiPicker from '@/components/EmojiPicker.vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
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
  // 禁用页面初始滚动
  document.body.style.overflowY = 'hidden'

  // 等待图片初始化完成后再启用滚动
  nextTick(() => {
    setTimeout(() => {
      document.body.style.overflowY = 'auto'
      window.addEventListener('scroll', handleWindowScroll)
    }, 500)  // 给予足够的时间让图片加载和布局稳定
  })
})

interface Props {
  dataList: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onLoadMore?: (page: number) => Promise<boolean>
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
  canEdit: false,
  canDelete: false,
  onLoadMore: undefined
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

// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
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
      message.success('评论成功')
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

// 新增用于移动端分页的页码变量
const page = ref(1)
const isEndOfData = ref(false)
const isLoading = ref(false)

// 预加载阈值和节流时间
const SCROLL_THRESHOLD = 2000  // 更早开始预加载
const THROTTLE_TIME = 500  // 减少节流时间，使滚动更平滑

// 处理滚动事件
const handleWindowScroll = throttle(() => {
  if (isLoading.value || isEndOfData.value || !props.onLoadMore) return

  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 提前更多距离开始加载下一页
  if (documentHeight - scrollTop - windowHeight < SCROLL_THRESHOLD) {
    loadMore()
  }
}, THROTTLE_TIME, { leading: true, trailing: true })

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

// 添加宠物动画相关逻辑
const petAnimation = ref(null)

// 修改图片加载处理函数
const handleImageLoad = (event: Event) => {
  const imgElement = event.target as HTMLImageElement
  if (imgElement) {
    // 设置一个短暂的延时，确保图片完全加载
    setTimeout(() => {
      imgElement.style.opacity = '1'
    }, 50)
  }
}

// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    name: '图片详情',
    params: {
      id: picture.id
    }
  })
}

// 修改图片错误处理函数
const handleImageError = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  if (imgElement && picture.url) {
    imgElement.src = picture.url
  }
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
// 用于存储每个分享按钮的颜色，以图片id作为键
const shareButtonColor = ref<{ [key: string]: string }>({})

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
  window.removeEventListener('scroll', handleWindowScroll)
  document.body.style.overflowY = 'auto'
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

// 判断是否为移动端
const isMobile = ref(getDeviceType() === DEVICE_TYPE_ENUM.MOBILE)

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

// 滚动加载相关变量
const currentPage = ref(1)

// 加载更多数据
const loadMore = async () => {
  if (isLoading.value || isEndOfData.value || !props.onLoadMore) return
  isLoading.value = true

  const startTime = Date.now()

  try {
    const hasMore = await props.onLoadMore(currentPage.value + 1)
    if (hasMore) {
      currentPage.value++
    } else {
      isEndOfData.value = true
    }
  } catch (error) {
    console.error('加载更多数据失败:', error)
  } finally {
    // 确保加载状态至少显示一定时间
    const loadTime = Date.now() - startTime
    const minLoadTime = 300  // 减少最小加载时间
    if (loadTime < minLoadTime) {
      await new Promise(resolve => setTimeout(resolve, minLoadTime - loadTime))
    }
    isLoading.value = false
  }
}

// 监听滚动事件
onMounted(() => {
  window.addEventListener('scroll', handleWindowScroll)
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowScroll)
})

// 监听数据列表变化，重置状态
watch(() => props.dataList, (newVal, oldVal) => {
  // 只在数据完全重置时重置状态
  if (oldVal?.length && newVal.length === 0) {
    currentPage.value = 1
    isEndOfData.value = false
    isLoading.value = false
    // 重置时也暂时禁用滚动
    document.body.style.overflowY = 'hidden'
    nextTick(() => {
      setTimeout(() => {
        document.body.style.overflowY = 'auto'
      }, 300)
    })
  }
})

// 计算列数
const getColumnCount = () => {
  const width = window.innerWidth
  if (width > 1920) return 8
  if (width > 1600) return 7
  if (width > 1400) return 6
  if (width > 1200) return 5
  if (width > 900) return 4
  return 3
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: API.PictureVO[][] = Array.from({ length: columnCount }, () => [])

  // 按照从左到右、从上到下的顺序分配数据
  props.dataList.forEach((item, index) => {
    const columnIndex = index % columnCount
    cols[columnIndex].push(item)
  })

  return cols
})

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = throttle(() => {
  // 触发重新计算列
  columns.value = computed(() => {
    const columnCount = getColumnCount()
    const cols: API.PictureVO[][] = Array.from({ length: columnCount }, () => [])

    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })

    return cols
  }).value
}, 200)
</script>

<style scoped>
.waterfall-picture-list {
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
  overflow: hidden;
  border-radius: 12px 12px 0 0;
  background: #f5f5f5;
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  background: #f5f5f5;
}

.masonry-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
  will-change: opacity;
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
  min-height: calc(100% - 80px); /* 减去输入框的高度 */
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

/* PC端瀑布流样式 */
.masonry-wrapper {
  width: 100%;
  min-height: 100vh;
  padding: 0;
}

.masonry-grid {
  display: flex;
  gap: 12px;
  width: 100%;
  padding: 0;
  margin: 0;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.masonry-item {
  width: 100%;
  margin: 0;
  break-inside: avoid;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
  background: #f5f5f5;
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  background: #f5f5f5;
}

.masonry-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
  will-change: opacity;
}

.picture-info {
  padding: 12px;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.03) 0%,
    rgba(0, 0, 0, 0) 100%
  );
}

.picture-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.picture-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
  cursor: pointer;

  &:hover {
    color: #ff8e53;
  }
}

.picture-title {
  font-size: 14px;
  color: #333;
  margin: 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.picture-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #ff8e53;
  }

  .anticon {
    font-size: 16px;
  }

  span {
    font-size: 12px;
  }

  .active {
    color: #ff8e53;
  }
}

.action-item.view-count {
  cursor: default;
  &:hover {
    color: #666;
    transform: none;
  }
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  width: 100%;
  background: transparent;
}

.loading-text {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}

/* 优化加载动画样式 */
:deep(.ant-spin) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

:deep(.ant-spin-dot) {
  font-size: 24px;
}

/* 优化宠物动画样式 */
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

/* 添加淡入动画 */
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

