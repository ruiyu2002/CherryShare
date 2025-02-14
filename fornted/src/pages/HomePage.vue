<template>
  <div id="homePage">
    <!--    &lt;!&ndash; PC端搜索区域 &ndash;&gt;-->
    <!--    <div-->
    <!--      v-if="device === DEVICE_TYPE_ENUM.PC"-->
    <!--      class="search-wrapper"-->
    <!--      :class="{ 'search-fixed': isSearchFixed, 'search-transitioning': isSearchTransitioning }"-->
    <!--    >-->
    <!--      <div-->
    <!--        class="search-bar"-->
    <!--        :class="{ 'search-bar-transitioning': isSearchTransitioning }"-->
    <!--        @click="handleSearchClick"-->
    <!--      >-->
    <!--        <a-input-search-->
    <!--          v-model:value="searchParams.searchText"-->
    <!--          :placeholder="isSearchFixed ? '搜索' : '探索海量精彩图片...'"-->
    <!--          :enter-button="isSearchFixed ? false : '搜索'"-->
    <!--        >-->
    <!--          <template #prefix>-->
    <!--            <SearchOutlined class="search-icon" />-->
    <!--          </template>-->
    <!--        </a-input-search>-->
    <!--      </div>-->
    <!--    </div>-->

    <!-- 移动端搜索区域 -->
    <div
      v-if="device !== DEVICE_TYPE_ENUM.PC"
      class="mobile-search mobile-search-fixed"
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

    <!-- 移动端顶部导航 -->
    <div v-if="device !== DEVICE_TYPE_ENUM.PC" class="mobile-nav">
      <van-tabs
        v-model:active="activeTab"
        swipeable
        animated
        style="width: 76%"
        :duration="0.3"
        :swipe-threshold="5"
        title-inactive-color="#8b9eb0"
        title-active-color="#ff8e53"
        :line-width="20"
      >
        <van-tab title="全部" name="all" />
        <van-tab title="关注" name="following" />
        <van-tab title="榜单" name="ranking" />
      </van-tabs>
      <div class="m-filter-section" v-if="activeTab == 'all'" >
        <a-tabs
          v-model:active-key="selectedCategory"
          @change="handleCategoryChange"
          class="category-tabs"
        >
          <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
        </a-tabs>
      </div>
      <!-- 榜单子标签 -->
      <div v-if="activeTab === 'ranking'" class="ranking-tabs">
        <van-tabs v-model:active="rankingType" @change="handleRankingTypeChange">
          <van-tab v-for="tab in rankingTabs"
                   :key="tab.value"
                   :title="tab.name"
                   :name="tab.value"
          />
        </van-tabs>
      </div>

    </div>


    <!-- 分类和标签筛选 -->
    <div class="filter-section" v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- PC端导航 -->
      <div class="pc-nav">
        <a-tabs v-model:activeKey="activeTab" >
          <a-tab-pane key="all" tab="全部" />
          <a-tab-pane key="following" tab="关注" />
          <a-tab-pane key="ranking" tab="榜单" />
        </a-tabs>
      </div>

      <!-- 分类和标签筛选 -->
      <div class="filter-section" v-if="activeTab === 'all'">
        <a-tabs v-model:active-key="selectedCategory" @change="doSearch" class="category-tabs">
          <a-tab-pane key="all" tab="全部" />
          <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
        </a-tabs>
      </div>

      <!-- 榜单子标签 -->
      <div v-if="activeTab === 'ranking'" class="pc-ranking-nav">
        <div
          v-for="tab in rankingTabs"
          :key="tab.value"
          class="pc-ranking-item"
          :class="{ active: rankingType === tab.value }"
          @click="handleRankingTypeChange(tab.value)"
        >
          <span>{{ tab.name }}</span>
        </div>
      </div>
      <!--      <div>-->
      <!--        <a-tabs v-model:active-key="selectedCategory" @change="doSearch" class="category-tabs">-->
      <!--          <a-tab-pane key="all" tab="全部" />-->
      <!--          <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />-->
      <!--        </a-tabs>-->
      <!--      </div>-->
    </div>

    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <!-- 图片列表 -->
      <WaterfallPictureList
        :dataList="pcDataList"
        :loading="loading"
        :onLoadMore="loadMorePictures"
      />
    </div>
    <div v-else>
      <van-pull-refresh style="margin-left: -20px;margin-right: -20px" :style="activeTab === 'following' ? {marginTop: '-60px'}:{}" v-model="loading" @refresh="onRefresh" :distance="80" :head-height="60">
        <div class="mobile-list-container">
          <!-- 修改关注页面空状态 -->
          <div v-if="activeTab === 'following' && mobileDataList.length === 0 && !loading" class="empty-following">
            <van-empty
              class="custom-empty"
              image="search"
              description="暂无关注内容"
            >
              <template #description>
                <p class="empty-desc">关注感兴趣的创作者，获取第一手图片更新</p>
              </template>
              <template #default>
                <a-button type="primary" class="discover-btn" @click="activeTab = 'all'">
                  去发现
                </a-button>
              </template>
            </van-empty>
          </div>

          <!-- 内部组件 -->
          <MobilePictureList v-else :dataList="mobileDataList" :loading="loading" />

          <!-- 加载状态提示 -->
          <div v-if="!isEndOfData && !loading && mobileDataList.length > 0&&activeTab === 'all'"  class="loading-more">
            <van-loading type="spinner" size="24px" color="#c4947e">加载中...</van-loading>
          </div>
          <div v-if="isEndOfData && mobileDataList.length > 0" class="no-more-data-tip">没有更多数据了哦~</div>
        </div>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, onUnmounted, onActivated, onDeactivated, watch, nextTick } from 'vue'
import {
  getFollowPictureUsingPost,
  getTop100PictureUsingGet,
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { useRoute, useRouter } from 'vue-router'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
// import {prevRoute} from '@/router'
import { debounce } from 'lodash-es'
import MobilePictureList from '@/components/MobilePictureList.vue'
import WaterfallPictureList from '@/components/WaterfallPictureList.vue'

const router = useRouter()
const route = useRoute()

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

// 定义PC端数据
const pcDataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
const pcIsEndOfData = ref(false) // 新增 PC 端是否加载完所有数据的标记

// 定义移动端数据
const mobileDataList = ref<API.PictureVO[]>([])

// 搜索条件（PC端和移动端共用部分）
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20, // 增加每页加载数量
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 新增用于移动端分页的页码变量
const page = ref(1)

// 获取标签和分类选项（PC端和移动端共用）
const getTagCategoryOptions = async () => {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      tagList.value = res.data.data.tagList ?? []
      categoryList.value = res.data.data.categoryList ?? []
    } else {
      // message.error('获取标签分类列表失败，' + res.data.message)
    }
  }

;(() => {
  // console.log(mobileDataList.value.length)
  if (mobileDataList.value.length === 0 || pcDataList.value.length === 0) {
    // console.log('空数组刷新数据')
    loading.value = true
    // 更彻底地重置搜索参数（移动端和PC端共用部分）
    searchParams.searchText = ''
    searchParams.current = 1
    searchParams.pageSize = 12
    searchParams.sortField = 'createTime'
    searchParams.sortOrder = 'descend'
    page.value = 1 // 确保移动端分页页码重置为初始值
    // 重新获取标签和分类选项以及数据
    getTagCategoryOptions()
      .then(() => {
        return fetchMobileData()
      })
      .then(() => {
        return fetchPcData()
      })
      .then(() => {
        // console.log(mobileDataList.value.length)
        loading.value = false
      })
      .catch((error) => {
        // console.error('数据获取过程出现错误:', error)
        loading.value = false
      })
  }
})()

// PC端获取数据函数
const fetchPcData = async () => {
  loading.value = true
  try {
    const params = {
      ...searchParams,
      tags: [] as string[],
    }
    if (selectedCategory.value !== 'all') {
      params.category = selectedCategory.value
    }
    selectedTagList.value.forEach((useTag, index) => {
      if (useTag) {
        params.tags.push(tagList.value[index])
      }
    })
    const res = await listPictureVoByPageUsingPost(params)
    if (res?.data?.code === 0 && res.data.data) {
      const data = res.data.data
      if ('records' in data) {
        pcDataList.value = data.records ?? []
        total.value = data.total ?? 0
        pcIsEndOfData.value = data.records.length < (params.pageSize || 20)
      } else {
        pcDataList.value = data as API.PictureVO[]
        total.value = data.length
        pcIsEndOfData.value = data.length < (params.pageSize || 20)
      }
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    // console.error('获取数据失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 移动端获取数据函数
// 移动端顶部导航激活标签
const activeTab = ref('all')

// 监听标签切换
watch(activeTab, async (newTab, oldTab) => {
  // 即使是相同标签也刷新数据
  mobileDataList.value = []
  page.value = 1
  searchParams.current = 1
  isEndOfData.value = false
  loading.value = true

  try {
    if (newTab === 'all') {
      await fetchMobileData()
    } else if (newTab === 'following') {
      await fetchFollowData()
    } else if (newTab === 'ranking') {
      await fetchRankingData()
    }
  } catch (error) {
    // console.error('切换标签加载数据失败:', error)
    message.error('加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}, { immediate: true })

const fetchMobileData = async () => {
  loading.value = true
  try {
    const res = await listPictureVoByPageUsingPost(searchParams)
    if (res.data.code === 0 && res.data.data) {
      const newData = res.data.data.records ?? []
      // 为每个图片添加加载状态标记
      const processedData = newData.map((item) => ({
        ...item,
        loaded: false,
      }))
      mobileDataList.value = [...mobileDataList.value, ...processedData]
    }
  } catch (error) {
    // console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

// PC端分页参数改变时触发
const onPageChange = (page: number, pageSize: number) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    searchParams.current = page
    searchParams.pageSize = pageSize
    fetchPcData()
  }
}
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    mobileDataList.value = res.data.data.records ?? []
    pcDataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
// 搜索操作触发（PC端和移动端共用）// 搜索
const doSearch = () => {
  // 重置数据
  resetPcData()
  // 重新加载数据
  fetchPcData()
}

const isEndOfData = ref(false)
const scroll = debounce(() => {
  const isLoading = ref(true)
  const throttledFetchData = debounce(async () => {
    if (isLoading.value) {
      page.value = page.value + 1
      searchParams.current = page.value
      let res = null
      if(activeTab.value==='all'){
        res = await listPictureVoByPageUsingPost(searchParams)
      }
      if(activeTab.value==='following'){
        res = await getFollowPictureUsingPost(searchParams)
      }
      if (res.data.code === 0 && res.data.data) {
        const newData = res.data.data.records ?? []
        mobileDataList.value = [...mobileDataList.value, ...newData]
        isEndOfData.value = newData.length === 0
        isLoading.value = true
      } else {
        message.error('获取数据失败，' + res.data.message)
        isLoading.value = false
      }
    }
  }, 1000) // 设置节流时间间隔为4000毫秒，即每4000毫秒内最多触发一次请求，可根据实际情况调整

  window.onscroll = () => {
    const bottomOfWindow =
      document.documentElement.offsetHeight -
      document.documentElement.scrollTop -
      window.innerHeight <=
      100
    if (bottomOfWindow) {
      throttledFetchData()
    }
  }
})

// 添加滚动位置记忆变量
const scrollPosition = ref(0)

// 修改现有的 onDeactivated 钩子
onDeactivated(() => {
  // 保存滚动位置
  scrollPosition.value = window.pageYOffset || document.documentElement.scrollTop
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.onscroll = null
  }
})

// 修改现有的 onActivated 钩子
onActivated(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    // 先恢复滚动位置
    nextTick(() => {
      window.scrollTo({
        top: scrollPosition.value,
        behavior: 'instant'
      })
    })
    // 然后重新绑定滚动监听
    scroll()
  }
})

// 页面加载时设置滚动分页监听（移动端）
onMounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    scroll()
  }
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) {
    window.onscroll = null
  }
})

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 下拉刷新处理函数
const onRefresh = async () => {
  loading.value = true;
  try {
    // 重置搜索参数
    searchParams.searchText = '';
    searchParams.current = 1;
    searchParams.pageSize = 12;
    searchParams.sortField = 'createTime';
    searchParams.sortOrder = 'descend';
    searchParams.category = '';
    page.value = 1;

    let res;
    switch (activeTab.value) {
      case 'all':
        // 重置到"全部"标签
        activeTab.value = 'all';
        selectedCategory.value = '';
        categoryList.value = [];
        // 重新获取标签和分类选项以及数据
        await getTagCategoryOptions();
        res = await listPictureVoByPageUsingPost(searchParams);
        break;
      case 'following':
        res = await getFollowPictureUsingPost(searchParams);
        break;
      case 'ranking':
        res = await getTop100PictureUsingGet({ id: rankingType.value });
        break;
    }

    if (res && res.data.code === 0) {
      if (activeTab.value === 'ranking') {
        mobileDataList.value = res.data.data || [];
      } else {
        mobileDataList.value = res.data.data?.records || [];
      }
      isEndOfData.value = false;
      message.success('刷新成功');
    } else if (res) {
      message.error('刷新失败，' + res.data.message);
    }
  } catch (error) {
    // console.error('刷新数据出错:', error);
    message.error('刷新失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 添加搜索框固定状态
const isSearchFixed = ref(false)

// 监听路由参数变化
watch(
  () => route.query.refresh,
  async (newVal) => {
    if (newVal === 'true') {
      // 重置数据
      page.value = 1
      searchParams.current = 1
      isEndOfData.value = false
      mobileDataList.value = []
      pcDataList.value = []

      // 重新获取数据
      if (device.value === DEVICE_TYPE_ENUM.PC) {
        await fetchPcData()
      } else {
        await fetchMobileData()
      }
    }
  },
)


// 榜单相关变量
const rankingType = ref(3) // 默认周榜
const rankingTabs = [
  { name: '日榜', value: 1 },
  { name: '周榜', value: 2 },
  { name: '月榜', value: 3 },
  { name: '总榜', value: 4 }
]

// 获取榜单数据
const fetchRankingData = async () => {
  try {
    const res = await getTop100PictureUsingGet({
      id: rankingType.value
    })
    if (res.data.code === 0) {
      // 直接替换数据而不是追加
      mobileDataList.value = res.data.data || []
      loading.value = false
      isEndOfData.value = true
    } else {
      message.error('获取榜单数据失败：' + res.data.message)
    }
  } catch (error) {
    // console.error('获取榜单数据出错:', error)
    message.error('获取榜单数据失败，请稍后重试')
    throw error // 向上抛出错误以便统一处理
  }
}

// 修改榜单类型切换的处理函数
const handleRankingTypeChange = async (type: number) => {
  // 保存新的榜单类型
  rankingType.value = type
  loading.value = true

  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      // PC端处理
      pcDataList.value = []
      pcIsEndOfData.value = false
      const res = await getTop100PictureUsingGet({ id: type })
      if (res.data?.code === 0) {
        pcDataList.value = res.data.data || []
      }
    } else {
      // 移动端处理
      mobileDataList.value = []
      await fetchRankingData()
    }
  } catch (error) {
    console.error('切换榜单类型失败:', error)
    message.error('获取榜单数据失败，请重试')
  } finally {
    loading.value = false
  }
}

//获取关注数据
const fetchFollowData = async () => {
  try {
    const res = await getFollowPictureUsingPost(searchParams)
    if (res.data.code === 0 && res.data.data) {
      mobileDataList.value = res.data.data.records ?? []
      isEndOfData.value = res.data.data.records.length === 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  }catch (error) {
    // console.error('获取关注数据出错:', error)
    message.error('获取关注数据出错，请稍后重试')
    throw error // 向上抛出错误以便统一处理
  }
}
// 修改分类标签切换处理函数
const handleCategoryChange = async (category: string) => {
  if (activeTab.value === 'all') {
    loading.value = true
    mobileDataList.value = []
    page.value = 1
    searchParams.current = 1
    searchParams.category = category === 'all' ? undefined : category
    isEndOfData.value = false

    try {
      await fetchMobileData()
    } catch (error) {
      console.error('切换分类失败:', error)
      message.error('切换分类失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }
}

// 加载更多图片
const loadMorePictures = async (nextPage: number) => {
  if (pcIsEndOfData.value) return false
  if (loading.value) return false

  loading.value = true
  try {
    let res;
    // 根据当前标签页加载不同的数据
    switch (activeTab.value) {
      case 'following':
        res = await getFollowPictureUsingPost({
          ...searchParams,
          current: nextPage,
          pageSize: 20
        })
        break
      case 'ranking':
        // 榜单数据不需要分页加载
        return false
      case 'all':
      default:
        res = await listPictureVoByPageUsingPost({
          ...searchParams,
          current: nextPage,
          pageSize: 20
        })
        break
    }

    if (res.data?.code === 0 && res.data.data) {
      // 处理不同类型的响应数据
      let newData = []
      if (activeTab.value === 'ranking') {
        newData = res.data.data || []
      } else {
        newData = res.data.data.records || []
      }

      if (newData.length > 0) {
        await nextTick(() => {
          pcDataList.value = [...pcDataList.value, ...newData]
          // 只有在全部标签页时才更新总数
          if (activeTab.value === 'all') {
            total.value = res.data.data.total ?? 0
          }
          pcIsEndOfData.value = newData.length < 20
        })
        return true
      } else {
        pcIsEndOfData.value = true
      }
    }
    return false
  } catch (error) {
    console.error('加载更多图片失败:', error)
    return false
  } finally {
    loading.value = false
  }
}

// 重置 PC 端数据的方法
const resetPcData = () => {
  pcDataList.value = []
  searchParams.current = 1
  pcIsEndOfData.value = false
}

const isSearchTransitioning = ref(false)
// 处理搜索点击
const handleSearchClick = () => {
  isSearchTransitioning.value = true

  // 获取搜索框元素
  const searchBar = document.querySelector('.search-bar') as HTMLElement
  const searchWrapper = document.querySelector('.search-wrapper') as HTMLElement

  if (searchBar && searchWrapper) {
    // 第一阶段：搜索框缩小并上移
    searchBar.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchBar.style.transform = 'scale(0.9) translateY(-20px)'
    searchBar.style.opacity = '0.8'

    // 第二阶段：背景区域收缩
    searchWrapper.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
    searchWrapper.style.transform = 'scaleY(0.8)'
    searchWrapper.style.opacity = '0.6'
  }

  // 第三阶段：完成动画并跳转
  setTimeout(() => {
    if (searchBar && searchWrapper) {
      searchBar.style.transform = 'scale(0.8) translateY(-40px)'
      searchBar.style.opacity = '0'
      searchWrapper.style.transform = 'scaleY(0)'
      searchWrapper.style.opacity = '0'
    }

    // 导航跳转
    router.push('/search')
  }, 300)
}


// 监听路由变化，重置过渡状态
// 监听路由变化，重置过渡状态
watch(
  () => route.path,
  () => {
    isSearchTransitioning.value = false
  }
)

// 监听 activeTab 变化
watch(activeTab, async (newTab) => {
  // 重置数据
  if (device.value === DEVICE_TYPE_ENUM.PC) {
    pcDataList.value = []
    pcIsEndOfData.value = false
    loading.value = true

    try {
      if (newTab === 'all') {
        // 获取全部数据
        const res = await listPictureVoByPageUsingPost(searchParams)
        if (res.data?.code === 0 && res.data.data) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total ?? 0
        }
      } else if (newTab === 'following') {
        // 获取关注数据
        const res = await getFollowPictureUsingPost(searchParams)
        if (res.data?.code === 0 && res.data.data) {
          pcDataList.value = res.data.data.records || []
          total.value = res.data.data.total ?? 0
        }
      } else if (newTab === 'ranking') {
        // 获取榜单数据
        const res = await getTop100PictureUsingGet({ type: rankingType.value })
        if (res.data?.code === 0) {
          pcDataList.value = res.data.data || []
        }
      }
    } catch (error) {
      console.error('切换标签页失败:', error)
      message.error('获取数据失败，请重试')
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
  margin-top: -24px;
  background: #ffffff;
  position: relative;
}

/* PC端搜索框样式 */
.search-wrapper {
  padding: 8px 0;
  padding-bottom: 24px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border-bottom: 1px solid #f1f5f9;
  margin-top: -8px;
  will-change: transform;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-bar {
  max-width: 480px;
  margin: 0 auto;
  padding: 0 16px;
}

:deep(.ant-input-search) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.ant-input-search .ant-input) {
  margin: auto;
  height: 28px;
  font-size: 14px;
  padding: 0 12px;
}

:deep(.ant-input-search .ant-input-group-addon:last-child) {
  inset-inline-start: 0;
  padding: 0;
  border: 0;
}

:deep(.ant-input-search .ant-btn) {
  height: 36px;
  font-size: 14px;
  background: #ff8e53;
  border-color: #ff8e53;
  box-shadow: none;
}

:deep(.ant-input-search .ant-btn:hover) {
  background: #ff7a33;
  border-color: #ff7a33;
}

.search-icon {
  color: #114da1;
  font-size: 16px;
}

/* 分类和标签区域 */
.filter-section {
  padding: 6px 6px 6px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

.m-filter-section{
  padding: 6px 6px 6px 24px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

/* 分类标签样式 */
:deep(.category-tabs .ant-tabs-nav) {
  margin: 0 0 8px 0;
}

:deep(.category-tabs .ant-tabs-tab) {
  padding: 4px 12px;
  margin: 0 4px;
  font-size: 12px;
  color: #64748b;
  transition: all 0.3s ease;
  border-radius: 12px;

  &:hover {
    color: #ff8e53;
  }
}

:deep(.category-tabs .ant-tabs-tab-active) {
  background: rgba(255, 142, 83, 0.1);
  border-radius: 16px;
  color: #ff8e53 !important;
  font-weight: 500;
}

:deep(.category-tabs .ant-tabs-tab-btn) {
  color: inherit;
}

:deep(.category-tabs .ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #ff8e53 !important;
}

:deep(.category-tabs .ant-tabs-ink-bar) {
  display: none;
}

/* 标签样式 */
.tag-bar {
  padding: 4px 0;
}

.custom-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 13px;
  background: #f1f5f9;
  border: none;
  color: #64748b;
  transition: all 0.3s ease;
}

.custom-tag:hover {
  background: #e2e8f0;
  color: #3b82f6;
}

:deep(.ant-tag-checkable-checked) {
  background: #3b82f6 !important;
  color: white !important;
}

/* 移动端搜索框样式 */
.mobile-search {
  padding-top: 0;
  background: white;
  margin-top: -12px;
  margin-right: -40px;
  position: relative;
  z-index: 0;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mobile-search .search-bar {
  width: 100%;
}

.mobile-search :deep(.ant-btn-icon-only){
  width: 78px;
}

/* 固定状态的搜索框样式 */
.mobile-search-fixed {
  z-index: 4;
  position: fixed !important;
  top: 8px !important;
  right: 48px !important;
  width: 96px !important;
  height: 64px !important;
  padding: 0 4px !important;
  padding-right: 12px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  background: transparent !important;
  transform: translateY(0) !important;
  opacity: 1 !important;
}
.mobile-search-fixed :deep(.ant-btn-icon-only){
  width: 28px;
}
/* 搜索框过渡动画 */
.mobile-search {
  transform: translateX(0) scale(1);
  opacity: 1;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mobile-search.mobile-search-fixed {
  transform: translateX(calc(50vw - 50%)) scale(0.9);
}


/* 搜索框样式 */
.mobile-search :deep(.ant-input-search) {
  border: none !important;
  background: rgba(255, 255, 255, 0.95) !important;
  border-radius: 32px !important;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 2px 4px rgba(255, 142, 83, 0.05) !important;
  backdrop-filter: blur(8px) !important;
  border: 1px solid rgba(255, 142, 83, 0.1) !important;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  width: 100% !important;

  /* 移除输入框聚焦时的蓝色边框 */
  .ant-input-wrapper {
    .ant-input:focus {
      box-shadow: none !important;
      outline: none !important;
    }

    .ant-input-group-addon {
      border: none !important;
      background: transparent !important;
    }
  }
}

/* 固定状态时的搜索框样式 */
.mobile-search-fixed :deep(.ant-input-search) {
  width: 96px !important;
  transform: scale(1) !important;
  height: 32px;
  line-height: 32px;
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(255, 142, 83, 0.08) !important;
  border: 1px solid rgba(255, 142, 83, 0.15) !important;
  /* 修改搜索图标颜色 */
  .anticon-search {
    color: #fff !important;
    font-size: 20px !important;
    opacity: 0.9 !important;
  }
}

.mobile-search :deep(.ant-input) {
  height: 32px !important;
  font-size: 13px !important;
  padding: 0 12px !important;
  background: transparent !important;
  border: none !important;
  text-align: center !important;
  color: #1a1a1a !important;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

/* 添加动画效果 */
.mobile-search :deep(.ant-input-search) {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

.mobile-search :deep(.ant-input-search:active) {
  transform: scale(0.95) !important;
  background: rgba(255, 255, 255, 0.98) !important;
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.12),
    0 1px 4px rgba(0, 0, 0, 0.04) !important;
}

/* 占位符样式 */
.mobile-search :deep(.ant-input::placeholder) {
  color: #94a3b8 !important;
  font-size: 13px !important;
}

/* 新增加载更多样式 */
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
  color: #999;
  font-size: 14px;
}

.loading-more :deep(.van-loading) {
  margin-right: 8px;
}

.loading-more :deep(.van-loading__spinner) {
  color: #c4947e;
}

/* 修改无更多数据提示样式 */
.no-more-data-tip {
  margin: auto;
  text-align: center;
  padding: 16px;
  color: #c4947e;
  font-size: 14px;
  opacity: 0.8;
}

/* PC端内容区域 */
.pc-content {
  margin-top: 16px;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 移动端内容区域 */
.mobile-content {
  margin-top: 12px;
}

/* 移动端分类和标签样式 */
.mobile-categories,
.mobile-tags {
  padding: 12px 16px;

  .category-title,
  .tag-title {
    font-size: 14px;
    color: #1a1a1a;
    margin-bottom: 12px;
    font-weight: 500;
  }

  .category-list,
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .category-item,
  .tag-item {
    font-size: 13px;
    padding: 6px 12px;
    border-radius: 16px;
    background: #f8fafc;
    color: #64748b;
    transition: all 0.3s ease;
  }
}

/* 分页器样式美化 */
.pagination-wrapper {
  margin-top: 32px;
  padding-bottom: 12px;
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
  .ant-pagination-item {
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #ff8e53;
      a {
        color: #ff8e53;
      }
    }
  }

  .ant-pagination-item-active {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;

    a {
      color: white !important;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
    }
  }

  .ant-select-selector {
    border-radius: 8px !important;
    transition: all 0.3s ease;

    &:hover {
      border-color: #ff8e53 !important;
    }
  }

  .ant-select-focused .ant-select-selector {
    border-color: #ff8e53 !important;
    box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
  }

  .ant-pagination-options-quick-jumper {
    input {
      border-radius: 8px;
      transition: all 0.3s ease;

      &:hover,
      &:focus {
        border-color: #ff8e53;
      }

      &:focus {
        box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
      }
    }
  }
}

.mobile-list-container{
  margin-top: 84px;
}
/* 移动端顶部导航样式 */
.mobile-nav {
  position: fixed !important;
  top: 10px !important;
  left: 12px !important;
  width:100%;
  height: 64px !important;
  padding-top: 16px;
  transform: translateY(0) !important;
  opacity: 1 !important;
  margin: -16px -16px 0;
  padding-bottom: 10px;
  background: white;
  z-index: 2;
}

.mobile-nav :deep(.van-tabs__wrap) {
  padding: 0 16px;
  /* 添加滑动过渡效果 */
  .van-tabs__nav {
    user-select: none;
    transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  }
}

.mobile-nav :deep(.van-tab) {
  flex: none;
  min-width: 72px;
  font-size: 18px;
  color: #64748b;
  position: relative;
  transition: all 0.3s ease;

  /* 添加点击涟漪效果 */
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
    transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:active::after {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0.1;
  }
}

.mobile-nav :deep(.van-tab--active) {
  color: #ff8e53;
  font-weight: 500;
  font-size: 18px;
  transform: scale(1.05);
}

/* 优化滑动切换动画 */
.mobile-nav :deep(.van-tabs__content) {
  transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.mobile-nav :deep(.van-tabs__track) {
  transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

/* 榜单标签样式 */
.ranking-tabs {
  padding: 8px 16px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
}

.ranking-tabs :deep(.van-tabs__wrap) {
  height: 40px;
}

.ranking-tabs :deep(.van-tabs__nav) {
  background: #f8fafc;
  border-radius: 20px;
  height: 40px;
  margin-bottom: 20px;
}

.ranking-tabs :deep(.van-tab) {
  flex: 1;
  min-width: 80px;
  height: 32px;
  line-height: 32px;
  font-size: 11px;
  color: #64748b;
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  padding: 0;
}

.ranking-tabs :deep(.van-tab--active) {
  color: #ff8e53;
  font-weight: 500;
  font-size: 11px;
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.1),
    0 1px 4px rgba(255, 142, 83, 0.05);
}

/* 添加点击效果 */
.ranking-tabs :deep(.van-tab)::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  background: currentColor;
  opacity: 0;
  transition: opacity 0.3s;
}

.ranking-tabs :deep(.van-tab:active)::before {
  opacity: 0.1;
}

/* 隐藏底部线条 */
.ranking-tabs :deep(.van-tabs__line) {
  display: none;
}

/* 添加渐变背景效果 */
.ranking-tabs :deep(.van-tab--active) {
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.1),
    0 1px 4px rgba(255, 142, 83, 0.05);
}

/* 添加过渡动画 */
.ranking-tabs :deep(.van-tabs__nav) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ranking-tabs :deep(.van-tab) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

.discover-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.discover-btn:active {
  transform: translateY(1px);
}

/* PC端搜索框过渡动画 */
.search-wrapper {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-transitioning {
  transform: translateY(20px);
  opacity: 0;
}

.search-bar {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-bar-transitioning {
  transform: scale(1.05);
  opacity: 0;
}

/* 移动端搜索框过渡动画 */
.mobile-search {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-search-transitioning {
  transform: scale(1.1) translateY(-10px);
  opacity: 0;
}

/* 搜索框样式优化 */
.search-bar {
  cursor: pointer;
  transform-origin: center center;

  &:hover {
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.98);
  }
}

:deep(.ant-input-search) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &:active {
    transform: scale(0.98);
  }
}

/* 移动端搜索框样式优化 */
.mobile-search {
  &:active {
    transform: scale(0.95);
  }

  :deep(.ant-input-search) {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:active {
      transform: scale(0.95);
      background: rgba(255, 255, 255, 0.98) !important;
      box-shadow:
        0 2px 8px rgba(255, 142, 83, 0.12),
        0 1px 4px rgba(0, 0, 0, 0.04) !important;
    }
  }
}

/* 搜索按钮样式 */
.search-button {
  border: none;
  background: rgba(255, 142, 83, 0.1);
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
    background: linear-gradient(45deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.2));
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);

    .search-icon {
      transform: rotate(-5deg) scale(1.1);
    }

    .search-text {
      transform: translateX(2px);
    }
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(255, 142, 83, 0.1);
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
  color: #ff8e53;
  font-size: 16px;
  opacity: 0.8;
  transition: all 0.3s ease;
}

.search-divider {
  color: rgba(255, 142, 83, 0.3);
  font-size: 14px;
  transform: scale(0.9);
  margin: 0 -1px;
}

.search-text {
  color: #ff8e53;
  font-size: 13px;
  opacity: 0.8;
  transition: all 0.3s ease;
  font-weight: 500;
}

/* 标签样式优化 */
.mobile-nav :deep(.van-tabs__wrap) {
  padding: 0 16px;

  .van-tabs__nav {
    background: #f8fafc;
    border-radius: 20px;
    height: 24px;
  }

  .van-tab {
    flex: 1;
    min-width: 80px;
    height: 40px;
    line-height: 40px;
    font-size: 14px;
    color: #64748b;
    border-radius: 16px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    padding: 0;

    &--active {
      color: #ff8e53;
      font-weight: 500;
      background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
      box-shadow:
        0 2px 8px rgba(255, 142, 83, 0.1),
        0 1px 4px rgba(255, 142, 83, 0.05);
      backdrop-filter: blur(4px);
    }

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      border-radius: 16px;
      background: currentColor;
      opacity: 0;
      transition: opacity 0.3s;
    }

    &:active::before {
      opacity: 0.1;
    }
  }

  .van-tabs__line {
    display: none;
  }
}

.pc-category-item,
.pc-ranking-item {
  padding: 2px 12px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  background: rgba(24, 144, 255, 0.05);
  margin: 0 4px;

  &:hover {
    color: #ff8e53;
    background: rgba(24, 144, 255, 0.08);
  }

  &.active {
    color: #ff8e53;
    font-weight: 500;
    background: linear-gradient(135deg, rgba(199, 164, 129, 0.12), rgba(207, 131, 54, 0.15));
    box-shadow:
      0 2px 8px rgba(192, 153, 100, 0.1),
      0 1px 4px rgba(24, 144, 255, 0.05);
  }
}

/* 分类导航样式 */
.pc-category-nav,
.pc-ranking-nav {
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
  margin-bottom: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  &::-webkit-scrollbar {
    display: none;
  }
}

.pc-category-item,
.pc-ranking-item {
  padding: 2px 12px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 16px;
  background: rgba(24, 144, 255, 0.05);
  margin: 0 4px;

  &:hover {
    color: #ff8e53;
    background: rgba(24, 144, 255, 0.08);
  }

  &.active {
    color: #ff8e53;
    font-weight: 500;
    background: linear-gradient(135deg, rgba(199, 164, 129, 0.12), rgba(207, 131, 54, 0.15));
    box-shadow:
      0 2px 8px rgba(192, 153, 100, 0.1),
      0 1px 4px rgba(24, 144, 255, 0.05);
  }
}

.pc-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  padding: 24px;
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
    padding: 16px 24px;
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
</style>
