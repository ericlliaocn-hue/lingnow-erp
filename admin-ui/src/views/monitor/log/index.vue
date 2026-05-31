<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true">
        <el-form-item label="日志级别">
          <el-segmented v-model="levelFilter" :options="levelOptions" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="请输入关键词" clearable :prefix-icon="Search" />
        </el-form-item>
        <el-form-item>
          <el-button :type="connected ? 'success' : 'info'" :icon="Connection" plain>
            {{ connected ? '已连接' : '未连接' }}
          </el-button>
          <el-button :icon="autoScroll ? VideoPause : VideoPlay" @click="toggleScroll">
            {{ autoScroll ? '暂停滚动' : '继续滚动' }}
          </el-button>
          <el-button :icon="Refresh" @click="reconnect">重连</el-button>
          <el-button type="danger" plain :icon="Delete" @click="clearLogs">清屏</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-wrapper">
      <template #header>
        <div class="card-header">
          <span>实时日志</span>
          <span class="sub-text">保留最近 {{ maxLogs }} 条，当前 {{ logs.length }} 条</span>
        </div>
      </template>

      <div ref="logPanelRef" class="log-panel">
        <el-empty v-if="filteredLogs.length === 0" description="暂无实时日志" />
        <div v-for="item in filteredLogs" :key="item.localId" class="log-row">
          <span class="log-time">{{ item.timestamp }}</span>
          <el-tag class="log-level" :type="levelTagType(item.level)" size="small">{{ item.level }}</el-tag>
          <span class="log-thread">{{ item.thread }}</span>
          <span class="log-logger">{{ item.logger }}</span>
          <span class="log-message">{{ item.message }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Delete, Refresh, Search, VideoPause, VideoPlay } from '@element-plus/icons-vue'

interface LogEntry {
  localId: number
  id?: number
  timestamp: string
  level: string
  thread: string
  logger: string
  message: string
}

const maxLogs = 500
const levelOptions = ['ALL', 'INFO', 'WARN', 'ERROR', 'DEBUG']
const logs = ref<LogEntry[]>([])
const keyword = ref('')
const levelFilter = ref('ALL')
const connected = ref(false)
const autoScroll = ref(true)
const logPanelRef = ref<HTMLElement>()

let ws: WebSocket | null = null
let localId = 0

const filteredLogs = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return logs.value.filter((item) => {
    const levelMatched = levelFilter.value === 'ALL' || item.level === levelFilter.value
    const keyMatched = !key || [item.timestamp, item.level, item.thread, item.logger, item.message]
      .some((value) => String(value || '').toLowerCase().includes(key))
    return levelMatched && keyMatched
  })
})

const levelTagType = (level: string) => {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  if (level === 'DEBUG') return 'info'
  return 'success'
}

const maskSensitive = (text: string) => {
  return String(text || '')
    .replace(/((?:token|password|passwd|pwd|secret|authorization)\s*[=:]\s*)[^\s,;&]+/gi, '$1******')
    .replace(/(jdbc:mysql:\/\/[^?]+)\?[^ ]*/gi, '$1?******')
}

const buildWebSocketUrl = () => {
  const token = localStorage.getItem('token-admin')
  if (!token) {
    ElMessage.error('未获取到登录凭证，无法连接实时日志')
    return ''
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/admin-api/ws/log/${encodeURIComponent(token)}`
}

const appendLog = (entry: any) => {
  logs.value.push({
    localId: ++localId,
    id: entry.id,
    timestamp: entry.timestamp || '',
    level: String(entry.level || 'INFO').toUpperCase(),
    thread: entry.thread || '',
    logger: entry.logger || '',
    message: maskSensitive(entry.message || '')
  })
  if (logs.value.length > maxLogs) {
    logs.value.splice(0, logs.value.length - maxLogs)
  }
  if (autoScroll.value) {
    nextTick(() => {
      if (logPanelRef.value) {
        logPanelRef.value.scrollTop = logPanelRef.value.scrollHeight
      }
    })
  }
}

const connect = () => {
  const url = buildWebSocketUrl()
  if (!url) return
  ws?.close()
  ws = new WebSocket(url)
  ws.onopen = () => {
    connected.value = true
  }
  ws.onmessage = (event) => {
    try {
      appendLog(JSON.parse(event.data))
    } catch (error) {
      appendLog({
        timestamp: new Date().toLocaleString(),
        level: 'INFO',
        thread: 'WebSocket',
        logger: 'System',
        message: event.data
      })
    }
  }
  ws.onclose = () => {
    connected.value = false
  }
  ws.onerror = () => {
    connected.value = false
  }
}

const reconnect = () => {
  connect()
}

const toggleScroll = () => {
  autoScroll.value = !autoScroll.value
}

const clearLogs = () => {
  logs.value = []
}

onMounted(() => {
  connect()
})

onBeforeUnmount(() => {
  ws?.close()
  ws = null
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.log-panel {
  height: calc(100vh - 260px);
  min-height: 420px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  background: #101418;
  color: #e5eaf3;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
}

.log-row {
  display: grid;
  grid-template-columns: 180px 72px minmax(120px, 180px) minmax(220px, 320px) minmax(360px, 1fr);
  gap: 10px;
  align-items: start;
  min-height: 28px;
  line-height: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 4px 0;
}

.log-time,
.log-thread,
.log-logger {
  color: #9aa4b2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-level {
  width: 62px;
  justify-content: center;
}

.log-message {
  word-break: break-word;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .log-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
