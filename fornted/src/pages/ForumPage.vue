<template>
  <div id="forumPage">
    <!-- PC端界面 -->
    <template v-if="device === DEVICE_TYPE_ENUM.PC">
      <div class="pc-container" @scroll="handleScroll" ref="pcContainer">
        <div class="pc-content-wrapper">
          <!-- PC端导航 -->
          <div class="pc-nav">
            <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
              <a-tab-pane key="all" tab="发现" />
              <a-tab-pane key="following" tab="关注" />
            </a-tabs>
          </div>

          <!-- PC端分类 -->
          <div class="pc-category-nav" v-if="activeTab === 'all'">
            <div
              class="pc-category-item"
              @click="handleCategoryChange('all')"
            >
              <span>全部</span>
            </div>
            <div
              v-for="category in categories"
              :key="category"
              class="pc-category-item"
              :class="{ active: selectedCategory === category }"
              @click="handleCategoryChange(category)"
            >
              <span>{{ category }}</span>
            </div>
          </div>

          <!-- PC端帖子列表 -->
          <div class="pc-content">
            <!-- 关注页面空状态 -->
            <div v-if="activeTab === 'following' && posts.length === 0 && !loading" class="empty-following">
              <a-empty description="暂无关注内容">
                <template #description>
                  <p class="empty-desc">关注感兴趣的创作者，获取第一手帖子更新</p>
                </template>
                <a-button type="primary" @click="activeTab = 'all'">去发现</a-button>
              </a-empty>
            </div>

            <!-- 帖子列表 -->
            <post-list
              v-else
              class="post-list-container"
              :loading="loading"
              :data-list="posts"
            />

            <!-- 添加加载状态和没有更多数据提示 -->
            <div v-if="loading && !isRefreshing && posts.length > 0" class="loading-more">
              <a-spin />
              <span>加载中...</span>
            </div>
            <div v-if="!hasMore && posts.length > 0" class="no-more-data">
              没有更多数据了~
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 移动端界面 -->
    <div v-else class="mobile-container">
      <van-pull-refresh
        v-model="loading"
        @refresh="onRefresh"
        :distance="80"
        :head-height="60"
        :disabled="!isAtTop || loading"
        :loading="isRefreshing"
      >
        <!-- 固定顶部导航区域 -->
        <div class="fixed-header">
          <!-- 原有的移动端导航代码 -->
          <div class="mobile-nav-wrapper">
            <van-tabs
              v-model:active="activeTab"
              swipeable
              animated
              style="width: 76%"
              :duration="0.3"
              :swipe-threshold="5"
              title-inactive-color="#8b9eb0"
              title-active-color="#1890ff"
              :line-width="20"
            >
              <van-tab title="发现" name="all" />
              <van-tab title="关注" name="following" />
            </van-tabs>
          </div>

          <!-- 原有的移动端搜索区域代码 -->
          <div
            class="mobile-search"
            :class="{ 'mobile-search-transitioning': isSearchTransitioning }"
            @click="handleSearchClick"
          >
            <a-button class="search-button">
              <div class="search-content">
                <SearchOutlined class="search-icon" />
                <span class="search-divider">|</span>
                <span class="search-text">搜索</span>
              </div>
            </a-button>
          </div>
        </div>

        <!-- 分类标签区域 -->
        <div class="category-nav" v-if="activeTab === 'all'">
          <div
            class="category-item"
            @click="handleCategoryChange('all')"
          >
            <span>全部</span>
          </div>
          <div
            v-for="category in categories"
            :key="category"
            class="category-item"
            :class="{ active: selectedCategory === category }"
            @click="handleCategoryChange(category)"
          >
            <span>{{ category }}</span>
          </div>
        </div>

        <!-- 帖子列表区域 -->
        <div
          class="post-list-wrapper"
          :style="{
            marginTop: activeTab === 'all' ? '44px' : '4px'
          }"
        >
          <div class="mobile-list-container">
            <!-- 关注页面空状态 -->
            <div v-if="activeTab === 'following' && posts.length === 0 && !loading" class="empty-following">
              <van-empty
                class="custom-empty"
                image="search"
                description="暂无关注内容"
              >
                <template #description>
                  <p class="empty-desc">关注感兴趣的创作者，获取第一手帖子更新</p>
                </template>
                <template #default>
                  <a-button type="primary" class="discover-btn" @click="activeTab = 'all'">
                    去发现
                  </a-button>
                </template>
              </van-empty>
            </div>

            <template v-else>
              <!-- 帖子列表 -->
              <post-list
                class="post-list-container"
                :loading="loading"
                :data-list="posts"
              />

              <!-- 添加加载状态和没有更多数据提示 -->
              <div v-if="loading && !isRefreshing && posts.length > 0" class="loading-more">
                <van-loading type="spinner" size="24px" color="#ff8e53">加载中...</van-loading>
              </div>
              <div v-if="!hasMore && posts.length > 0" class="no-more-data">
                没有更多数据了~
              </div>
            </template>
          </div>
        </div>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { listPostByPageUsingPost, getFollowPostsUsingPost } from '@/api/postController'
import { listCategoryByTypeUsingGet } from '@/api/categoryController'
import PostList from '@/components/PostList.vue'
import { POST_STATUS_ENUM } from '@/constants/post'
import { message } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { throttle } from 'lodash-es'

const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const posts = ref<API.Post[]>([])
const searchText = ref('')
const current = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const selectedCategory = ref('all')
const categories = ref<string[]>([])
const isSearchTransitioning = ref(false)
const device = ref<string>('')
const isRefreshing = ref(false)
const isAtTop = ref(true)
const pcContainer = ref(null)
const isLoadingMore = ref(false)

// 监听标签切换
watch(activeTab, (newTab) => {
  // 重置数据
  posts.value = []
  current.value = 1
  hasMore.value = true

  // 重置滚动位置
  window.scrollTo({
    top: 0,
    behavior: 'instant' // 使用 instant 避免平滑滚动
  })

  // 重新获取数据
  if (newTab === 'following') {
    fetchFollowPosts()
  } else {
    fetchPosts(true)
  }
})

// 获取帖子分类列表
const fetchCategories = async () => {
  try {
    const res = await listCategoryByTypeUsingGet({ type: 1 }) // 1 表示帖子分类
    if (res.data?.code === 0 && res.data.data) {
      categories.value = res.data.data
    } else {
      message.error('获取分类列表失败：' + res.data.message)
    }
  } catch (error: any) {
    console.error('获取分类列表失败:', error)
    message.error('获取分类列表失败')
  }
}

// 获取帖子列表
const fetchPosts = async (reset = false) => {
  if (reset) {
    current.value = 1
    posts.value = []
    hasMore.value = true
  }

  if (!hasMore.value || (loading.value && !isRefreshing.value)) return

  if (isRefreshing.value) {
    loading.value = true
  }

  try {
    const res = await listPostByPageUsingPost({
      sortField: 'createTime',
      sortOrder: 'desc',
      current: current.value,
      pageSize: pageSize.value,
      status: POST_STATUS_ENUM.PASS,
      searchText: searchText.value || undefined,
      category: selectedCategory.value === 'all' ? undefined : selectedCategory.value
    })




    if (res.data?.data?.records) {
      if (reset || isRefreshing.value) {
        posts.value = res.data.data.records
      } else {
        posts.value = [...posts.value, ...res.data.data.records]
      }
      // 判断是否还有更多数据
      hasMore.value = res.data.data.records.length === pageSize.value
    } else {
      hasMore.value = false
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error: any) {
    console.error('获取帖子列表失败：' + error.message)
    message.error('获取帖子列表失败')
  } finally {
    if (isRefreshing.value) {
      loading.value = false
    }
    isRefreshing.value = false
  }
}

// 获取关注的帖子
const fetchFollowPosts = async () => {
  if (!hasMore.value || (loading.value && !isRefreshing.value)) return
  loading.value = true
  try {
    const res = await getFollowPostsUsingPost({
      current: current.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'desc'
    })
    if (res.data?.data?.records) {
      if (isRefreshing.value) {
        posts.value = res.data.data.records
      } else {
        posts.value = [...posts.value, ...res.data.data.records]
      }
      // 判断是否还有更多数据
      hasMore.value = res.data.data.records.length === pageSize.value
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('获取关注帖子失败:', error)
    message.error('获取关注帖子失败')
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

// 处理搜索点击
const handleSearchClick = () => {
  isSearchTransitioning.value = true
  router.push({
    path: '/search',
    query: {
      type: 'post'
    }
  })
}

// 创建帖子
const handleCreatePost = () => {
  router.push('/post/edit')
}

// 恢复原来的滚动加载处理函数，并添加 PC 端的处理
const handleScroll = throttle(async () => {
  // PC 端滚动处理
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    const container = pcContainer.value
    if (!container) return

    const { scrollTop, clientHeight, scrollHeight } = container
    const threshold = 100

    if (
      !loading.value &&
      !isLoadingMore.value &&
      hasMore.value &&
      scrollTop + clientHeight + threshold >= scrollHeight
    ) {
      isLoadingMore.value = true
      try {
        current.value++
        if (activeTab.value === 'following') {
          await fetchFollowPosts()
        } else {
          await fetchPosts()
        }
      } finally {
        isLoadingMore.value = false
      }
    }
  } else {
    // 移动端滚动处理
    if (loading.value || !hasMore.value || isRefreshing.value) return

    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    const windowHeight = window.innerHeight
    const documentHeight = document.documentElement.scrollHeight

    if (documentHeight - scrollTop - windowHeight < 100) {
      if (!loading.value) {
        current.value++
        if (activeTab.value === 'following') {
          await fetchFollowPosts()
        } else {
          await fetchPosts()
        }
      }
    }
  }
}, 200)

// 处理分类切换
const handleCategoryChange = async (category: string) => {
  if (category === selectedCategory.value) return

  selectedCategory.value = category
  posts.value = []
  current.value = 1
  hasMore.value = true

  // 重置滚动位置
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    pcContainer.value.scrollTop = 0
  } else {
    window.scrollTo({
      top: 0,
      behavior: 'instant'
    })
  }

  await fetchPosts(true)
}

// 监听路由变化，重置过渡状态
watch(
  () => router.currentRoute.value.path,
  () => {
    isSearchTransitioning.value = false
  }
)

// 添加滚动监听来更新顶部状态
const handleScrollForRefresh = () => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    isAtTop.value = scrollTop <= 0
  }
}

onMounted(async () => {
  device.value = await getDeviceType()
  await fetchCategories()
  await fetchPosts() // 首次加载数据

  // 只在移动端添加滚动监听
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.addEventListener('scroll', handleScrollForRefresh)
  }
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.removeEventListener('scroll', handleScrollForRefresh)
  }
  window.removeEventListener('scroll', handleScroll)
})

// 修改下拉刷新处理函数
const onRefresh = async () => {
  // 如果不在顶部，则不执行刷新
  if (!isAtTop.value) {
    loading.value = false
    return
  }

  try {
    isRefreshing.value = true
    current.value = 1
    posts.value = []
    hasMore.value = true

    if (activeTab.value === 'following') {
      await fetchFollowPosts()
    } else {
      await fetchPosts(true)
    }
  } catch (error) {
    console.error('刷新失败:', error)
    message.error('刷新失败，请稍后重试')
  } finally {
    isRefreshing.value = false
    loading.value = false
  }
}

// 添加 PC 端标签切换处理函数
const handleTabChange = (key: string) => {
  activeTab.value = key
}
</script>

<style scoped>
#forumPage {
  min-height: 100vh;
  padding: 0;
  margin-left: -24px;
  margin-right: -24px;
  overflow-x: hidden;
  background: #ffffff;
}

/* 固定顶部导航区域 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  display: flex;
  align-items: center;
  height: 44px;
  border-bottom: 1px solid #f0f0f0;
}

/* 移动端导航包装器 */
.mobile-nav-wrapper {
  flex: 1;
  width: calc(100% - 60px);
}

/* 移动端导航样式 */
.mobile-nav-wrapper :deep(.van-tabs__wrap) {
  height: 48px;
  padding: 8px 16px 0;
}

.mobile-nav-wrapper :deep(.van-tabs__nav) {
  background: transparent;
  border-radius: 20px;
  padding: 4px;
  display: flex;
  justify-content: flex-start;
  height: 40px;
}

.mobile-nav-wrapper :deep(.van-tab) {
  padding: 0;
  margin: 0 8px;
  font-size: 15px;
  font-weight: 400;
  color: #64748b;
  flex: none;
  min-width: 72px !important;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;

  &:hover {
    color: #1890ff;
    background: rgba(24, 144, 255, 0.05);
  }

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100%;
    height: 100%;
    background: currentColor;
    border-radius: 50%;
    transform: translate(-50%, -50%) scale(0);
    opacity: 0;
    transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:active::after {
    opacity: 0.1;
  }
}

.mobile-nav-wrapper :deep(.van-tab--active) {
  color: #1890ff;
  font-weight: 500;
  font-size: 15px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(54, 207, 201, 0.15));
  box-shadow:
    0 2px 8px rgba(24, 144, 255, 0.1),
    0 1px 4px rgba(24, 144, 255, 0.05);
  border-radius: 16px;
  transform: translateY(-1px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-nav-wrapper :deep(.van-tabs__line) {
  display: none;
}

/* 分类导航样式 */
.category-nav {
  position: fixed;
  top: 44px;
  left: 0;
  right: 0;
  height: 36px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px 1px;
  overflow-x: auto;
  border-bottom: 1px solid #f0f0f0;
  z-index: 99;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

.category-item {
  padding: 2px 12px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  background: rgba(24, 144, 255, 0.05);

  &:hover {
    color: #1890ff;
    background: rgba(24, 144, 255, 0.08);
  }
}

.category-item.active {
  color: #1890ff;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12), rgba(54, 207, 201, 0.15));
  box-shadow:
    0 2px 8px rgba(24, 144, 255, 0.1),
    0 1px 4px rgba(24, 144, 255, 0.05);
}


/* 分类导航样式 */
.pc-category-nav {
  height: 36px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px 1px;
  overflow-x: auto;
  border-bottom: 1px solid #f0f0f0;
  z-index: 99;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

.pc-category-item {
  padding: 2px 12px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  background: rgba(24, 144, 255, 0.05);

  &:hover {
    color: #ff8e53;
    background: rgba(24, 144, 255, 0.08);
  }
}

.pc-category-item.active {
  color: #ff8e53;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(199, 164, 129, 0.12), rgba(207, 131, 54, 0.15));
  box-shadow:
    0 2px 8px rgba(192, 153, 100, 0.1),
    0 1px 4px rgba(24, 144, 255, 0.05);
}

/* 搜索框样式 */
.mobile-search {
  position: absolute;
  right: 12px;
  width: 80px;
  margin-top: 8px;
  height: 48px;
  display: flex;
  align-items: center;
  z-index: 2;
}

.search-button {
  border: none;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 16px;
  width: 100%;
  height: 32px;
  transition: all 0.3s ease;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, rgba(24, 144, 255, 0.1), rgba(54, 207, 201, 0.2));
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.12), rgba(54, 207, 201, 0.15));
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(24, 144, 255, 0.1);
  }
}

.search-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  position: relative;
  z-index: 1;
}

.search-icon {
  color: #1890ff;
  font-size: 16px;
  opacity: 0.8;
  transition: all 0.3s ease;
}

.search-divider {
  color: rgba(24, 144, 255, 0.3);
  font-size: 14px;
  transform: scale(0.9);
  margin: 0 -1px;
}

.search-text {
  color: #1890ff;
  font-size: 13px;
  opacity: 0.8;
  transition: all 0.3s ease;
  font-weight: 500;
}

/* 列表容器样式 */
.post-list-wrapper {
  margin-left: -24px;
  margin-right: -24px;
  background: #fff;
  transition: margin-top 0.3s ease;
}

.empty-following {
  padding: 40px 20px;
  text-align: center;
}

.custom-empty {
  padding: 32px 0;
}

.empty-desc {
  margin: 8px 0 16px;
  color: #94a3b8;
  font-size: 14px;
}

.discover-btn {
  width: 120px;
  height: 36px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  border: none;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
  transition: all 0.3s ease;
}

.discover-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

.discover-btn:active {
  transform: translateY(1px);
}

.post-item {
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.02);
  }
}

.post-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;

  .post-category {
    font-size: 12px;
    color: #666;
    background: #f5f5f5;
    padding: 2px 8px;
    border-radius: 4px;
  }
}

.post-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #666;
  margin-bottom: 8px;

  .post-stats {
    display: flex;
    gap: 16px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;

      &:hover {
        color: #1890ff;
      }
    }
  }

  .liked {
    color: #36cfc9;
  }
}

.post-attachments {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  overflow-x: auto;

  .attachment-preview {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: 4px;
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: scale(1.05);
    }
  }
}

/* 搜索框过渡动画 */
.mobile-search-transitioning {
  transform: scale(1.1) translateY(-10px);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-nav {
  flex: 1;
  background: #fff;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
  color: #999;
  font-size: 14px;
  gap: 8px;
}

.no-more-data {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 14px;
  opacity: 0.8;
}

.mobile-list-container {
  padding: 0 12px 12px;
  background: #fff;
}

/* 修改下拉刷新样式 */
:deep(.van-pull-refresh) {
  min-height: 100vh;
  background: #fff;
  overflow: visible;
}

:deep(.van-pull-refresh__track) {
  min-height: 100vh;
  background: #fff;
}

/* PC端样式 */
.pc-container {
  background: #f8fafc;
  margin: 0;
  width: 100%;

  .pc-content-wrapper {
    margin: 0 auto;
  }

  .pc-nav {
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    margin-bottom: 16px;

    :deep(.ant-tabs-nav) {
      margin: 0;
      padding: 0 24px;
    }

    :deep(.ant-tabs-tab) {
      padding: 0 24px 6px;
      font-size: 16px;
      transition: all 0.3s;
      margin: 0;

      &:hover {
        color: #ff8e53;
      }

      &.ant-tabs-tab-active {
        .ant-tabs-tab-btn {
          color: #ff8e53;
          font-weight: 500;
        }
      }
    }

    :deep(.ant-tabs-ink-bar) {
      background: #ff8e53;
    }
  }

  .pc-category {
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    margin-bottom: 16px;

    :deep(.ant-tabs-nav) {
      margin: 0;
      padding: 0 24px;
    }

    :deep(.ant-tabs-tab) {
      padding: 12px 20px;
      font-size: 14px;
      transition: all 0.3s;
      margin: 0;

      &:hover {
        color: #ff8e53;
      }

      &.ant-tabs-tab-active {
        .ant-tabs-tab-btn {
          color: #ff8e53;
          font-weight: 500;
        }
      }
    }

    :deep(.ant-tabs-ink-bar) {
      background: #ff8e53;
    }
  }

  .pc-content {
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    padding: 24px;

    .post-list-container {
      margin: 0;
      padding: 0;
    }
  }

  .empty-following {
    padding: 60px 0;
    text-align: center;

    .empty-desc {
      color: #666;
      margin: 16px 0;
    }

    .ant-btn {
      min-width: 120px;
      height: 36px;
      border-radius: 18px;
      font-weight: 500;
      background: #ff8e53;
      border-color: #ff8e53;

      &:hover {
        background: #ff7a33;
        border-color: #ff7a33;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(255, 142, 83, 0.2);
      }

      &:active {
        background: #ff6b1a;
        border-color: #ff6b1a;
        transform: translateY(0);
      }
    }
  }
}
</style>
