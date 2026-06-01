<template>
  <div class="app-container summary-page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>经营汇总</strong>
          <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col v-for="item in items" :key="item.key" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="metric-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.money ? moneyValue(data[item.key]) : numberValue(data[item.key]) }}</strong>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { erpSummary } from '@/api/erp/report'

const loading = ref(false)
const data = ref<Record<string, any>>({})
const items = [
  { key: 'productCount', label: '商品数' },
  { key: 'customerCount', label: '客户数' },
  { key: 'supplierCount', label: '供应商数' },
  { key: 'todaySaleAmount', label: '今日销售额', money: true },
  { key: 'todayPurchaseAmount', label: '今日进货额', money: true },
  { key: 'stockAmount', label: '库存金额', money: true },
  { key: 'receivable', label: '应收余额', money: true },
  { key: 'payable', label: '应付余额', money: true },
  { key: 'accountBalance', label: '账户余额', money: true }
]

function loadData() {
  loading.value = true
  erpSummary().then(res => data.value = res).finally(() => loading.value = false)
}

function numberValue(value: any) { return Number(value || 0).toLocaleString() }
function moneyValue(value: any) { return Number(value || 0).toFixed(2) }

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.metric-item {
  min-height: 96px;
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.metric-item span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.metric-item strong {
  font-size: 24px;
  color: var(--el-text-color-primary);
}
</style>
