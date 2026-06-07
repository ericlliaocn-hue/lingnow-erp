<template>
  <template v-if="item.visible === 1">
    <!-- 目录类型（menuType = 0）且有子菜单 -->
    <el-sub-menu
      v-if="item.menuType === 0 && item.children && item.children.length > 0"
      :index="String(item.menuId)"
    >
      <template #title>
        <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
        <span class="nav-section-title">{{ t(item.menuName) }}</span>
      </template>
      
      <!-- 递归渲染子菜单 -->
      <sidebar-item
        v-for="child in item.children"
        :key="child.menuId"
        :item="child"
      />
    </el-sub-menu>

    <!-- 菜单类型（menuType = 1）直接显示 -->
    <el-menu-item
      v-else
      :index="item.path"
    >
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <template #title>
        <span class="nav-section-title">{{ t(item.menuName) }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/useI18n'
import type { MenuItem } from '@/api/sys/menu.ts'

defineOptions({
  name: 'SidebarItem'
})

const props = defineProps<{
  item: MenuItem
}>()

const { t } = useI18n()
</script>

<style scoped>
/* 复用 NavMenu 中的样式，或者依赖全局样式 */
/* 注意：这里需要确保样式能正确继承或应用 */
.nav-section-title {
  font-size: 14px;
  font-weight: 600;
}
</style>
