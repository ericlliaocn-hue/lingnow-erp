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
            <el-button :disabled="single" @click="handleCopy" v-permission="`erp:${module}:add`">复制</el-button>
            <el-button :disabled="single" @click="handlePrint" v-permission="`erp:${module}:print`">打印</el-button>
            <el-button @click="handleExport" v-permission="`erp:${module}:export`">导出</el-button>
            <el-button type="warning" :disabled="!canSubmitSelected" @click="handleSubmitApproval" v-permission="`erp:${module}:audit`">提交审批</el-button>
            <el-button :disabled="!canUnauditSelected" @click="handleUnaudit" v-permission="`erp:${module}:unaudit`">反审核</el-button>
            <el-button type="danger" :disabled="!canDeleteSelected" @click="handleDelete" v-permission="`erp:${module}:remove`">删除</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="partnerName" :label="partnerLabel" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="payableAmount" label="应收/应付" min-width="110" align="right" />
        <el-table-column prop="paidAmount" label="实收/实付" min-width="110" align="right" />
        <el-table-column prop="debtAmount" label="欠款" min-width="110" align="right" />
        <el-table-column prop="auditStatus" label="审核" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : 'info'">{{ row.auditStatus === 1 ? '已审核' : '未审核' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="approvalStatus" label="审批" width="100" align="center">
          <template #default="{ row }"><el-tag :type="approvalStatusTag[row.approvalStatus || 'NONE'] || 'info'">{{ approvalStatusText[row.approvalStatus || 'NONE'] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">查看/修改</el-button>
            <el-button link type="primary" @click="copy(row)" v-permission="`erp:${module}:add`">复制</el-button>
            <el-button link type="primary" @click="print(row)" v-permission="`erp:${module}:print`">打印</el-button>
            <el-button v-if="row.auditStatus === 1" link type="primary" @click="unaudit(row)" v-permission="`erp:${module}:unaudit`">反审核</el-button>
            <el-button v-else link type="primary" :disabled="!canSubmit(row)" @click="submitApprovalRow(row)" v-permission="`erp:${module}:audit`">提交审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="printOpen" :title="printData.title || '打印预览'" width="900px" append-to-body>
      <div class="print-preview" v-if="printData.bill">
        <h2>{{ printData.title }}</h2>
        <div class="print-grid">
          <span>单号：{{ printData.bill.billNo }}</span>
          <span>日期：{{ printData.bill.billDate }}</span>
          <span>{{ partnerLabel }}：{{ printData.bill.partnerName }}</span>
          <span>仓库：{{ printData.bill.warehouseName }}</span>
          <span>应收/应付：{{ printData.bill.payableAmount }}</span>
          <span>实收/实付：{{ printData.bill.paidAmount }}</span>
          <span>欠款：{{ printData.bill.debtAmount }}</span>
          <span>审核：{{ printData.bill.auditStatus === 1 ? '已审核' : '未审核' }}</span>
        </div>
        <el-table :data="printData.items || []" border>
          <el-table-column prop="productCode" label="商品编号" />
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="spec" label="规格" />
          <el-table-column prop="unitName" label="单位" />
          <el-table-column prop="qty" label="数量" align="right" />
          <el-table-column prop="price" label="单价" align="right" />
          <el-table-column prop="finalAmount" label="折后金额" align="right" />
        </el-table>
        <div class="print-remark">备注：{{ printData.bill.remark || '' }}</div>
      </div>
      <template #footer>
        <el-button @click="printOpen = false">关闭</el-button>
        <el-button type="primary" @click="doBrowserPrint">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { copyBill, deleteBill, exportBill, listBill, printBill, unauditBill, type BillModule, type BillQuery, type ErpBill } from '@/api/erp/bill'
import { approvalStatusTag, approvalStatusText, submitApproval, type ApprovalBizType } from '@/api/erp/approval'
import { downloadBlob } from '@/utils/download'

const route = useRoute()
const router = useRouter()
const module = computed<BillModule>(() => {
  if (route.path.includes('/sale-return')) return 'sale-return'
  if (route.path.includes('/purchase-return')) return 'purchase-return'
  return route.path.includes('/purchase') ? 'purchase' : 'sale'
})
const titleMap: Record<BillModule, string> = { sale: '销售单', 'sale-return': '销售退货单', purchase: '进货单', 'purchase-return': '进货退货单' }
const title = computed(() => titleMap[module.value])
const partnerLabel = computed(() => module.value.startsWith('sale') ? '客户' : '供应商')
const loading = ref(false)
const total = ref(0)
const list = ref<ErpBill[]>([])
const ids = ref<string[]>([])
const selected = ref<ErpBill[]>([])
const single = ref(true)
const multiple = ref(true)
const queryFormRef = ref()
const printOpen = ref(false)
const printData = ref<Record<string, any>>({})
const state = reactive({ queryParams: { current: 1, size: 10 } as BillQuery })
const { queryParams } = toRefs(state)
const canSubmitSelected = computed(() => selected.value.length === 1 && canSubmit(selected.value[0]))
const canUnauditSelected = computed(() => selected.value.length === 1 && selected.value[0].auditStatus === 1)
const canDeleteSelected = computed(() => selected.value.length > 0 && selected.value.every(row => row.auditStatus !== 1))
const bizType = computed<ApprovalBizType>(() => {
  const map: Record<BillModule, ApprovalBizType> = {
    sale: 'SALE',
    'sale-return': 'SALE_RETURN',
    purchase: 'PURCHASE',
    'purchase-return': 'PURCHASE_RETURN'
  }
  return map[module.value]
})

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
function canSubmit(row: ErpBill) {
  const status = row.approvalStatus || 'NONE'
  return row.auditStatus !== 1 && ['NONE', 'REJECTED', 'REVOKED'].includes(status)
}
function submitApprovalRow(row: ErpBill) {
  submitApproval(bizType.value, row.id!).then(() => { ElMessage.success('提交审批成功'); getList() })
}
function unaudit(row: ErpBill) {
  unauditBill(module.value, row.id!).then(() => { ElMessage.success('反审核成功'); getList() })
}
function copy(row: ErpBill) {
  copyBill(module.value, row.id!).then(id => {
    ElMessage.success('复制成功')
    router.push(`/erp/${module.value}/add?id=${id}`)
  })
}
function print(row: ErpBill) {
  printBill(module.value, row.id!).then(res => {
    printData.value = res
    printOpen.value = true
  })
}
function handleSubmitApproval() { submitApprovalRow(selected.value[0]) }
function handleUnaudit() { unaudit(selected.value[0]) }
function handleCopy() { copy(selected.value[0]) }
function handlePrint() { print(selected.value[0]) }
function handleExport() {
  exportBill(module.value, queryParams.value).then(blob => downloadBlob(blob, `${title.value}.csv`))
}
function handleDelete() {
  ElMessageBox.confirm('确定删除选中的未审核单据吗？已审核单据需先反审核。', '提示', { type: 'warning' })
    .then(() => deleteBill(module.value, ids.value))
    .then(() => { ElMessage.success('删除成功'); getList() })
}
function doBrowserPrint() { window.print() }
watch(() => route.path, getList)
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.print-preview h2 { text-align: center; margin: 0 0 18px; }
.print-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px 16px; margin-bottom: 16px; }
.print-remark { margin-top: 14px; }
</style>
