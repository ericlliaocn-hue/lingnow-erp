<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>应收应付</strong>
          <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无往来余额">
        <el-table-column prop="partnerType" label="类型" width="100" :formatter="partnerTypeText" />
        <el-table-column prop="partnerName" label="往来单位" min-width="180" />
        <el-table-column prop="receivable" label="应收余额" align="right" :formatter="money" />
        <el-table-column prop="payable" label="应付余额" align="right" :formatter="money" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { partnerBalance } from '@/api/erp/report'

const loading = ref(false)
const list = ref<any[]>([])

function loadData() {
  loading.value = true
  partnerBalance().then(res => list.value = res).finally(() => loading.value = false)
}

function partnerTypeText(_row: any, _column: any, value: string) {
  return value === 'CUSTOMER' ? '客户' : value === 'SUPPLIER' ? '供应商' : value || '-'
}

function money(_row: any, _column: any, value: any) { return Number(value || 0).toFixed(2) }

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
