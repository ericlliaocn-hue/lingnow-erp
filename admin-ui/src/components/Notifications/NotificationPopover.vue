<template>
  <el-popover
    :visible="visible"
    placement="bottom-end"
    :width="420"
    trigger="click"
    popper-class="notification-popover"
    @update:visible="handleVisibleChange"
  >
    <template #reference>
      <slot />
    </template>

    <div class="notification-container">
      <div class="notification-header">
        <h3 class="title">通知</h3>
        <el-button text :disabled="unreadCount === 0" @click="markAllRead">全部已读</el-button>
      </div>

      <div class="notification-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span class="tab-badge">{{ tab.count }}</span>
        </div>
      </div>

      <el-scrollbar class="notification-list" max-height="480px">
        <el-empty v-if="filteredNotifications.length === 0" description="暂无通知" />
        <template v-else>
          <div
            v-for="item in filteredNotifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0 }"
            @click="handleItemClick(item)"
          >
            <div class="item-icon">
              <el-icon><Bell /></el-icon>
            </div>

            <div class="item-content">
              <div class="item-title">
                <span class="text">{{ item.title || '系统通知' }}</span>
                <span v-if="item.isRead === 0" class="unread-dot"></span>
              </div>
              <div class="item-meta">
                <span class="time">{{ item.createTime || '-' }}</span>
                <span v-if="item.type" class="separator">•</span>
                <span v-if="item.type" class="category">{{ item.type }}</span>
              </div>
              <div v-if="item.content" class="item-message">{{ item.content }}</div>
            </div>
          </div>
        </template>
      </el-scrollbar>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/store/modules/notification'

defineProps<{
  visible?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const notificationStore = useNotificationStore()
const activeTab = ref<'all' | 'unread'>('all')

const unreadCount = computed(() => notificationStore.unreadCount)
const notifications = computed(() => notificationStore.list)
const filteredNotifications = computed(() => {
  if (activeTab.value === 'unread') {
    return notifications.value.filter(item => item.isRead === 0)
  }
  return notifications.value
})

const tabs = computed(() => [
  { label: '全部', value: 'all', count: notifications.value.length },
  { label: '未读', value: 'unread', count: unreadCount.value }
])

const handleVisibleChange = (visible: boolean) => {
  emit('update:visible', visible)
  if (visible) {
    notificationStore.fetchList()
    notificationStore.fetchUnreadCount()
  }
}

const markAllRead = async () => {
  await notificationStore.markAllRead()
}

const handleItemClick = async (item: any) => {
  if (item.isRead === 0) {
    await notificationStore.markRead(item.id)
  }
}

watch(
  () => unreadCount.value,
  value => {
    if (value === 0 && activeTab.value === 'unread') {
      activeTab.value = 'all'
    }
  }
)
</script>

<style scoped>
.notification-container {
  background: transparent;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(145, 158, 171, 0.08);
}

.title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--el-text-color-primary);
}

.notification-tabs {
  display: flex;
  padding: 0 20px;
  border-bottom: 1px solid rgba(145, 158, 171, 0.08);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 0;
  cursor: pointer;
  position: relative;
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 600;
}

.tab-item.active {
  color: var(--el-text-color-primary);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--el-color-primary);
}

.tab-badge {
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 12px;
  background: rgba(145, 158, 171, 0.16);
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 24px;
  text-align: center;
}

.notification-list {
  min-height: 160px;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(145, 158, 171, 0.08);
}

.notification-item:hover {
  background: rgba(145, 158, 171, 0.08);
}

.notification-item.unread {
  background: rgba(64, 158, 255, 0.08);
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.item-title .text {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  flex-shrink: 0;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 8px;
}

.item-message {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}
</style>
