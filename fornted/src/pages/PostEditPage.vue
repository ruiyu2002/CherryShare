<template>
  <div id="postEditPage">
    <a-card class="edit-card" :bordered="false">
      <template #title>
        <div class="page-header">
          <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>
        </div>
      </template>

      <a-form :model="postForm" layout="vertical">
        <!-- 标题 -->
        <a-form-item label="标题" required>
          <a-input v-model:value="postForm.title" placeholder="请输入标题" />
        </a-form-item>

        <!-- 分类 -->
        <a-form-item label="分类" required>
          <a-select v-model:value="postForm.category" placeholder="请选择分类">
            <a-select-option
              v-for="category in categories"
              :key="category"
              :value="category"
            >
              {{ category }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 内容编辑区 -->
        <a-form-item label="内容" required>
          <div class="editor-wrapper">
            <!-- wangEditor 编辑器 -->
            <div class="editor-container">
              <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
              <Editor
                class="editor-content"
                v-model="postForm.content"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>
        </a-form-item>

        <!-- 图片预览区 -->
        <div class="image-preview" v-if="postForm.attachments?.length">
          <div v-for="(img, index) in postForm.attachments" :key="index" class="image-item">
            <img :src="img.url" :alt="img.name" />
            <div class="image-actions">
              <DeleteOutlined @click="removeImage(index)" />
            </div>
          </div>
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

        <!-- 提交按钮 -->
        <div class="form-actions">
          <a-space>
            <a-button class="cancel-button" @click="router.back()">取消</a-button>
            <a-button
              class="submit-button"
              type="primary"
              @click="handleSubmit"
              :loading="submitting"
            >
              {{ isEdit ? '保存' : '发布' }}
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { addPostUsingPost, updatePostUsingPost, getPostByIdUsingGet } from '@/api/postController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import { listCategoryByTypeUsingGet } from '@/api/categoryController.ts'

const route = useRoute()
const router = useRouter()
const editorRef = shallowRef()
const submitting = ref(false)
const mode = 'default'
const uploading = ref(false)
const uploadProgress = ref(0)

// 编辑器配置
const editorConfig = {
  placeholder: '请输入内容...',
  html: true,
  MENU_CONF: {
    uploadImage: {
      // 自定义图片上传
      async customUpload(file: File, insertFn: any) {
        try {
          uploading.value = true
          uploadProgress.value = 0

          const res = await uploadPostImageUsingPost(
            {},
            {},
            file
          )

          if (res.data.code === 0 && res.data.data) {
            const imageCount = postForm.value.attachments.length
            const marker = `{img-${imageCount + 1}}`
            // 添加到附件列表
            postForm.value.attachments.push({
              type: 1,
              url: res.data.data.thumbnailUrl,
              name: file.name,
              size: file.size,
              sort: postForm.value.attachments.length + 1
            })

            // 插入图片但保存标记信息
            if (editorRef.value) {
              insertFn(res.data.data.url, marker, res.data.data.url)
              message.success('图片上传成功')
            }
          } else {
            message.error('图片上传失败')
          }
        } catch (error: any) {
          console.error('图片上传失败:', error)
          message.error('图片上传失败: ' + error.message)
        } finally {
          uploading.value = false
          uploadProgress.value = 0
        }
      }
    }
  },
  hoverbarKeys: {
    image: {
      menuKeys: ['deleteImage']
    }
  },
  parseHtml: (html: string) => {
    return html.replace(/<img[^>]*>/g, (match) => {
      return match.replace(/\s+/g, ' ').replace(/\s*\/?>$/, ' />')
    })
  }
}

// 工具栏配置
const toolbarConfig = {
  excludeKeys: [
    'group-video',  // 禁用视频
    'insertTable'   // 禁用表格
  ]
}

// 从路由状态获取帖子数据
const initPostData = () => {
  const postData = route.query.post
  if (postData) {
    const post = JSON.parse(postData as string)
    postForm.value = {
      title: post.title || '',
      category: post.category,
      attachments: [], // 清空附件列表
      content: ''     // 清空内容
    }
    // 设置编辑器内容
    if (editorRef.value) {
      // 清空编辑器内容
      editorRef.value.setHtml('')
    }
  } else if (isEdit.value) {
    message.error('获取帖子数据失败')
    router.back()
  }
}

// 编辑器创建完成时的回调
const handleCreated = (editor: any) => {
  editorRef.value = editor
  // 如果是编辑模式，加载帖子数据
  if (isEdit.value) {
    initPostData()
  }
}
const categories = ref([])
// 获取帖子分类列表
const fetchCategories = async () => {
  try {
    const res = await listCategoryByTypeUsingGet({ type: 1 }) // 1 表示帖子分类
    if (res.data?.code === 0 && res.data.data) {
      categories.value = res.data.data
    }
  } catch (error: any) {
    console.error('获取分类列表失败:', error)
    message.error('获取分类列表失败')
  }
}

onMounted(async () => {
  await fetchCategories()
})

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const isEdit = computed(() => !!route.params.id)

const postForm = ref({
  title: '',
  content: '',
  category: undefined,
  attachments: [] as API.PostAttachment[]
})

// 计算预览内容
const previewContent = computed(() => {
  let content = postForm.value.content

  // 替换图片标记为 Markdown 格式
  postForm.value.attachments.forEach((attach, index) => {
    const marker = `{img-${index + 1}}`
    content = content.replace(marker, `![${attach.name}](${attach.url})`)
  })

  return content
})

// 移除图片
const removeImage = (index: number) => {
  const marker = `{img-${index + 1}}`
  // 移除图片元素
  const imgRegex = new RegExp(`<img[^>]*alt="${marker}"[^>]*>`, 'g')
  const html = editorRef.value?.getHtml() || ''
  editorRef.value?.setHtml(html.replace(imgRegex, ''))

  postForm.value.attachments.splice(index, 1)

  // 重新排序剩余图片的标记
  postForm.value.attachments.forEach((attach, i) => {
    const oldMarker = `{img-${attach.sort}}`
    const newMarker = `{img-${i + 1}}`
    const oldImgRegex = new RegExp(`<img[^>]*alt="${oldMarker}"[^>]*>`, 'g')
    const html = editorRef.value?.getHtml() || ''
    editorRef.value?.setHtml(html.replace(oldImgRegex, (match) => {
      return match.replace(oldMarker, newMarker)
    }))
    attach.sort = i + 1
  })
}

// 提交表单
const handleSubmit = async () => {
  console.log('当前内容:', editorRef.value?.getText())
  console.log('当前附件:', postForm.value.attachments)
  if (!postForm.value.title?.trim()) {
    message.warning('请输入标题')
    return
  }
  // 获取HTML内容
  let htmlContent = editorRef.value?.getHtml() || ''

  // 将HTML中的图片转换为标记
  let editorContent = htmlContent
  postForm.value.attachments.forEach((attach, index) => {
    const marker = `{img-${index + 1}}`
    const imgRegex = new RegExp(`<img[^>]*alt="${marker}"[^>]*>`, 'g')
    editorContent = editorContent
      .replace(imgRegex, `\n${marker}\n`)
      .replace(/<p>/g, '')
      .replace(/<\/p>/g, '\n')
      .replace(/\n\n+/g, '\n\n')
      .trim()
  })

  // 检查每个图片标记是否在内容中
  for (let i = 0; i < postForm.value.attachments.length; i++) {
    const marker = `{img-${i + 1}}`
    if (!editorContent.includes(marker)) {
      message.error(`内容中缺少图片标记 ${marker}`)
      return
    }
  }

  if (!editorContent?.trim()) {
    message.warning('请输入内容')
    return
  }
  if (!postForm.value.category) {
    message.warning('请选择分类')
    return
  }

  submitting.value = true
  try {
    const submitData = {
      ...postForm.value,
      content: editorContent
    }
    if (isEdit.value) {
      await updatePostUsingPost({
        ...submitData,
        id: route.params.id as string
      })
      message.success('更新成功')
    } else {
      await addPostUsingPost(submitData)
      message.success('发布成功，等待审核')
    }
    router.push('/forum')
  } catch (error: any) {
    message.error((isEdit.value ? '更新' : '发布') + '失败：' + error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#postEditPage {
  width: 100%;
  margin: 0;
  padding: 0;
}

.edit-card {
  border-radius: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  :deep(.ant-card-body) {
    padding: 16px;
  }

  @media screen and (max-width: 768px) {
    margin: 0 -20px;
  }
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.editor-wrapper {
  position: relative;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 8px;
}

.image-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  margin: 16px 0;
}

.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 1;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .image-actions {
    position: absolute;
    top: 8px;
    right: 8px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    padding: 4px;
    border-radius: 4px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover .image-actions {
    opacity: 1;
  }
}

.form-actions {
  margin-top: 24px;
  text-align: right;

  .cancel-button,
  .submit-button {
    height: 40px;
    padding: 0 24px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
  }

  .cancel-button {
    border-color: #e2e8f0;

    &:hover {
      border-color: #ff8e53;
      color: #ff8e53;
    }
  }

  .submit-button {
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
    }

    &:active {
      transform: translateY(1px);
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #postEditPage {
    padding: 0;
    margin: 0;
  }

  .edit-card {
    border-radius: 0;
  }

  .editor-wrapper {
    flex-direction: column;
    padding: 4px;
  }

  .editor-content {
    height: 300px;
  }

  .image-preview {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }

  .upload-progress {
    margin: 12px 0;
    padding: 12px;
    border-radius: 8px;
  }

  .progress-text {
    font-size: 13px;
  }

  .form-actions {
    .cancel-button,
    .submit-button {
      height: 36px;
      padding: 0 20px;
      font-size: 13px;
    }
  }
}

/* wangEditor 相关样式 */
.editor-container {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid #d9d9d9;
}

.editor-content {
  height: 400px;
  overflow-y: auto;
}

:deep(.w-e-text-container) {
  background-color: #fff !important;
  height: auto !important;
  min-height: 300px !important;
}

:deep(.w-e-toolbar) {
  background-color: #fafafa !important;
  border-bottom: 1px solid #f0f0f0 !important;
  flex-wrap: wrap !important;
  padding: 4px !important;

  .w-e-bar-item {
    margin: 4px !important;
  }

  .w-e-bar-divider {
    margin: 4px !important;
  }
}

:deep(.w-e-text-container [data-slate-editor]) {
  padding: 12px !important;
}

:deep(.w-e-text-container [data-slate-editor] p) {
  margin: 8px 0 !important;
}

:deep(.w-e-text-container [data-slate-editor] img) {
  max-width: 100% !important;
  height: auto !important;
  margin: 8px 0 !important;
}

/* 上传进度样式 */
.upload-progress {
  margin: 16px 0;
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
</style>
