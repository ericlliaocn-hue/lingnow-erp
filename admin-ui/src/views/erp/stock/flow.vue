<template>
  <div class="app-container stock-flow-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryFormRef">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="query.productId" clearable filterable placeholder="请选择商品" style="width: 220px">
            <el-option v-for="item in products" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="query.warehouseId" clearable filterable placeholder="请选择仓库" style="width: 180px">
            <el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方向" prop="direction">
          <el-select v-model="query.direction" clearable placeholder="请选择方向" style="width: 120px">
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-header"><strong>商品收发明细</strong><el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button></div></template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无库存流水">
        <el-table-column prop="operateTime" label="时间" min-width="170" />
        <el-table-column prop="sourceBillNo" label="来源单号" min-width="150" />
        <el-table-column prop="sourceBillType" label="业务类型" :formatter="billTypeText" />
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column prop="warehouseName" label="仓库" min-width="140" />
        <el-table-column prop="direction" label="方向" :formatter="directionText" />
        <el-table-column prop="qty" label="数量" align="right" />
        <el-table-column prop="price" label="成本单价" align="right" :formatter="money" />
        <el-table-column prop="amount" label="成本金额" align="right" :formatter="money" />
        <el-table-column prop="beforeQty" label="变动前" align="right" />
        <el-table-column prop="afterQty" label="变动后" align="right" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { stockFlow } from '@/api/erp/report'
import { productOptions, type ErpProduct } from '@/api/erp/product'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const products = ref<ErpProduct[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const query = reactive({ current: 1, size: 10, productId: undefined as string | undefined, warehouseId: undefined as string | undefined, direction: undefined as string | undefined })
function getList() {
  loading.value = true
  stockFlow(query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false)
}
function handleQuery() { query.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function loadOptions() {
  productOptions().then(res => products.value = res)
  listMaster('warehouse', { current: 1, size: 200 }).then(res => warehouses.value = res.records)
}
function directionText(_row: any, _column: any, value: string) { return value === 'IN' ? '入库' : '出库' }
function billTypeText(_row: any, _column: any, value: string) {
  const map: Record<string, string> = { SALE: '销售单', PURCHASE: '进货单', RECEIPT: '收款单', PAYMENT: '付款单' }
  return map[value] || value || '-'
}
function money(_row: any, _column: any, value: any) { return Number(value || 0).toFixed(2) }
onMounted(() => { loadOptions(); getList() })
</script>
<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
