<template>
  <el-menu
      router
      :default-active="activeMenu"
      background-color="transparent"
      :text-color="textColor"
      :active-text-color="activeTextColor"
      :collapse="collapse"
      class="custom-menu"
      :class="{ 'horizontal-menu': mode === 'horizontal' }"
      :collapse-transition="false"
      :mode="mode"
  >
    <!-- 动态渲染菜单 -->
    <sidebar-item
        v-for="item in menuList"
        :key="item.menuId"
        :item="item"
    />
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '@/store/modules/settings'
import { usePermissionStore } from '@/store/modules/permission'
import type { MenuItem } from '@/api/sys/menu.ts'
import { useI18n } from '@/hooks/useI18n'
import SidebarItem from './SidebarItem.vue'

const props = defineProps({
  collapse: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String as () => 'vertical' | 'horizontal',
    default: 'vertical'
  }
})

const route = useRoute()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const { t } = useI18n()

// 使用 store 中的菜单数据
const menuList = computed(() => permissionStore.menus)

const isDark = computed(() => settingsStore.settings.themeMode === 'dark')

const textColor = computed(() => {
  // 根据主题模式调整文字颜色
  return isDark.value ? '#DDE3EA' : '#334155'
})

const activeTextColor = computed(() => {
  // 激活状态使用主题色
  return settingsStore.primaryColorPresets[settingsStore.settings.primaryColor] || settingsStore.primaryColorPresets.default
})

// 菜单激活项，处理 / 和 /dashboard 的匹配
const activeMenu = computed(() => {
  const path = route.path
  if (path === '/dashboard' || path === '/') {
    return '/dashboard'
  }
  return path
})
</script>

<style scoped>
.custom-menu {
  border-right: none;
  padding: 0 8px;
  background-color: transparent !important;
  --el-menu-base-level-padding: 20px; /* Reduced indentation step */
  --el-menu-icon-width: 20px;
}

/* Mini Sidebar specific spacing */
.custom-menu.el-menu--collapse {
  padding: 0 8px;
  width: 100% !important;
}

.custom-menu.horizontal-menu {
  border-bottom: none;
  padding: 0;
  display: flex;
  align-items: center;
  height: 100%;
}

.nav-section-title {
  padding: 0;
  font-size: 14px !important;
  font-weight: 600;
  color: #334155;
  text-transform: none;
  letter-spacing: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Show labels for both menu items and sub-menus in collapse mode */
.el-menu--collapse .nav-section-title {
  display: block;
  font-size: 12px; /* Increased from 10px */
  line-height: 1.2;
  margin-top: 4px;
  opacity: 0.7;
  text-transform: none; /* Don't force uppercase in mini labels */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 白天模式下的标题颜色 - 使用更淡的灰色 */
:global(.nav-color-integrate:not(.dark)) .nav-section-title,
:global(.nav-color-apparent:not(.dark)) .nav-section-title {
  color: #334155;
}

/* 子菜单标题样式 */
:deep(.el-sub-menu__title) {
  height: 48px !important;
  line-height: 48px !important;
  padding-right: 20px !important;
  margin-bottom: 4px !important;
  border-radius: 8px !important;
  transition: all 0.3s;
  color: #334155;
  display: flex !important;
  align-items: center;
  position: relative !important;
  font-size: 14px !important;
  font-weight: 600;
}

/* Ensure padding exists for top-level items via other means if needed,
   but let Element Plus handle the inline style for depth */
:deep(.el-menu-item), :deep(.el-sub-menu__title) {
  padding-right: 12px;
}

:deep(.el-icon) {
  font-size: 20px !important;
  margin-right: 8px !important;
}

:deep(.el-sub-menu__title) {
  padding-right: 28px !important; /* 为右侧箭头留空间 */
}

/* Tree Structure Container Line */
:deep(.el-menu--inline) {
  position: relative;
  background-color: transparent !important;
}

/*
:deep(.el-menu--inline)::before {
  content: "";
  position: absolute;
  left: 24px;
  top: 0;
  bottom: 0;
  width: 1px;
  background-color: rgba(145, 158, 171, 0.24);
  z-index: 1;
}

:global(.dark) :deep(.el-menu--inline)::before {
  background-color: rgba(255, 255, 255, 0.12);
}
*/

.el-menu--collapse :deep(.el-sub-menu__title) {
  height: 56px;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 0 !important;
}

:deep(.el-sub-menu__icon-arrow) {
  position: absolute !important;
  right: 12px !important;
  left: auto !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  margin: 0 !important;
  font-size: 12px;
  color: #64748b;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* 确保图标和文字在左边 */
:deep(.el-sub-menu__title .el-icon:not(.el-sub-menu__icon-arrow)) {
  order: -2;
  margin-right: 12px;
}

:deep(.el-sub-menu__title .nav-section-title) {
  order: -1;
  flex: 1;
}

:deep(.el-sub-menu__title:hover) {
  background-color: rgba(145, 158, 171, 0.08) !important;
}

:deep(.el-sub-menu__title:hover) .nav-section-title {
  color: var(--el-color-primary) !important;
}

:global(.dark) :deep(.el-sub-menu__title:hover) .nav-section-title {
  color: var(--el-color-primary) !important;
}

:deep(.el-sub-menu__title:hover) .el-sub-menu__icon-arrow {
  color: var(--el-color-primary) !important;
}

:global(.dark) :deep(.el-sub-menu__title:hover) .el-sub-menu__icon-arrow {
  color: var(--el-color-primary) !important;
}

/* Add icon support */
:deep(.el-sub-menu__title:hover .el-icon) {
  color: var(--el-color-primary) !important;
}

/* 确保 el-sub-menu__title 本身也变色（如果 nav-section-title 没生效） */
:deep(.el-sub-menu__title:hover) {
  color: var(--el-color-primary) !important;
}

:deep(.el-sub-menu__title.is-active) {
  color: var(--el-color-primary);
}

.el-menu--collapse :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

/* 菜单项基础样式 */
:deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  border-radius: 8px;
  margin-bottom: 4px;
  color: #334155;
  font-size: 14px;
  padding-right: 12px !important; /* Keep right padding */
  transition: all 0.2s;
}

/* Sub-item Indentation and Tree Dot */
:deep(.el-menu--inline .el-menu-item) {
  /* padding-left: 48px !important; Remove this to allow Element Plus dynamic padding */
  position: relative;
}

/* Base dot for all sub-items (can be hidden or shown behind icon) */
:deep(.el-menu--inline .el-menu-item)::before {
  content: "";
  position: absolute;
  left: 22px; /* Align with the vertical line (24px center - 2px half-width) */
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: #64748b;
  opacity: 0.5;
  transition: all 0.2s;
  z-index: 2;
}

/* Hide dot if icon is present to avoid overlap, OR keep it as a marker */
:deep(.el-menu--inline .el-menu-item:has(.el-icon))::before {
  display: none; /* Keep it clean if icon exists */
}

:deep(.el-menu--inline .el-menu-item.is-active)::before {
  background-color: var(--el-color-primary);
  opacity: 1;
  transform: translateY(-50%) scale(1.5);
}

:deep(.el-menu--inline .el-menu-item.is-active) {
  background-color: rgba(0, 167, 111, 0.08) !important;
}

:global(.dark) :deep(.el-menu--inline .el-menu-item.is-active) {
  background-color: rgba(0, 167, 111, 0.16) !important;
}

:deep(.el-menu-item .el-icon) {
  font-size: 24px;
  margin-right: 12px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), color 0.3s;
}

/* Mini Layout overrides */
.el-menu--collapse :deep(.el-menu-item) {
  height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 !important;
  margin-bottom: 8px;
  font-size: 10px; /* Small label text */
}

.el-menu--collapse :deep(.el-menu-item .el-icon),
.el-menu--collapse :deep(.el-sub-menu__title .el-icon) {
  margin-right: 0;
  margin-bottom: 4px;
}

/* Force show label in mini mode */
.el-menu--collapse :deep(.el-menu-item span) {
  visibility: visible;
  height: auto;
  width: auto;
  line-height: 1.2;
  text-align: center;
  display: inline-block;
  opacity: 0.7;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-menu--collapse :deep(.el-menu-item.is-active span) {
  opacity: 1;
}

/* Horizontal Menu item spacing */
.horizontal-menu :deep(.el-menu-item) {
  margin-bottom: 0;
  margin-right: 4px;
  height: 32px;
  line-height: 32px;
  font-size: 14px;
}

/* Active State Styles */
:deep(.el-menu-item.is-active) {
  background-color: rgba(0, 167, 111, 0.12) !important;
  color: var(--el-color-primary) !important;
  font-weight: 600;
}

:global(.dark) :deep(.el-menu-item.is-active) {
  background-color: rgba(0, 167, 111, 0.16) !important;
}

:deep(.el-menu-item.is-active .el-icon) {
  color: var(--el-color-primary) !important;
}

/* Hover States */
:deep(.el-menu-item:hover) {
  background-color: rgba(15, 23, 42, 0.06) !important;
}

/* 移除 NavMenu 中的 color 覆盖，交给全局 style.css 处理，避免冲突 */
/* 但保留 hover 背景色 */


/* Hover Scaling and Color for Mini Sidebar */
.el-menu--collapse :deep(.el-menu-item:hover .el-icon),
.el-menu--collapse :deep(.el-sub-menu__title:hover .el-icon) {
  transform: scale(1.15);
  color: var(--el-color-primary) !important;
}

.el-menu--collapse :deep(.el-menu-item:hover span),
.el-menu--collapse :deep(.el-sub-menu__title:hover .nav-section-title) {
  color: var(--el-color-primary) !important;
  opacity: 1;
}

/* Popover Menu Styling (Minimals Dashboard Tree style) */
:global(.el-menu--popup) {
  min-width: 160px !important;
  padding: 8px !important;
  border-radius: 12px !important;
  box-shadow: -20px 20px 40px -4px rgba(0, 0, 0, 0.24) !important;
  border: none !important;
  background-color: #FFFFFF !important;
}

:global(.dark .el-menu--popup) {
  background-color: #212B36 !important;
}

:global(.el-menu--popup .el-menu-item) {
  height: 36px !important;
  line-height: 36px !important;
  padding-left: 32px !important; /* Spacing for dot */
  font-size: 13px !important;
  color: #919EAB !important;
  background-color: transparent !important;
  position: relative !important;
}

/* Dot Indicator for Sub-items in Popover */
:global(.el-menu--popup .el-menu-item::before) {
  content: "";
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: currentColor;
  opacity: 0.5;
  transition: all 0.2s;
}

:global(.el-menu--popup .el-menu-item:hover) {
  color: #212B36 !important;
  background-color: rgba(145, 158, 171, 0.08) !important;
}

:global(.dark .el-menu--popup .el-menu-item:hover) {
  color: #FFFFFF !important;
}

:global(.el-menu--popup .el-menu-item:hover::before) {
  opacity: 1;
  transform: translateY(-50%) scale(1.5);
}

:global(.el-menu--popup .el-menu-item.is-active) {
  color: var(--el-color-primary) !important;
  font-weight: 600 !important;
}

:global(.el-menu--popup .el-menu-item.is-active::before) {
  background-color: var(--el-color-primary) !important;
  opacity: 1;
  transform: translateY(-50%) scale(1.5);
}

/* Remove icons in popover if desired, or keep them with styling */
:global(.el-menu--popup .el-menu-item .el-icon) {
  display: none;
}
</style>
