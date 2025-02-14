<template>
  <div class="markdown-content" :class="{ 'mobile-view': isMobile }">
    <div class="content-wrapper">
      <div v-for="(block, index) in blocks" :key="index">
        <!-- 文本块 -->
        <div v-if="block.type === 'text'" class="text-block" v-html="block.content"></div>
        <!-- 图片块 -->
        <div v-else-if="block.type === 'image-group'" class="image-wrapper">
          <div class="image-grid" :class="getGridClass(block.images?.length || 0)">
            <div
              v-for="(img, imgIndex) in block.images"
              :key="imgIndex"
              class="grid-item"
            >
              <img
                :src="img.src"
                :alt="img.alt"
                class="grid-image"
                @click="handleImageClick(img.src)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, h } from 'vue'
import { Modal } from 'ant-design-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

const props = defineProps<{
  content: string
}>()

// 判断是否为移动端
const isMobile = computed(() => {
  return getDeviceType() === DEVICE_TYPE_ENUM.MOBILE
})

interface ImageItem {
  src: string
  alt: string
}

interface ContentBlock {
  type: 'text' | 'image-group'
  content?: string
  images?: ImageItem[]
}

const blocks = ref<ContentBlock[]>([])

// 解析内容
const parseContent = (content: string) => {
  const result: ContentBlock[] = []
  const lines = content.split('\n')
  let currentText = ''
  let currentImages: ImageItem[] = []

  lines.forEach(line => {
    const imgMatch = line.match(/!\[(.*?)\]\((.*?)\)/)
    if (imgMatch) {
      // 如果之前有文本，先添加文本块
      if (currentText) {
        result.push({ type: 'text', content: currentText.trim() })
        currentText = ''
      }
      // 收集图片
      currentImages.push({
        alt: imgMatch[1],
        src: imgMatch[2]
      })
    } else {
      // 只有当当前行不为空时，才处理图片组
      if (line.trim()) {
        // 如果有收集的图片，先添加图片组
        if (currentImages.length > 0) {
          result.push({
            type: 'image-group',
            images: [...currentImages]
          })
          currentImages = []
        }
        currentText += line + '\n'
      }
    }
  })

  // 处理最后的块
  if (currentImages.length > 0) {
    result.push({
      type: 'image-group',
      images: currentImages
    })
  }
  if (currentText.trim()) {
    result.push({ type: 'text', content: currentText.trim() })
  }

  // 合并连续的图片组
  const mergedResult: ContentBlock[] = []
  let currentGroup: ImageItem[] = []

  result.forEach((block, index) => {
    if (block.type === 'image-group') {
      currentGroup.push(...(block.images || []))
    } else {
      // 如果有收集的图片组，先添加
      if (currentGroup.length > 0) {
        mergedResult.push({
          type: 'image-group',
          images: currentGroup
        })
        currentGroup = []
      }
      mergedResult.push(block)
    }
  })

  // 处理最后可能剩余的图片组
  if (currentGroup.length > 0) {
    mergedResult.push({
      type: 'image-group',
      images: currentGroup
    })
  }

  blocks.value = mergedResult
}

// 获取网格类名
const getGridClass = (count: number) => {
  if (count === 1) return 'single'
  if (count === 2) return 'double'
  if (count === 4) return 'four'
  if (count === 3) return 'three'
  if (count === 6) return 'six'
  return count > 6 ? 'nine' : 'grid'  // 超过6张使用九宫格
}

// 处理图片点击
const handleImageClick = (src: string) => {
  const urls = blocks.value
    .filter(block => block.type === 'image-group')
    .flatMap(block => block.images?.map(img => img.src) || [])

  Modal.info({
    title: null,
    icon: null,
    content: h('img', {
      src: src.replace('_thumbnail', ''), // 移除缩略图后缀以显示原图
      style: {
        width: '100%',
        height: 'auto',
        maxHeight: '80vh',
        objectFit: 'contain'
      }
    }),
    width: '90%',
    centered: true,
    maskClosable: true,
    okText: '关闭',
    wrapClassName: 'image-preview-modal'
  })
}

// 监听内容变化
watch(() => props.content, (newContent) => {
  if (newContent) {
    parseContent(newContent)
  }
}, { immediate: true })
</script>

<style scoped>
.markdown-content {
  font-size: 16px;
  line-height: 1.8;
  color: #374151;
}

.content-wrapper {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  width: 100%;
}

/* 文本样式优化 */
.text-block {
  margin: 1em 0;
  letter-spacing: 0.5px;
  width: 100%;
  display: block;
}

.text-block:first-child {
  margin-top: 0;
}

.text-block:last-child {
  margin-bottom: 0;
}

.image-wrapper {
  width: 100%;
  margin: 16px 0;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  max-width: 800px;
  width: 100%;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.grid-item {
  aspect-ratio: 1;
  overflow: hidden;
}

.image-grid.single .grid-item {
  aspect-ratio: 16/9;
}

.grid-image {
  width: 100%;
  height: 100%;
  cursor: pointer;
  object-fit: cover;
  transition: transform 0.3s ease;
  border-radius: 4px;
  display: block;
}

.grid-image:hover {
  transform: scale(1.02);
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .mobile-view .content-wrapper {
    border-radius: 0;
    box-shadow: none;
    min-width: 98vw;
    margin: 0 -12px;
  }

  .text-block {
    font-size: 15px;
    line-height: 1.6;
  }

  .image-grid {
    gap: 2px;
    padding: 4px;
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .content-wrapper {
    background: #1a1a1a;
    color: #e5e7eb;
  }

  .image-grid {
    background: #262626;
  }
}

/* 不同数量图片的网格布局 */
.image-grid.single {
  grid-template-columns: 1fr;
}

.image-grid.double {
  grid-template-columns: repeat(2, 1fr);
}

.image-grid.three {
  grid-template-columns: repeat(3, 1fr);
}

.image-grid.four {
  grid-template-columns: repeat(2, 1fr);
}

.image-grid.six {
  grid-template-columns: repeat(3, 1fr);
}

.image-grid.nine {
  grid-template-columns: repeat(3, 1fr);
}

/* 添加预览模态框样式 */
:deep(.image-preview-modal) {
  .ant-modal-content {
    background: rgba(0, 0, 0, 0.85);
    padding: 0;
  }
  .ant-modal-body {
    padding: 24px;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .ant-modal-confirm-btns {
    text-align: center;
    margin-top: 16px;
  }
  .ant-btn-primary {
    background: transparent;
    border-color: #fff;
    color: #fff;
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}
</style>
