<template>
  <div class="app-container stock-check-form-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>新增库存盘点</strong>
          <div>
            <el-button @click="back">返回</el-button>
            <el-button type="primary" :disabled="readonly" @click="submit(false)" v-permission="form.id ? 'erp:stock-check:edit' : 'erp:stock-check:add'">保存</el-button>
            <el-button type="warning" :disabled="readonly" @click="submit(true)" v-permission="'erp:stock-check:audit'">保存并提交</el-button>
          </div>
        </div>
      </template>
      <el-alert v-if="readonly" title="已审核盘点单只能查看，需反审核后才能修改。" type="warning" :closable="false" show-icon class="readonly-alert" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" :disabled="readonly">
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="单号"><el-input v-model="form.checkNo" placeholder="自动生成" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="日期" prop="checkDate"><el-date-picker v-model="form.checkDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="仓库" prop="warehouseId"><el-select v-model="form.warehouseId" filterable style="width: 100%"><el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
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
          <el-table-column prop="bookQty" label="账面数" width="120" align="right" />
          <el-table-column label="盘点数" width="150"><template #default="{ row }"><el-input-number v-model="row.checkQty" :min="0" :precision="2" @change="calc" /></template></el-table-column>
          <el-table-column prop="diffQty" label="差异数" width="120" align="right" />
          <el-table-column prop="costPrice" label="成本价" width="120" align="right" />
          <el-table-column prop="diffAmount" label="差异金额" width="120" align="right" />
          <el-table-column label="操作" width="90"><template #default="{ $index }"><el-button link type="primary" :disabled="readonly" @click="form.items.splice($index, 1); calc()">删除</el-button></template></el-table-column>
        </el-table>
        <div class="totals">
          <span>盘盈数量：{{ totalProfitQty }}</span>
          <span>盘亏数量：{{ totalLossQty }}</span>
          <span>盘盈金额：{{ totalProfitAmount }}</span>
          <span>盘亏金额：{{ totalLossAmount }}</span>
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
import { productOptions, type ErpProduct } from '@/api/erp/product'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
import { stockBalance } from '@/api/erp/report'
import { addStockCheck, getStockCheck, listStockCheck, nextStockCheckNo, updateStockCheck, type StockCheck, type StockCheckItem } from '@/api/erp/stock'
import { submitApproval } from '@/api/erp/approval'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const products = ref<ErpProduct[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const readonly = computed(() => form.value.auditStatus === 1)
const state = reactive({
  form: { checkDate: new Date().toISOString().slice(0, 10), warehouseId: '', items: [] } as StockCheck,
  rules: {
    checkDate: [{ required: true, message: '日期不能为空', trigger: 'change' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(state)

const totalProfitQty = computed(() => total(true, false))
const totalLossQty = computed(() => total(false, false))
const totalProfitAmount = computed(() => total(true, true))
const totalLossAmount = computed(() => total(false, true))

function loadOptions() {
  productOptions().then(res => products.value = res)
  listMaster('warehouse', { current: 1, size: 200 }).then(res => warehouses.value = res.records)
}
function loadData() {
  const id = route.query.id as string
  if (id) {
    getStockCheck(id).then(res => form.value = { ...res, items: res.items || [] })
  } else {
    nextStockCheckNo().then(no => form.value.checkNo = no)
  }
}
function addRow() { form.value.items.push({ productId: '', checkQty: 0, bookQty: 0, costPrice: 0, diffQty: 0, diffAmount: 0 } as StockCheckItem) }
function productChanged(row: StockCheckItem) {
  const product = products.value.find(item => item.id === row.productId)
  if (!product) return
  row.productCode = product.code
  row.productName = product.name
  row.spec = product.spec
  row.unitId = product.unitId
  row.costPrice = Number(product.purchasePrice || 0)
  if (!form.value.warehouseId) {
    calc()
    return
  }
  stockBalance({ current: 1, size: 1, productId: row.productId, warehouseId: form.value.warehouseId }).then(res => {
    const balance = res.records[0]
    row.bookQty = Number(balance?.qty || 0)
    row.costPrice = Number(balance?.avgCost || row.costPrice || 0)
    row.checkQty = row.bookQty
    calc()
  })
}
function calc() {
  form.value.items.forEach(row => {
    row.diffQty = Number(row.checkQty || 0) - Number(row.bookQty || 0)
    row.diffAmount = Number(row.diffQty || 0) * Number(row.costPrice || 0)
  })
}
function total(profit: boolean, amount: boolean) {
  return form.value.items.reduce((sum, row) => {
    const diff = Number(row.diffQty || 0)
    if (profit ? diff > 0 : diff < 0) {
      return sum + Math.abs(Number(amount ? row.diffAmount || 0 : diff))
    }
    return sum
  }, 0).toFixed(2)
}
function submit(needAudit: boolean) {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (!form.value.items.length) { ElMessage.warning('请添加盘点明细'); return }
    calc()
    const action = form.value.id ? updateStockCheck(form.value) : addStockCheck(form.value)
    action.then(() => {
      ElMessage.success('保存成功')
      if (needAudit) auditSavedCheck()
      else back()
    })
  })
}
function auditSavedCheck() {
  if (form.value.id) {
    submitApproval('STOCK_CHECK', form.value.id).then(() => { ElMessage.success('提交审批成功'); back() })
    return
  }
  listStockCheck({ current: 1, size: 1, billNo: form.value.checkNo }).then(res => {
    const saved = res.records[0]
    if (!saved?.id) { back(); return }
    submitApproval('STOCK_CHECK', saved.id).then(() => { ElMessage.success('提交审批成功'); back() })
  })
}
function back() { router.push('/erp/stock/check') }
onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.card-header, .toolbar, .totals { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.toolbar { justify-content: flex-start; margin-bottom: 12px; }
.totals { justify-content: flex-end; padding: 16px 0; font-weight: 600; }
.readonly-alert { margin-bottom: 16px; }
</style>
