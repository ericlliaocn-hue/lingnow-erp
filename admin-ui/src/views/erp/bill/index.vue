<template>
  <div class="app-container bill-page">
    <el-card shadow="never" class="search-wrapper">
        <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="单号" prop="billNo"><el-input v-model="queryParams.billNo" placeholder="请输入单号" clearable /></el-form-item>
        <el-form-item v-if="isProduction" label="业务员" prop="employeeId">
          <el-select
            v-if="isProductionAdminUser"
            v-model="queryParams.employeeId"
            clearable
            filterable
            placeholder="请选择业务员"
            style="width: 180px"
          >
            <el-option v-for="item in employeeOptions" :key="item.userId" :label="employeeLabel(item)" :value="item.userId" />
          </el-select>
          <el-input v-else :model-value="currentEmployeeLabel" disabled style="width: 180px" />
        </el-form-item>
        <el-form-item v-if="isProduction" label="客户" prop="partnerId">
          <el-select v-model="queryParams.partnerId" clearable filterable placeholder="请选择客户" style="width: 180px">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isProduction" label="日期">
          <el-date-picker
            v-model="productionDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
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
          <div class="bill-header-actions" :class="{ 'is-mobile-hidden': isProduction }">
            <el-button v-if="!isProduction" type="primary" :icon="Plus" @click="goAdd" v-permission="`erp:${module}:add`">新增</el-button>
            <el-button type="success" :disabled="single" @click="goEdit" v-permission="`erp:${module}:edit`">{{ isProduction ? '维护' : '修改' }}</el-button>
            <el-button :disabled="single" @click="handleCopy" v-permission="copyPermission">复制</el-button>
            <el-button :disabled="single" @click="handlePrint" v-permission="`erp:${module}:print`">打印</el-button>
            <el-button v-if="!isProduction" @click="handleExport" v-permission="`erp:${module}:export`">导出</el-button>
            <el-button v-if="!isProduction" type="danger" :disabled="!canDeleteSelected" @click="handleDelete" v-permission="`erp:${module}:remove`">删除</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="partnerName" :label="partnerLabel" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="payableAmount" :label="isProduction ? '成本' : '应收/应付'" min-width="110" align="right" />
        <el-table-column prop="paymentMethod" label="付款方式" min-width="110">
          <template #default="{ row }">{{ row.paymentMethod || '-' }}</template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="付款金额" min-width="110" align="right" />
        <el-table-column v-if="isProduction" prop="productionProgress" label="生产进度" min-width="120">
          <template #default="{ row }">{{ row.productionProgress || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="isProduction" prop="trackingNo" label="快递单号" min-width="150">
          <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="isProduction" prop="productionUserName" label="生产人员" min-width="120">
          <template #default="{ row }">{{ row.productionUserName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" :width="isProduction ? 190 : 250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)" v-permission="`erp:${module}:edit`">{{ isProduction ? '维护' : '查看/修改' }}</el-button>
            <el-button link type="primary" @click="copy(row)" v-permission="copyPermission">复制</el-button>
            <el-button link type="primary" @click="print(row)" v-permission="`erp:${module}:print`">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="printOpen" :title="printData.title || '打印预览'" width="1040px" append-to-body>
      <div class="print-preview" v-if="printData.bill">
        <h2>{{ printData.title }}</h2>
        <div class="print-grid">
          <span>单号：{{ printData.bill.billNo }}</span>
          <span>日期：{{ printData.bill.billDate }}</span>
          <span v-if="showReceiver">业务员：{{ printData.bill.employeeName || '-' }}</span>
          <span v-else>{{ partnerLabel }}：{{ printData.bill.partnerName || '-' }}</span>
          <span>仓库：{{ printData.bill.warehouseName }}</span>
          <span v-if="showReceiver">客户：{{ printData.bill.partnerName || '-' }}</span>
          <span v-if="showReceiver">收货电话：{{ printData.bill.receiverPhone || '-' }}</span>
          <span v-if="showReceiver">收货地址：{{ printData.bill.receiverAddress || '-' }}</span>
          <span v-if="isProduction">生产进度：{{ printData.bill.productionProgress || '-' }}</span>
          <span v-if="isProduction">快递单号：{{ printData.bill.trackingNo || '-' }}</span>
          <span v-if="isProduction">生产人员：{{ printData.bill.productionUserName || '-' }}</span>
          <span v-if="!hideSalePrintFinancialFields">应收/应付：{{ printData.bill.payableAmount }}</span>
          <span>付款方式：{{ printData.bill.paymentMethod || '-' }}</span>
          <span v-if="!hideSalePrintFinancialFields">付款金额：{{ printData.bill.paidAmount }}</span>
        </div>
        <el-table :data="printData.items || []" border>
          <el-table-column prop="productCode" label="商品编号" />
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column label="商品图片" width="156" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.productImageUrl"
                class="print-item-image"
                :src="row.productImageUrl"
                :preview-src-list="[row.productImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="showPrintLogoColumn" label="LOGO图片" width="156" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.logoImageUrl"
                class="print-item-image"
                :src="row.logoImageUrl"
                :preview-src-list="[row.logoImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="72" show-overflow-tooltip />
          <el-table-column label="类目选项" min-width="190">
            <template #default="{ row }">
              <div v-if="formatAttributeLines(row.attributeText).length" class="print-attribute-lines">
                <div v-for="(line, index) in formatAttributeLines(row.attributeText)" :key="index" class="print-attribute-line">
                  <span v-if="line.label">{{ line.label }}：</span>
                  <strong>{{ line.value }}</strong>
                </div>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" align="right" />
          <el-table-column v-if="!hideSalePrintFinancialFields" prop="price" label="单价" align="right" />
          <el-table-column v-if="!hideSalePrintFinancialFields" prop="finalAmount" label="折后金额" align="right" />
        </el-table>
        <div class="print-remark">{{ isSaleLike ? '订单留言' : '备注' }}：{{ printData.bill.remark || '' }}</div>
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
import { copyBill, deleteBill, exportBill, listBill, printBill, type BillModule, type BillQuery, type ErpBill } from '@/api/erp/bill'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
import { allocatedUserList, getActiveRoles, type Role } from '@/api/sys/role'
import { useUserStore } from '@/store/modules/user'
import { downloadBlob } from '@/utils/download'

type EmployeeOption = { userId: string, username?: string, nickname?: string, status?: number }
type AttributeLine = { label: string, value: string }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const module = computed<BillModule>(() => {
  if (route.path.includes('/production')) return 'production'
  if (route.path.includes('/sale-return')) return 'sale-return'
  if (route.path.includes('/purchase-return')) return 'purchase-return'
  return route.path.includes('/purchase') ? 'purchase' : 'sale'
})
const titleMap: Record<BillModule, string> = { sale: '销售单', 'sale-return': '销售退货单', purchase: '进货单', 'purchase-return': '进货退货单', production: '生产单' }
const title = computed(() => titleMap[module.value])
const isProduction = computed(() => module.value === 'production')
const isSaleLike = computed(() => module.value.startsWith('sale') || isProduction.value)
const partnerLabel = computed(() => isSaleLike.value ? '客户' : '供应商')
const showReceiver = computed(() => isSaleLike.value)
const hideSalePrintFinancialFields = computed(() => isSaleLike.value)
const showPrintLogoColumn = computed(() => (module.value === 'sale' || isProduction.value) && Array.isArray(printData.value.items) && printData.value.items.some((item: any) => item.logoImageUrl))
const copyPermission = computed(() => isProduction.value ? 'erp:production:copy' : `erp:${module.value}:add`)
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
const customerOptions = ref<ErpMasterVO[]>([])
const employeeUsers = ref<EmployeeOption[]>([])
const activeRoles = ref<Role[]>([])
const productionDateRange = ref<[string, string] | []>([])
const state = reactive({ queryParams: { current: 1, size: 10 } as BillQuery })
const { queryParams } = toRefs(state)
const canDeleteSelected = computed(() => selected.value.length > 0)
const isProductionAdminUser = computed(() => {
  const username = (userStore.userInfo?.username || '').toLowerCase()
  const roles = ((userStore.userInfo as any)?.roles || []) as Array<{ roleKey?: string, roleName?: string }>
  return hasPermission('*:*:*') || username === 'admin' || username === 'superadmin' || roles.some(role => role.roleKey === 'admin' || role.roleName === '管理员')
})
const employeeOptions = computed(() => employeeUsers.value.filter(item => !isHiddenEmployee(item)))
const currentEmployeeLabel = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '-')

function getList() {
  loading.value = true
  listBill(module.value, queryParams.value).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}
function handleQuery() {
  queryParams.value.current = 1
  applyProductionDateRange()
  getList()
}
function resetQuery() {
  queryFormRef.value?.resetFields()
  productionDateRange.value = []
  queryParams.value.beginDate = undefined
  queryParams.value.endDate = undefined
  queryParams.value.employeeId = undefined
  queryParams.value.partnerId = undefined
  handleQuery()
}
function applyProductionDateRange() {
  if (!isProduction.value) {
    queryParams.value.beginDate = undefined
    queryParams.value.endDate = undefined
    return
  }
  const range = productionDateRange.value
  queryParams.value.beginDate = range?.[0] || undefined
  queryParams.value.endDate = range?.[1] || undefined
}
function loadProductionFilterOptions() {
  if (!isProduction.value) {
    return
  }
  listMaster('customer', { current: 1, size: 1000 }).then(res => {
    customerOptions.value = res.records
  })
  if (isProductionAdminUser.value) {
    loadEmployeeUsers().catch(() => {
      employeeUsers.value = []
    })
  }
}
async function ensureSalespersonRole() {
  if (!activeRoles.value.length) {
    activeRoles.value = await getActiveRoles() as Role[]
  }
  return activeRoles.value.find(item => item.roleKey === 'salesperson')
}
async function loadEmployeeUsers() {
  const role = await ensureSalespersonRole()
  if (!role) {
    employeeUsers.value = []
    return employeeUsers.value
  }
  const res: any = await allocatedUserList({ current: 1, size: 1000, roleId: role.roleId })
  employeeUsers.value = (res.records || []).map((item: any) => ({
    userId: String(item.userId),
    username: item.username,
    nickname: item.nickname,
    status: item.status
  })).filter(item => (item.status === undefined || Number(item.status) === 1) && !isHiddenEmployee(item))
  return employeeUsers.value
}
function employeeLabel(item: EmployeeOption) {
  return item.nickname || item.username || String(item.userId)
}
function isHiddenEmployee(item: EmployeeOption) {
  const username = String(item.username || '').toLowerCase()
  const label = employeeLabel(item)
  return username === 'admin' || username === 'superadmin' || label === '超级管理员' || label.includes('测试业务员')
}
function hasPermission(permission: string) {
  const permissions = JSON.parse(localStorage.getItem('permissions') || '[]') as string[]
  return permissions.includes('*:*:*') || permissions.includes(permission)
}
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
function handleCopy() { copy(selected.value[0]) }
function handlePrint() { print(selected.value[0]) }
function handleExport() {
  exportBill(module.value, queryParams.value).then(blob => downloadBlob(blob, `${title.value}.csv`))
}
function handleDelete() {
  ElMessageBox.confirm('确定删除选中的单据吗？', '提示', { type: 'warning' })
    .then(() => deleteBill(module.value, ids.value))
    .then(() => { ElMessage.success('删除成功'); getList() })
}
function doBrowserPrint() { window.print() }
function formatAttributeLines(value?: string): AttributeLine[] {
  return String(value || '')
    .split(/\s*\/\s*|[；;]\s*/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(item => {
      const match = item.match(/^([^:：]+)[:：]\s*(.+)$/)
      return match ? { label: (match[1] || '').trim(), value: (match[2] || '').trim() } : { label: '', value: item }
    })
}
function clearProductionFilters() {
  productionDateRange.value = []
  queryParams.value.employeeId = undefined
  queryParams.value.partnerId = undefined
  queryParams.value.beginDate = undefined
  queryParams.value.endDate = undefined
}
function initPage() {
  clearProductionFilters()
  loadProductionFilterOptions()
  getList()
}
watch(() => route.path, initPage)
onMounted(initPage)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.print-preview h2 { text-align: center; margin: 0 0 18px; }
.print-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px 16px; margin-bottom: 16px; }
.print-remark { margin-top: 14px; }
.print-item-image {
  width: 128px;
  height: 128px;
  object-fit: cover;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}
.print-attribute-lines {
  display: grid;
  gap: 4px;
  line-height: 1.35;
}
.print-attribute-line span {
  color: var(--el-text-color-regular);
  font-weight: 400;
}
.print-attribute-line strong {
  color: var(--el-text-color-primary);
  font-weight: 700;
}

@media (max-width: 768px) {
  .bill-page {
    min-width: 0;
  }

  .table-wrapper {
    height: auto;
    min-height: 430px;
  }

  .card-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .card-header > div {
    width: 100%;
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .bill-header-actions.is-mobile-hidden {
    display: none;
  }

  .print-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .print-preview {
    min-width: 760px;
  }
}
</style>
