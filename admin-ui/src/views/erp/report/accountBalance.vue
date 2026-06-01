<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>账户余额</strong>
          <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无账户余额">
        <el-table-column prop="accountName" label="账户" min-width="180" />
        <el-table-column prop="balance" label="余额" align="right" :formatter="money" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { accountBalance } from '@/api/erp/report'

const loading = ref(false)
const list = ref<any[]>([])

function loadData() {
  loading.value = true
  accountBalance().then(res => list.value = res).finally(() => loading.value = false)
}

function money(_row: any, _column: any, value: any) { return Number(value || 0).toFixed(2) }

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
