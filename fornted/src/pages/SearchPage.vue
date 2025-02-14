<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-bar">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索用户、帖子或图片..."
          enter-button
          @search="doSearch"
          @input="handleInput"
          ref="searchInput"
          class="apple-search"
          autofocus
        >
          <template #prefix>
            <SearchOutlined class="search-icon" />
          </template>
        </a-input-search>
      </div>
    </div>

    <!-- 搜索类型切换 -->
    <div class="search-type">
      <div class="type-buttons">
        <div
          class="type-button"
          :class="{ active: searchType === 'picture' }"
          @click="() => handleTypeChange('picture')"
        >
          <PictureOutlined class="icon" :class="{ inactive: searchType !== 'picture' }" />
          <span>图片</span>
        </div>
        <div
          class="type-button"
          :class="{ active: searchType === 'post' }"
          @click="() => handleTypeChange('post')"
        >
          <FileTextOutlined class="icon" :class="{ inactive: searchType !== 'post' }" />
          <span>帖子</span>
        </div>
        <div
          class="type-button"
          :class="{ active: searchType === 'space' }"
          @click="() => handleTypeChange('space')"
        >
          <TeamOutlined class="icon" :class="{ inactive: searchType !== 'space' }" />
          <span>空间</span>
        </div>
        <div
          class="type-button"
          :class="{ active: searchType === 'user' }"
          @click="() => handleTypeChange('user')"
        >
          <UserOutlined class="icon" :class="{ inactive: searchType !== 'user' }" />
          <span>用户</span>
        </div>
      </div>
    </div>

    <!-- 搜索历史和热门搜索 -->
    <div v-if="!searchText" class="search-suggestions">
      <div class="suggestions-content">
        <!-- 搜索历史 -->
        <div class="search-history" v-if="searchHistory.length > 0">
          <div class="section-header">
            <span class="section-title">搜索历史</span>
            <span  @click="clearHistory" style="color: #0ea5e9">清空</span>
          </div>
          <div class="history-tags">
            <a-tag
              v-for="item in searchHistory"
              :key="item"
              @click="searchByTag(item)"
              class="custom-tag"
            >
              {{ item }}
            </a-tag>
          </div>
        </div>

        <!-- 热门搜索 -->
        <div class="hot-searches">
          <div class="section-header">
            <span class="section-title">热门搜索</span>
          </div>
          <div class="hot-list">
            <div
              v-for="(item, index) in hotSearches"
              :key="item"
              class="hot-item"
              @click="searchByTag(item)"
            >
              <span class="hot-rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
              <span class="hot-text">{{ item }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-else class="search-results">
      <template v-if="loading">
        <div class="loading-container">
          <a-spin />
        </div>
      </template>
      <template v-else>
        <div v-if="searchType === 'picture'">
          <!-- PC端图片列表 -->
          <WaterfallPictureList
            v-if="!isMobile && pictureList.length > 0"
            :dataList="pictureList"
            :loading="loading"
          />
          <!-- 移动端列表 -->
          <MobilePictureList
            v-else-if="pictureList.length > 0"
            :dataList="pictureList"
            :loading="loading"
          />
          <a-empty v-else description="暂无搜索结果" />
        </div>
        <div v-else-if="searchType === 'post'" class="post-results">
          <div v-if="postList.length > 0" class="post-list">
            <PostList
              :dataList="postList"
              :loading="loading"
              :showStatus="false"
            />
          </div>
          <a-empty v-else description="暂无搜索结果" />
        </div>
        <div v-else-if="searchType === 'space'" class="space-results">
          <div v-if="spaceList.length > 0" class="space-list">
            <div v-for="space in spaceList" :key="space.id" class="space-card">
              <div class="space-info">
                <div class="space-header">
                  <h3 class="space-name">{{ space.spaceName }}</h3>
                  <a-tag color="#4096ff" class="space-type-tag">
                    团队空间
                  </a-tag>
                </div>
                <div  class="space-owner" @click="showSpaceDetail(space)">
                  <a-avatar @click="handleUserClick(space.user)"  :size="32" :src="space.user?.userAvatar" />
                  <span @click="handleUserClick(space.user)"  class="owner-name">{{ space.user?.userName }}</span>
                </div>
                <div class="space-stats">
                  <span class="stat-item">
                    <FileImageOutlined /> {{ space.totalCount }} 张图片
                  </span>
                  <span class="stat-item">
                    <TeamOutlined /> {{ space.memberCount || 0 }} 位成员
                  </span>
                </div>
                <div class="space-actions">
                  <a-button
                    type="primary"
                    @click="handleJoinSpace(space)"
                    class="join-button"
                  >
                    <UserAddOutlined /> 申请加入
                  </a-button>
                  <a-button type="link" @click="showSpaceDetail(space)">
                    查看详情
                  </a-button>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无搜索结果" />
        </div>
        <div v-else class="user-results">
          <div v-if="userList.length > 0" class="user-list">
            <div v-for="user in userList" :key="user.id" class="user-card">
              <div @click.stop="handleUserClick(user)" class="user-link">
                <a-avatar :size="48" :src="user.userAvatar" />
                <div class="user-info">
                  <div class="user-name">{{ user.userName }}</div>
                  <div class="user-profile">{{ user.userProfile || '这个人很懒，什么都没写~' }}</div>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无搜索结果" />
        </div>
      </template>
    </div>

    <!-- 添加空间详情弹窗组件 -->
    <SpaceDetailModal
      v-model="spaceDetailVisible"
      :space-detail="selectedSpace"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { SearchOutlined, PictureOutlined, UserOutlined, FileTextOutlined, TeamOutlined, FileImageOutlined, UserAddOutlined, TagOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { searchAllUsingPost, getHotSearchKeywordsUsingGet } from '@/api/searchController'
import { joinSpaceUsingPost } from '@/api/spaceUserController'
import MobilePictureList from '@/components/MobilePictureList.vue'
import PictureList from '@/components/PictureList.vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'
import PostList from '@/components/PostList.vue'
import { debounce } from 'lodash-es'
import SpaceDetailModal from '@/components/SpaceDetailModal.vue'

const router = useRouter()
const route = useRoute()
const searchInput = ref()
const searchText = ref('')
const searchType = ref('picture')
const loading = ref(false)

// 添加类型声明
interface PictureUser {
  userId: number;
  userName: string;
  userAvatar: string;
}

interface PictureVO {
  id: number;
  url: string;
  description: string;
  thumbNum: number;
  commentCount: number;
  isThumb: boolean;
  user: PictureUser;
}

// 修改类型声明
const pictureList = ref<PictureVO[]>([])
const userList = ref<API.UserVO[]>([])
const postList = ref<API.Post[]>([])
const spaceList = ref<API.SpaceVO[]>([])
const spaceDetailVisible = ref(false)
const selectedSpace = ref<API.SpaceVO | null>(null)

// 搜索历史
const searchHistory = ref<string[]>([])
// 获取热门搜索
const hotSearches = ref([])

// 判断是否为移动端
const isMobile = ref(getDeviceType() !== DEVICE_TYPE_ENUM.PC)

// 获取热门搜索
const fetchHotSearches = async (type: string) => {
  try {
    const res = await getHotSearchKeywordsUsingGet({
      type,
      size: 9
    })
    if (res.data?.code === 0) {
      hotSearches.value = res.data.data
    }
  } catch (error) {
    console.error('获取热门搜索失败:', error)
  }
}

onMounted(() => {
  // 从localStorage加载搜索历史
  const history = localStorage.getItem('searchHistory')
  if (history) {
    searchHistory.value = JSON.parse(history)
  }

  // 根据路由参数设置初始搜索类型
  if (route.query.type) {
    searchType.value = route.query.type as string
  }

  // 自动聚焦搜索框
  nextTick(() => {
    searchInput.value?.focus()
  })

  // 获取初始热门搜索
  fetchHotSearches(searchType.value)
})

// 监听路由参数变化
watch(
  () => route.query.type,
  (newType) => {
    if (newType) {
      searchType.value = newType as string
      // 如果有搜索文本，立即执行搜索
      if (searchText.value) {
        doSearch()
      }
    }
  }
)

// 添加防抖搜索函数
const debouncedSearch = debounce(async (text: string) => {
  if (!text.trim()) {
    pictureList.value = []
    userList.value = []
    postList.value = []
    spaceList.value = []
    return
  }

  loading.value = true

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: 1,
      pageSize: 20
    })

    if (res.data.code === 0) {
      if (searchType.value === 'picture') {
        pictureList.value = res.data.data.content || []
      } else if (searchType.value === 'post') {
        postList.value = res.data.data.content || []
      } else if (searchType.value === 'space') {
        spaceList.value = res.data.data.content || []
      } else {
        userList.value = res.data.data.content || []
      }
      // 添加到搜索历史
      addToHistory(text)
    } else {
      message.error('搜索失败：' + res.data.message)
    }
  } catch (error) {
    message.error('搜索出错，请稍后重试')
  } finally {
    loading.value = false
  }
}, 500)  // 设置 0.5 秒的延迟

// 执行搜索
const doSearch = async (text = searchText.value) => {
  if (!text.trim()) {
    message.warning('请输入搜索内容')
    return
  }

  loading.value = true

  try {
    const res = await searchAllUsingPost({
      searchText: text,
      type: searchType.value,
      current: 1,
      pageSize: 20
    })

    if (res.data.code === 0) {
      if (searchType.value === 'picture') {
        pictureList.value = res.data.data.content || []
      } else if (searchType.value === 'post') {
        postList.value = res.data.data.content || []
      } else if (searchType.value === 'space') {
        spaceList.value = res.data.data.content || []
      } else {
        userList.value = res.data.data.content || []
      }
      // 添加到搜索历史
      addToHistory(text)
    } else {
      message.error('搜索失败：' + res.data.message)
    }
  } catch (error) {
    message.error('搜索出错，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 修改处理输入框内容变化的函数
const handleInput = () => {
  if (!searchText.value) {
    pictureList.value = []
    userList.value = []
    postList.value = []
    spaceList.value = []
    return
  }
  // 触发防抖搜索
  debouncedSearch(searchText.value)
}

// 修改搜索类型切换处理函数
const handleTypeChange = (type: string) => {
  searchType.value = type
  // 清空之前的搜索结果
  pictureList.value = []
  postList.value = []
  userList.value = []
  spaceList.value = []
  // 当输入框为空时，获取对应类型的热门搜索
  if (!searchText.value) {
    fetchHotSearches(type)
  } else {
    doSearch()
  }
}

// 添加到搜索历史
const addToHistory = (text: string) => {
  if (!searchHistory.value.includes(text)) {
    searchHistory.value.unshift(text)
    if (searchHistory.value.length > 10) {
      searchHistory.value.pop()
    }
    localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
  }
}

// 清空搜索历史
const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

// 修改点击标签搜索函数
const searchByTag = (text: string) => {
  searchText.value = text
  // 立即执行搜索，不使用防抖
  doSearch(text)
}

// 处理图片点击
const handlePictureClick = (item: API.PictureVO) => {
  router.push(`/picture/${item.id}`)
}

// 处理用户点击
const handleUserClick = (user) => {
  if (!user) return
  // console.log('用户点击', user)
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

// 显示空间详情
const showSpaceDetail = (space: API.SpaceVO) => {
  selectedSpace.value = space
  spaceDetailVisible.value = true
}

// 申请加入空间
const handleJoinSpace = async (space: API.SpaceVO) => {
  if (!space || !space.id) return

  try {
    const res = await joinSpaceUsingPost({
      spaceId: space.id
    })

    if (res.data?.code === 0) {
      message.success('申请已提交，请等待管理员审核')
    } else {
      message.error(res.data?.message || '申请失败')
    }
  } catch (error) {
    message.error('申请失败，请稍后重试')
  }
}

// 组件卸载时取消未执行的防抖函数
onUnmounted(() => {
  debouncedSearch.cancel()
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #fff;
  padding: 24px 4px;
  margin: 0 auto;
}

.search-header {
  margin-bottom: 32px;
}

.search-bar {
  flex: 1;
  max-width: 680px;
  margin: 0 auto;
  position: relative;
  height: 40px;
}

/* 苹果风格搜索框 */
:deep(.apple-search) {
  position: absolute;
  width: 100%;

  .ant-input-wrapper {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    height: 40px;
  }

  .ant-input-affix-wrapper {
    height: 40px;
    padding-top: 0;
    padding-bottom: 0;
    display: flex;
    align-items: center;
    border: 1px solid #d9d9d9;

    &:hover,
    &:focus,
    &-focused {
      border-color: #ff8e53;
      box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
    }
  }

  .ant-input {
    height: 40px;
    font-size: 16px;
    padding-left: 16px;
    line-height: 40px;
  }

  .ant-input-search-button {
    height: 40px;
    background: #ff8e53;
    border-color: #ff8e53;
    line-height: 40px;

    &:hover {
      background: #ff7a33;
      border-color: #ff7a33;
    }
  }
}

.search-type {
  text-align: center;
  margin: 12px 0;  /* 调整搜索类型切换按钮的上下间距 */
}

.type-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 0 12px;
}

.type-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s ease, color 0.3s ease;
  color: #666;
  min-width: 80px;
  justify-content: center;

  &:hover {
    color: #ff8e53;
    background: #fff0eb;
  }

  &.active {
    color: #ff8e53;
    background: #fff0eb;
    font-weight: 500;
  }

  .icon {
    font-size: 18px;
    transition: opacity 0.3s ease;
  }

  .inactive {
    opacity: 0.6;
  }
}

.suggestions-content {
  max-width: 680px;
  margin: 0 auto;
}

.search-suggestions {
  padding: 0 4px;
  margin-top: 12px;  /* 调整搜索建议区域的上边距 */
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 13px;
  color: #999;
  letter-spacing: 0.5px;
}

.custom-tag {
  cursor: pointer;
  border: none;
  background: #f5f5f7;
  color: #666;
  border-radius: 4px;
  font-size: 13px;
  transition: all 0.2s;

  &:hover {
    background: #fff0eb;
    color: #ff8e53;
  }
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #f5f5f7;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #fff0eb;
    transform: translateY(-1px);
  }
}

.hot-rank {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #999;
}

.hot-rank.top3 {
  color: #ff8e53;
  font-weight: bold;
}

.hot-text {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 680px;
  margin: 0 auto;
  padding: 0 4px;
}

.user-card {
  padding: 12px;
  background: #f5f5f7;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background: #fff0eb;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.1);
  }
}

.user-link {
  display: flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-profile {
  font-size: 14px;
  color: #666;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 32px;
}

.search-results {
  width: 100%;
  max-width: 100%;
  padding: 0;
}

/* 帖子搜索结果样式 */
.post-results {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

.post-list {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.space-results {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

.space-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.space-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.space-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.space-name {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
  color: #1a1a1a;
}

.space-owner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0;
  padding: 8px;
  border-radius: 8px;
  background: #f8fafc;
  transition: all 0.2s;

  &:hover {
    background: #f0f7ff;
  }
}

.owner-name {
  font-size: 14px;
  color: #666;
}

.space-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
  padding: 4px 8px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.space-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.join-button {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 20px;
  border-radius: 16px;
  font-size: 14px;
  background: #4096ff;
  border-color: #4096ff;
  min-width: 120px;
  justify-content: center;

  &:hover {
    background: #1677ff;
    border-color: #1677ff;
  }
}

:deep(.ant-btn-link) {
  padding: 0 20px;
  height: 32px;
  font-size: 14px;
  border-radius: 16px;
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: #f0f7ff;
  }
}

.space-type-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: normal;
  border: none;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .search-page {
    padding: 16px 4px;
  }

  .search-header {
    margin-bottom: 20px;
  }

  .search-type {
    margin: 8px 0 16px;
  }

  .type-buttons {
    gap: 8px;
    padding: 0 8px;
  }

  .type-button {
    padding: 6px 8px;
    min-width: 70px;
    font-size: 13px;

    .icon {
      font-size: 16px;
    }
  }

  .join-button,
  :deep(.ant-btn-link) {
    min-width: 100px;
    padding: 0 16px;
  }

  .space-results,
  .post-results {
    padding: 0 8px;
  }

  .space-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .space-card {
    padding: 16px;
    width: 100%;
  }

  .space-stats {
    padding: 8px;
    gap: 8px;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .stat-item {
    padding: 4px 6px;
    font-size: 12px;
    gap: 4px;
    flex: 0 0 auto;
    white-space: nowrap;
  }

  .space-actions {
    flex-direction: column;
    gap: 8px;
  }

  .join-button,
  :deep(.ant-btn-link) {
    width: 100%;
    min-width: unset;
    padding: 0;
    height: 36px;
  }

  .space-name {
    font-size: 15px;
    max-width: calc(100% - 80px);  /* 留出标签的空间 */
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .space-type-tag {
    flex-shrink: 0;
  }

  .owner-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
