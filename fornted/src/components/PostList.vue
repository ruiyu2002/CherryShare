<template>
  <div class="post-list">
    <div class="masonry-wrapper">
      <div class="masonry-grid">
        <!-- 使用计算后的列数据进行渲染 -->
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
          <div
            v-for="post in column"
            :key="post.id"
            class="masonry-item"
            @click="handlePostClick(post)"
          >
            <!-- 添加状态标签 -->
            <div v-if="props.showStatus" class="post-status" @click.stop="handleStatusClick(post)">
              <a-tag v-if="post.status === 0" color="orange">待审核</a-tag>
              <a-tag v-else-if="post.status === 2" color="red" class="reject-tag">已拒绝</a-tag>
            </div>

            <!-- 封面图片 -->
            <div class="image-wrapper">
              <div class="aspect-ratio-box">
                <img
                  :src="getPostCoverImage(post)"
                  :alt="post.title"
                  class="masonry-image"
                  @load="handleImageLoad"
                  @error="handleImageError"
                />
              </div>
            </div>

            <!-- 帖子信息 -->
            <div class="post-info">
              <div class="post-header">
                <div class="post-title">{{ post.title }}</div>
                <div class="category-tags">
                  <span v-if="post.category" class="category-tag">{{ post.category }}</span>
                </div>
              </div>
              <div class="post-meta">
                <div class="author-info">
                  <a-avatar :size="24" :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName)" />
                  <span class="author-name">{{ post.user?.userName }}</span>
                </div>
                <div class="post-time">{{ formatTime(post.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加拒绝原因弹框 -->
    <a-modal
      v-model:open="showRejectModal"
      :title="null"
      :footer="null"
      :closable="false"
      class="reject-modal"
      width="320px"
    >
      <div class="reject-content">
        <CloseCircleOutlined class="reject-icon" />
        <h3 class="reject-title">审核未通过</h3>
        <p class="reject-message">{{ currentPost?.reviewMessage || '内容不符合规范' }}</p>
        <a-button type="primary" @click="showRejectModal = false">
          我知道了
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { LikeOutlined, EyeOutlined, CommentOutlined, FileTextOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { likePostUsingPost } from '@/api/postController'
import { getPostCoverImage, getRandomCoverImage } from '@/utils/imageUtils'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { throttle } from 'lodash'

const router = useRouter()

interface Props {
  dataList?: API.Post[]
  loading?: boolean
  showStatus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showStatus: false
})

const emit = defineEmits(['update:dataList'])

// 格式化数字
const formatNumber = (num: number): string => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 点击帖子
const handlePostClick = (post: API.Post) => {
  if (!post?.id) {
    message.error('无效的帖子')
    return
  }
  router.push({
    name: 'postDetail',
    params: { id: post.id }
  })
}

// 点赞
const handleLike = async (post: API.Post) => {
  try {
    await likePostUsingPost(post.id)
    post.isLiked = post.isLiked === 1 ? 0 : 1
    post.likeCount = (post.likeCount || 0) + (post.isLiked ? 1 : -1)
  } catch (error: any) {
    message.error('点赞失败：' + error.message)
  }
}

// 处理图片加载
const handleImageLoad = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.opacity = '1'
}

// 处理图片加载失败
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = getRandomCoverImage() // 加载失败时使用另一张随机图片
}

// 计算列数
const getColumnCount = () => {
  const width = window.innerWidth
  if (width > 1920) return 6
  if (width > 1600) return 5
  if (width > 1400) return 4
  if (width > 1200) return 3
  if (width > 900) return 2
  return 1
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: API.Post[][] = Array.from({ length: columnCount }, () => [])

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
    const cols: API.Post[][] = Array.from({ length: columnCount }, () => [])

    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })

    return cols
  }).value
}, 200)

// 添加状态相关变量
const showRejectModal = ref(false)
const currentPost = ref<API.Post | null>(null)

// 添加状态点击处理函数
const handleStatusClick = (post: API.Post) => {
  if (post.status === 2) { // 2 表示已拒绝
    currentPost.value = post
    showRejectModal.value = true
  }
}
</script>

<style scoped>
.post-list {
  width: 100%;
  padding: 4px;
  margin: 0 auto;
}

.masonry-wrapper {
  width: 100%;
  min-height: 100vh;
  padding: 0;
}

.masonry-grid {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 1800px;
  margin: 0 auto;
  padding: 0 16px;
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
  background: #f5f5f5;
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  padding-top: 66.67%; /* 3:2 比例 */
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
}

.post-info {
  padding: 12px;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.03) 0%,
    rgba(0, 0, 0, 0) 100%
  );
}

.post-header {
  margin-bottom: 8px;
}

.post-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.category-tags {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
}

.category-tag {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .author-name {
    font-size: 14px;
    color: #1f2937;
  }
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .masonry-grid {
    padding: 0 8px;
    gap: 8px;
  }

  .masonry-column {
    gap: 8px;
  }

  .post-info {
    padding: 8px;
  }

  .post-title {
    font-size: 14px;
  }
}

@media screen and (min-width: 1600px) {
  .masonry-grid {
    max-width: 2000px;
  }
}

/* 添加状态标签样式 */
.post-status {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.reject-tag {
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(255, 77, 79, 0.2);
  }
}

/* 添加拒绝原因弹框样式 */
.reject-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
    background: white;
  }
}

.reject-content {
  padding: 24px;
  text-align: center;
}

.reject-icon {
  font-size: 48px;
  color: #ff4d4f;
  margin-bottom: 16px;
}

.reject-title {
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.reject-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
  padding: 0 16px;
  line-height: 1.6;
}

.reject-content .ant-btn {
  min-width: 120px;
  height: 36px;
  border-radius: 18px;
  font-weight: 500;
  background: #ff4d4f;
  border-color: #ff4d4f;

  &:hover {
    background: #ff7875;
    border-color: #ff7875;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
  }

  &:active {
    background: #f5222d;
    border-color: #f5222d;
    transform: translateY(0);
  }
}
</style>
