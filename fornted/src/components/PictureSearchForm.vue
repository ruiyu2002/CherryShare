<template>
  <div class="picture-search-form">
    <div class="search-container">
      <!-- 搜索表单 -->
      <a-form name="searchForm" layout="inline" :model="searchParams" @finish="doSearch">
        <!-- 关键词搜索部分 -->
        <a-form-item :label="device === DEVICE_TYPE_ENUM.PC ? '关键词' : ''" name="searchText">
          <a-input-group compact>
            <a-input
              v-model:value="searchParams.searchText"
              :style="device === DEVICE_TYPE_ENUM.PC ? 'width: 300px' : 'flex: 1'"
              placeholder="输入关键词"
              allow-clear
            />
            <a-button
              @click="showFilterModal = true"
              type="primary"
              class="filter-btn theme-btn"
            >
              <FilterOutlined />
              <span v-if="device === DEVICE_TYPE_ENUM.PC">高级筛选</span>
            </a-button>
          </a-input-group>
        </a-form-item>
      </a-form>
    </div>

    <!-- 高级筛选弹框 -->
    <a-modal
      v-model:open="showFilterModal"
      title="高级筛选"
      @ok="handleApplyFilter"
      @cancel="handleCancelFilter"
      class="custom-modal"
      :wrapClassName="'modal-wrapper'"
      :width="500"
    >
      <template #footer>
        <a-space>
          <a-button class="custom-clear-btn" type="default" @click="doClear">清空筛选</a-button>
          <a-button @click="handleCancelFilter">取消</a-button>
          <a-button type="primary" class="custom-submit-btn" @click="handleApplyFilter">
            应用筛选
          </a-button>
        </a-space>
      </template>

      <a-form layout="vertical" class="filter-form">
        <a-form-item name="category" label="分类">
          <a-auto-complete
            v-model:value="tempSearchParams.category"
            style="width: 100%"
            placeholder="请输入分类"
            :options="categoryOptions"
            allow-clear
          />
        </a-form-item>

        <a-form-item name="tags" label="标签">
          <a-select
            v-model:value="tempSearchParams.tags"
            style="width: 100%"
            mode="tags"
            placeholder="请输入标签"
            :options="tagOptions"
            allow-clear
          />
        </a-form-item>

        <a-form-item label="日期" name="dateRange">
          <template v-if="device === DEVICE_TYPE_ENUM.PC">
            <a-range-picker
              v-model:value="dateRange"
              class="date-picker-wrapper"
              :placeholder="['开始时间', '结束时间']"
              format="YYYY-MM-DD"
              :presets="rangePresets"
              @change="onRangeChange"
            />
          </template>
          <template v-else>
            <div class="mobile-date-picker">
              <van-field
                readonly
                clickable
                :value="formatMobileDate"
                :placeholder="'选择日期范围'"
                @click="showMobilePicker = true"
              />
              <van-calendar
                v-model:show="showMobilePicker"
                type="range"
                :show-confirm="true"
                @confirm="onMobileRangeConfirm"
                color="#ff8e53"
              />
            </div>
          </template>
        </a-form-item>

        <a-form-item label="图片格式">
          <a-select
            v-model:value="tempSearchParams.format"
            style="width: 100%"
            mode="multiple"
            placeholder="请选择图片格式"
            :options="picFormatOptions"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.picture-search-form {
  padding: 20px 0 0;
}

:deep(.theme-btn.filter-btn) {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%) !important;
  border: none !important;
  max-width: 100px;
  transition: all 0.3s ease !important;

  &:hover {
    opacity: 0.9 !important;
    box-shadow: 0 2px 8px rgba(255, 142, 83, 0.3) !important;
  }

  &, & * {
    color: #fff !important;
  }

  &:focus, &:active {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%) !important;
  }
}

.search-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 16px;
}

@media screen and (min-width: 768px) {
  .search-container :deep(.ant-form) {
    display: flex;
    justify-content: center;
  }

  .search-container :deep(.ant-form-item) {
    margin-bottom: 0;
  }

  .search-container :deep(.ant-input-group) {
    display: flex;
    align-items: center;
  }

  .search-container :deep(.ant-btn) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
}

/* 移动端样式 */
@media screen and (max-width: 768px) {
  .search-container :deep(.ant-input-group) {
    display: flex;
  }

  .search-container :deep(.ant-btn) {
    min-width: 80px;
    padding: 0 8px;
  }
}

/* 弹框全局样式 */
:deep(.modal-wrapper) {
  .custom-modal {
    .ant-modal-content {
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    }

    .ant-modal-header {
      background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
      padding: 16px 24px;
      border-bottom: 1px solid #f3f4f6;

      .ant-modal-title {
        font-size: 16px;
        font-weight: 600;
        color: #345750;
      }
    }

    .ant-modal-body {
      padding: 24px;
      background: #fff;
    }

    .ant-modal-footer {
      padding: 16px 24px;
      border-top: 1px solid #f3f4f6;
      background: #fafafa;
    }

    /* 表单控件样式 */
    .ant-form-item-label > label {
      color: #345750;
      font-weight: 500;
      font-size: 14px;
    }

    .ant-input,
    .ant-select-selector,
    .ant-picker {
      border-radius: 8px;
      border: 1px solid #e2e8f0;
      height: 40px;
      padding: 4px 12px;
      background: #fff;
      transition: all 0.3s ease;

      &:hover {
        border-color: #ff8e53;
      }

      &:focus {
        border-color: #ff8e53;
        box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
      }
    }

    /* 按钮样式 */
    .custom-submit-btn {
      background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
      border: none;
      height: 36px;
      border-radius: 8px;
      font-weight: 500;
      color: #fff;
      box-shadow: 0 2px 8px rgba(255, 142, 83, 0.2);
    }

    .custom-clear-btn {
      height: 36px;
      border-radius: 8px;
      color: #64748b;
      border-color: #e2e8f0;
      background: #fff;

      &:hover {
        color: #ff8e53;
        border-color: #ff8e53;
        background: #fff6f3;
      }
    }

    /* 日期选择器样式 */
    .custom-date-picker {
      width: 100%;

      &.mobile {
        :deep(.ant-picker-panels) {
          flex-direction: column;
          width: 100%;
        }

        :deep(.ant-picker-panel) {
          width: 100%;
        }
      }
    }

    :deep(.ant-picker) {
      width: 100%;
    }

    :deep(.ant-picker-panel-container) {
      @media screen and (max-width: 768px) {
        width: 100%;
      }
    }

    :deep(.ant-picker-range-wrapper) {
      @media screen and (max-width: 768px) {
        width: 100%;
        min-width: unset;
      }
    }
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  :deep(.modal-wrapper) {
    .custom-modal {
      .ant-modal-content {
        border-radius: 8px;
      }

      .ant-modal-header {
        padding: 12px 16px;
      }

      .ant-modal-body {
        padding: 16px;
      }

      .ant-modal-footer {
        padding: 12px 16px;
      }

      .custom-submit-btn,
      .custom-clear-btn {
        height: 32px;
        padding: 0 16px;
        font-size: 13px;
      }

      .ant-form-item {
        margin-bottom: 12px;
      }

      .ant-form-item-label > label {
        font-size: 13px;
      }
    }
  }
}

/* PC端日期选择器样式 */
.date-picker-wrapper {
  width: 100% !important;
}

/* 移动端日期选择器样式 */
.mobile-date-picker {
  :deep(.van-field) {
    padding: 8px 12px;
    background-color: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;

    &:focus {
      border-color: #ff8e53;
    }
  }

  :deep(.van-calendar) {
    --van-calendar-selected-day-background: #ff8e53;

    .van-calendar__confirm {
      background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    }
  }
}
</style>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import dayjs from 'dayjs'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { RightOutlined, DownOutlined, SearchOutlined, RedoOutlined, FilterOutlined } from '@ant-design/icons-vue'
import { Calendar, Field } from 'vant'
import 'vant/es/calendar/style'
import 'vant/es/field/style'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

interface Props {
  onSearch?: (searchParams: API.PictureQueryRequest) => void
}

const props = defineProps<Props>()

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({})

// 新增控制展开折叠的响应式变量
const isExpanded = ref(false)

// 搜索数据
const doSearch = () => {
  props.onSearch?.(searchParams)
}

// 标签和分类选项
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

const dateRange = ref<[]>([])

const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates?.length >= 2) {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  } else {
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
  }
}

// 时间范围预设
const rangePresets = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])

// 定义允许的图片格式选项
const ALLOW_FORMAT_LIST = ['jpeg', 'png', 'jpg', 'webp', 'gif']
const picFormatOptions = ref(ALLOW_FORMAT_LIST.map((format) => ({ value: format, label: format })))

// 清理
const doClear = () => {
  // 取消所有对象的值
  Object.keys(searchParams).forEach((key) => {
    searchParams[key] = undefined
  })
  // 日期筛选项单独清空，必须定义为空数组
  dateRange.value = []
  // 清空后重新搜索
  props.onSearch?.(searchParams)
}

// 修改状态变量
const showFilterModal = ref(false)

// 临时搜索参数
const tempSearchParams = reactive({
  category: '',
  tags: [],
  name: '',
  format: [],
})

// 修改处理函数
const handleApplyFilter = () => {
  Object.assign(searchParams, tempSearchParams)
  showFilterModal.value = false
  props.onSearch?.(searchParams)
}

const handleCancelFilter = () => {
  showFilterModal.value = false
  Object.assign(tempSearchParams, searchParams)
}

const showMobilePicker = ref(false)
const formatMobileDate = computed(() => {
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) return ''
  return `${dayjs(dateRange.value[0]).format('YYYY-MM-DD')} 至 ${dayjs(dateRange.value[1]).format('YYYY-MM-DD')}`
})

const onMobileRangeConfirm = (dates: Date[]) => {
  dateRange.value = [dayjs(dates[0]), dayjs(dates[1])]
  showMobilePicker.value = false
  onRangeChange(dateRange.value, [
    dayjs(dates[0]).format('YYYY-MM-DD'),
    dayjs(dates[1]).format('YYYY-MM-DD')
  ])
}
</script>
