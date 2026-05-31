<template>
  <div class="app-wrapper" :class="[navLayout, `nav-color-${settingsStore.settings.navColor}`, { 'mobile': isMobile }]">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && !isCollapsed" class="mobile-overlay" @click="toggleCollapse"></div>

    <el-container class="layout-container">
      <!-- SIDEBAR -->
      <el-aside
        v-if="navLayout !== 'horizontal'"
        :width="asideWidth"
        class="layout-aside"
        :class="{ 'is-mini': isMini }"
      >
        <div class="logo-container">
          <div class="logo-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100%" height="100%" aria-hidden="true">
              <path d="M 50 10 L 90 30 L 50 95 L 10 30 Z" fill="currentColor" />
              <path d="M 50 35 L 75 25 L 50 55 L 25 25 Z" fill="var(--el-bg-color)" />
              <path d="M 50 65 L 50 85" stroke="var(--el-bg-color)" stroke-width="4" stroke-linecap="round" />
            </svg>
          </div>
        </div>

        <el-scrollbar class="menu-scrollbar">
          <NavMenu :collapse="isCollapsed || isMini" />
        </el-scrollbar>

      </el-aside>

      <el-container>
        <el-header class="layout-header">
           <div class="header-left">
              <div v-if="navLayout === 'vertical'" class="collapse-btn" @click="toggleCollapse">
                 <el-icon><component :is="isCollapsed ? 'Expand' : 'Fold'" /></el-icon>
              </div>

              <Breadcrumb v-if="navLayout === 'vertical' && !isMobile" class="breadcrumb-container" />

              <!-- Logo for horizontal layout -->
              <div class="logo-icon horizontal-logo" v-if="navLayout === 'horizontal'">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100%" height="100%" aria-hidden="true">
                       <path d="M 50 10 L 90 30 L 50 95 L 10 30 Z" fill="currentColor" />
                       <path d="M 50 35 L 75 25 L 50 55 L 25 25 Z" fill="var(--el-bg-color)" />
                       <path d="M 50 65 L 50 85" stroke="var(--el-bg-color)" stroke-width="4" stroke-linecap="round" />
                  </svg>
              </div>

           </div>

           <!-- Horizontal Menu Area -->
           <div v-if="navLayout === 'horizontal'" class="horizontal-nav-area">
               <NavMenu mode="horizontal" />
           </div>

           <div class="header-right">
             <div class="search-bar">
               <el-icon class="search-icon"><Search /></el-icon>
               <span class="search-placeholder">Search...</span>
               <div class="search-key">⌘K</div>
             </div>
             <!-- Language -->
             <el-dropdown trigger="click" class="hide-on-mobile" @command="handleLanguageChange">
              <div class="icon-btn-wrapper">
                <span class="flag-symbol" :aria-label="currentLanguage?.label">{{ currentLanguage?.flag }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="zh-CN">🇨🇳 简体中文</el-dropdown-item>
                  <el-dropdown-item command="en">🇬🇧 English</el-dropdown-item>
                  <el-dropdown-item command="fr">🇫🇷 Français</el-dropdown-item>
                </el-dropdown-menu>
              </template>
             </el-dropdown>

              <!-- Notifications -->
              <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" class="header-badge" type="danger">
               <div class="icon-btn-wrapper" @click="notificationDrawerVisible = true">
                  <el-icon :size="22"><Bell /></el-icon>
               </div>
              </el-badge>

              <!-- Contacts -->
              <div class="icon-btn-wrapper hide-on-mobile">
                   <el-icon :size="22"><UserFilled /></el-icon>
              </div>

              <!-- Settings -->
              <div class="icon-btn-wrapper loading-spin hide-on-mobile" @click="settingsStore.setDrawerOpen(true)">
                   <el-icon :size="22"><Setting /></el-icon>
              </div>

              <!-- Profile -->
              <el-dropdown trigger="click">
               <el-avatar :size="36" class="header-avatar">{{ userInitial }}</el-avatar>
               <template #dropdown>
                 <div class="profile-dropdown-header">
                    <div class="profile-name">{{ displayName }}</div>
                    <div v-if="userStore.userInfo?.email" class="profile-email">{{ userStore.userInfo.email }}</div>
                 </div>
                 <el-dropdown-menu>
                   <el-dropdown-item>{{ t('layout.header.home') }}</el-dropdown-item>
                   <el-dropdown-item>{{ t('layout.header.profile') }}</el-dropdown-item>
                   <el-dropdown-item>{{ t('layout.header.settings') }}</el-dropdown-item>
                   <el-dropdown-item divided @click="handleLogout" style="color: #FF5630; font-weight: bold;">{{ t('layout.header.logout') }}</el-dropdown-item>
                 </el-dropdown-menu>
               </template>
              </el-dropdown>
           </div>
        </el-header>

        <div v-if="!isMobile" class="tags-view-outer">
          <TagsView />
        </div>

        <el-main class="layout-main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- 设置抽屉 -->
    <SettingsDrawer />

    <!-- 通知抽屉 -->
    <NotificationDrawer v-model="notificationDrawerVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
    Search, Bell, Setting, UserFilled, Expand, Fold
} from '@element-plus/icons-vue'
import SettingsDrawer from '@/components/Settings/SettingsDrawer.vue'
import NotificationDrawer from '@/components/Notifications/NotificationDrawer.vue'
import NavMenu from './components/NavMenu.vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import TagsView from './components/TagsView/index.vue'
import { useSettingsStore } from '@/store/modules/settings'
import { useNotificationStore } from '@/store/modules/notification'
import { useUserStore } from '@/store/modules/user'
import { useI18n } from '@/hooks/useI18n'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/fr'

const router = useRouter()
const settingsStore = useSettingsStore()
const notificationStore = useNotificationStore()
const userStore = useUserStore()
const { t } = useI18n()
const notificationDrawerVisible = ref(false)
const isCollapsed = ref(false)
const isMobile = ref(false)

// Global dayjs locale watcher
watch(() => settingsStore.settings.language, (lang) => {
  if (lang === 'zh-CN') {
    dayjs.locale('zh-cn')
  } else if (lang === 'fr') {
    dayjs.locale('fr')
  } else {
    dayjs.locale('en')
  }
}, { immediate: true })

const languageMap: Record<string, { flag: string; label: string }> = {
  'en': { flag: 'EN', label: 'English' },
  'zh-CN': { flag: '中', label: '简体中文' },
  'fr': { flag: 'FR', label: 'Français' }
}

const currentLanguage = computed(() => {
  const lang = settingsStore.settings.language || 'zh-CN'
  return languageMap[lang] || languageMap['zh-CN']
})

const handleLanguageChange = (lang: string) => {
  settingsStore.settings.language = lang
  // 保存到 localStorage (store watch 会处理，但 settings 是 reactive 对象，需要确保 store 里有 watch 或直接修改 state)
  // 这里 settingsStore.settings 是 reactive，直接修改会触发 watch（如果有的话，但目前 store 只 watch 了部分属性）
  // 我们需要手动保存或添加 watch
  localStorage.setItem('app-settings-v5', JSON.stringify(settingsStore.settings))

  const label = languageMap[lang]?.label || lang
  ElMessage.success(`Switched to ${label}`)
}

// 检测是否移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  // 移动端默认收起侧边栏
  if (isMobile.value) {
    isCollapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  notificationStore.connectWebSocket()
  notificationStore.fetchUnreadCount()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const navLayout = computed(() => settingsStore.settings.navLayout)
const isMini = computed(() => navLayout.value === 'mini')
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '')
const userInitial = computed(() => displayName.value.trim().slice(0, 1).toUpperCase() || 'U')

const asideWidth = computed(() => {
    if (navLayout.value === 'mini') return '88px'
    return isCollapsed.value ? '88px' : '240px'
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('Logout success')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 移动端遮罩 */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  transition: opacity 0.3s;
}

/* Sidebar Styles */
.layout-aside {
  background-color: #FFFFFF; /* Default light */
  border-right: 1px dashed rgba(145, 158, 171, 0.24);
  display: flex;
  flex-direction: column;
  transition: width 0.3s, background-color 0.3s;
  overflow: hidden;
  z-index: 10;
}

/* 移动端侧边栏样式 */
.mobile .layout-aside {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 1000;
  transform: translateX(-100%);
  transition: transform 0.3s;
}

.mobile .layout-aside:not(.is-mini) {
  transform: translateX(0);
}

/* 暗黑模式默认背景 */
:global(.dark) .layout-aside {
  background-color: #161C24;
}

/* Nav Color Logic */
:global(.nav-color-integrate) .layout-aside {
    background-color: #F9FAFB !important; /* Blends with light bg */
}
:global(.dark.nav-color-integrate) .layout-aside {
    background-color: #161C24 !important; /* Blends with dark bg */
}
:global(.nav-color-apparent) .layout-aside {
    background-color: #FFFFFF !important; /* Stands out light */
    box-shadow: 4px 0 10px -2px rgba(145, 158, 171, 0.12);
}
:global(.dark.nav-color-apparent) .layout-aside {
    background-color: #212B36 !important; /* Stands out dark */
    box-shadow: 10px 0 20px -4px rgba(0, 0, 0, 0.24);
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  padding-left: var(--app-spacing);
  transition: all 0.3s;
}

.is-mini .logo-container,
.app-wrapper.vertical .layout-aside[style*="width: 88px"] .logo-container {
  padding-left: 0;
  justify-content: center;
}

.logo-icon {
  width: 40px;
  height: 40px;
}

/* Logo使用主题色 */
.logo-icon {
  color: var(--el-color-primary);
}

.logo-path {
  fill: currentColor;
  stroke: currentColor;
}

.horizontal-logo {
    margin-right: 24px;
}

.team-switcher-wrapper {
  padding: 16px 16px 0;
  margin-bottom: 32px;
}

.team-card {
  display: flex;
  align-items: center;
  gap: 12px; /* Increased from 10px */
  padding: 12px; /* Increased from 10px */
  background: var(--el-fill-color-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.team-card:hover {
  background: var(--el-fill-color);
  border-color: var(--el-border-color);
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-size: 16px !important; /* Increased from 14px */
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
:global(.nav-color-integrate:not(.dark)) .team-name,
:global(.nav-color-apparent:not(.dark)) .team-name {
    color: #212B36;
}

.team-tag {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
  height: 20px;
  padding: 0 6px;
  font-weight: 700;
  border-radius: 6px;
}

.team-icon {
  color: #919EAB;
}

.menu-scrollbar {
  flex: 1;
}

.nav-section-title {
  padding: 16px 20px 8px;
  font-size: 11px;
  font-weight: 200;
  color: #919EAB;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.custom-menu {
  border-right: none;
  padding: 0 16px;
}

/* Header Styles */
.layout-header {
  height: 60px;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--app-spacing);
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: background-color 0.3s;
}

.horizontal .layout-header {
    height: 120px; /* Taller header to accommodate top nav */
    flex-wrap: wrap;
    padding: 0 24px;
}

:global(.dark) .layout-header {
  background-color: #161C24;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.horizontal-nav-area {
    flex: 1;
    height: 40px;
    display: flex;
    align-items: center;
    order: 3;
    width: 100%;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #637381;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-left: -10px; /* 视觉对齐：抵消按钮内边距，使图标与下方内容左对齐 */
}

.collapse-btn:hover {
  background-color: rgba(145, 158, 171, 0.08);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #919EAB;
  font-weight: 600;
  cursor: pointer;
}

.search-key {
  font-size: 12px;
  font-weight: 700;
  background-color: rgba(145, 158, 171, 0.16);
  padding: 0 6px;
  border-radius: 6px;
  height: 20px;
  line-height: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #637381;
}

.icon-btn-wrapper:hover {
  background-color: rgba(145, 158, 171, 0.08);
}

.flag-symbol {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.loading-spin:hover .el-icon {
  animation: el-icon-spin 2s infinite linear;
}

.header-badge :deep(.el-badge__content) {
  border: 2px solid #fff;
}

.header-avatar {
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.header-avatar:hover {
  border-color: var(--el-color-primary);
}

.profile-dropdown-header {
  padding: 16px 20px;
  border-bottom: 1px dashed rgba(145, 158, 171, 0.24);
  margin-bottom: 8px;
}

.profile-name {
  font-weight: 600;
  font-size: 14px;
  color: #212B36;
}
:global(.dark) .profile-name {
    color: #fff;
}

.profile-email {
  font-size: 16px;
  color: #919EAB;
  margin-top: 4px;
}



.layout-main {
  padding: 0 !important; /* Remove padding to avoid double padding with app-container */
  margin-top: 0 !important; /* 强制去除上边距 */
  background-color: #F9FAFB; /* Light bg */
  overflow-x: hidden;
  transition: background-color 0.3s;
}

:global(.dark) .layout-main {
  background-color: #161C24;
}

/* Scrollbar customization */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-thumb {
  background-color: rgba(145, 158, 171, 0.24);
  border-radius: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  /* 隐藏不重要的元素 */
  .hide-on-mobile {
    display: none !important;
  }

  /* 搜索栏隐藏文字 */
  .search-bar .search-placeholder {
    display: none;
  }

  .search-bar .search-key {
    display: none;
  }

  /* Header 调整 */
  .layout-header {
    padding-left: 16px;
    height: 64px;
    margin-bottom: 0 !important;
  }

  .header-right {
    gap: 8px;
  }

  /* Main 内容区调整 */
  .layout-main {
    padding: 20px 16px;
  }

  /* Team 切换器隐藏 */
  .team-switcher {
    display: none;
  }
}
</style>
