<template>
  <div class="app-container stock-warning-page">
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
        <el-form-item label="预警" prop="warningType">
          <el-select v-model="query.warningType" clearable style="width: 140px">
            <el-option label="低库存" value="LOW" />
            <el-option label="高库存" value="HIGH" />
            <el-option label="正常" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-header"><strong>库存预警</strong><el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button></div></template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无预警数据">
        <el-table-column prop="productCode" label="编号" min-width="120" />
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column prop="warehouseName" label="仓库" min-width="140" />
        <el-table-column prop="qty" label="库存数量" align="right" />
        <el-table-column prop="minStock" label="最低库存" align="right" />
        <el-table-column prop="maxStock" label="最高库存" align="right" />
        <el-table-column prop="warningType" label="预警" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.warningType === 'LOW' ? 'danger' : row.warningType === 'HIGH' ? 'warning' : 'success'">
              {{ warningLabel(row.warningType) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { stockWarning } from '@/api/erp/report'
import { productOptions, type ErpProduct } from '@/api/erp/product'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const products = ref<ErpProduct[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const query = reactive({ current: 1, size: 10, productId: undefined as string | undefined, warehouseId: undefined as string | undefined, warningType: undefined as string | undefined })

function getList() {
  loading.value = true
  stockWarning(query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false)
}
function handleQuery() { query.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function warningLabel(type: string) {
  if (type === 'LOW') return '低库存'
  if (type === 'HIGH') return '高库存'
  return '正常'
}
function loadOptions() {
  productOptions().then(res => products.value = res)
  listMaster('warehouse', { current: 1, size: 200 }).then(res => warehouses.value = res.records)
}
onMounted(() => { loadOptions(); getList() })
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
