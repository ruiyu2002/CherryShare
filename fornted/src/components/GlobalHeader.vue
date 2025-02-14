<template>
  <div id="globalHeader">
    <div v-if="device === DEVICE_TYPE_ENUM.PC">
      <a-row :wrap="false">
        <a-col flex="200px">
          <router-link to="/">
            <div>
              <div class="logo">
                <img src="../assets/nuv.png" alt="logo" />
              </div>
            </div>
          </router-link>
        </a-col>
        <a-col flex="auto" style="margin-left: -66px">
          <a-menu
            v-model:selectedKeys="current"
            mode="horizontal"
            :items="items"
            @click="doMenuClick"
            class="custom-menu"
          />
        </a-col>
        <a-col >
          <div
            class="header-search"
            v-if="!showSearch"
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
        </a-col>
        <!-- 邮箱提示 -->
        <a-col v-if="shouldShowEmailTip">
          <div class="email-tip" @click="handleEmailTipClick">
            <div class="tip-content">
              <MailOutlined class="tip-icon" />
              <span class="tip-text">设置邮箱，体验更多功能</span>
            </div>
          </div>
        </a-col>
        <!-- 用户信息展示栏 -->
        <a-col flex="120px">
          <div class="user-login-status">
            <div v-if="loginUserStore.loginUser.id">
              <a-dropdown>
                <a-space>
                  <a-avatar class="user-avatar" :src="loginUserStore.loginUser?.userAvatar || getDefaultAvatar(loginUserStore.loginUser?.userName)"/>
                  <span class="username-text">{{
                      loginUserStore.loginUser.userName ?? '无名'
                    }}</span>
                </a-space>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="setting">
                      <router-link to="/user/setting">
                        <SettingOutlined />
                        个人中心
                      </router-link>
                    </a-menu-item>
                    <a-menu-item>
                      <router-link to="/my_space">
                        <UserOutlined />
                        我的空间
                      </router-link>
                    </a-menu-item>
                    <a-menu-item key="mypros">
                      <router-link to="/my_ports">
                        <CloudUploadOutlined />
                        我的发布
                      </router-link>
                    </a-menu-item>
                    <a-menu-item key="contact">
                      <a href="http://my.lumenglover.com/contact" target="_blank">
                        <LinkOutlined/>
                        联系我们
                      </a>
                    </a-menu-item>
                    <a-menu-item @click="showLogoutConfirm">
                      <LogoutOutlined />
                      退出登录
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
            <div v-else>
              <a-button type="primary" href="/user/login" class="login-button">
                <span class="button-content">
                  <UserOutlined />
                  <span>登录</span>
                </span>
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
    <div v-else class="mobile-header">
      <div class="mobile-header-content">
        <div class="page-title">
          <span class="title-text">{{ currentTopText }}</span>
        </div>
      </div>
    </div>
    <!-- 退出登录确认弹框 -->
    <a-modal
      v-model:open="logoutConfirmVisible"
      :title="null"
      :footer="null"
      :width="400"
      class="logout-confirm-modal"
    >
      <div class="logout-confirm-content">
        <div class="warning-icon">
          <LogoutOutlined />
        </div>
        <h3 class="confirm-title">确认退出登录？</h3>
        <p class="confirm-desc">退出后需要重新登录才能继续使用</p>
        <div class="confirm-actions">
          <a-button class="cancel-button" @click="logoutConfirmVisible = false">取消</a-button>
          <a-button class="confirm-button" danger @click="confirmLogout">
            确认退出
          </a-button>
        </div>
      </div>
    </a-modal>
    <!-- 添加上传动作面板组件 -->
    <UploadActionSheet v-model="showActionSheet" />
  </div>
</template>

<script lang="ts" setup>
import { computed,watch, h, ref, onMounted, onUnmounted } from 'vue'
import {
  HomeOutlined,
  LogoutOutlined,
  CloudUploadOutlined,
  SettingOutlined,
  UserOutlined,
  LinkOutlined,
  LeftOutlined,
  CodeOutlined,
  TeamOutlined,
  PictureOutlined,
  AppstoreOutlined,
  TagsOutlined,
  FolderOutlined,
  FileTextOutlined,
  PlusOutlined,
  MessageOutlined,
  BellOutlined,
  SearchOutlined,
  MailOutlined,
} from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import Router from '@/router'
import UploadActionSheet from './UploadActionSheet.vue'

// 获取默认头像
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}
const loginUserStore = useLoginUserStore()

const route = useRoute()

const isSearchTransitioning = ref(false)

//是否展示pc端搜索组件
const showSearch = ref(false)
//如果当前路径为path: '/search',设置为true
watch(
  () => route.path,
  (newPath) => {
    showSearch.value = newPath === '/search'
  },
  { immediate: true }
)
// 处理搜索点击
const handleSearchClick = () => {
  isSearchTransitioning.value = true

  // 添加过渡动画后再跳转
  setTimeout(() => {
    router.push('/search')
  }, 300) // 与 CSS 动画时间保持一致
}
// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
})

// 用于存储顶部显示的文字，根据路由动态变化
const currentTopText = ref('')

// 定义图标颜色配置
const iconColors = {
  home: {
    default: '#94a3b8',
    active: '#3B82F6',
    shadow: 'rgba(59, 130, 246, 0.3)',
  },
  create: {
    default: '#94a3b8',
    active: '#10b981',
    shadow: 'rgba(16, 185, 129, 0.3)',
  },
  contact: {
    default: '#94a3b8',
    active: '#f43f5e',
    shadow: 'rgba(244, 63, 94, 0.3)',
  },
  users: {
    default: '#94a3b8',
    active: '#0ea5e9',
    shadow: 'rgba(14, 165, 233, 0.3)',
  },
  pictures: {
    default: '#94a3b8',
    active: '#8b5cf6',
    shadow: 'rgba(139, 92, 246, 0.3)',
  },
  spaces: {
    default: '#94a3b8',
    active: '#f59e0b',
    shadow: 'rgba(245, 158, 11, 0.3)',
  },
  tags: {
    default: '#94a3b8',
    active: '#ec6099',
    shadow: 'rgba(236, 72, 153, 0.3)',
  },
}

// 添加上传相关的状态
const showActionSheet = ref(false)
// 保存当前路由的状态
const previousRoute = ref('')

// 处理添加按钮点击
const handleAddClick = () => {
  if (!loginUserStore.loginUser.id) {
    router.push('/user/login')
    return
  }
  previousRoute.value = route.fullPath
  showActionSheet.value = true
}

// 监听 ActionSheet 的显示状态
watch(showActionSheet, (newVal) => {
  if (!newVal && previousRoute.value && route.fullPath === previousRoute.value) {
    // 只有当路由没有改变时（说明是通过取消按钮关闭的），才恢复路由
    router.push(previousRoute.value)
    previousRoute.value = ''
  }
})

// 未经过滤的菜单项
const originItems = [
  {
    key: '/',
    icon: () =>
      h(HomeOutlined, {
        style: {
          fontSize: '18px',
          color: route.path === '/' ? iconColors.home.active : iconColors.home.default,
          filter: route.path === '/' ? `drop-shadow(0 2px 4px ${iconColors.home.shadow})` : 'none',
          transition: 'all 0.3s ease',
        },
      }),
    label: '主页',
    title: '主页',
  },
  {
    key: '/forum',
    icon: () =>
      h(FileTextOutlined, {
        style: {
          fontSize: '18px',
          color: route.path === '/forum' ? iconColors.spaces.active : iconColors.spaces.default,
          filter: route.path === '/forum' ? `drop-shadow(0 2px 4px ${iconColors.spaces.shadow})` : 'none',
          transition: 'all 0.3s ease',
        },
      }),
    label: '论坛',
    title: '论坛',
  },
  {
    key: 'add',
    icon: () =>
      h('div', {
        class: 'header-add-button',
        onClick: handleAddClick
      }, [
        h(PlusOutlined)
      ]),
    label: '',
    title: '添加'
  },
  {
    key: '/chat-list',
    label: '聊天',
    title: '聊天',
    icon: () =>
      h(MessageOutlined, {
        style: {
          fontSize: '18px',
          color: route.path === '/chat-list' ? '#52c41a' : '#94a3b8',
          filter: route.path === '/chat-list' ? 'drop-shadow(0 2px 4px rgba(82, 196, 26, 0.3))' : 'none',
          transition: 'all 0.3s ease',
        },
      }),
  },
  {
    key: '/message-center',
    label: '消息',
    title: '消息',
    icon: () =>
      h(BellOutlined, {
        style: {
          fontSize: '18px',
          color: route.path === '/message-center' ? '#ff8e53' : '#94a3b8',
          filter: route.path === '/message-center' ? 'drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3))' : 'none',
          transition: 'all 0.3s ease',
        },
      }),
  },
  {
    key: '/admin/manage',
    label: '管理中心',
    title: '管理中心',
    icon: () =>
      h(AppstoreOutlined, {
        style: {
          fontSize: '18px',
          color:
            route.path.startsWith('/admin/')
              ? iconColors.tags.active
              : iconColors.tags.default,
          filter:
            route.path.startsWith('/admin/')
              ? `drop-shadow(0 2px 4px ${iconColors.tags.shadow})`
              : 'none',
          transition: 'all 0.3s ease',
        },
      }),
    children: [
      {
        key: '/admin/userManage',
        label: '用户管理',
        icon: () => h(TeamOutlined),
      },
      {
        key: '/admin/pictureManage',
        label: '图片管理',
        icon: () => h(PictureOutlined),
      },
      {
        key: '/admin/postManage',
        label: '帖子管理',
        icon: () => h(FileTextOutlined),
      },
      {
        key: '/admin/spaceManage',
        label: '空间管理',
        icon: () => h(AppstoreOutlined),
      },
      {
        key: '/admin/tagManage',
        label: '标签管理',
        icon: () => h(TagsOutlined),
      },
      {
        key: '/admin/categoryManage',
        label: '分类管理',
        icon: () => h(FolderOutlined),
      },
    ],
  },
]

// 根据权限过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 管理员才能看到 /admin 开头的菜单
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const items = computed(() => filterMenus(originItems))

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to) => {
  current.value = [to.path]
  // 根据路由路径设置顶部显示的文字
  if (to.path === '/my') {
    currentTopText.value = '我的'
  }else{
    currentTopText.value = ''
  }
})

// 路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 退出确认相关的状态
const logoutConfirmVisible = ref(false)

// 显示退出确认框
const showLogoutConfirm = () => {
  logoutConfirmVisible.value = true
}

// 确认退出
const confirmLogout = async () => {
  try {
    const res = await userLogoutUsingPost()
    if (res.data.code === 0) {
      logoutConfirmVisible.value = false
      loginUserStore.setLoginUser({
        userName: '未登录',
      })
      message.success('退出登录成功')
      await router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch (error) {
    message.error('退出登录失败，请稍后重试')
  }
}

// 添加滚动监听
onMounted(() => {
  const handleScroll = () => {
    const header = document.querySelector('.mobile-header')
    if (header) {
      if (window.scrollY > 0) {
        header.classList.add('scrolled')
      } else {
        header.classList.remove('scrolled')
      }
    }
  }

  window.addEventListener('scroll', handleScroll)
  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
  })
})

// 判断是否需要显示邮箱提示
const shouldShowEmailTip = computed(() => {
  return loginUserStore.loginUser.id && !loginUserStore.loginUser.email
})

// 处理邮箱提示点击
const handleEmailTipClick = () => {
  router.push('/user/setting')
}
</script>

<style scoped>
/* Logo样式 */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
}

.logo img {
  max-width: 50%;
  height: 32px;
  margin-right: 10px;
  transition: transform 0.3s ease;
}

.logo:hover img {
  transform: scale(1.05);
}

/* 菜单样式 */
:deep(.ant-menu) {
  background: transparent;
  border-bottom: none;
  line-height: 60px;
}

:deep(.ant-menu-item) {
  height: 60px;
  line-height: 60px;
  padding: 0 20px !important;
  margin: 0 4px !important;
  border-radius: 6px;
  transition: all 0.3s ease;
}

:deep(.ant-menu-item:hover) {
  background: rgba(0, 0, 0, 0.04);
}


:deep(.ant-menu-item-selected) {
  background: transparent !important;
  color: inherit !important;
}


:deep(.ant-menu-item-selected) {
  position: relative;
  font-weight: 500;
}


:deep(.ant-menu-item-selected)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: transparent;
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* 选中项悬停效果 */
:deep(.ant-menu-item-selected:hover) {
  background: rgba(0, 0, 0, 0.02) !important;
}

/* 未选中项的图标样式 */
:deep(.ant-menu-item:not(.ant-menu-item-selected) .anticon) {
  opacity: 0.7;
}

/* 选中项的图标容器样式 */
:deep(.ant-menu-item-selected .anticon) {
  position: relative;
  opacity: 1;
}

/* 选中项的图标光晕效果 */
:deep(.ant-menu-item-selected .anticon)::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 24px;
  height: 24px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 3;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.2;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.5;
  }
}

/* 菜单项文字样式 */
:deep(.ant-menu-item) {
  color: #666;
  transition: color 0.3s ease;
}

:deep(.ant-menu-item-selected) {
  color: #333;
}

/* 子菜单样式优化 */
:deep(.ant-menu-submenu-title) {
  height: 60px !important;
  line-height: 60px !important;
  transition: all 0.3s ease !important;
}

:deep(.ant-menu-submenu) {
  height: 60px !important;
  line-height: 60px !important;
  padding: 0 20px !important;
  margin: 0 4px !important;
  border-radius: 6px;
}

:deep(.ant-menu-submenu-selected) {
  color: #333 !important;
}

:deep(.ant-menu-submenu:hover) {
  background: rgba(0, 0, 0, 0.04);
}

/* 子菜单图标样式 */
:deep(.ant-menu-submenu .anticon) {
  position: relative;
  font-size: 18px !important;
  margin-right: 8px;
}

/* 移除子菜单的默认箭头 */
:deep(.ant-menu-submenu-arrow) {
  display: none !important;
}

/* 确保子菜单项与父级对齐 */
:deep(.ant-menu-submenu-title) {
  padding: 0 !important;
  margin: 0 !important;
}

/* 下拉菜单样式 */
:deep(.ant-menu-sub) {
  min-width: 120px;
  padding: 4px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.ant-menu-sub .ant-menu-item) {
  height: 40px !important;
  line-height: 40px !important;
  margin: 2px 0 !important;
  padding: 0 12px !important;
  border-radius: 6px;
}

/* 用户区域样式 */
.user-login-status {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: flex-end;

  .username-text {
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.user-login-status :deep(.ant-avatar) {
  transition: transform 0.3s ease;
}

.user-login-status :deep(.ant-avatar:hover) {
  transform: scale(1.1);
}

/* 移动端样式优化 */
.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  z-index: 2;
  margin: 0;
  padding: 0;
}

.mobile-header-content {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
}

/* 返回按钮样式 */
:deep(.back-button) {
  width: 64px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent; /* 移除背景 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 0;
  padding: 0;
  border: none;
}

:deep(.back-button .anticon) {
  font-size: 20px;
  color: #1a1a1a;
}

/* 标题样式 */
:deep(.page-title) {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 占位元素样式 */
:deep(.right-placeholder) {
  width: 64px;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 标题文字样式 */
:deep(.title-text) {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

/* 适配刘海屏 */
@supports (padding-top: env(safe-area-inset-top)) {
  .mobile-header {
    padding-top: env(safe-area-inset-top);
    height: calc(64px + env(safe-area-inset-top));
  }

  :deep(.mobile-header-content) {
    height: 64px;
    padding-left: env(safe-area-inset-left);
    padding-right: env(safe-area-inset-right);
  }
}

/* 返回按钮激活状态 */
:deep(.back-button:active) {
  background: transparent;
}

:deep(.back-button:active .anticon) {
  transform: scale(0.9);
  opacity: 0.7;
}

/* 移除所有可能的默认样式 */
.mobile-header,
:deep(.mobile-header-content),
:deep(.back-button),
:deep(.page-title),
:deep(.right-placeholder) {
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}

/* 添加进入动画 */
@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.mobile-header {
  animation: slideDown 0.3s ease-out;
}

/* 添加滚动阴影效果 */
.mobile-header.scrolled {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

/* 登录按钮样式 */
.login-button {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
  }

  &:active {
    transform: translateY(1px);
  }

  .button-content {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  :deep(.anticon) {
    font-size: 15px;
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .login-button {
    height: 32px;
    padding: 0 16px;
    font-size: 13px;

    :deep(.anticon) {
      font-size: 14px;
    }
  }
}

/* 顶部导航菜单样式 */
:deep(.custom-menu) {
  background: transparent;
  border-bottom: none;
  line-height: 60px;

  .ant-menu-item {
    padding: 0 24px;
    margin: 0 4px;
    border-radius: 8px;
    color: #64760b;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &::after {
      display: none !important;
    }

    /* 未选中状态的图标 */
    .anticon {
      margin-right: 8px;
      transition: all 0.3s ease;
    }

    /* 悬浮状态 */
    &:hover {
      color: #ff8e53;
      background: #fff6f3;

      .anticon {
        color: #ff8e53 !important;
        filter: drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3)) !important;
      }
    }

    /* 选中状态 */
    &.ant-menu-item-selected {
      color: #ff8e53;
      background: #fff6f3;
      font-weight: 500;

      .anticon {
        color: #ff8e53 !important;
        filter: drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3)) !important;
      }
    }
  }

  /* 子菜单样式 */
  .ant-menu-submenu {
    padding: 0 16px;
    margin: 0 4px;
    color: #64760b;

    &::after {
      display: none !important;
    }

    /* 悬浮状态 */
    &:hover {
      color: #ff8e53;
      background: #fff6f3;

      .anticon {
        color: #ff8e53 !important;
        filter: drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3)) !important;
      }
    }

    /* 选中状态 */
    &.ant-menu-submenu-selected {
      color: #ff8e53;
      background: #fff6f3;

      .anticon {
        color: #ff8e53 !important;
        filter: drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3)) !important;
      }
    }

    /* 展开状态 */
    &.ant-menu-submenu-open {
      color: #ff8e53;
      background: #fff6f3;

      .anticon {
        color: #ff8e53 !important;
        filter: drop-shadow(0 2px 4px rgba(255, 142, 83, 0.3)) !important;
      }
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 992px) {
  :deep(.custom-menu) {
    .ant-menu-item,
    .ant-menu-submenu {
      padding: 0 16px;
      margin: 0 2px;
    }
  }
}

/* 退出确认弹框样式 */
:deep(.logout-confirm-modal) {
  .ant-modal-content {
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
  }

  .ant-modal-body {
    padding: 0;
  }
}

.logout-confirm-content {
  padding: 32px 24px;
  text-align: center;
}

.warning-icon {
  font-size: 48px;
  color: #ff8e53;
  margin-bottom: 16px;

  .anticon {
    animation: pulse 2s infinite;
  }
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.confirm-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    color: #1a1a1a;
    border-color: #94a3b8;
    background: #f8fafc;
  }
}

.confirm-button {
  min-width: 100px;
  height: 38px;
  border-radius: 19px;
  background: #ff6b6b;
  border: none;
  color: white;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background: #ff5252;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:active {
    transform: translateY(1px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
/* 新增搜索按钮样式 */
.header-search {
  position: relative;
  z-index: 1000;
}

.search-button {
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.15)) !important;
  border-radius: 32px !important;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 2px 4px rgba(255, 142, 83, 0.05) !important;
  backdrop-filter: blur(8px) !important;
  border: 1px solid rgba(255, 142, 83, 0.1) !important;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  height: 36px;
  padding: 0 16px;
  position: relative;
  z-index: 1000;

  &:hover {
    transform: translateY(-1px);
    background: linear-gradient(135deg, rgba(255, 142, 83, 0.15), rgba(255, 107, 107, 0.2)) !important;
    box-shadow:
      0 6px 20px rgba(0, 0, 0, 0.08),
      0 2px 8px rgba(255, 142, 83, 0.08) !important;
  }

  &:active {
    transform: scale(0.95);
    background: linear-gradient(135deg, rgba(255, 142, 83, 0.2), rgba(255, 107, 107, 0.25)) !important;
  }
}

/* 调整搜索按钮容器的位置 */
:deep(.ant-col:has(.header-search)) {
  position: relative;
  z-index: 1000;
  margin-right: 16px;
  margin-top: 2px;
}

/* 搜索按钮内容样式 */
.search-content {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ff8e53;
  min-width: 80px;
  justify-content: center;
}

.search-icon {
  font-size: 16px;
  color: #ff8e53;
  opacity: 0.9;
}

.search-divider {
  color: rgba(255, 142, 83, 0.3);
  font-size: 12px;
}

.search-text {
  font-size: 13px;
  color: #ff8e53;
  opacity: 0.9;
  font-weight: 500;
}

/* 添加按钮样式 - 参考移动端底部导航栏 */
:deep(.header-add-button) {
  position: relative;
  z-index: 2;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 6px 8px;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
  border: 4px solid #fff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.header-add-button .anticon) {
  font-size: 24px;
  margin: auto;
  color: white;
}

:deep(.header-add-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
}

:deep(.header-add-button:active) {
  transform: scale(0.95) rotate(-45deg);
}

/* 移除菜单项的默认样式 */
:deep(.ant-menu-item) {
  &:has(.header-add-button) {
    padding: 0 !important;
    margin: 0 4px !important;
    background: transparent !important;

    &:hover {
      background: transparent !important;
    }

    &.ant-menu-item-selected {
      background: transparent !important;

      &::after {
        display: none !important;
      }
    }
  }
}

/* 确保菜单项高度适应添加按钮 */
:deep(.ant-menu-item) {
  height: 60px !important;
  line-height: 60px !important;
  display: flex !important;
  align-items: center !important;
}

/* 邮箱提示样式 */
.email-tip {
  display: flex;
  align-items: center;
  height: 36px;
  margin-top: 16px;
margin-right: 6px;
  padding: 0 16px;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.15));
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  animation: tipFloat 3s ease-in-out infinite;
}

.email-tip:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.15), rgba(255, 107, 107, 0.2));
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.15);
}

.tip-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip-icon {
  font-size: 16px;
  color: #ff8e53;
}

.tip-text {
  font-size: 13px;
  color: #ff8e53;
  white-space: nowrap;
}

@keyframes tipFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

/* 响应式调整 */
@media screen and (max-width: 1200px) {
  .email-tip {
    padding: 0 12px;
  }

  .tip-text {
    font-size: 12px;
  }
}
</style>
