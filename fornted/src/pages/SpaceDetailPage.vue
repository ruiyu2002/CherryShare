<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->

    <!-- 背景动画 -->
    <div class="background-animation" v-if="device !== DEVICE_TYPE_ENUM.PC">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <a-flex
      justify="space-between"
      style="
        display: flex;
        height: 40px;
        align-items: center;
        width: 100%;
        margin-top: -20px;
        margin-bottom: -4px;
        background-color: #fdfaf9;
        border-radius: 10px;
      "
    >
      <a-tooltip
        v-if="device !== DEVICE_TYPE_ENUM.PC"
        :title="`${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
      >
        <a-progress
          style="margin-left: 10px"
          type="circle"
          :size="device !== DEVICE_TYPE_ENUM.PC ? 30 : 42"
          :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
        />
      </a-tooltip>
      <div v-if="device === DEVICE_TYPE_ENUM.PC ">
        <a-button
          type="link"
          size="large"
          ghost
          @click="showSpaceDetail"
          class="space-name-btn"
        >
          <span>{{ space.spaceName }}</span>
          <InfoCircleOutlined class="info-icon" />
        </a-button>
      </div>
      <div>
        <a-tooltip
          v-if="device === DEVICE_TYPE_ENUM.PC"
          :style="device === DEVICE_TYPE_ENUM.PC ? { marginLeft: '10px' } : { marginLeft: '0px' }"
          :title="`${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            style="margin-right: 10px"
            type="circle"
            :size="device !== DEVICE_TYPE_ENUM.PC ? 28 : 36"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
          />
        </a-tooltip>
        <a-button
          v-if="device !== DEVICE_TYPE_ENUM.PC"
          :icon="h(InfoCircleOutlined)"
          style="margin-right: 10px; background-color: #ffb071"
          type="primary"
          @click="showSpaceDetail"
        />
        <a-button
          v-if="canUploadPicture"
          :icon="h(PlusOutlined)"
          style="margin-right: 10px; background-color: #ffb071"
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 添加图片 </span>
        </a-button>
        <a-button
          style="margin-right: 10px; background-color: #35cb6a"
          v-if="canManageSpaceUser && space.spaceType === SPACE_TYPE_ENUM.TEAM"
          ghost
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage/${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 成员管理 </span>
        </a-button>
        <a-button
          v-if="space.spaceType === SPACE_TYPE_ENUM.TEAM"
          style="margin-right: 10px; background-color: #4096ff"
          :icon="h(MessageOutlined)"
          :href="`/space_chat/${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 团队聊天 </span>
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          style="margin-right: 10px; background-color: #e787cf"
          type="primary"
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 空间分析 </span>
        </a-button>
        <a-button
          v-if="canEditPicture"
          :icon="h(EditOutlined)"
          style="margin-right: 10px; background-color: #c1edf5"
          @click="doBatchEdit"
        >
          <span v-if="device === DEVICE_TYPE_ENUM.PC"> 批量编辑 </span>
        </a-button>
        <span>
          <color-picker format="hex" @pureColorChange="onColorChange" />
        </span>
      </div>
    </a-flex>
    <div style="margin-bottom: 2px"></div>
    <!-- 搜索表单 -->
    <div class="search-form-container">
      <PictureSearchForm
        ref="searchFormRef"
        :search-params="searchParams"
        @search="onSearch"
      />
    </div>

    <div class="content-spacing"></div>

    <!-- 图片列表 -->
    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <PictureList
        :dataList="dataList"
        :loading="loading"
        :showOp="true"
        :canEdit="canEditPicture"
        :canDelete="canDeletePicture"
        :onReload="fetchData"
      />
    </div>
    <div v-else>
      <van-pull-refresh v-model="loading" @refresh="onRefresh" :distance="80" :head-height="60">
        <!-- 内部组件 -->
        <MobilePictureList
          :dataList="dataList"
          :loading="loading"
          :canEdit="canEditPicture"
          :canDelete="canDeletePicture"
          :showOp="true"
          :onReload="fetchData"
        />
      </van-pull-refresh>
    </div>
    <!-- 分页 -->
    <a-pagination v-if="device === DEVICE_TYPE_ENUM.PC"
                  class="pc-pagination"
                  v-model:current="searchParams.current"
                  v-model:pageSize="searchParams.pageSize"
                  :page-size-options="['6', '12', '18', '24', '30']"
                  :total="total"
                  :show-total="(total) => `共 ${total} 条`"
                  @change="onPageChange"
                  show-size-changer
                  show-quick-jumper
    />
    <!-- 移动端分页器 -->
    <div v-else class="mobile-pagination-wrapper">
      <div class="pagination-info">
        <span>第 {{ searchParams.current }} 页</span>
        <span class="separator">/</span>
        <span>共 {{ Math.ceil(total / searchParams.pageSize) }} 页</span>
        <span class="total-count">· 共 {{ total }} 条</span>
      </div>
      <div class="page-actions">
        <a-button
          class="page-button prev"
          :disabled="searchParams.current === 1"
          @click="() => onPageChange(searchParams.current - 1, searchParams.pageSize)"
        >
          <LeftOutlined />
        </a-button>
        <div class="page-jump">
          <van-field
            v-model="jumpPage"
            type="number"
            :placeholder="searchParams.current"
            @keyup.enter="handleJumpPage"
          >
            <template #button>
              <van-button size="small" class="jump-btn" @click="handleJumpPage">跳转</van-button>
            </template>
          </van-field>
        </div>
        <a-button
          class="page-button next"
          :disabled="searchParams.current >= Math.ceil(total / searchParams.pageSize)"
          @click="() => onPageChange(searchParams.current + 1, searchParams.pageSize)"
        >
          <RightOutlined />
        </a-button>
      </div>
      <van-popup
        v-model:show="showPageSizePopup"
        position="bottom"
        round
        :style="{ maxHeight: '40vh' }"
      >
        <div class="page-size-popup">
          <div class="popup-title">每页显示条数</div>
          <div class="page-size-list">
            <div
              v-for="option in pageSizeOptions"
              :key="option.value"
              class="page-size-item"
              :class="{ active: searchParams.pageSize === option.value }"
              @click="() => {
                onPageSizeChange(option.value)
                showPageSizePopup = false
              }"
            >
              {{ option.text }}
            </div>
          </div>
        </div>
      </van-popup>
      <div class="page-size-trigger" @click="showPageSizePopup = true">
        每页 {{ searchParams.pageSize }} 条
      </div>
    </div>
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
    <!-- 添加空间详情模态框 -->
    <SpaceDetailModal
      v-model="detailVisible"
      :space-detail="spaceDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, computed, watch, ref } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import { EditOutlined, PlusOutlined, BarChartOutlined, TeamOutlined, LeftOutlined, RightOutlined, MessageOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import MobilePictureList from '@/components/MobilePictureList.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { useRouter } from 'vue-router'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM } from '@/constants/space.ts'
import { prevRoute } from '@/router'
import {
  Pagination as VanPagination,
  Field as VanField,
  Button as VanButton,
  DropdownMenu as VanDropdownMenu,
  DropdownItem as VanDropdownItem
} from 'vant'
import 'vant/es/pagination/style'
import 'vant/es/field/style'
import 'vant/es/button/style'
import 'vant/es/dropdown-menu/style'
import 'vant/es/dropdown-item/style'
import SpaceDetailModal from '@/components/SpaceDetailModal.vue'

interface Props {
  id: string | number,
}
const loginUserStore = useLoginUserStore()
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  await fetchSpaceDetail()
})
const router = useRouter()
const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})
// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

//是否显示成员管理按钮
const showUserButton = computed(() => {
  // 仅登录用户可分享
  console.log('prevRoute.name', prevRoute)
  return prevRoute.name !== '空间成员管理'
})
// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
// -------- 获取空间详情 --------
// 获取空间详情
const fetchSpaceDetail = async () => {
  loading.value = true
  try {
    // 如果没有传入空间id，则获取用户的第一个空间
    if (!props.id) {
      const res = await listSpaceVoByPageUsingPost({
        userId: loginUserStore.loginUser.id,
        current: 1,
        pageSize: 1,
        spaceType: SPACE_TYPE_ENUM.PRIVATE,
      })

      if (res.data.code === 0) {
        if (res.data.data?.records?.length > 0) {
          const firstSpace = res.data.data.records[0]
          // 更新路由到第一个空间
          router.replace(`/space/${firstSpace.id}`)
          space.value = firstSpace
        } else {
          // 如果用户没有空间，跳转到创建空间页面
          // message.info('您还没有空间，即将为您创建')
          router.replace('/add_space')
          return
        }
      } else {
        // message.error('获取空间列表失败：' + res.data.message)
        return
      }
    } else {
      // 有空间 ID 时直接获取空间详情
      const res = await getSpaceVoByIdUsingGet({
        id: props.id,
      })

      if (res.data.code === 0 && res.data.data) {
        space.value = res.data.data
      } else {
        // message.error('获取空间详情失败：' + res.data.message)
      }
    }

    // 获取空间详情后加载图片列表
    await fetchData()
  } catch (e: any) {
    // message.error('获取空间信息失败：' + e.message)
  } finally {
    loading.value = false
  }
}

// --------- 获取图片列表 --------
// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    if(!props.id){
      await router.push('/')
      return
    }
    const res = await listPictureVoByPageUsingPost({
      ...searchParams,
      spaceId: props.id,
    })
    if (res.data.code === 0) {
      dataList.value = res.data.data?.records ?? []
      total.value = res.data.data?.total ?? 0
      // 重置跳转页码输入框
      if (jumpPage.value) {
        jumpPage.value = ''
      }
    } else {
      // message.error('获取图片列表失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
    // message.error('获取图片列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 按照颜色搜索
const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
    total.value = dataList.value.length
  } else {
    // message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  if (!newSearchParams) return

  console.log('新的搜索参数:', newSearchParams)
  // 更新所有搜索参数
  Object.assign(searchParams, {
    ...newSearchParams,
    current: 1,
    pageSize: searchParams.pageSize,
    sortField: 'createTime',
    sortOrder: 'descend'
  })
  console.log('更新后的搜索参数:', searchParams)
  fetchData()
}

// ---- 批量编辑图片 -----
const batchEditPictureModalRef = ref()

// 批量编辑图片成功
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开批量编辑图片弹窗
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

// 新增用于判断是否正在刷新的变量，避免重复触发刷新操作
const isRefreshing = ref(false)

// 下拉刷新处理函数
const onRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true

  try {
    // 重置搜索参数
    searchParams.current = 1
    searchParams.pageSize = 12
    searchParams.searchText = undefined
    searchParams.category = undefined
    searchParams.tags = undefined
    searchParams.format = undefined
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
    searchParams.reviewStatus = undefined
    searchParams.sortField = 'createTime'
    searchParams.sortOrder = 'descend'

    // 重新获取数据
    await fetchData()
    // message.success('刷新成功')
  } catch (error) {
    console.error('刷新失败:', error)
    // message.error('刷新失败，请稍后重试')
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

// 空间 id 改变时，必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)

// 计算总页数
const totalPages = computed(() => {
  const pages = Math.ceil(total.value / searchParams.pageSize)
  return isNaN(pages) ? 1 : pages
})

// 跳转页码
const jumpPage = ref('')

// 处理页码跳转
const handleJumpPage = () => {
  const page = parseInt(jumpPage.value)
  if (isNaN(page) || page < 1 || page > Math.ceil(total.value / searchParams.pageSize)) {
    message.error(`请输入1-${Math.ceil(total.value / searchParams.pageSize)}之间的页码`)
    return
  }
  searchParams.current = page
  fetchData()
  jumpPage.value = ''
}

// 每页条数选项
const pageSizeOptions = [
  { text: '每页 6 条', value: 6 },
  { text: '每页 12 条', value: 12 },
  { text: '每页 18 条', value: 18 },
  { text: '每页 24 条', value: 24 },
  { text: '每页 30 条', value: 30 },
]

// 处理每页条数变化
const onPageSizeChange = (value: number) => {
  searchParams.current = 1
  searchParams.pageSize = value
  fetchData()
}

const showPageSizePopup = ref(false)

// 添加搜索表单的ref
const searchFormRef = ref()

const detailVisible = ref(false)
const spaceDetail = ref(null)

// 显示空间详情
const showSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data?.code === 0) {
      spaceDetail.value = res.data.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('获取空间详情失败:', error)
    // message.error('获取空间详情失败')
  }
}
</script>

<style scoped>
#spaceDetailPage {
  margin-bottom: 16px;
  margin-left: -20px;
  margin-right: -20px;
}

.search-spacing {
  margin-bottom: 16px;
}

.search-form-container {
  position: relative;
  width: 100%;
  margin: 0 auto;
  max-width: 800px;
}

.search-form-container :deep(.picture-search-form) {
  width: 100%;
  max-width: 800px;
}

.search-form-container :deep(.ant-input) {
  border-radius: 8px 0 0 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.search-form-container :deep(.ant-input):hover {
  border-color: #ff8e53;
}

.search-form-container :deep(.ant-input):focus {
  border-color: #ff8e53;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
}

.search-form-container :deep(.ant-input-group) {
  display: flex;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  overflow: hidden;
  height: 36px;
}

.search-form-container :deep(.ant-btn) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  transition: all 0.3s ease;
}

.search-form-container :deep(.ant-btn:hover) {
  opacity: 0.9;
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.3);
}

.content-spacing {
  margin-bottom: 16px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .search-spacing {
    margin-bottom: 8px;
  }

  .search-form-container {
    width: 100%;
    margin: 0;
    padding: 0 0 8px;
    max-width: none;
    left: 0;
    right: 0;
  }

  .search-form-container :deep(.search-container) {
    width: 100%;
    padding: 0 16px;
    margin: -8px 0 0;
  }

  .search-form-container :deep(.ant-form) {
    width: 100%;
    margin: 0;
  }

  .search-form-container :deep(.ant-form-item) {
    width: 100%;
    margin: 0;
  }

  .search-form-container :deep(.ant-input-group) {
    width: 100%;
    display: flex !important;
    height: 36px;
  }

  .search-form-container :deep(.ant-input) {
    flex: 1;

    line-height: 36px !important;
  }

  .search-form-container :deep(.ant-btn) {
    padding: 0 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    flex-shrink: 0;
    height: 36px !important;
  }

  .search-form-container :deep(.ant-btn > span:not(.anticon)) {
    display: none;
  }

  .search-form-container :deep(.ant-btn .anticon) {
    font-size: 16px;
    margin: 0;
    display: flex !important;
  }

  .content-spacing {
    margin-bottom: 0;
  }
}

#spaceDetailPage :deep(.vc-color-wrap) {
  width: 30px;
  height: 30px;
  border-radius: 5px;
  margin-top: -4px;
  margin-right: 8px;
}

/* 移动端分页器样式 */
.mobile-pagination-wrapper {
  margin: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination-info {
  text-align: center;
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.separator {
  margin: 0 8px;
  color: #999;
}

.total-count {
  color: #94a3b8;
  margin-left: 8px;
}

.page-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: center;
}

.page-button {
  width: 40px;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  border: none;
  transition: all 0.3s ease;
}

.page-button.prev,
.page-button.next {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
}

.page-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.page-button:disabled {
  background: #f5f5f5;
  color: #999;
}

.page-size-trigger {
  text-align: center;
  color: #64748b;
  font-size: 14px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
  margin-top: 12px;
}

.page-size-popup {
  padding: 16px;
}

.popup-title {
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #345750;
  margin-bottom: 16px;
}

.page-size-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-size-item {
  padding: 12px 16px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.page-size-item.active {
  color: #ff8e53;
  background: #fff6f3;
  font-weight: 500;
}

.page-size-item:active {
  opacity: 0.8;
}

.page-jump {
  flex: 1;
  margin: 0 8px;

  :deep(.van-field) {
    background: #f8fafc;
    border-radius: 8px;
    padding: 0 4px;
    height: 36px;

    .van-field__control {
      text-align: center;
      height: 36px;
      font-size: 14px;
      color: #64748b;
    }

    .van-field__right-icon {
      height: 100%;
      padding-right: 4px;
    }
  }

  .jump-btn {
    height: 28px;
    margin: 4px;
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;
    border-radius: 6px;
    padding: 0 12px;
    color: #fff;
  }
}

/* PC端分页器样式 */
.pc-pagination {
  margin-top: 8px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: flex-end;
  align-items: center;

  :deep(.ant-pagination) {
    .ant-pagination-total-text {
      margin-right: 16px;
      color: #64748b;
    }

    .ant-pagination-prev,
    .ant-pagination-next,
    .ant-pagination-item {
      margin-right: 8px;
      border-radius: 8px;
      border: 1px solid #e2e8f0;
      background: white;
      transition: all 0.3s ease;

      &:hover {
        border-color: #ff8e53;
        background: #fff6f3;

        a, button {
          color: #ff8e53;
        }
      }

      a, button {
        color: #64748b;
      }
    }

    .ant-pagination-item-active {
      background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%) !important;
      border: none !important;

      a {
        color: white !important;
      }

      &:hover {
        opacity: 0.9;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
      }
    }

    .ant-pagination-jump-prev,
    .ant-pagination-jump-next {
      .ant-pagination-item-container {
        .ant-pagination-item-link-icon {
          color: #ff8e53;
        }

        .ant-pagination-item-ellipsis {
          color: #64748b !important;
        }

        &:hover {
          .ant-pagination-item-ellipsis {
            color: #ff8e53 !important;
          }
        }
      }
    }

    .ant-pagination-options {
      .ant-select {
        .ant-select-selector {
          height: 32px !important;
          padding: 0 11px !important;
          border-radius: 8px !important;
          border: 1px solid #e2e8f0 !important;
          background: white !important;

          .ant-select-selection-item {
            line-height: 30px !important;
            color: #64748b;
          }

          &:hover {
            border-color: #ff8e53 !important;
            background: #fff6f3;
          }
        }

        &.ant-select-focused .ant-select-selector {
          border-color: #ff8e53 !important;
          box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1) !important;
        }
      }

      .ant-pagination-options-quick-jumper {
        color: #64748b;
        margin-left: 16px;

        input {
          width: 50px;
          height: 32px;
          margin: 0 8px;
          text-align: center;
          border-radius: 8px;
          border: 1px solid #e2e8f0;
          transition: all 0.3s ease;

          &:hover {
            border-color: #ff8e53;
            background: #fff6f3;
          }

          &:focus {
            border-color: #ff8e53;
            box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
          }
        }
      }
    }
  }

  :deep(.ant-select-dropdown) {
    padding: 4px;
    border-radius: 8px;
    overflow: hidden;

    .ant-select-item {
      margin: 2px 0;
      padding: 8px 12px;
      border-radius: 6px;
      transition: all 0.3s ease;

      &:hover {
        background: #fff6f3;
        color: #ff8e53;
      }

      &-option-selected {
        background: #fff6f3 !important;
        color: #ff8e53 !important;
        font-weight: 500;
      }

      &-option-active {
        background: #fff6f3 !important;
        color: #ff8e53 !important;
      }
    }
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .pc-pagination {
    margin: 16px;
    padding: 12px;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;

    :deep(.ant-pagination-options-quick-jumper) {
      display: none;
    }
  }
}

.space-name-btn {
  color: #345750;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    transform: translateY(-1px);
  }

  .info-icon {
    font-size: 16px;
    opacity: 0.7;
  }
}

.mobile-space-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 142, 83, 0.1);
  }
}

.mobile-space-name {
  font-size: 16px;
  color: #345750;
  font-weight: 500;
}

/* 背景动画 */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  opacity: 0.6;
}

.circle {
  position: absolute;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.08));
  border-radius: 50%;
  animation: float 20s infinite;
}

@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(2%, 2%) rotate(120deg);
  }
  66% {
    transform: translate(-2%, -1%) rotate(240deg);
  }
  100% {
    transform: translate(0, 0) rotate(360deg);
  }
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: -5s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  top: 40%;
  right: -200px;
  animation-delay: -10s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: 30%;
  animation-delay: -15s;
}

</style>
