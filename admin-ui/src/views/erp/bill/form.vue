<template>
  <div class="app-container bill-form-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <div>
            <el-button @click="back">返回</el-button>
            <el-button type="primary" :disabled="readonly" @click="submit(false)" v-permission="form.id ? `erp:${module}:edit` : `erp:${module}:add`">保存</el-button>
            <el-button type="warning" :disabled="readonly" @click="submit(true)" v-permission="`erp:${module}:audit`">保存并提交</el-button>
          </div>
        </div>
      </template>
        <el-alert v-if="readonly" title="已审核单据只能查看，需反审核后才能修改。" type="warning" :closable="false" show-icon class="readonly-alert" />
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" :disabled="readonly">
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="单号"><el-input v-model="form.billNo" placeholder="自动生成" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="日期" prop="billDate"><el-date-picker v-model="form.billDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="partnerLabel" prop="partnerId"><el-select v-model="form.partnerId" filterable style="width: 100%"><el-option v-for="item in partners" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="仓库" prop="warehouseId"><el-select v-model="form.warehouseId" filterable style="width: 100%"><el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="账户"><el-select v-model="form.accountId" clearable filterable style="width: 100%"><el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="实收/实付"><el-input-number v-model="form.paidAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="整单优惠"><el-input-number v-model="form.discountAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="其他费用"><el-input-number v-model="form.otherAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-divider />
        <div class="toolbar"><el-button type="primary" :disabled="readonly" @click="addRow">添加商品</el-button></div>
        <el-table :data="form.items" border>
          <el-table-column label="商品" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.productId" filterable placeholder="选择商品" style="width: 100%" @change="productChanged(row)">
                <el-option v-for="item in products" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" min-width="100" />
          <el-table-column label="数量" width="140"><template #default="{ row }"><el-input-number v-model="row.qty" :min="0" :precision="2" @change="calc" /></template></el-table-column>
          <el-table-column label="单价" width="140"><template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" @change="calc" /></template></el-table-column>
          <el-table-column prop="amount" label="金额" width="120" align="right" />
          <el-table-column label="操作" width="90"><template #default="{ $index }"><el-button link type="primary" :disabled="readonly" @click="form.items.splice($index, 1); calc()">删除</el-button></template></el-table-column>
        </el-table>
        <div class="totals">
          <span>总数量：{{ totalQty }}</span>
          <span>总金额：{{ totalAmount }}</span>
          <span>应收/应付：{{ payableAmount }}</span>
          <span>欠款：{{ debtAmount }}</span>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addBill, getBill, listBill, nextBillNo, updateBill, type BillItem, type BillModule, type ErpBill } from '@/api/erp/bill'
import { submitApproval, type ApprovalBizType } from '@/api/erp/approval'
import { productOptions, type ErpProduct } from '@/api/erp/product'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'

const route = useRoute()
const router = useRouter()
const module = computed<BillModule>(() => {
  if (route.path.includes('/sale-return')) return 'sale-return'
  if (route.path.includes('/purchase-return')) return 'purchase-return'
  return route.path.includes('/purchase') ? 'purchase' : 'sale'
})
const titleMap: Record<BillModule, string> = { sale: '新增销售单', 'sale-return': '新增销售退货', purchase: '新增进货单', 'purchase-return': '新增进货退货' }
const title = computed(() => titleMap[module.value])
const partnerLabel = computed(() => module.value.startsWith('sale') ? '客户' : '供应商')
const bizType = computed<ApprovalBizType>(() => {
  const map: Record<BillModule, ApprovalBizType> = {
    sale: 'SALE',
    'sale-return': 'SALE_RETURN',
    purchase: 'PURCHASE',
    'purchase-return': 'PURCHASE_RETURN'
  }
  return map[module.value]
})
const formRef = ref()
const products = ref<ErpProduct[]>([])
const partners = ref<ErpMasterVO[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const readonly = computed(() => form.value.auditStatus === 1)
const state = reactive({
  form: { billDate: new Date().toISOString().slice(0, 10), partnerId: '', warehouseId: '', paidAmount: 0, discountAmount: 0, otherAmount: 0, items: [] } as ErpBill,
  rules: {
    billDate: [{ required: true, message: '日期不能为空', trigger: 'change' }],
    partnerId: [{ required: true, message: '往来单位不能为空', trigger: 'change' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(state)
const totalQty = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.qty || 0), 0).toFixed(2))
const totalAmount = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.amount || 0), 0).toFixed(2))
const payableAmount = computed(() => (Number(totalAmount.value) - Number(form.value.discountAmount || 0) + Number(form.value.otherAmount || 0)).toFixed(2))
const debtAmount = computed(() => (Number(payableAmount.value) - Number(form.value.paidAmount || 0)).toFixed(2))

function loadOptions() {
  productOptions().then(res => products.value = res)
  listMaster(module.value.startsWith('sale') ? 'customer' : 'supplier', { current: 1, size: 200 }).then(res => partners.value = res.records)
  listMaster('warehouse', { current: 1, size: 200 }).then(res => warehouses.value = res.records)
  listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records)
}
function loadData() {
  const id = route.query.id as string
  if (id) {
    getBill(module.value, id).then(res => form.value = { ...res, items: res.items || [] })
  } else {
    nextBillNo(module.value).then(no => form.value.billNo = no)
  }
}
function addRow() { form.value.items.push({ productId: '', qty: 1, price: 0 } as BillItem) }
function productChanged(row: BillItem) {
  const product = products.value.find(item => item.id === row.productId)
  if (!product) return
  row.productCode = product.code
  row.productName = product.name
  row.spec = product.spec
  row.unitId = product.unitId
  row.price = module.value.startsWith('sale') ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0)
  calc()
}
function calc() {
  form.value.items.forEach(row => {
    row.amount = Number(row.qty || 0) * Number(row.price || 0)
  })
}
function submit(needAudit: boolean) {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (!form.value.items.length) { ElMessage.warning('请添加商品明细'); return }
    calc()
    const action = form.value.id ? updateBill(module.value, form.value) : addBill(module.value, form.value)
    action.then(() => {
      ElMessage.success('保存成功')
      if (needAudit) {
        auditSavedBill()
      } else {
        back()
      }
    })
  })
}
function auditSavedBill() {
  const id = form.value.id
  if (id) {
    submitApproval(bizType.value, id).then(() => {
      ElMessage.success('提交审批成功')
      back()
    })
    return
  }
  listBill(module.value, { current: 1, size: 1, billNo: form.value.billNo }).then(res => {
    const saved = res.records[0]
    if (!saved?.id) {
      back()
      return
    }
    submitApproval(bizType.value, saved.id).then(() => {
      ElMessage.success('提交审批成功')
      back()
    })
  })
}
function back() { router.push(`/erp/${module.value}/list`) }
onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.card-header, .toolbar, .totals { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.toolbar { justify-content: flex-start; margin-bottom: 12px; }
.totals { justify-content: flex-end; padding: 16px 0; font-weight: 600; }
.readonly-alert { margin-bottom: 16px; }
</style>
