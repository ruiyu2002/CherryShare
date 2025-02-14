<template>
  <div id="postDetailPage" v-show="mounted" :class="{ mounted }">
    <a-row :gutter="[16, 16]">
      <!-- 帖子内容 -->
      <a-col :sm="24" :md="16" :xl="18" class="content-col">
        <a-card class="content-card" :bordered="false">
          <!-- 帖子标题 -->
          <template #title>
            <div class="post-header">
              <h1 class="post-title">{{ post.title }}</h1>
              <div class="post-meta">
                <a-tag v-if="post.status === 0" color="orange">待审核</a-tag>
                <a-tag v-else-if="post.status === 2" color="red">已拒绝</a-tag>
              </div>
            </div>
          </template>

          <!-- 帖子内容 -->
          <markdown-content :content="post.content" class="post-content" />
        </a-card>
      </a-col>

      <!-- 作者信息和互动区域 -->
      <a-col :sm="24" :md="8" :xl="6" class="info-col">
        <a-card class="info-card" :bordered="false">
          <!-- 作者信息 -->
          <a-descriptions :column="1" class="info-descriptions">
            <a-descriptions-item label="作者" class="author-item">
              <a-space>
                <a-avatar
                  :size="28"
                  @click="handleUserClick(post.user)"
                  :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName)"
                />
                <div class="author-name">{{ post.user?.userName }}</div>
                <a-button
                  v-if="post.user?.id !== loginUserId"
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
            <a-descriptions-item label="发布时间">
              {{ formatTime(post.createTime) }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 互动数据 -->
          <div class="interaction-stats">
            <div class="stat-item" @click="handleLike">
              <LikeOutlined :class="{ 'active': post.isLiked === 1 }" />
              <span class="stat-count">{{ post.likeCount || 0 }}</span>
              <span class="stat-label">点赞</span>
            </div>
            <div class="stat-item">
              <EyeOutlined />
              <span class="stat-count">{{ post.viewCount || 0 }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item" @click="visible = true">
              <CommentOutlined />
              <span class="stat-count">{{ post.commentCount || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
            <div class="stat-item" @click="handleShare">
              <ShareAltOutlined :class="{ 'active': post.isShared === 1 }" />
              <span class="stat-count">{{ post.shareCount || 0 }}</span>
              <span class="stat-label">分享</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons" v-if="canEdit || canDelete">
            <a-button type="primary" @click="handleEdit" v-if="canEdit" class="edit-button">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-button @click="showDeleteConfirm" v-if="canDelete" class="delete-button">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 添加分享模态框 -->
    <ShareModal
      ref="shareModalRef"
      :link="shareLink"
      :imageUrl="shareImage"
      :title="post.title"
      @share-success="handleShareSuccess"
    />

    <!-- 添加评论抽屉 -->
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

      <div class="drawer-content" ref="scrollContainer" @scroll="handleScroll">
        <div class="comments-area" @click="cancelReply">
          <!-- 加载中状态 -->
          <div v-if="commentloading" class="loading-container">
            <a-spin tip="加载评论中..." />
          </div>

          <!-- 评论列表 -->
          <template v-else>
            <CommentList
              :comments="comments"
              @reply-clicked="handleReplyClick"
              @update-comments="queryComments"
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
        <div v-if="showEmojiPicker" class="emoji-picker-container">
          <EmojiPicker
            @select="onEmojiSelect"
            :i18n="emojiI18n"
            class="custom-emoji-picker"
          />
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { Image } from 'ant-design-vue'
import { LikeOutlined, EyeOutlined, CommentOutlined, EditOutlined, DeleteOutlined, ShareAltOutlined, SmileOutlined, MessageOutlined, ArrowRightOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { getPostByIdUsingGet, deletePostUsingPost } from '@/api/postController'
import { addUserFollowsUsingPost, findIsFollowUsingPost } from '@/api/userFollowsController'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ShareModal from '@/components/ShareModal.vue'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import { doLikeUsingPost } from '@/api/likeRecordController'
import { doShareUsingPost } from '@/api/shareRecordController'
import EmojiPicker from '@/components/EmojiPicker.vue'
import '@lottiefiles/lottie-player'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const mounted = ref(false)
const post = ref<API.Post>({} as API.Post)
const followLoading = ref(false)
const isFollowed = ref(false)

// 添加全局预览方法
window.$previewImage = (options: any) => {
  const { src } = options
  Image.preview({
    src,
    maskClosable: true,
    icons: {
      close: true,
    }
  })
}

// 权限判断
const loginUserId = computed(() => loginUserStore.loginUser?.id)
const canEdit = computed(() => {
  return post.value.userId === loginUserId.value ||
    loginUserStore.loginUser?.userRole === 'admin'
})
const canDelete = computed(() => canEdit.value)

// 检查是否已关注
const checkIsFollowed = async () => {
  if (!loginUserStore.loginUser?.id || !post.value?.user?.id) {
    return
  }
  try {
    const res = await findIsFollowUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: post.value.user.id
    })
    if (res.data?.data) {
      isFollowed.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 获取帖子详情
const fetchPostDetail = async () => {
  const id = route.params.id
  if (!id || typeof id !== 'string') {
    // message.error('无效的帖子ID')
    router.push('/forum')
    return
  }

  try {
    const res = await getPostByIdUsingGet({ id })
    if (res.data?.data) {
      post.value = res.data.data
      mounted.value = true
      await checkIsFollowed()
    }
  } catch (error: any) {
    // message.error('获取帖子详情失败：' + error.message)
    router.push('/forum')
  }
}

// 修改点赞处理函数
const handleLike = async () => {
  try {
    const requestBody: API.LikeRequest = {
      targetId: post.value.id,
      targetType: 2, // 2 表示帖子类型
      isLiked: post.value.isLiked !== 1
    }

    const res = await doLikeUsingPost(requestBody)
    if (res.data.code === 0) {
      // 更新前端数据
      if (requestBody.isLiked) {
        post.value.likeCount = String(Number(post.value.likeCount || 0) + 1)
        post.value.isLiked = 1
      } else {
        post.value.likeCount = String(Number(post.value.likeCount || 0) - 1)
        post.value.isLiked = 0
      }
    }
  } catch (error) {
    // console.error('点赞失败:', error)
    // message.error('点赞失败')
  }
}

// 关注作者
const handleFollow = async () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  followLoading.value = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: post.value.user.id,
      followStatus: isFollowed.value ? 0 : 1
    })

    if (res.data?.code === 0) {
      isFollowed.value = !isFollowed.value
      // message.success(isFollowed.value ? '关注成功' : '取消关注成功')
    } else {
      // message.error('操作失败')
    }
  } catch (error) {
    // message.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 编辑帖子
const handleEdit = () => {
  router.push({
    path: `/post/edit/${post.value.id}`,
    query: {
      post: JSON.stringify({
        id: post.value.id,
        title: post.value.title,
        content: post.value.content,
        category: post.value.category,
        attachments: post.value.attachments
      })
    }
  })
}

// 删除帖子
const showDeleteConfirm = () => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，确定要删除这篇帖子吗？',
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await deletePostUsingPost({ id: post.value.id })
        if (res.data?.code === 0) {
          // message.success('删除成功')
          router.push('/forum')
        } else {
          // message.error('删除失败')
        }
      } catch (error: any) {
        // message.error('删除失败：' + error.message)
      }
    },
  })
}

// 添加评论相关状态
const visible = ref(false)
const comments = ref<API.Comment[]>([])
const commentContent = ref('')
const replyCommentId = ref('')
const commentloading = ref(false)
const showEmojiPicker = ref(false)
const isEndOfData = ref(false)

// 查询评论请求对象
const queryRequest = reactive<API.CommentsQueryRequest>({
  targetId: 0,
  targetType: 2, // 2 表示帖子类型
  current: 1,
  pageSize: 15,
})

// 查询评论
const queryComments = async () => {
  try {
    commentloading.value = true
    const res = await queryCommentUsingPost(queryRequest)
    if (res.data.data != null) {
      comments.value = res.data.data.records.map(comment => ({
        ...comment,
        commentId: comment.commentId?.toString(),
        parentCommentId: comment.parentCommentId?.toString(),
      }))
      isEndOfData.value = comments.value.length < queryRequest.pageSize
    } else {
      comments.value = []
      isEndOfData.value = true
    }
  } catch (error) {
    // console.error('查询评论异常', error)
    // message.error('获取评论失败')
  } finally {
    commentloading.value = false
  }
}

// 添加评论
const addComment = async () => {
  if (!commentContent.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }

  try {
    const requestBody: API.CommentsAddRequest = {
      targetId: post.value.id,
      targetType: 2,
      content: commentContent.value.trim(),
      parentCommentId: replyCommentId.value || '0'
    }

    const res = await addCommentUsingPost(requestBody)
    if (res.data.code === 0) {
      // message.success('评论成功')
      commentContent.value = ''
      replyCommentId.value = ''
      // 刷新评论列表
      queryRequest.current = 1
      await queryComments()
      // 更新评论数
      post.value.commentCount = String(Number(post.value.commentCount || 0) + 1)
    }
  } catch (error) {
    // console.error('评论失败:', error)
    // message.error('评论失败')
  }
}

// 修改分享处理函数
const handleShare = async () => {
  // 如果已经分享过,则执行取消分享
  if (post.value.isShared === 1) {
    try {
      const requestBody: API.ShareRequest = {
        targetId: post.value.id,
        targetType: 2, // 2 表示帖子类型
        isShared: false
      }
      const res = await doShareUsingPost(requestBody)
      if (res.data.code === 0) {
        post.value.shareCount = String(Number(post.value.shareCount || 0) - 1)
        post.value.isShared = 0
        // message.success('取消分享成功')
      }
    } catch (error) {
      // console.error('取消分享失败:', error)
      // message.error('取消分享失败')
    }
    return
  }

  // 未分享过,显示分享模态框并调用分享接口
  try {
    const requestBody: API.ShareRequest = {
      targetId: post.value.id,
      targetType: 2,
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)
      post.value.isShared = 1
      // 成功后再显示分享模态框
      shareModalRef.value?.openModal()
    }
  } catch (error) {
    // console.error('分享失败:', error)
    // message.error('分享失败')
  }
}

// 处理分享成功
const handleShareSuccess = async () => {
  try {
    const requestBody: API.ShareRequest = {
      targetId: post.value.id,
      targetType: 2, // 2 表示帖子类型
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      post.value.shareCount = String(Number(post.value.shareCount || 0) + 1)
      post.value.isShared = 1
      // message.success('分享成功')
    }
  } catch (error) {
    // console.error('分享失败:', error)
    // message.error('分享失败')
  }
}

// 分享相关
const shareModalRef = ref()
const shareLink = computed(() => window.location.origin + '/post/' + post.value?.id)
const shareImage = computed(() => {
  // 如果有附件图片就用第一张，否则用默认图片
  return post.value?.attachments?.[0]?.url || '/default-share-image.png'
})

// 添加宠物动画相关代码
const PETS = [
  {
    name: 'dog',
    url: 'https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json'
  },
  {
    name: 'cat',
    url: 'https://assets2.lottiefiles.com/packages/lf20_bkqn2x.json'
  },
  // ... 其他宠物
]

const currentPet = ref(PETS[Math.floor(Math.random() * PETS.length)])
const petAnimation = ref(null)

// 添加表情相关代码
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const onEmojiSelect = (emoji: string) => {
  commentContent.value += emoji
}

// 添加评论相关方法
const handleReplyClick = (commentId: string) => {
  replyCommentId.value = commentId
  nextTick(() => {
    const inputEl = document.querySelector('.comment-input') as HTMLInputElement
    if (inputEl) {
      inputEl.focus()
      inputEl.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

const cancelReply = () => {
  replyCommentId.value = ''
}

// 添加表情选择器国际化配置
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

// 关闭弹窗
const closeModal = () => {
  replyCommentId.value = ''
  visible.value = false
  commentContent.value = ''
  showEmojiPicker.value = false
}

// 添加滚动处理函数
const handleScroll = () => {
  // 实现滚动加载更多的逻辑
  const container = scrollContainer.value
  if (!container) return

  const { scrollTop, clientHeight, scrollHeight } = container
  if (scrollTop + clientHeight >= scrollHeight - 50 && !commentloading.value && !isEndOfData.value) {
    // 加载更多评论
    loadMoreComments()
  }
}

// 加载更多评论的函数
const loadMoreComments = async () => {
  if (commentloading.value || isEndOfData.value) return

  queryRequest.current++
  await queryComments()
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

onMounted(() => {
  fetchPostDetail().then(() => {
    if (post.value.id) {
      queryRequest.targetId = post.value.id
      queryComments()
    }
  })
})
</script>

<style scoped>
#postDetailPage {
  min-height: calc(100vh - 50px);
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
  opacity: 0;
  transition: opacity 0.3s ease;
  @media screen and (min-width: 769px) {
    padding: 20px;
  }

  &.mounted {
    opacity: 1;
  }
}

.content-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  @media screen and (max-width: 768px) {
    margin: 0 -20px;
    border-radius: 0;
  }
}

.post-header {
  padding: 16px 20px;

  .post-title {
    font-size: 24px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 12px;
  }

  .post-meta {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  @media screen and (max-width: 768px) {
    padding: 12px 16px;

    .post-title {
      font-size: 20px;
      margin-bottom: 8px;
    }
  }
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px 20px;
  margin-left: -30px;
  margin-right: -30px;
  overflow: hidden;


  .image-container {
    margin: 16px auto;
    max-width: 800px;
    width: 100%;
    text-align: center;
    background: #f8fafc;
    border-radius: 8px;
    padding: 8px;
  }

  .content-image {
    max-width: 100%;
    height: auto;
    display: inline-block;
    border-radius: 4px;
    cursor: pointer;
    transition: transform 0.3s ease;
    vertical-align: middle;

    &:hover {
      transform: scale(1.02);
    }
  }

  :deep(p) {
    margin: 16px 0;
  }

  @media screen and (max-width: 768px) {
    padding: 12px 16px;
    font-size: 15px;

    .image-container {
      margin: 12px -16px;
      border-radius: 0;
      background: none;
      padding: 0;
    }
  }
}

.info-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  @media screen and (max-width: 768px) {
    margin: 0 -20px;
    border-radius: 0;
  }
}

.author-item {
  .author-name {
    font-weight: 500;
    color: #1a1a1a;
  }

  .follow-button {
    border-radius: 16px;
    height: 32px;
    padding: 0 16px;
  }
}

.interaction-stats {
  display: flex;
  justify-content: space-around;
  padding: 24px 0;
  border-top: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  margin: 16px 0;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }

    .anticon {
      font-size: 24px;
      color: #94a3b8;

      &.active {
        color: #ff4d4f;

        &.anticon-share-alt {
          color: #60c3d5;
        }
      }
    }

    .stat-count {
      font-size: 16px;
      font-weight: 500;
      color: #1a1a1a;
    }

    .stat-label {
      font-size: 12px;
      color: #64748b;
    }
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;

  .ant-btn {
    flex: 1;
    height: 40px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .anticon {
      font-size: 16px;
      margin-right: 6px;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &:active {
      transform: translateY(1px);
    }
  }

  .edit-button {
    background: linear-gradient(135deg, #60c3d5 0%, #4c9aff 100%);
    border: none;
    color: white;

    &:hover {
      background: linear-gradient(135deg, #4c9aff 0%, #60c3d5 100%);
    }
  }

  .delete-button {
    background: white;
    border: 1px solid #ff4d4f;
    color: #ff4d4f;

    &:hover {
      background: #fff1f0;
      color: #ff4d4f;
      border-color: #ff4d4f;
    }

    .anticon {
      color: #ff4d4f;
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .post-header .post-title {
    font-size: 20px;
  }

  .post-content {
    font-size: 15px;
  }

  .interaction-stats .stat-item {
    .anticon {
      font-size: 20px;
    }
    .stat-count {
      font-size: 14px;
    }
  }

  .action-buttons {
    margin: 16px -16px -16px;
    padding: 12px 16px;
    border-radius: 0;
    border-left: none;
    border-right: none;
    border-bottom: none;
    background: #ffffff;

    .ant-btn {
      height: 36px;
      font-size: 13px;

      .anticon {
        font-size: 15px;
      }
    }
  }
}

/* 在大屏幕上限制图片容器的最大宽度 */
@media screen and (min-width: 768px) {
  .post-content {
    .image-container {
      max-width: 800px;
    }
  }
}

/* 在移动设备上优化显示 */
@media screen and (max-width: 767px) {
  :deep(.ant-row) {
    margin: 0 !important;
  }

  :deep(.ant-col) {
    padding: 0 !important;
  }

  .post-content {
    .image-container {
      margin: 12px -16px;
    }

    .content-image {
      :deep(.ant-image-img) {
        border-radius: 4px;
      }
    }
  }

  .info-card {
    :deep(.ant-card-body) {
      padding: 16px;
    }
  }

  .interaction-stats {
    margin: 12px -16px;
    padding: 16px;
  }

  .action-buttons {
    padding: 0 16px 16px;
    margin-top: 0;
  }
}

/* 复制 MobilePictureList.vue 中的评论相关样式 */
.comments-drawer {
  :deep(.ant-drawer-content) {
    border-radius: 16px 16px 0 0;
    background: #f8fafc;
  }

  :deep(.ant-drawer-header) {
    padding: 16px;
    border-bottom: 1px solid #e5e7eb;
  }

  :deep(.ant-drawer-body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    height: 100%;
  }
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 120px;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.no-more-data {
  text-align: center;
  color: #94a3b8;
  padding: 16px 0;
  font-size: 13px;
}

.comment-input-wrapper {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.05);
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

  .emoji-trigger {
    font-size: 20px;
    color: #94a3b8;
    cursor: pointer;
    padding: 8px;
    transition: all 0.3s ease;

    &:hover {
      color: #ff8e53;
    }

    &.active {
      color: #ff8e53;
    }
  }
}

.input-area.is-replying {
  background: #fff6f3;
}

.reply-icon {
  color: #ff8e53;
  margin-right: 4px;
}

.comment-input {
  flex: 1;
  border-radius: 18px;
  background: #f8fafc;
  transition: all 0.3s ease;

  :deep(.ant-input) {
    background: transparent;
    padding: 8px 16px;
    font-size: 14px;

    &::placeholder {
      color: #94a3b8;
    }
  }

  &:hover {
    background: #f1f5f9;
  }

  &:focus-within {
    background: white;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
  }
}

.word-count {
  font-size: 12px;
  color: #94a3b8;
  margin-right: 8px;
}

.send-button {
  min-width: 64px;
  height: 36px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.2);

  &:not(:disabled):hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
    background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  }

  &:not(:disabled):active {
    transform: translateY(1px);
  }

  &:disabled {
    opacity: 0.6;
    background: #e2e8f0;
    color: #94a3b8;
    box-shadow: none;
  }
}

.reply-button {
  background: linear-gradient(135deg, #ff9c6e 0%, #ff8e53 100%);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);

  &:not(:disabled):hover {
    background: linear-gradient(135deg, #ff8e53 0%, #ff9c6e 100%);
  }
}

/* 宠物动画样式 */
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
</style>
