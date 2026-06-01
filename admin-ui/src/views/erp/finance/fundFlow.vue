<template>
  <div class="app-container fund-flow-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryFormRef">
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="query.accountId" clearable filterable placeholder="请选择账户" style="width: 180px">
            <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-header"><strong>资金流水</strong><el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button></div></template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无资金流水">
        <el-table-column prop="operateTime" label="时间" min-width="170" />
        <el-table-column prop="sourceBillNo" label="来源单号" min-width="150" />
        <el-table-column prop="sourceBillType" label="业务类型" :formatter="billTypeText" />
        <el-table-column prop="accountName" label="账户" min-width="140" />
        <el-table-column prop="direction" label="方向" :formatter="directionText" />
        <el-table-column prop="amount" label="金额" align="right" :formatter="money" />
        <el-table-column prop="beforeBalance" label="变动前" align="right" :formatter="money" />
        <el-table-column prop="afterBalance" label="变动后" align="right" :formatter="money" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { listFundFlow } from '@/api/erp/finance'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const accounts = ref<ErpMasterVO[]>([])
const query = reactive({ current: 1, size: 10, accountId: undefined as string | undefined })
function getList() {
  loading.value = true
  listFundFlow(query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false)
}
function handleQuery() { query.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function directionText(_row: any, _column: any, value: string) { return value === 'IN' ? '收入' : '支出' }
function billTypeText(_row: any, _column: any, value: string) {
  const map: Record<string, string> = { SALE: '销售单', PURCHASE: '进货单', RECEIPT: '收款单', PAYMENT: '付款单' }
  return map[value] || value || '-'
}
function money(_row: any, _column: any, value: any) { return Number(value || 0).toFixed(2) }
onMounted(() => {
  listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records)
  getList()
})
</script>
<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
