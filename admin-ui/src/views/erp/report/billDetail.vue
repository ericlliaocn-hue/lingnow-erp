<template>
  <div class="app-container bill-detail-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryFormRef">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="query.productId" clearable filterable placeholder="请选择商品" style="width: 220px">
            <el-option v-for="item in products" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border empty-text="暂无单据明细">
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="partnerName" :label="isSale ? '客户' : '供应商'" min-width="150" />
        <el-table-column prop="productCode" label="商品编号" min-width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="qty" label="数量" align="right" />
        <el-table-column prop="price" label="单价" align="right" :formatter="money" />
        <el-table-column prop="amount" label="原金额" align="right" :formatter="money" />
        <el-table-column prop="discountAmount" label="优惠" align="right" :formatter="money" />
        <el-table-column prop="finalAmount" label="成交金额" align="right" :formatter="money" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { billDetail } from '@/api/erp/report'
import { productOptions, type ErpProduct } from '@/api/erp/product'

const route = useRoute()
const isSale = computed(() => route.path.includes('sale'))
const title = computed(() => isSale.value ? '销售明细' : '进货明细')
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const products = ref<ErpProduct[]>([])
const queryFormRef = ref()
const query = reactive({ current: 1, size: 10, productId: undefined as string | undefined })

function getList() {
  loading.value = true
  billDetail({ ...query, billType: isSale.value ? 'SALE' : 'PURCHASE' })
    .then(res => { list.value = res.records; total.value = Number(res.total) })
    .finally(() => loading.value = false)
}

function handleQuery() { query.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function money(_row: any, _column: any, value: any) { return Number(value || 0).toFixed(2) }

watch(() => route.path, () => handleQuery())
onMounted(() => {
  productOptions().then(res => products.value = res)
  getList()
})
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
