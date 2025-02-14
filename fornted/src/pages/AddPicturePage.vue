<template>
  <div id="addPicturePage">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 空间信息 -->
      <div v-if="spaceId" class="space-info">
        保存至空间：<a :href="`/space/${spaceId}`">{{ spaceId }}</a>
      </div>

      <!-- 根据是否有图片使用不同的布局 -->
      <div :class="['content-layout', { 'has-picture': picture }]">
        <!-- 上传区域 -->
        <div class="upload-section">
          <div class="upload-tabs">
            <a-tabs v-model:activeKey="uploadType">
              <a-tab-pane key="file" tab="文件上传">
                <PictureUpload
                  :picture="picture"
                  :spaceId="spaceId"
                  :onSuccess="onSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
              </a-tab-pane>
              <a-tab-pane key="url" tab="URL 上传" force-render>
                <UrlPictureUpload
                  :picture="picture"
                  :spaceId="spaceId"
                  :onSuccess="onSuccess"
                  :onUploadStart="onUploadStart"
                  :onUploadProgress="onUploadProgress"
                />
              </a-tab-pane>
            </a-tabs>
          </div>
          <!-- 上传进度提示 -->
          <div v-if="uploading" class="upload-progress">
            <a-progress
              :percent="uploadProgress"
              :status="uploadProgress >= 100 ? 'success' : 'active'"
              :stroke-color="{ from: '#ff8e53', to: '#ff6b6b' }"
            />
            <div class="progress-text">
              {{ uploadProgress >= 100 ? '处理中...' : '上传中...' }}
            </div>
          </div>
        </div>

        <!-- 表单区域 -->
        <div v-if="picture && !uploading" class="form-section">
          <!-- 图片编辑工具栏移到这里 -->
          <div class="edit-bar">
            <a-space size="middle">
              <a-button class="edit-button" @click="doEditPicture">
                <template #icon><EditOutlined /></template>
                编辑图片
              </a-button>
              <a-button type="primary" class="ai-button" @click="doImagePainting">
                <template #icon><FullscreenOutlined /></template>
                AI 扩图
              </a-button>
            </a-space>
          </div>

          <div class="form-container">
            <!-- 图片信息表单 -->
            <a-form
              name="pictureForm"
              layout="vertical"
              :model="pictureForm"
              @finish="handleSubmit"
            >
              <a-form-item name="name" label="名称">
                <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
              </a-form-item>

              <a-form-item name="introduction" label="简介">
                <a-textarea
                  v-model:value="pictureForm.introduction"
                  placeholder="请输入简介"
                  :auto-size="{ minRows: 2, maxRows: 5 }"
                  allow-clear
                />
              </a-form-item>

              <a-form-item name="category" label="分类">
                <a-auto-complete
                  v-model:value="pictureForm.category"
                  placeholder="请输入分类"
                  :options="categoryOptions"
                  allow-clear
                />
              </a-form-item>

              <a-form-item name="tags" label="标签">
                <a-select
                  v-model:value="pictureForm.tags"
                  mode="tags"
                  placeholder="请输入标签"
                  :options="tagOptions"
                  allow-clear
                />
              </a-form-item>

              <a-form-item>
                <a-button type="primary" html-type="submit" class="submit-button">创建</a-button>
              </a-form-item>
            </a-form>
          </div>
        </div>
      </div>

      <!-- 编辑器组件 -->
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="spaceInfo"
        :onSuccess="onCropSuccess"
      />
      <ImageOutPainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { h, onMounted, reactive, ref, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { SPACE_LEVEL_ENUM } from '@/constants/space'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})
const uploadType = ref<'file' | 'url'>('file')

// 上传状态
const uploading = ref(false)
const uploadProgress = ref(0)

// 开始上传
const onUploadStart = () => {
  uploading.value = true
  uploadProgress.value = 0
  picture.value = undefined // 清空之前的图片数据
}

// 上传进度
const onUploadProgress = (progress: number) => {
  uploadProgress.value = Math.min(progress, 99) // 保留最后1%给处理时间
}

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  uploading.value = false
  uploadProgress.value = 100
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

const router = useRouter()

// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})

// 获取空间信息
const spaceInfo = ref<API.SpaceVO>()
const getSpaceInfo = async () => {
  if (!spaceId.value) return
  const res = await getSpaceVoByIdUsingGet({ id: spaceId.value })
  if (res.data.code === 0 && res.data.data) {
    spaceInfo.value = res.data.data
  }
}

onMounted(() => {
  getSpaceInfo()
})

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  // console.log(values)
  const pictureId = picture.value.id
  const newspaceId = spaceId.value
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    // 判断是否为公共空间
    if (spaceInfo.value?.spaceLevel === SPACE_LEVEL_ENUM.PUBLIC) {
      const modal = Modal.success({
        title: '上传成功',
        content: h('div', {}, [
          h('p', '您的图片已成功上传到公共图库！'),
          h('p', '由于这是公共图库，您的图片需要经过审核后才能展示。'),
          h('p', '审核通过后，您的图片将出现在公共图库中。'),
        ]),
        maskClosable: true,
        centered: true,
        okText: '知道了',
        onOk: () => {
          router.push({
            path: `/picture/${pictureId}`,
          })
        },
        afterClose: () => {
          router.push({
            path: `/picture/${pictureId}`,
          })
        },
      })
      // 6秒后自动关闭
      setTimeout(() => {
        modal.destroy()
        router.push({
          path: `/picture/${pictureId}`,
        })
      }, 5000)
    } else {
      const modal = Modal.success({
        title: '上传成功',
        content: h('div', {}, [
          h('p', '您的图片已成功上传到相应空间！'),
          h('p', '您可以在对应的空间中查看和管理您的图片。'),
        ]),
        maskClosable: true,
        centered: true,
        okText: '知道了',
        onOk: () => {
          router.push({
            path: `/space/${newspaceId}`,
          })
        },
        afterClose: () => {
          router.push({
            path: `/space/${newspaceId}`,
          })
        },
      })
      // 6秒后自动关闭
      setTimeout(() => {
        modal.destroy()
        router.push({
          path: `/space/${spaceId.value}`,
        })
      }, 6000)
    }
  } else {
    message.error('创建失败，' + res.data.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
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

const route = useRoute()

// 获取老数据
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

onMounted(() => {
  getOldPicture()
})

const imageCropperRef = ref()

// 编辑图片
const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- AI 扩图引用 -----
const imageOutPaintingRef = ref()

// 打开 AI 扩图弹窗
const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

// AI 扩图保存事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}
</script>

<style scoped>
#addPicturePage {
  min-height: calc(100vh - 120px);
  background: #f8fafc;
  padding: 16px 0;
}

.main-content {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 16px;
}

.space-info {
  color: #64748b;
  margin-bottom: 12px;
  font-size: 14px;
}

.space-info a {
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.3s ease;
}

.space-info a:hover {
  color: #2563eb;
}

.upload-tabs {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

:deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

:deep(.ant-tabs-tab) {
  font-size: 15px;
  padding: 8px 16px;
}

:deep(.ant-tabs-tab-active) {
  font-weight: 500;
}

:deep(.ant-tabs-ink-bar) {
  background: #ff8e53;
}

.edit-bar {
  text-align: center;
  margin-bottom: 16px;
  background: white;
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.edit-button,
.ai-button {
  height: 40px;
  padding: 0 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.edit-button {
  border-color: #e2e8f0;
}

.edit-button:hover {
  border-color: #ff8e53;
  color: #ff8e53;
}

.ai-button {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.ai-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.form-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.ant-form-item-label > label) {
  font-size: 14px;
  color: #64748b;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
  border-color: #e2e8f0;
  transition: all 0.3s ease;
}

:deep(.ant-input:hover),
:deep(.ant-select:hover .ant-select-selector),
:deep(.ant-input-affix-wrapper:hover) {
  border-color: #ff8e53;
}

:deep(.ant-input:focus),
:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: #ff8e53;
  box-shadow: 0 0 0 2px rgba(255, 142, 83, 0.1);
}

.submit-button {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

.submit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.submit-button:active {
  transform: translateY(1px);
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #addPicturePage {
    padding: 12px 0;
  }

  .main-content {
    padding: 0 12px;
  }

  .upload-tabs,
  .form-container {
    padding: 16px;
    border-radius: 12px;
  }

  .edit-bar {
    margin-bottom: 12px;
    padding: 12px;
    border-radius: 12px;
  }

  .edit-button,
  .ai-button {
    height: 36px;
    padding: 0 16px;
    font-size: 13px;
  }
}

/* 添加新的布局样式 */
.content-layout {
  margin-top: 16px;
  /* 初始状态下上传区域居中 */
  display: flex;
  justify-content: center;
}

/* 有图片时的布局 */
.content-layout.has-picture {
  justify-content: flex-start;
  gap: 24px;
}

.upload-section {
  /* 初始状态下的宽度 */
  width: 100%;
  max-width: 800px; /* 限制初始状态下的最大宽度 */
}

/* 有图片时的布局样式 */
.has-picture .upload-section {
  flex: 2;
  min-width: 0;
}

.form-section {
  flex: 1;
  min-width: 0;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .content-layout,
  .content-layout.has-picture {
    flex-direction: column;
    gap: 16px;
  }

  .upload-section,
  .has-picture .upload-section,
  .form-section {
    width: 100%;
  }
}

/* 大屏幕优化 */
@media screen and (min-width: 1200px) {
  .main-content {
    max-width: 1400px;
  }

  .content-layout.has-picture {
    gap: 32px;
  }
}

/* 上传进度样式 */
.upload-progress {
  margin-top: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.progress-text {
  margin-top: 8px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
}

:deep(.ant-progress-bg) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.ant-progress-text) {
  color: #64748b;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .upload-progress {
    margin-top: 12px;
    padding: 12px;
    border-radius: 8px;
  }

  .progress-text {
    font-size: 13px;
  }
}
</style>
