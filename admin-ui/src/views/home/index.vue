<template>
  <div class="data-board-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>数据看板</h2>
        <p>通用基座首页聚焦用户维度，后续行业业务数据由业务模块接入。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="item in summaryItems" :key="item.key" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon" :class="item.tone">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ formatValue(metrics[item.key]) }}</div>
            <div class="stat-desc">{{ item.desc }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户状态</span>
              <span>在线率 {{ formatPercent(metrics.onlineRate) }}</span>
            </div>
          </template>
          <div class="bar-list">
            <div v-for="item in userStatusStats" :key="item.name" class="bar-item">
              <div class="bar-head">
                <span>{{ item.name }}</span>
                <strong>{{ formatValue(item.value) }}</strong>
              </div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item.value, userStatusStats) }"></div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近 7 日新增用户</span>
              <span>本周新增 {{ formatValue(metrics.weekNewUsers) }}</span>
            </div>
          </template>
          <div ref="growthChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户性别分布</span>
              <span>总用户 {{ formatValue(metrics.totalUsers) }}</span>
            </div>
          </template>
          <div ref="genderChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card online-card">
          <template #header>
            <div class="card-header">
              <span>当前在线用户</span>
              <span>{{ formatValue(metrics.onlineCount) }} 人在线</span>
            </div>
          </template>
          <el-empty v-if="onlineUsers.length === 0" description="暂无在线用户" />
          <div v-else class="online-list">
            <div v-for="user in onlineUsers" :key="user.userId" class="online-item">
              <div class="avatar">{{ avatarText(user) }}</div>
              <div class="online-info">
                <strong>{{ user.nickname || user.username }}</strong>
                <span>{{ user.username }}</span>
              </div>
              <el-tag type="success" size="small">在线</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, CircleCheck, CircleClose, Connection, Refresh, TrendCharts, User } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getUserDashboardData } from '@/api/monitor/admin'

interface StatItem {
  name: string
  value: number
}

interface OnlineUser {
  userId: string
  username: string
  nickname?: string
  status?: number
}

interface DashboardData {
  totalUsers?: number
  activeUsers?: number
  disabledUsers?: number
  onlineCount?: number
  todayNewUsers?: number
  weekNewUsers?: number
  monthNewUsers?: number
  onlineRate?: number
  disabledRate?: number
  userStatusStats?: StatItem[]
  genderStats?: StatItem[]
  growthTrend?: Array<{ date: string; value: number }>
  onlineUsers?: OnlineUser[]
}

type MetricKey = keyof Pick<DashboardData,
  'totalUsers' | 'activeUsers' | 'disabledUsers' | 'onlineCount' | 'todayNewUsers' | 'weekNewUsers' | 'monthNewUsers'
>

const loading = ref(false)
const metrics = ref<DashboardData>({})
const growthChartRef = ref<HTMLElement>()
const genderChartRef = ref<HTMLElement>()

const summaryItems: Array<{ key: MetricKey; label: string; desc: string; icon: any; tone: string }> = [
  { key: 'onlineCount', label: '在线用户', desc: '当前登录会话', icon: Connection, tone: 'green' },
  { key: 'totalUsers', label: '用户总数', desc: '系统累计用户', icon: User, tone: 'blue' },
  { key: 'activeUsers', label: '正常用户', desc: '可登录可使用', icon: CircleCheck, tone: 'cyan' },
  { key: 'disabledUsers', label: '禁用用户', desc: '被停用账号', icon: CircleClose, tone: 'red' },
  { key: 'todayNewUsers', label: '今日新增', desc: '今天创建用户', icon: Calendar, tone: 'orange' },
  { key: 'weekNewUsers', label: '近 7 日新增', desc: '最近一周创建', icon: TrendCharts, tone: 'purple' },
  { key: 'monthNewUsers', label: '本月新增', desc: '本月创建用户', icon: TrendCharts, tone: 'blue' }
]

const userStatusStats = computed(() => metrics.value.userStatusStats || [])
const genderStats = computed(() => metrics.value.genderStats || [])
const growthTrend = computed(() => metrics.value.growthTrend || [])
const onlineUsers = computed(() => metrics.value.onlineUsers || [])

let growthChart: echarts.ECharts | null = null
let genderChart: echarts.ECharts | null = null

const formatValue = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '0'
  return Number(value).toLocaleString()
}

const formatPercent = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '0%'
  return `${Number(value).toFixed(2)}%`
}

const barWidth = (value: number, group: StatItem[]) => {
  const max = Math.max(...group.map(item => Number(item.value || 0)), 1)
  return `${Math.round((Number(value || 0) / max) * 100)}%`
}

const avatarText = (user: OnlineUser) => {
  const text = user.nickname || user.username || 'U'
  return text.slice(0, 1).toUpperCase()
}

const initCharts = () => {
  if (growthChartRef.value) {
    growthChart?.dispose()
    growthChart = echarts.init(growthChartRef.value)
    growthChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 28, right: 16, top: 24, bottom: 32 },
      xAxis: {
        type: 'category',
        data: growthTrend.value.map(item => item.date.slice(5)),
        axisTick: { show: false }
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          name: '新增用户',
          type: 'line',
          smooth: true,
          symbolSize: 7,
          areaStyle: { opacity: 0.14 },
          data: growthTrend.value.map(item => Number(item.value || 0))
        }
      ]
    })
  }

  if (genderChartRef.value) {
    genderChart?.dispose()
    genderChart = echarts.init(genderChartRef.value)
    genderChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          name: '用户数',
          type: 'pie',
          radius: ['48%', '68%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          data: genderStats.value.map(item => ({
            name: item.name,
            value: Number(item.value || 0)
          }))
        }
      ]
    })
  }
}

const resizeCharts = () => {
  growthChart?.resize()
  genderChart?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    metrics.value = await getUserDashboardData()
    await nextTick()
    initCharts()
  } catch (error) {
    console.error(error)
    ElMessage.error('数据看板加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  growthChart?.dispose()
  genderChart?.dispose()
})
</script>

<style scoped>
.data-board-page {
  padding: var(--app-spacing);
  background: var(--el-bg-color-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.stat-card,
.chart-card {
  margin-bottom: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 104px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  font-size: 22px;
}

.stat-icon.green {
  color: #059669;
  background: rgba(16, 185, 129, 0.16);
}

.stat-icon.blue {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.14);
}

.stat-icon.cyan {
  color: #0891b2;
  background: rgba(8, 145, 178, 0.14);
}

.stat-icon.red {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.14);
}

.stat-icon.orange {
  color: #d97706;
  background: rgba(217, 119, 6, 0.14);
}

.stat-icon.purple {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.14);
}

.stat-content {
  min-width: 0;
}

.stat-label,
.stat-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.stat-value {
  margin: 8px 0 6px;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header span:last-child {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 260px;
  justify-content: center;
}

.bar-head {
  display: flex;
  justify-content: space-between;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: var(--el-color-primary);
  transition: width 0.2s ease;
}

.chart-box {
  height: 300px;
}

.online-card :deep(.el-card__body) {
  min-height: 300px;
}

.online-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.online-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.online-item:last-child {
  border-bottom: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--el-color-primary);
  font-weight: 700;
  flex: none;
}

.online-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.online-info strong,
.online-info span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.online-info span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
