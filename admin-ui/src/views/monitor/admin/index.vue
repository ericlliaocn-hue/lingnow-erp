<template>
  <div class="app-container service-monitor" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>服务监控</h2>
        <p>监控 lingnow-admin 服务、JVM、数据库、Redis 摘要和 Quartz 调度状态。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="item in statusCards" :key="item.key" :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="status-card">
          <div class="status-icon" :class="item.tone">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="status-body">
            <div class="status-label">{{ item.label }}</div>
            <div class="status-value">{{ item.value }}</div>
            <div class="status-desc">{{ item.desc }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>运行资源</span>
              <span>JVM / 线程 / 磁盘</span>
            </div>
          </template>
          <div class="metric-list">
            <metric-row label="堆内存" :value="formatBytes(data.heapUsed)" :suffix="`/ ${formatBytes(data.heapMax)}`" :percent="data.heapUsage" />
            <metric-row label="磁盘空间" :value="formatBytes(data.diskUsed)" :suffix="`/ ${formatBytes(data.diskTotal)}`" :percent="data.diskUsage" />
            <metric-row label="线程数" :value="formatValue(data.threadCount)" :suffix="`峰值 ${formatValue(data.peakThreadCount)}`" :percent="threadPercent" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span>中间件摘要</span>
              <span>详细 Redis 指标在缓存监控</span>
            </div>
          </template>
          <div class="dependency-list">
            <dependency-row title="MySQL" :status="data.database?.status" :meta="databaseMeta" />
            <dependency-row title="Redis" :status="data.redis?.status" :meta="redisMeta" />
            <dependency-row title="Quartz" :status="data.scheduler?.status" :meta="schedulerMeta" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <span>服务信息</span>
          <span>{{ data.serviceName || 'lingnow-admin' }}</span>
        </div>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="服务状态">
          <el-tag :type="data.serviceStatus === 'UP' ? 'success' : 'warning'">{{ data.serviceStatus || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="运行时长">{{ data.uptimeText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进程 ID">{{ data.processId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Java 版本">{{ data.javaVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CPU 核心">{{ formatValue(data.availableProcessors) }}</el-descriptions-item>
        <el-descriptions-item label="系统负载">{{ formatLoad(data.systemLoadAverage) }}</el-descriptions-item>
        <el-descriptions-item label="非堆内存">{{ formatBytes(data.nonHeapUsed) }}</el-descriptions-item>
        <el-descriptions-item label="守护线程">{{ formatValue(data.daemonThreadCount) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { Cpu, DataLine, Monitor, Refresh, Switch, Timer } from '@element-plus/icons-vue'
import { ElMessage, ElProgress, ElTag } from 'element-plus'
import { getDashboardData } from '@/api/monitor/admin'

interface HealthInfo {
  status?: string
  responseTimeMs?: number
  keyCount?: number
  databaseProduct?: string
  standby?: boolean
  jobGroupCount?: number
  triggerGroupCount?: number
}

interface ServiceMonitorData {
  serviceStatus?: string
  serviceName?: string
  uptimeText?: string
  uptimeMs?: number
  processId?: string
  javaVersion?: string
  availableProcessors?: number
  systemLoadAverage?: number
  heapUsed?: number
  heapMax?: number
  heapUsage?: number
  nonHeapUsed?: number
  threadCount?: number
  daemonThreadCount?: number
  peakThreadCount?: number
  diskUsed?: number
  diskTotal?: number
  diskUsage?: number
  database?: HealthInfo
  redis?: HealthInfo
  scheduler?: HealthInfo
}

const data = ref<ServiceMonitorData>({})
const loading = ref(false)

const statusCards = computed(() => [
  {
    key: 'service',
    label: '服务状态',
    value: data.value.serviceStatus || '-',
    desc: data.value.uptimeText ? `已运行 ${data.value.uptimeText}` : '等待检测',
    icon: Monitor,
    tone: data.value.serviceStatus === 'UP' ? 'green' : 'orange'
  },
  {
    key: 'database',
    label: '数据库',
    value: data.value.database?.status || '-',
    desc: data.value.database?.databaseProduct || 'MySQL 连接检测',
    icon: DataLine,
    tone: data.value.database?.status === 'UP' ? 'green' : 'red'
  },
  {
    key: 'redis',
    label: 'Redis',
    value: data.value.redis?.status || '-',
    desc: `${formatValue(data.value.redis?.keyCount)} keys`,
    icon: Switch,
    tone: data.value.redis?.status === 'UP' ? 'green' : 'red'
  },
  {
    key: 'scheduler',
    label: 'Quartz',
    value: data.value.scheduler?.status || '-',
    desc: `${formatValue(data.value.scheduler?.jobGroupCount)} 个任务组`,
    icon: Timer,
    tone: data.value.scheduler?.status === 'UP' ? 'green' : 'red'
  }
])

const databaseMeta = computed(() => `${data.value.database?.databaseProduct || '-'} · ${formatValue(data.value.database?.responseTimeMs)}ms`)
const redisMeta = computed(() => `${formatValue(data.value.redis?.keyCount)} keys · ${formatValue(data.value.redis?.responseTimeMs)}ms`)
const schedulerMeta = computed(() => {
  const standby = data.value.scheduler?.standby ? '待机' : '运行'
  return `${standby} · ${formatValue(data.value.scheduler?.jobGroupCount)} 个任务组`
})
const threadPercent = computed(() => {
  const current = Number(data.value.threadCount || 0)
  const peak = Math.max(Number(data.value.peakThreadCount || 0), current, 1)
  return Math.round((current / peak) * 100)
})

const MetricRow = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: String, required: true },
    suffix: { type: String, default: '' },
    percent: { type: Number, default: 0 }
  },
  setup(props) {
    return () => h('div', { class: 'metric-row' }, [
      h('div', { class: 'metric-head' }, [
        h('span', props.label),
        h('strong', [props.value, props.suffix ? h('small', ` ${props.suffix}`) : null])
      ]),
      h(ElProgress, {
        percentage: Math.min(Math.max(Number(props.percent || 0), 0), 100),
        strokeWidth: 10,
        showText: false
      })
    ])
  }
})

const DependencyRow = defineComponent({
  props: {
    title: { type: String, required: true },
    status: { type: String, default: '-' },
    meta: { type: String, default: '-' }
  },
  setup(props) {
    return () => h('div', { class: 'dependency-row' }, [
      h('div', [
        h('strong', props.title),
        h('span', props.meta)
      ]),
      h(ElTag, { type: props.status === 'UP' ? 'success' : 'danger' }, () => props.status)
    ])
  }
})

const formatValue = (value: number | string | null | undefined) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue.toLocaleString() : String(value)
}

const formatBytes = (value: number | null | undefined) => {
  const bytes = Number(value || 0)
  if (bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

const formatLoad = (value: number | null | undefined) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue) || numberValue < 0) return '-'
  return numberValue.toFixed(2)
}

const loadDashboard = async () => {
  loading.value = true
  try {
    data.value = await getDashboardData()
  } catch (error) {
    console.error(error)
    ElMessage.error('服务监控加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.service-monitor {
  background: var(--el-bg-color-page);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.page-header p,
.card-header span:last-child {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.status-card,
.section-card {
  margin-bottom: 16px;
}

.status-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 104px;
}

.status-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  font-size: 22px;
}

.status-icon.green {
  color: #059669;
  background: rgba(16, 185, 129, 0.16);
}

.status-icon.orange {
  color: #d97706;
  background: rgba(217, 119, 6, 0.16);
}

.status-icon.red {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.14);
}

.status-body {
  min-width: 0;
}

.status-label,
.status-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.status-value {
  margin: 8px 0 6px;
  font-size: 26px;
  line-height: 1;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.metric-list,
.dependency-list {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 22px;
}

.metric-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.metric-head small {
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

.dependency-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.dependency-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.dependency-row div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dependency-row span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
