<template>
  <div class="app-container" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="24" class="card-box">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><Monitor /></el-icon> 基本信息</span>
            </div>
          </template>
          <div class="monitor-info">
            <el-descriptions :column="4" border>
              <el-descriptions-item label="Redis版本">{{ cache.info?.redis_version }}</el-descriptions-item>
              <el-descriptions-item label="运行模式">{{ cache.info?.redis_mode == 'standalone' ? '单机' : '集群' }}</el-descriptions-item>
              <el-descriptions-item label="端口">{{ cache.info?.tcp_port }}</el-descriptions-item>
              <el-descriptions-item label="客户端数">{{ cache.info?.connected_clients }}</el-descriptions-item>
              <el-descriptions-item label="运行时间(天)">{{ cache.info?.uptime_in_days }}</el-descriptions-item>
              <el-descriptions-item label="使用内存">{{ cache.info?.used_memory_human }}</el-descriptions-item>
              <el-descriptions-item label="使用CPU">{{ parseFloat(cache.info?.used_cpu_user_children || 0).toFixed(2) }}</el-descriptions-item>
              <el-descriptions-item label="内存配置">{{ cache.info?.maxmemory_human }}</el-descriptions-item>
              <el-descriptions-item label="AOF是否开启">{{ cache.info?.aof_enabled == '0' ? '否' : '是' }}</el-descriptions-item>
              <el-descriptions-item label="RDB是否成功">{{ cache.info?.rdb_last_bgsave_status }}</el-descriptions-item>
              <el-descriptions-item label="Key数量">{{ cache.dbSize }}</el-descriptions-item>
              <el-descriptions-item label="网络入口/出口">{{ cache.info?.instantaneous_input_kbps }}kps / {{ cache.info?.instantaneous_output_kbps }}kps</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12" class="card-box mt-4">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><PieChart /></el-icon> 命令统计</span>
            </div>
          </template>
          <div class="chart-box" ref="commandStatsRef"></div>
        </el-card>
      </el-col>

      <el-col :span="12" class="card-box mt-4">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><Odometer /></el-icon> 内存信息</span>
            </div>
          </template>
          <div class="chart-box" ref="memoryRef"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, toRaw } from 'vue'
import { getCacheInfo } from '@/api/monitor/cache'
import * as echarts from 'echarts'

const loading = ref(true)
const cache = ref<any>({ info: {} })
const commandStatsRef = ref()
const memoryRef = ref()

const initCharts = (retryCount = 0) => {
  // 检查 DOM 是否准备就绪
  if (!commandStatsRef.value || commandStatsRef.value.clientWidth === 0) {
    // 限制重试次数，防止死循环
    if (retryCount > 50) return
    // 如果 DOM 尚未渲染或宽度为 0，延迟重试
    setTimeout(() => initCharts(retryCount + 1), 200)
    return
  }

  const rawCache = toRaw(cache.value)
  // 命令统计
  if (commandStatsRef.value) {
    // 销毁旧实例，防止重复初始化
    if (echarts.getInstanceByDom(commandStatsRef.value)) {
      echarts.dispose(commandStatsRef.value)
    }
    const commandChart = echarts.init(commandStatsRef.value)
    commandChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b} : {c} ({d}%)'
      },
      series: [
        {
          name: '命令',
          type: 'pie',
          roseType: 'radius',
          radius: [15, 95],
          center: ['50%', '38%'],
          data: rawCache.commandStats?.map((item: any) => ({
            name: item.name,
            value: Number(item.value)
          })) || [],
          animationEasing: 'cubicInOut',
          animationDuration: 1000
        }
      ]
    })
  }
  
  // 内存信息
  if (memoryRef.value) {
    if (echarts.getInstanceByDom(memoryRef.value)) {
      echarts.dispose(memoryRef.value)
    }
    const memoryChart = echarts.init(memoryRef.value)
    const used = Number(rawCache.info?.used_memory_rss || 0)
    const total = Number(rawCache.info?.total_system_memory || 1)
    const usage = parseFloat((used / total * 100).toFixed(2))

    memoryChart.setOption({
      tooltip: {
        formatter: '{a} <br/>{b} : {c}%'
      },
      series: [
        {
          name: '内存使用率',
          type: 'gauge',
          detail: {
            formatter: '{value}%'
          },
          data: [
            {
              value: usage,
              name: '内存使用率'
            }
          ]
        }
      ]
    })
  }
}

const getData = async () => {
  loading.value = true
  try {
    const res = await getCacheInfo()
    cache.value = res
    loading.value = false
    await nextTick()
    initCharts()
  } catch (error) {
    loading.value = false
    console.error(error)
  }
}

onMounted(() => {
  getData()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.monitor-info {
  background-color: var(--el-bg-color);
}
.mt-4 {
  margin-top: 20px;
}
.chart-box {
  height: 400px;
}
</style>
