<template>
  <div id="globalFooter">
    <div v-if="device === DEVICE_TYPE_ENUM.PC" class="pc-footer">
      <div class="wave-container">
        <div class="wave wave1"></div>
        <div class="wave wave2"></div>
        <div class="wave wave3"></div>
      </div>
      <div class="footer-content">
        <p>
          © {{ currentYear }} 鹿梦. All rights reserved. <span style="width: 10px"></span>
          <a href="https://beian.miit.gov.cn/" target="_blank">陇ICP备2024012699号</a>
        </p>
      </div>
    </div>

    <van-tabbar v-else v-model="active" class="mobile-tabbar" :safe-area-inset-bottom="true">
      <van-tabbar-item to="/">
        <template #icon="props">
          <div id="footerHomeIcon" class="custom-icon">
            <van-icon name="wap-home" :class="{ 'icon-active': props.active }" />
          </div>
        </template>
        <span :class="['tab-text', { 'text-active-home': active === 0 }]">首页</span>
      </van-tabbar-item>

      <van-tabbar-item to="/forum">
        <template #icon="props">
          <div id="footerForumIcon" class="custom-icon">
            <van-icon name="friends" :class="{ 'icon-active-forum': props.active }" />
          </div>
        </template>
        <span :class="['tab-text', { 'text-active-forum': active === 1 }]">论坛</span>
      </van-tabbar-item>

      <van-tabbar-item @click="handleAddClick">
        <template #icon>
          <div id="footerAddButton" class="add-button">
            <van-icon name="plus" />
          </div>
        </template>
      </van-tabbar-item>

      <van-tabbar-item to="/chat-list">
        <template #icon="props">
          <div id="footerChatIcon" class="custom-icon">
            <van-icon name="chat" :class="{ 'icon-active-chat': props.active }" />
          </div>
        </template>
        <span :class="['tab-text', { 'text-active-chat': active === 3 }]">聊天</span>
      </van-tabbar-item>

      <van-tabbar-item to="/my">
        <template #icon="props">
          <div id="footerMyIcon" class="custom-icon">
            <van-icon name="manager" :class="{ 'icon-active-my': props.active }" />
          </div>
        </template>
        <span :class="['tab-text', { 'text-active-my': active === 4 }]">我的</span>
      </van-tabbar-item>
    </van-tabbar>

    <UploadActionSheet v-model="showActionSheet" />
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { getDeviceType } from '@/utils/device.ts'
import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import '@lottiefiles/lottie-player'
import UploadActionSheet from './UploadActionSheet.vue'
import { getCurrentYear } from '@/utils/date'

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')

// 用于跟踪当前活动的底部标签页，初始化为 0，对应第一个tabbar项（可根据实际默认高亮项调整）
const active = ref(0)

// 上传选项相关
const showActionSheet = ref(false)
const loginUserStore = useLoginUserStore()

// 动作面板选项
const actions = [
  {
    name: '发布帖子',
    color: '#ff8e53',
    subname: '分享您的想法和图片',
    icon: 'edit',
    path: '/post/edit'
  },
  {
    name: '上传到公共图库',
    color: '#ff8e53',
    subname: '您的图片将在审核通过后展示给所有用户',
    icon: 'photo-o',
    path: '/add_picture'
  },
  {
    name: '上传到个人空间',
    color: '#ff6b6b',
    subname: '仅您自己可以查看和管理',
    icon: 'user-o',
    path: '/add_picture',
    needSpace: true
  },
]

// 处理添加按钮点击
const handleAddClick = () => {
  if (!loginUserStore.loginUser.id) {
    router.push('/user/login')
    return
  }
  showActionSheet.value = true
}

// 页面加载时获取设备类型并获取数据，同时设置初始高亮项
onMounted(async () => {
  device.value = await getDeviceType()
  const currentRoute = router.currentRoute.value
  const currentPath = currentRoute.path
  if (currentPath === '/') {
    active.value = 0
  } else if (currentPath === '/add_picture') {
    active.value = 1
  } else if (currentPath === '/my') {
    active.value = 4
  }
})

// 组件卸载前关闭 action sheet
onBeforeUnmount(() => {
  showActionSheet.value = false
})

// 监听路由变化，更新高亮菜单项对应的active值
router.afterEach((to) => {
  const currentPath = to.path
  if (currentPath === '/') {
    active.value = 0
    showActionSheet.value = false
  } else if (currentPath === '/forum') {
    active.value = 1
    showActionSheet.value = false
  } else if (currentPath === '/chat-list') {
    active.value = 3
    showActionSheet.value = false
  } else if (currentPath === '/my') {
    active.value = 4
    showActionSheet.value = false
  }
})

const currentYear = computed(() => getCurrentYear())
</script>

<style scoped>
#globalFooter {
  z-index: 1;
}

.pc-footer {
  position: relative;
  height: 24px;
  text-align: center;
  padding: 4px 0;
  background: linear-gradient(to bottom, transparent, rgba(255, 142, 83, 0.03));
  overflow: hidden;
  z-index: 2;
}

.wave-container {
  position: absolute;
  top: -8px;
  left: 0;
  width: 100%;
  height: 20px;
  overflow: hidden;
}

.wave {
  position: absolute;
  left: 0;
  width: 200%;
  height: 100%;
  background-repeat: repeat-x;
  background-size: 50% 100%;
  transform-origin: center bottom;
}

.wave1 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%23ff8e5308'/%3E%3C/svg%3E");
  animation: wave 15s linear infinite;
  z-index: 3;
  opacity: 0.8;
}

.wave2 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%23ff6b6b08'/%3E%3C/svg%3E");
  animation: wave 10s linear infinite;
  z-index: 2;
  opacity: 0.6;
}

.wave3 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%23ff8e5305'/%3E%3C/svg%3E");
  animation: wave 7s linear infinite;
  z-index: 1;
  opacity: 0.4;
}

@keyframes wave {
  0% {
    transform: translateX(0) translateZ(0) scaleY(1);
  }
  50% {
    transform: translateX(-25%) translateZ(0) scaleY(0.95);
  }
  100% {
    transform: translateX(-50%) translateZ(0) scaleY(1);
  }
}

.footer-content {
  position: relative;
  z-index: 4;
  padding: 2px 0;
}

.footer-content p {
  color: #8d8a8a;
  margin-bottom: 1px;
  font-size: 12px;
  line-height: 1.2;
}

.footer-content a {
  color: #666;
  text-decoration: none;
  position: relative;
  transition: color 0.3s ease;
  font-size: 11px;
  line-height: 1.2;
}

.footer-content a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, #ff8e53, #ff6b6b);
  transform: scaleX(0);
  transform-origin: right;
  transition: transform 0.3s ease;
}

.footer-content a:hover {
  color: #ff8e53;
}

.footer-content a:hover::after {
  transform: scaleX(1);
  transform-origin: left;
}

/* 移动端底部导航栏样式 */
.mobile-tabbar {
  z-index: 2;

  :deep(.van-tabbar) {
    height: 50px;
    border-top: none;
    box-shadow: 0 -1px 10px rgba(0, 0, 0, 0.05);
    z-index: 2;
  }

  :deep(.van-tabbar-item) {
    color: #94a3b8;
    font-size: 10px;
  }

  .add-button {
    position: relative;
    z-index: 2;
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: -40px auto 0;
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
    border: 4px solid #fff;

    .van-icon {
      font-size: 24px;
      color: white;
    }
  }

  .custom-icon {
    margin-bottom: 4px;

    .van-icon {
      font-size: 20px;
    }
  }

  .icon-active {
    color: #ff8e53;
  }

  .text-active {
    color: #ff8e53;
  }
}

:deep(.van-tabbar-item) {
  padding: 6px 0 !important;
  height: 52px !important;
}

:deep(.van-tabbar-item__icon) {
  margin-bottom: 3px !important;
  height: auto !important;
}

:deep(.van-tabbar-item__text) {
  font-size: 12px !important;
  line-height: 1 !important;
  margin-top: 1px !important;
}

/* 自定义图标样式 */
#footerHomeIcon,
#footerForumIcon,
#footerChatIcon,
#footerMyIcon {
  font-size: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1px;
}

/* 中间的上传按钮 */
#footerAddButton {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: auto;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

#footerAddButton .van-icon {
  font-size: 22px;
  color: white;
}

/* 激活状态的图标和文字 */
.icon-active {
  color: #ff8e53;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active {
  color: #ff8e53;
  font-weight: 500;
}

/* 点击效果 */
.add-button:active {
  transform: scale(0.9) rotate(-45deg);
}

/* 标签文字样式 */
.tab-text {
  font-size: 12px;
  margin-top: 2px;
  padding-bottom: 2px;
  display: block;
}

:deep(.van-tabbar-item--active) {
  background-color: transparent;
}

/* 添加渐变背景 */
:deep(.van-tabbar-item)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 2px 2px 0 0;
}

:deep(.van-tabbar-item--active)::after {
  opacity: 1;
}

/* 弹框样式 */
#footerActionSheet {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2147483647;
}

.action-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  z-index: 2147483647;
}

.action-sheet-wrapper {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translateX(-50%);
  width: min(92vw, 560px);
  transform: translate(-50%, -50%);
  z-index: 2147483647;
}

.action-sheet-content {
  background: linear-gradient(to bottom, #fff9f6, #fff);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  position: relative;
  animation: popIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
}

.action-sheet-header {
  padding: 0 0 24px;
  text-align: center;
}

.header-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon .van-icon {
  font-size: 32px;
  color: white;
}

.action-sheet-header .title {
  font-size: 20px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.action-sheet-header .subtitle {
  font-size: 14px;
  color: #64748b;
  margin-top: 16px;
}

.action-sheet-items {
  margin: 32px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-sheet-item {
  display: flex;
  align-items: center;
  padding: 24px;
  padding-left: 32px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.action-sheet-item:hover {
  background: rgba(255, 142, 83, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
}

.action-sheet-item .van-icon {
  font-size: 32px;
  color: #ff8e53;
}

.item-content {
  margin-left: 24px;
  flex: 1;
}

.item-name {
  font-size: 20px;
  font-weight: 500;
  color: var(--color-primary, #ff8e53);
  margin-bottom: 6px;
}

.item-subname {
  font-size: 15px;
  color: #64748b;
  line-height: 1.5;
}

.action-sheet-cancel {
  margin-top: 16px;
  padding: 20px;
  text-align: center;
  font-size: 16px;
  color: #64748b;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-weight: 500;
}

.action-sheet-cancel:hover {
  background: #f8f9fa;
  color: #ff6b6b;
}

/* 优化动画 */
@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fadeInUp {
  from {
    transform: translateY(10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes iconPop {
  from {
    transform: scale(0.5);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 添加新的动画 */
@keyframes slideUpIn {
  from {
    transform: translate(-50%, 20px);
    opacity: 0;
  }
  to {
    transform: translate(-50%, 0);
    opacity: 1;
  }
}

/* 宠物动画样式 */
.action-sheet-pet {
  position: absolute;
  top: -85px;
  right: -45px;
  z-index: 1;
  pointer-events: none;
  animation: dogBounce 2s ease-in-out infinite;
}

@keyframes dogBounce {
  0%, 100% {
    transform: translateY(0) rotate(3deg);
  }
  50% {
    transform: translateY(-8px) rotate(-3deg);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .action-sheet-pet {
    top: -75px;
    right: -35px;
    transform: scale(0.7);
  }

  .action-sheet-content {
    padding: 32px 24px;
  }

  .action-sheet-item {
    padding: 20px;
    padding-left: 24px;
  }

  .item-name {
    font-size: 18px;
  }

  .item-subname {
    font-size: 14px;
  }
}

/* 首页激活样式 - 橙色系 */
.icon-active {
  color: #ff8e53;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-home {
  color: #ff8e53;
  font-weight: 500;
}

/* 论坛激活样式 - 蓝色系 */
.icon-active-forum {
  color: #1890ff;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-forum {
  color: #1890ff;
  font-weight: 500;
}

/* 聊天激活样式 - 绿色系 */
.icon-active-chat {
  color: #52c41a;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-chat {
  color: #52c41a;
  font-weight: 500;
}

/* 我的激活样式 - 玫瑰金色系 */
.icon-active-my {
  color: #ec6a9e;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-my {
  color: #f6649a;
  font-weight: 500;
}

/* 修改底部渐变线条样式 */
:deep(.van-tabbar-item)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 2px 2px 0 0;
}

/* 为每个标签页添加对应的渐变色 */
:deep(.van-tabbar-item:nth-child(1).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #ff8e53, #ff6b6b);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(2).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #1890ff, #36cfc9);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(4).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #52c41a, #73d13d);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(5).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #d76d97, #d5697f);
  opacity: 1;
}
</style>
