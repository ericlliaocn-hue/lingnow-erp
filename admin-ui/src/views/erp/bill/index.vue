<template>
  <div class="app-container bill-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="单号"><el-input v-model="queryParams.billNo" placeholder="请输入单号" clearable /></el-form-item>
        <el-form-item label="审核"><el-select v-model="queryParams.auditStatus" clearable style="width: 120px"><el-option label="未审核" :value="0" /><el-option label="已审核" :value="1" /></el-select></el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-wrapper">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <div>
            <el-button type="primary" :icon="Plus" @click="goAdd" v-permission="`erp:${module}:add`">新增</el-button>
            <el-button type="success" :disabled="single" @click="goEdit" v-permission="`erp:${module}:edit`">修改</el-button>
            <el-button type="warning" :disabled="single" @click="handleAudit" v-permission="`erp:${module}:audit`">审核</el-button>
            <el-button :disabled="single" @click="handleUnaudit" v-permission="`erp:${module}:unaudit`">反审核</el-button>
            <el-button type="danger" :disabled="multiple" @click="handleDelete" v-permission="`erp:${module}:remove`">删除</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="partnerName" :label="module === 'sale' ? '客户' : '供应商'" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="payableAmount" label="应收/应付" min-width="110" align="right" />
        <el-table-column prop="paidAmount" label="实收/实付" min-width="110" align="right" />
        <el-table-column prop="debtAmount" label="欠款" min-width="110" align="right" />
        <el-table-column prop="auditStatus" label="审核" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : 'info'">{{ row.auditStatus === 1 ? '已审核' : '未审核' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">查看/修改</el-button>
            <el-button v-if="row.auditStatus === 1" link type="primary" @click="unaudit(row)" v-permission="`erp:${module}:unaudit`">反审核</el-button>
            <el-button v-else link type="primary" @click="audit(row)" v-permission="`erp:${module}:audit`">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { auditBill, deleteBill, listBill, unauditBill, type BillModule, type BillQuery, type ErpBill } from '@/api/erp/bill'

const route = useRoute()
const router = useRouter()
const module = computed<BillModule>(() => route.path.includes('/purchase') ? 'purchase' : 'sale')
const title = computed(() => module.value === 'sale' ? '销售单' : '进货单')
const loading = ref(false)
const total = ref(0)
const list = ref<ErpBill[]>([])
const ids = ref<string[]>([])
const selected = ref<ErpBill[]>([])
const single = ref(true)
const multiple = ref(true)
const queryFormRef = ref()
const state = reactive({ queryParams: { current: 1, size: 10 } as BillQuery })
const { queryParams } = toRefs(state)

function getList() {
  loading.value = true
  listBill(module.value, queryParams.value).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}
function handleQuery() { queryParams.value.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function handleSelectionChange(rows: ErpBill[]) {
  selected.value = rows
  ids.value = rows.map(row => row.id!)
  single.value = rows.length !== 1
  multiple.value = rows.length === 0
}
function goAdd() { router.push(`/erp/${module.value}/add`) }
function goEdit(row?: ErpBill) {
  const id = row?.id || ids.value[0]
  router.push(`/erp/${module.value}/add?id=${id}`)
}
function audit(row: ErpBill) {
  auditBill(module.value, row.id!).then(() => { ElMessage.success('审核成功'); getList() })
}
function unaudit(row: ErpBill) {
  unauditBill(module.value, row.id!).then(() => { ElMessage.success('反审核成功'); getList() })
}
function handleAudit() { audit(selected.value[0]) }
function handleUnaudit() { unaudit(selected.value[0]) }
function handleDelete() {
  ElMessageBox.confirm('确定删除选中的单据吗？', '提示', { type: 'warning' })
    .then(() => deleteBill(module.value, ids.value))
    .then(() => { ElMessage.success('删除成功'); getList() })
}
watch(() => route.path, getList)
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
</style>
