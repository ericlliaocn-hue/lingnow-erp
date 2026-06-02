<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    :size="420"
    :show-close="false"
    :append-to-body="true"
    :modal="true"
    :with-header="true"
    :lock-scroll="false"
    :destroy-on-close="false"
    modal-class="notification-modal"
    class="notification-drawer"
    @open="handleOpen"
  >
    <template #header>
      <div class="drawer-header">
        <h3 class="title">{{ t('notification.title') }}</h3>
        <div class="header-actions">
          <el-button text circle class="action-btn" @click="handleMarkAllRead" :disabled="unreadCount === 0" :title="t('notification.markAllRead')">
            <el-icon><Check /></el-icon>
          </el-button>
          <el-button text circle class="action-btn" @click="visible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </template>

    <div class="drawer-content">
      <div class="voice-settings">
        <div class="voice-row">
          <span>语音提醒</span>
          <el-switch v-model="voiceSettings.enabled" @change="updateVoiceSettings" />
        </div>
        <div class="voice-options">
          <el-checkbox v-model="voiceSettings.orderEnabled" :disabled="!voiceSettings.enabled" @change="updateVoiceSettings">新订单</el-checkbox>
          <el-checkbox v-model="voiceSettings.noticeEnabled" :disabled="!voiceSettings.enabled" @change="updateVoiceSettings">通知</el-checkbox>
          <el-button size="small" :disabled="!voiceSettings.enabled" @click="notificationStore.testVoice()">测试播报</el-button>
        </div>
      </div>
      <!-- Tabs -->
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

      <!-- Notification List -->
      <el-scrollbar class="notification-list">
        <div v-if="loading" class="loading-wrapper" style="padding: 20px; text-align: center;">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="filteredNotifications.length === 0" class="empty-wrapper" style="padding: 20px; text-align: center; color: #999;">
          {{ t('notification.empty') }}
        </div>
        <div
          v-else
          v-for="(item, index) in filteredNotifications"
          :key="item.id || index"
          class="notification-item"
          :class="{ unread: item.unread }"
          @click="handleItemClick(item)"
        >
          <el-avatar
            :size="48"
            :src="item.avatar"
            class="item-avatar"
            :style="item.icon ? { backgroundColor: item.bg } : {}"
          >
            <el-icon v-if="item.icon" :size="24" :style="{ color: item.color }">
              <component :is="item.icon" />
            </el-icon>
            <el-icon v-else><Bell /></el-icon>
          </el-avatar>

          <div class="item-content">
            <div class="item-title">
              <span class="text">{{ item.title }}</span>
              <span v-if="item.unread" class="unread-dot"></span>
            </div>

            <div class="item-meta">
              <span class="time">{{ item.time }}</span>
              <span class="separator">•</span>
              <span class="category">{{ item.category }}</span>
            </div>

            <div v-if="item.message" class="item-message">{{ item.message }}</div>
          </div>
        </div>
      </el-scrollbar>

      <!-- View All -->
      <!-- <div class="notification-footer">
        <el-button text class="view-all-btn">View all</el-button>
      </div> -->
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Close, Bell, Loading, InfoFilled, CircleCheckFilled, WarningFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/store/modules/notification'
import { useSettingsStore } from '@/store/modules/settings'
import { useI18n } from '@/hooks/useI18n'
import { storeToRefs } from 'pinia'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/fr'

dayjs.extend(relativeTime)

const visible = defineModel<boolean>({ default: false })
const router = useRouter()
const notificationStore = useNotificationStore()
const settingsStore = useSettingsStore()
const { t } = useI18n()
const { list, unreadCount, voiceSettings } = storeToRefs(notificationStore)

const activeTab = ref('all')
const loading = ref(false)

const tabs = computed(() => [
  { label: t('notification.tab.all'), value: 'all', count: list.value.length },
  { label: t('notification.tab.unread'), value: 'unread', count: list.value.filter(i => i.isRead === 0).length },
  { label: t('notification.tab.archived'), value: 'archived', count: list.value.filter(i => i.isRead === 1).length }
])

const filteredNotifications = computed(() => {
  // 依赖语言设置以触发重新计算
  const lang = settingsStore.settings.language
  if (lang === 'zh-CN') {
    dayjs.locale('zh-cn')
  } else if (lang === 'fr') {
    dayjs.locale('fr')
  } else {
    dayjs.locale('en')
  }

  const getTypeInfo = (type: string) => {
    switch (type) {
      case 'info': return { icon: InfoFilled, color: 'var(--el-color-info)', bg: 'var(--el-color-info-light-9)', label: t('notification.type.info') }
      case 'success': return { icon: CircleCheckFilled, color: 'var(--el-color-success)', bg: 'var(--el-color-success-light-9)', label: t('notification.type.success') }
      case 'warning': return { icon: WarningFilled, color: 'var(--el-color-warning)', bg: 'var(--el-color-warning-light-9)', label: t('notification.type.warning') }
      case 'error': return { icon: CircleCloseFilled, color: 'var(--el-color-danger)', bg: 'var(--el-color-danger-light-9)', label: t('notification.type.error') }
      default: return { icon: null, color: '', bg: '', label: type || t('notification.category.system') }
    }
  }

  let result = list.value.map(item => {
    const typeInfo = getTypeInfo(item.type)
    return {
      id: item.id,
      title: item.title,
      message: item.content,
      time: dayjs(item.createTime).fromNow(),
      category: typeInfo.label,
      unread: item.isRead === 0,
      avatar: '',
      icon: typeInfo.icon,
      color: typeInfo.color,
      bg: typeInfo.bg,
      bizId: item.bizId,
      bizType: item.bizType,
      actionUrl: item.actionUrl,
      raw: item
    }
  })

  if (activeTab.value === 'unread') {
    return result.filter(item => item.unread)
  } else if (activeTab.value === 'archived') {
    return result.filter(item => !item.unread)
  }
  return result
})

const handleMarkAllRead = () => {
  notificationStore.markAllRead()
}

const handleItemClick = (item: any) => {
  if (item.unread) {
    notificationStore.markRead(item.id)
  }
  if (item.actionUrl) {
    visible.value = false
    router.push(item.actionUrl)
  }
}

const handleOpen = () => {
  // Handled by watch
}

const updateVoiceSettings = () => {
  notificationStore.saveVoiceSettings({
    enabled: voiceSettings.value.enabled,
    orderEnabled: voiceSettings.value.orderEnabled,
    noticeEnabled: voiceSettings.value.noticeEnabled
  })
}

watch(visible, (newVal) => {
  if (newVal) {
    loading.value = true
    notificationStore.fetchList().finally(() => {
      loading.value = false
    })
  }
})
</script>

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--el-text-color-primary);
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.voice-settings {
  padding: 0 20px 16px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.voice-row,
.voice-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.voice-row {
  margin-bottom: 12px;
  font-weight: 600;
}

.voice-options {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.notification-tabs {
  display: flex;
  padding: 0 20px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  flex-shrink: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  margin-right: 24px;
  cursor: pointer;
  position: relative;
  color: var(--el-text-color-regular);
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s;
}

.tab-item:last-child {
  margin-right: 0;
}

.tab-item.active {
  color: var(--el-text-color-primary);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--el-color-primary);
}

.tab-badge {
  background-color: var(--el-fill-color-darker);
  color: var(--el-text-color-secondary);
  border-radius: 6px;
  padding: 0 6px;
  font-size: 12px;
  height: 20px;
  line-height: 20px;
  transition: all 0.3s;
}

.tab-item.active .tab-badge {
  background-color: var(--el-color-primary);
  color: #fff;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.3s;
}

.notification-item:hover {
  background-color: var(--el-fill-color-light);
}

.notification-item.unread {
  background-color: var(--el-color-primary-light-9);
}

.item-avatar {
  flex-shrink: 0;
  background-color: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.item-title .text {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.5;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.item-meta {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.separator {
  margin: 0 6px;
}

.item-message {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Customize scrollbar */
.notification-list :deep(.el-scrollbar__bar.is-vertical) {
  width: 6px;
}

/* Adjust Drawer Header default padding if needed */
:global(.notification-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

:global(.notification-drawer .el-drawer__body) {
  padding: 0;
}
</style>
