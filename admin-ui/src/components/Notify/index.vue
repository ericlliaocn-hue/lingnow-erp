<template>
  <el-dropdown trigger="click" class="notify-dropdown" @visible-change="handleVisibleChange">
    <div class="icon-btn-wrapper">
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notify-badge">
        <el-icon class="header-icon"><Bell /></el-icon>
      </el-badge>
    </div>
    <template #dropdown>
      <div class="notify-container">
        <div class="notify-header">
          <span class="title">通知</span>
          <el-button link type="primary" size="small" @click="handleReadAll" :disabled="unreadCount === 0">
            全部已读
          </el-button>
        </div>
        
        <el-scrollbar max-height="400px">
          <div v-if="loading" class="loading-wrapper">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <div v-else-if="list.length === 0" class="empty-wrapper">
            <el-empty description="暂无通知" :image-size="60" />
          </div>
          <div v-else class="notify-list">
            <div 
              v-for="item in list" 
              :key="item.id" 
              class="notify-item"
              :class="{ 'is-read': item.isRead === 1 }"
              @click="handleItemClick(item)"
            >
              <div class="item-icon" :class="item.type">
                <el-icon v-if="item.type === 'success'"><CircleCheckFilled /></el-icon>
                <el-icon v-else-if="item.type === 'warning'"><WarningFilled /></el-icon>
                <el-icon v-else-if="item.type === 'error'"><CircleCloseFilled /></el-icon>
                <el-icon v-else><InfoFilled /></el-icon>
              </div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-desc" :title="item.content">{{ item.content }}</div>
                <div class="item-time">{{ formatTime(item.createTime) }}</div>
              </div>
              <div v-if="item.isRead === 0" class="item-dot"></div>
            </div>
          </div>
        </el-scrollbar>

        <div class="notify-footer">
          <el-button link type="primary" @click="viewAll">查看全部</el-button>
        </div>
      </div>
    </template>
  </el-dropdown>

  <!-- 详情弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="currentNotify?.title"
    width="500px"
    align-center
  >
    <div class="notify-detail">
      <div class="detail-time">{{ formatTime(currentNotify?.createTime) }}</div>
      <div class="detail-content">{{ currentNotify?.content }}</div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" v-if="currentNotify?.bizId" @click="handleJump">
          查看详情
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useNotificationStore } from '@/store/modules/notification'
import { storeToRefs } from 'pinia'
import { Bell, CircleCheckFilled, WarningFilled, CircleCloseFilled, InfoFilled, Loading } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const notificationStore = useNotificationStore()
const { unreadCount, list } = storeToRefs(notificationStore)
const loading = ref(false)
const dialogVisible = ref(false)
const currentNotify = ref<any>(null)

onMounted(() => {
  notificationStore.connectWebSocket()
  notificationStore.fetchUnreadCount()
})

const handleVisibleChange = (visible: boolean) => {
  if (visible) {
    loading.value = true
    notificationStore.fetchList().finally(() => {
      loading.value = false
    })
  }
}

const handleReadAll = () => {
  notificationStore.markAllRead()
}

const handleItemClick = (item: any) => {
  currentNotify.value = item
  dialogVisible.value = true
  if (item.isRead === 0) {
    notificationStore.markRead(item.id)
  }
}

const handleJump = () => {
  // 根据 bizType 跳转
  dialogVisible.value = false
}

const viewAll = () => {
  // 跳转到通知中心页面 (如果有)
}

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped lang="scss">
.icon-btn-wrapper {
  padding: 8px;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.3s;
  
  &:hover {
    background-color: var(--el-fill-color-light);
  }
}

.header-icon {
  font-size: 20px;
  color: var(--el-text-color-regular);
}

.notify-container {
  width: 360px;
  background: var(--el-bg-color);
}

.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  .title {
    font-weight: 600;
    font-size: 16px;
  }
}

.notify-list {
  padding: 0;
}

.notify-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid var(--el-border-color-lighter);
  position: relative;

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  &.is-read {
    opacity: 0.7;
    .item-title {
      font-weight: normal;
    }
  }

  .item-icon {
    margin-right: 12px;
    display: flex;
    align-items: flex-start;
    padding-top: 2px;
    
    .el-icon {
      font-size: 20px;
    }

    &.success { color: var(--el-color-success); }
    &.warning { color: var(--el-color-warning); }
    &.error { color: var(--el-color-error); }
    &.info { color: var(--el-color-info); }
  }

  .item-content {
    flex: 1;
    min-width: 0;

    .item-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 4px;
      color: var(--el-text-color-primary);
    }

    .item-desc {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-time {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }

  .item-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: var(--el-color-danger);
    position: absolute;
    right: 16px;
    top: 16px;
  }
}

.loading-wrapper, .empty-wrapper {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.notify-footer {
  padding: 8px;
  text-align: center;
  border-top: 1px solid var(--el-border-color-lighter);
}

.notify-detail {
  padding: 10px 0;
  
  .detail-time {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    margin-bottom: 12px;
  }
  
  .detail-content {
    font-size: 14px;
    line-height: 1.6;
    color: var(--el-text-color-primary);
    white-space: pre-wrap;
  }
}
</style>
