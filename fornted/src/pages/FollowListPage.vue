<template>
  <div id="followListPage">
    <!-- 顶部标签页切换 -->
    <van-tabs v-model:active="activeTab" sticky animated swipeable class="custom-tabs">
      <template #nav-right>
        <div class="tab-count-wrapper">
          <div class="tab-count">
            {{ formatCount(activeTab === 'follow' ? followCount : fansCount) }}
          </div>
        </div>
      </template>
      <van-tab title="关注" name="follow">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="finished"
            finished-text="没有更多了"
            @load="loadMore"
            class="user-list"
          >
            <div class="list-container">
              <!-- 搜索框 -->
              <div class="search-wrapper">
                <van-search
                  v-model="searchText"
                  placeholder="请输入用户ID"
                  shape="round"
                  @search="handleSearch"
                  @clear="handleClear"
                  class="search-box"
                  type="text"
                  :formatter="idFormatter"
                />
              </div>
              <template v-if="userList.length">
                <div v-for="user in userList" :key="user.id" class="user-item">
                  <div class="user-info" @click="goToUserSpace(user)">
                    <van-image
                      :src="user.userAvatar || getDefaultAvatar(user.userName)"
                      round
                      width="48"
                      height="48"
                      class="avatar"
                    />
                    <div class="user-detail">
                      <div class="username">{{ user.userName }}</div>
                      <div class="user-id">ID: {{ user.id }}</div>
                    </div>
                  </div>
                  <div class="button-group" v-if="activeTab === 'follow'">
<!--                    <van-button -->
<!--                      size="small"-->
<!--                      type="default"-->
<!--                      round-->
<!--                      :loading="user.loading"-->
<!--                      @click.stop="handleUnfollow(user)"-->
<!--                      class="unfollow-button"-->
<!--                    >-->
<!--                      取消关注-->
<!--                    </van-button>-->
                  </div>
                  <div v-else>
                    <van-button
                      size="small"
                      :type="user.isFollowing ? 'default' : 'primary'"
                      round
                      :loading="user.loading"
                      @click="toggleFollow(user)"
                      class="follow-button"
                    >
                      {{ user.isFollowing ? '已关注' : '关注' }}
                    </van-button>
                  </div>
                </div>
              </template>
              <template v-else-if="!loading">
                <div class="empty-state">
                  <van-empty
                    :image="activeTab === 'follow' ? 'search' : 'error'"
                    :description="activeTab === 'follow' ? '还没有关注任何人' : '还没有粉丝'"
                  >
                    <template #bottom>
                      <div class="empty-hint">
                        {{ activeTab === 'follow' ? '去发现更多有趣的人吧' : '分享更多内容来吸引粉丝吧' }}
                      </div>
                      <div class="empty-action">
                        <van-button
                          round
                          type="primary"
                          size="small"
                          @click="goToHome"
                          class="go-home-btn"
                        >
                          去首页看看
                        </van-button>
                      </div>
                    </template>
                  </van-empty>
                </div>
              </template>
            </div>
          </van-list>
        </van-pull-refresh>
      </van-tab>

      <van-tab title="粉丝" name="fans">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="finished"
            finished-text="没有更多了"
            @load="loadMore"
            class="user-list"
          >
            <div class="list-container">
              <!-- 搜索框 -->
              <div class="search-wrapper">
                <van-search
                  v-model="searchText"
                  placeholder="搜索粉丝"
                  shape="round"
                  @search="handleSearch"
                  @clear="handleClear"
                  class="search-box"
                />
              </div>
              <template v-if="userList.length">
                <div v-for="user in userList" :key="user.id" class="user-item">
                  <div class="user-info" @click="goToUserSpace(user)">
                    <van-image
                      :src="user.userAvatar || getDefaultAvatar(user.userName)"
                      round
                      width="48"
                      height="48"
                    />
                    <div class="user-detail">
                      <div class="username">{{ user.userName }}</div>
                      <div class="user-id">ID: {{ user.id }}</div>
                    </div>
                  </div>

                </div>
              </template>
            </div>
          </van-list>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { getFollowOrFanListUsingPost } from '@/api/userFollowsController'
import { addUserFollowsUsingPost } from '@/api/userFollowsController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const activeTab = ref(route.query.tab?.toString() || 'follow')
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const currentPage = ref(1)
const pageSize = 20
const userList = ref<any[]>([])
const followCount = ref(0)
const fansCount = ref(0)
const searchText = ref('')

// 格式化输入，只允许数字
const idFormatter = (value: string) => {
  return value.replace(/[^\d]/g, '')
}

// 格式化计数
const formatCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'w'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count
}

// 加载数据
const loadMore = async () => {
  if (refreshing.value) {
    userList.value = []
    refreshing.value = false
  }

  if (loading.value) return
  loading.value = true

  try {
    const res = await getFollowOrFanListUsingPost({
      current: currentPage.value,
      pageSize: pageSize,
      followerId: activeTab.value === 'follow'
        ? loginUserStore.loginUser.id
        : (searchText.value ? searchText.value : undefined),
      followingId: activeTab.value === 'fans'
        ? loginUserStore.loginUser.id
        : (searchText.value ? searchText.value : undefined),
      searchType: activeTab.value === 'follow' ? 0 : 1
    })

    if (res.data.code === 0) {
      const newUsers = res.data.data.records || []
      if (currentPage.value === 1) {
        if (activeTab.value === 'follow') {
          followCount.value = res.data.data.total || 0
        } else {
          fansCount.value = res.data.data.total || 0
        }
      }
      userList.value.push(...newUsers)

      if (newUsers.length < pageSize) {
        finished.value = true
      } else {
        currentPage.value++
      }
    }
  } catch (error) {
    console.error('加载失败:', error)
    message.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 下拉刷新
const onRefresh = async () => {
  finished.value = false
  currentPage.value = 1
  await loadMore()
}

// 切换关注状态
const toggleFollow = async (user: any) => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  user.loading = true
  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: user.id,
      followingName: user.userName,
      followerName: loginUserStore.loginUser.userName,
      followStatus: user.isFollowing ? 0 : 1
    })

    if (res.data?.code === 0) {
      user.isFollowing = !user.isFollowing
      message.success(user.isFollowing ? '关注成功' : '已取消关注')
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  } finally {
    user.loading = false
  }
}

// 跳转到用户空间
const goToUserSpace = (user: API.UserVO) => {
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

// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

// 监听路由参数变化
watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab.toString()
  }
})

// 监听标签页切换
watch(activeTab, () => {
  // 清空搜索框
  searchText.value = ''
  // 重置列表状态
  currentPage.value = 1
  userList.value = []
  finished.value = false
  // 重新加载数据
  loadMore()
})

// 取消关注
const handleUnfollow = async (user: any) => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    return
  }

  try {
    const res = await addUserFollowsUsingPost({
      followerId: loginUserStore.loginUser.id,
      followingId: user.id,
      followingName: user.userName,
      followerName: loginUserStore.loginUser.userName,
      followStatus: 0
    })

    if (res.data?.code === 0) {
      message.success('已取消关注')
      // 重新加载数据
      currentPage.value = 1
      userList.value = []
      loadMore()
    }
  } catch (error) {
    console.error('操作失败:', error)
    message.error('操作失败，请稍后重试')
  }
}

// 跳转到首页
const goToHome = () => {
  router.push('/')
}

// 处理搜索
const handleSearch = () => {
  if (searchText.value && !/^\d+$/.test(searchText.value)) {
    message.error('请输入有效的用户ID')
    return
  }
  // 检查ID长度
  if (searchText.value && (searchText.value.length > 19 || searchText.value.length < 1)) {
    message.error('请输入有效的用户ID')
    return
  }
  currentPage.value = 1
  userList.value = []
  finished.value = false
  loadMore()
}

// 处理清除搜索
const handleClear = () => {
  searchText.value = ''
  handleSearch()
}

onMounted(() => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    router.replace('/user/login')
    return
  }
  loadMore()
})
</script>

<style scoped>
#followListPage {
  min-height: calc(100vh - 100px);
  background: #f8fafc;
  padding: 0 12px;
}

.custom-tabs {
  margin: 0 -12px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.van-tabs__nav) {
  padding: 0 12px;
}

:deep(.van-tab) {
  padding: 0 20px;
  font-size: 15px;
  color: #64748b;
  position: relative;
}

.tab-count-wrapper {
  display: flex;
  align-items: center;
  padding-right: 16px;
}

.tab-count {
  font-size: 13px;
  color: #64748b;
  background: linear-gradient(135deg, #fff6f3 0%, #fff9f8 100%);
  border: 1px solid #ffeae3;
  padding: 2px 8px;
  border-radius: 14px;
  min-width: 32px;
  text-align: center;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(255, 142, 83, 0.1);
}

.user-list {
  padding: 12px;
}

.list-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 12px;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  margin-bottom: 12px;
}

.user-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.user-item:last-child {
  margin-bottom: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  cursor: pointer;
}

.avatar {
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.user-info:active .avatar {
  transform: scale(0.95);
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  line-height: 1.2;
  margin-bottom: 4px;
}

.user-id {
  font-size: 12px;
  color: #64748b;
  line-height: 1;
}

.button-group {
  display: flex;
  gap: 8px;
}

.follow-button,
.unfollow-button {
  min-width: 72px;
  height: 32px;
  font-size: 13px;
}

.unfollow-button {
  color: #64748b;
  border-color: #e2e8f0;
  background: #f8fafc;
}

.unfollow-button:active {
  opacity: 0.8;
}

:deep(.van-button--primary) {
  background: #ff8e53;
  border-color: #ff8e53;
  box-shadow: 0 2px 6px rgba(255, 142, 83, 0.2);
}

:deep(.van-button--default) {
  border-color: #e2e8f0;
  color: #64748b;
  background: #f8fafc;
}

:deep(.van-tabs__line) {
  background-color: #ff8e53;
  height: 3px;
  border-radius: 3px;
  bottom: 0;
}

:deep(.van-list__loading),
:deep(.van-list__finished-text) {
  padding: 16px 0;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
  opacity: 0.8;
}

:deep(.van-pull-refresh__track) {
  min-height: calc(100vh - 100px);
  background: #f8fafc;
  padding-bottom: 24px;
}

:deep(.van-pull-refresh__head),
:deep(.van-loading__spinner) {
  color: #ff8e53;
}

.empty-state {
  padding: 60px 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  text-align: center;
  margin-top: 12px;
}

.empty-hint {
  margin-top: 16px;
  color: #94a3b8;
  font-size: 15px;
}

.empty-action {
  margin-top: 24px;
}

.go-home-btn {
  min-width: 120px;
  height: 36px;
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, #ff8e53 0%, #ff7676 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.3);
  transition: all 0.3s ease;
  display: inline-block;
}

.go-home-btn:active {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
}

:deep(.van-empty__image) {
  width: 120px;
  height: 120px;
}

:deep(.van-empty__description) {
  color: #64748b;
  font-size: 16px;
  margin-top: 16px;
  font-weight: 500;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .list-container {
    padding: 0 8px;
  }

  .user-list {
    padding: 8px;
  }

  .user-item {
    margin-bottom: 8px;
    padding: 12px;
  }

  .empty-state {
    margin: 0 8px;
    padding: 40px 0;
    margin-top: 8px;
  }

  .empty-action {
    margin-top: 20px;
  }

  .go-home-btn {
    min-width: 110px;
    height: 34px;
    font-size: 13px;
  }

  :deep(.van-tab) {
    padding: 0 16px;
    font-size: 14px;
  }

  .empty-hint {
    font-size: 14px;
    margin-top: 16px;
  }
}

/* 标签页样式 */
:deep(.van-tab) {
  font-size: 17px;
  font-weight: 500;
}

:deep(.van-tabs__nav) {
  padding: 8px 0;
}

:deep(.van-tab--active) {
  font-weight: 500;
  color: #ff8e53;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  :deep(.van-tab) {
    font-size: 16px;
  }
}

/* 搜索框样式 */
.search-wrapper {
  padding: 12px 16px;
  margin-bottom: 8px;
}

.search-box {
  width: 100%;
}

:deep(.van-search) {
  padding: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

:deep(.van-search__content) {
  background: #f8fafc;
  border-radius: 6px;
}

:deep(.van-field__control) {
  color: #1a1a1a;
  font-size: 14px;
}

:deep(.van-field__control::placeholder) {
  color: #94a3b8;
}
</style>
