<template>
  <div class="app-container stock-check-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryFormRef">
        <el-form-item label="单号"><el-input v-model="query.billNo" clearable placeholder="请输入盘点单号" /></el-form-item>
        <el-form-item label="审核"><el-select v-model="query.auditStatus" clearable style="width: 120px"><el-option label="未审核" :value="0" /><el-option label="已审核" :value="1" /></el-select></el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-wrapper">
      <template #header>
        <div class="card-header">
          <strong>库存盘点</strong>
          <div>
            <el-button type="primary" :icon="Plus" @click="goAdd" v-permission="'erp:stock-check:add'">新增</el-button>
            <el-button type="success" :disabled="single" @click="goEdit" v-permission="'erp:stock-check:edit'">修改</el-button>
            <el-button type="warning" :disabled="!canSubmitSelected" @click="handleSubmitApproval" v-permission="'erp:stock-check:audit'">提交审批</el-button>
            <el-button :disabled="!canUnauditSelected" @click="handleUnaudit" v-permission="'erp:stock-check:unaudit'">反审核</el-button>
            <el-button type="danger" :disabled="!canDeleteSelected" @click="handleDelete" v-permission="'erp:stock-check:remove'">删除</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="checkNo" label="单号" min-width="150" />
        <el-table-column prop="checkDate" label="日期" width="120" />
        <el-table-column prop="warehouseName" label="仓库" min-width="140" />
        <el-table-column prop="totalProfitQty" label="盘盈数量" align="right" />
        <el-table-column prop="totalLossQty" label="盘亏数量" align="right" />
        <el-table-column prop="totalProfitAmount" label="盘盈金额" align="right" />
        <el-table-column prop="totalLossAmount" label="盘亏金额" align="right" />
        <el-table-column prop="auditStatus" label="审核" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : 'info'">{{ row.auditStatus === 1 ? '已审核' : '未审核' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="approvalStatus" label="审批" width="100" align="center">
          <template #default="{ row }"><el-tag :type="approvalStatusTag[row.approvalStatus || 'NONE'] || 'info'">{{ approvalStatusText[row.approvalStatus || 'NONE'] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">查看/修改</el-button>
            <el-button v-if="row.auditStatus === 1" link type="primary" @click="unaudit(row)" v-permission="'erp:stock-check:unaudit'">反审核</el-button>
            <el-button v-else link type="primary" :disabled="!canSubmit(row)" @click="submitApprovalRow(row)" v-permission="'erp:stock-check:audit'">提交审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { deleteStockCheck, listStockCheck, unauditStockCheck, type StockCheck } from '@/api/erp/stock'
import { approvalStatusTag, approvalStatusText, submitApproval } from '@/api/erp/approval'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<StockCheck[]>([])
const ids = ref<string[]>([])
const selected = ref<StockCheck[]>([])
const single = ref(true)
const multiple = ref(true)
const queryFormRef = ref()
const query = reactive({ current: 1, size: 10, billNo: '', auditStatus: undefined as number | undefined })
const canSubmitSelected = computed(() => selected.value.length === 1 && canSubmit(selected.value[0]))
const canUnauditSelected = computed(() => selected.value.length === 1 && selected.value[0].auditStatus === 1)
const canDeleteSelected = computed(() => selected.value.length > 0 && selected.value.every(row => row.auditStatus !== 1))

function getList() {
  loading.value = true
  listStockCheck(query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false)
}
function handleQuery() { query.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function handleSelectionChange(rows: StockCheck[]) {
  selected.value = rows
  ids.value = rows.map(row => row.id!)
  single.value = rows.length !== 1
  multiple.value = rows.length === 0
}
function goAdd() { router.push('/erp/stock/check-add') }
function goEdit(row?: StockCheck) { router.push(`/erp/stock/check-add?id=${row?.id || ids.value[0]}`) }
function canSubmit(row: StockCheck) {
  const status = row.approvalStatus || 'NONE'
  return row.auditStatus !== 1 && ['NONE', 'REJECTED', 'REVOKED'].includes(status)
}
function submitApprovalRow(row: StockCheck) { submitApproval('STOCK_CHECK', row.id!).then(() => { ElMessage.success('提交审批成功'); getList() }) }
function unaudit(row: StockCheck) { unauditStockCheck(row.id!).then(() => { ElMessage.success('反审核成功'); getList() }) }
function handleSubmitApproval() { submitApprovalRow(selected.value[0]) }
function handleUnaudit() { unaudit(selected.value[0]) }
function handleDelete() {
  ElMessageBox.confirm('确定删除选中的未审核盘点单吗？已审核盘点单需先反审核。', '提示', { type: 'warning' })
    .then(() => deleteStockCheck(ids.value))
    .then(() => { ElMessage.success('删除成功'); getList() })
}
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
</style>
