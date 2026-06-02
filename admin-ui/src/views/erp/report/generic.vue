<template>
  <div class="app-container report-generic-page" v-loading="loading">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryFormRef">
        <el-form-item label="日期" prop="dateRange">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item v-if="config.groupOptions?.length" label="维度">
          <el-select v-model="query.groupBy" style="width: 150px">
            <el-option v-for="item in config.groupOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ config.title }}</strong>
          <div>
            <el-button @click="handleExport">导出</el-button>
            <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>
      <div v-if="showChart" ref="chartRef" class="report-chart"></div>
      <el-table :data="rows" border empty-text="暂无报表数据">
        <el-table-column v-for="column in config.columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 120" :align="column.money || column.number ? 'right' : 'left'">
          <template #default="{ row }">{{ formatValue(row[column.prop], column) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { billStat, businessProfit, employeePerformance, exportReport, hotProducts, inventoryChange, profitReport, stockSummary, trendReport } from '@/api/erp/report'
import { downloadBlob } from '@/utils/download'

interface ColumnConfig {
  prop: string
  label: string
  width?: number
  money?: boolean
  number?: boolean
}

interface ReportConfig {
  title: string
  loader: (params: any) => Promise<any[] | Record<string, any>>
  params?: Record<string, any>
  groupOptions?: Array<{ label: string; value: string }>
  columns: ColumnConfig[]
}

const route = useRoute()
const loading = ref(false)
const rows = ref<any[]>([])
const queryFormRef = ref()
const chartRef = ref<HTMLElement>()
const dateRange = ref<string[]>([])
const query = reactive({ groupBy: '' })
let chart: echarts.ECharts | null = null

const configs: Record<string, ReportConfig> = {
  '/erp/report/sale-stat': {
    title: '销售统计',
    loader: billStat,
    params: { billType: 'SALE', groupBy: 'date' },
    groupOptions: [{ label: '按日期', value: 'date' }, { label: '按客户', value: 'customer' }, { label: '按业务员', value: 'employee' }],
    columns: statColumns('销售额')
  },
  '/erp/report/purchase-stat': {
    title: '进货统计',
    loader: billStat,
    params: { billType: 'PURCHASE', groupBy: 'date' },
    groupOptions: [{ label: '按日期', value: 'date' }, { label: '按供应商', value: 'supplier' }, { label: '按业务员', value: 'employee' }],
    columns: statColumns('进货额')
  },
  '/erp/report/sale-profit-product': {
    title: '销售利润表（按商品）',
    loader: profitReport,
    params: { groupBy: 'product' },
    columns: profitColumns()
  },
  '/erp/report/sale-profit-bill': {
    title: '销售利润表（按单据）',
    loader: profitReport,
    params: { groupBy: 'bill' },
    columns: profitColumns()
  },
  '/erp/report/sale-profit-customer': {
    title: '销售利润表（按客户）',
    loader: profitReport,
    params: { groupBy: 'customer' },
    columns: profitColumns()
  },
  '/erp/report/sale-analysis': {
    title: '销售分析',
    loader: trendReport,
    columns: [
      { prop: 'date', label: '日期' },
      { prop: 'saleAmount', label: '销售额', money: true },
      { prop: 'saleReturnAmount', label: '销售退货', money: true },
      { prop: 'purchaseAmount', label: '进货额', money: true },
      { prop: 'purchaseReturnAmount', label: '进货退货', money: true }
    ]
  },
  '/erp/report/business-profit': {
    title: '经营利润',
    loader: businessProfit,
    columns: [
      { prop: 'saleAmount', label: '销售额', money: true },
      { prop: 'saleReturnAmount', label: '销售退货', money: true },
      { prop: 'purchaseAmount', label: '进货额', money: true },
      { prop: 'purchaseReturnAmount', label: '进货退货', money: true },
      { prop: 'otherIncome', label: '其他收入', money: true },
      { prop: 'otherExpense', label: '其他支出', money: true },
      { prop: 'grossProfit', label: '毛利', money: true },
      { prop: 'netProfit', label: '净利润', money: true }
    ]
  },
  '/erp/report/hot-products': {
    title: '商品热销榜',
    loader: hotProducts,
    columns: [
      { prop: 'productCode', label: '商品编号' },
      { prop: 'productName', label: '商品名称' },
      { prop: 'qty', label: '销售数量', number: true },
      { prop: 'saleAmount', label: '销售额', money: true },
      { prop: 'lastBillDate', label: '最近销售日期' }
    ]
  },
  '/erp/report/employee-performance': {
    title: '员工业绩统计',
    loader: employeePerformance,
    columns: [
      { prop: 'employeeName', label: '业务员' },
      { prop: 'saleAmount', label: '销售额', money: true },
      { prop: 'returnAmount', label: '退货额', money: true },
      { prop: 'netAmount', label: '净业绩', money: true },
      { prop: 'commissionAmount', label: '提成', money: true }
    ]
  },
  '/erp/report/employee-commission': {
    title: '员工业绩提成',
    loader: employeePerformance,
    columns: [
      { prop: 'employeeName', label: '业务员' },
      { prop: 'netAmount', label: '净业绩', money: true },
      { prop: 'commissionAmount', label: '提成', money: true }
    ]
  },
  '/erp/report/stock-summary': {
    title: '商品收发汇总表',
    loader: stockSummary,
    columns: stockColumns()
  },
  '/erp/report/inventory-change': {
    title: '商品进销存变动统计',
    loader: inventoryChange,
    columns: [...stockColumns(), { prop: 'currentQty', label: '当前库存', number: true }]
  }
}

const config = computed(() => configs[route.path] || configs['/erp/report/sale-stat'])
const showChart = computed(() => chartSeries().length > 0)

function statColumns(amountLabel: string): ColumnConfig[] {
  return [
    { prop: 'groupName', label: '统计维度' },
    { prop: 'billCount', label: '单据数', number: true },
    { prop: 'totalQty', label: '数量', number: true },
    { prop: 'payableAmount', label: amountLabel, money: true },
    { prop: 'paidAmount', label: '已收/已付', money: true },
    { prop: 'debtAmount', label: '欠款', money: true }
  ]
}

function profitColumns(): ColumnConfig[] {
  return [
    { prop: 'groupName', label: '统计维度' },
    { prop: 'qty', label: '数量', number: true },
    { prop: 'saleAmount', label: '销售额', money: true },
    { prop: 'costAmount', label: '成本额', money: true },
    { prop: 'profitAmount', label: '利润', money: true }
  ]
}

function stockColumns(): ColumnConfig[] {
  return [
    { prop: 'productName', label: '商品' },
    { prop: 'warehouseName', label: '仓库' },
    { prop: 'inQty', label: '入库数量', number: true },
    { prop: 'outQty', label: '出库数量', number: true },
    { prop: 'inAmount', label: '入库金额', money: true },
    { prop: 'outAmount', label: '出库金额', money: true },
    { prop: 'netQty', label: '净变动', number: true }
  ]
}

function loadData() {
  loading.value = true
  const params = buildParams()
  config.value.loader(params)
    .then(async res => {
      rows.value = Array.isArray(res) ? res : [res]
      await nextTick()
      renderChart()
    })
    .finally(() => loading.value = false)
}

function resetQuery() {
  dateRange.value = []
  query.groupBy = config.value.params?.groupBy || ''
  loadData()
}

function buildParams() {
  const params = { ...(config.value.params || {}) }
  if (query.groupBy) params.groupBy = query.groupBy
  if (dateRange.value?.length === 2) {
    params.beginDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

function handleExport() {
  const reportCode = route.path.split('/').pop()
  exportReport({ reportCode, ...buildParams() }).then(blob => downloadBlob(blob, `${config.value.title}.csv`))
}

function formatValue(value: any, column: ColumnConfig) {
  if (column.money) return Number(value || 0).toFixed(2)
  if (column.number) return Number(value || 0).toLocaleString()
  return value ?? ''
}

function chartSeries() {
  return config.value.columns.filter(column => column.money || column.number)
}

function chartLabel(row: any) {
  return row.date || row.groupName || row.productName || row.employeeName || row.warehouseName || ''
}

function renderChart() {
  if (!showChart.value || !chartRef.value) {
    chart?.dispose()
    chart = null
    return
  }
  chart?.dispose()
  chart = echarts.init(chartRef.value)
  const seriesColumns = chartSeries()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 44, right: 20, top: 42, bottom: 36 },
    xAxis: { type: 'category', data: rows.value.map(chartLabel), axisTick: { show: false } },
    yAxis: { type: 'value' },
    series: seriesColumns.map(column => ({
      name: column.label,
      type: route.path.includes('analysis') || route.path.includes('trend') ? 'line' : 'bar',
      smooth: true,
      data: rows.value.map(row => Number(row[column.prop] || 0))
    }))
  })
}

function resizeChart() {
  chart?.resize()
}

watch(() => route.path, () => {
  query.groupBy = config.value.params?.groupBy || ''
  loadData()
})
onMounted(() => {
  query.groupBy = config.value.params?.groupBy || ''
  loadData()
  window.addEventListener('resize', resizeChart)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.report-chart { height: 320px; margin-bottom: 16px; }
</style>
