<template>
  <div class="app-container customer-order-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form ref="queryFormRef" :inline="true" :model="queryParams">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" clearable placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="客户" prop="customerName">
          <el-input v-model="queryParams.customerName" clearable placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 150px">
            <el-option label="待接单" value="PENDING" />
            <el-option label="已转销售单" value="CONFIRMED" />
            <el-option label="已作废" value="CANCELLED" />
          </el-select>
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
          <strong>客户订单</strong>
          <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%" @row-dblclick="openDetail">
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="orderTime" label="下单时间" min-width="170" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="accountName" label="下单账号" min-width="120" />
        <el-table-column prop="totalQty" label="总数量" width="100" align="right" />
        <el-table-column prop="totalAmount" label="金额" width="110" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收货人" min-width="110">
          <template #default="{ row }">{{ row.receiverName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="receiverPhone" label="电话" min-width="120">
          <template #default="{ row }">{{ row.receiverPhone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="billNo" label="销售单号" min-width="150">
          <template #default="{ row }">{{ row.billNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" :disabled="row.status !== 'PENDING'" @click="openConfirm(row)" v-permission="'erp:customer-order:confirm'">确认</el-button>
            <el-button link type="primary" @click="print(row)" v-permission="'erp:customer-order:print'">打印</el-button>
            <el-button link type="danger" :disabled="row.status !== 'PENDING'" @click="cancel(row)" v-permission="'erp:customer-order:cancel'">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <el-dialog v-model="detailOpen" title="客户订单详情" width="1040px" append-to-body>
      <template v-if="current">
        <el-descriptions :column="3" border class="detail-desc">
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ current.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(current.status) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ current.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ current.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ current.receiverAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单留言" :span="3">{{ current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="current.items || []" border>
          <el-table-column prop="productCode" label="编号" min-width="120" />
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column label="商品图片" width="142" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.productImageUrl"
                class="item-img"
                :src="row.productImageUrl"
                :preview-src-list="[row.productImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="LOGO" width="142" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.logoImageUrl"
                class="item-img"
                :src="row.logoImageUrl"
                :preview-src-list="[row.logoImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="72" />
          <el-table-column label="属性" min-width="190">
            <template #default="{ row }">
              <div v-if="formatAttributeLines(row.optionAttributeText).length" class="attribute-lines">
                <div v-for="(line, index) in formatAttributeLines(row.optionAttributeText)" :key="index" class="attribute-line">
                  <span v-if="line.label">{{ line.label }}：</span>
                  <strong>{{ line.value }}</strong>
                </div>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="90" align="right" />
          <el-table-column prop="price" label="单价" width="100" align="right">
            <template #default="{ row }">{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100" align="right">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <el-dialog v-model="confirmOpen" title="确认转销售单" width="520px" append-to-body destroy-on-close>
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="96px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="confirmForm.warehouseId" filterable style="width: 100%" placeholder="请选择仓库">
            <el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款金额">
          <el-input-number v-model="confirmForm.paidAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款账户" :required="Number(confirmForm.paidAmount || 0) > 0">
          <el-select v-model="confirmForm.accountId" clearable filterable style="width: 100%" placeholder="请选择账户">
            <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方式" :required="Number(confirmForm.paidAmount || 0) > 0">
          <el-select v-model="confirmForm.paymentMethod" clearable style="width: 100%" placeholder="请选择付款方式">
            <el-option v-for="item in paymentMethods" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单留言">
          <el-input v-model="confirmForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmOpen = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="submitConfirm">确认转销售单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { cancelCustomerOrder, confirmCustomerOrder, getCustomerOrder, listCustomerOrder, printCustomerOrder, type CustomerOrder, type CustomerOrderConfirmPayload, type CustomerOrderQuery, type CustomerOrderStatus } from '@/api/erp/customerOrder'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'

const loading = ref(false)
const confirming = ref(false)
const list = ref<CustomerOrder[]>([])
const total = ref(0)
const queryFormRef = ref()
const confirmFormRef = ref()
const detailOpen = ref(false)
const confirmOpen = ref(false)
const current = ref<CustomerOrder>()
const warehouses = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const paymentMethods = ['淘宝', '1688', '小红书', '微信', '支付宝', '银行卡']
const queryParams = reactive<CustomerOrderQuery>({ current: 1, size: 10 })
const confirmForm = reactive<CustomerOrderConfirmPayload>({ warehouseId: '', paidAmount: 0, accountId: '', paymentMethod: '', remark: '' })
const confirmRules = { warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }] }
type AttributeLine = { label: string, value: string }

function getList() {
  loading.value = true
  listCustomerOrder(queryParams).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}
function loadOptions() {
  listMaster('warehouse', { current: 1, size: 1000, status: 1 }).then(res => warehouses.value = res.records)
  listMaster('account', { current: 1, size: 1000, status: 1 }).then(res => accounts.value = res.records)
}
function handleQuery() { queryParams.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function openDetail(row: CustomerOrder) {
  getCustomerOrder(row.id).then(res => {
    current.value = res
    detailOpen.value = true
  })
}
function openConfirm(row: CustomerOrder) {
  current.value = row
  Object.assign(confirmForm, { warehouseId: warehouses.value[0]?.id || '', paidAmount: 0, accountId: '', paymentMethod: '', remark: row.remark || '' })
  confirmOpen.value = true
}
function submitConfirm() {
  if (Number(confirmForm.paidAmount || 0) > 0 && (!confirmForm.accountId || !confirmForm.paymentMethod)) {
    ElMessage.warning('有付款金额时必须选择账户和付款方式')
    return
  }
  confirmFormRef.value?.validate((valid: boolean) => {
    if (!valid || !current.value) return
    confirming.value = true
    confirmCustomerOrder(current.value.id, confirmForm).then(() => {
      ElMessage.success('已转销售单')
      confirmOpen.value = false
      getList()
    }).finally(() => confirming.value = false)
  })
}
function cancel(row: CustomerOrder) {
  ElMessageBox.prompt('请输入作废原因', '作废客户订单', {
    confirmButtonText: '作废',
    cancelButtonText: '取消',
    inputPlaceholder: '可不填'
  }).then(({ value }) => cancelCustomerOrder(row.id, value)).then(() => {
    ElMessage.success('已作废')
    getList()
  })
}
function print(row: CustomerOrder) {
  printCustomerOrder(row.id).then(res => {
    current.value = res
    detailOpen.value = true
    setTimeout(() => window.print(), 200)
  })
}
function statusText(status: CustomerOrderStatus) {
  return ({ PENDING: '待接单', CONFIRMED: '已转销售单', CANCELLED: '已作废' } as Record<CustomerOrderStatus, string>)[status] || status
}
function statusTag(status: CustomerOrderStatus) {
  return status === 'PENDING' ? 'warning' : status === 'CONFIRMED' ? 'success' : 'info'
}
function money(value?: number) {
  return Number(value || 0).toFixed(2)
}
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
onMounted(() => { loadOptions(); getList() })
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.detail-desc { margin-bottom: 16px; }
.item-img {
  width: 112px;
  height: 112px;
  object-fit: cover;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}
.attribute-lines {
  display: grid;
  gap: 4px;
  line-height: 1.35;
}
.attribute-line span {
  color: var(--el-text-color-regular);
  font-weight: 400;
}
.attribute-line strong {
  color: var(--el-text-color-primary);
  font-weight: 700;
}
@media print {
  :global(.el-overlay),
  :global(.el-overlay-dialog) {
    position: static !important;
  }
}
</style>
