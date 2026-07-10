<template>
  <div class="app-container finance-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <div>
            <el-button type="primary" @click="openAdd" v-permission="`erp:finance:${module}:add`">新增</el-button>
            <el-button type="danger" :disabled="!canDeleteSelected" @click="handleDelete" v-permission="`erp:finance:${module}:remove`">删除</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column v-if="hasPartner" prop="partnerName" :label="partnerLabel" />
        <el-table-column prop="accountName" label="账户" />
        <el-table-column prop="amount" label="金额" align="right" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)" v-permission="`erp:finance:${module}:edit`">修改</el-button>
            <el-button link type="primary" @click="remove(row)" v-permission="`erp:finance:${module}:remove`">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="open" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="单号"><el-input v-model="form.billNo" placeholder="自动生成" /></el-form-item>
        <el-form-item label="日期" prop="billDate"><el-date-picker v-model="form.billDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item v-if="hasPartner" :label="partnerLabel" prop="partnerId"><el-select v-model="form.partnerId" filterable style="width: 100%"><el-option v-for="item in partners" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="账户" prop="accountId"><el-select v-model="form.accountId" filterable style="width: 100%"><el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="金额" prop="amount"><el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="open = false">取消</el-button><el-button type="primary" @click="save">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { addFinanceBill, deleteFinanceBill, getFinanceBill, listFinanceBill, nextFinanceNo, updateFinanceBill, type FinanceBill, type FinanceModule } from '@/api/erp/finance'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
const route = useRoute()
const module = computed<FinanceModule>(() => {
  if (route.path.includes('/payment')) return 'payment'
  if (route.path.includes('/income')) return 'income'
  if (route.path.includes('/expense')) return 'expense'
  return 'receipt'
})
const titleMap: Record<FinanceModule, string> = { receipt: '收款单', payment: '付款单', income: '其他收入', expense: '其他支出' }
const title = computed(() => titleMap[module.value])
const hasPartner = computed(() => module.value === 'receipt' || module.value === 'payment')
const partnerLabel = computed(() => module.value === 'receipt' ? '客户' : '供应商')
const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const list = ref<FinanceBill[]>([])
const total = ref(0)
const ids = ref<string[]>([])
const selected = ref<FinanceBill[]>([])
const partners = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const query = reactive({ current: 1, size: 10 })
const formRef = ref<FormInstance>()
const form = ref<FinanceBill>({ billDate: new Date().toISOString().slice(0, 10), partnerId: '', accountId: '', amount: 0 })
const canDeleteSelected = computed(() => selected.value.length > 0)
const rules = computed<FormRules<FinanceBill>>(() => ({
  billDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  partnerId: hasPartner.value ? [{ required: true, message: `请选择${partnerLabel.value}`, trigger: 'change' }] : [],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  amount: [{
    validator: (_rule, value, callback) => {
      if (Number(value) > 0) {
        callback()
        return
      }
      callback(new Error('金额必须大于 0'))
    },
    trigger: 'change'
  }]
}))
function getList() { loading.value = true; listFinanceBill(module.value, query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false) }
function loadOptions() {
  if (hasPartner.value) {
    listMaster(module.value === 'receipt' ? 'customer' : 'supplier', { current: 1, size: 200 }).then(res => partners.value = res.records)
  } else {
    partners.value = []
  }
  listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records)
}
function openAdd() {
  form.value = { billDate: new Date().toISOString().slice(0, 10), partnerId: '', accountId: '', amount: 0 }
  nextFinanceNo(module.value).then(no => form.value.billNo = no)
  dialogTitle.value = `新增${title.value}`
  open.value = true
  nextTick(() => formRef.value?.clearValidate())
}
function edit(row: FinanceBill) { getFinanceBill(module.value, row.id!).then(res => { form.value = res; dialogTitle.value = `修改${title.value}`; open.value = true; nextTick(() => formRef.value?.clearValidate()) }) }
async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const action = form.value.id ? updateFinanceBill(module.value, form.value) : addFinanceBill(module.value, form.value)
  action.then(() => { ElMessage.success('保存成功'); open.value = false; getList() })
}
function handleSelectionChange(rows: FinanceBill[]) {
  selected.value = rows
  ids.value = rows.map(row => row.id!)
}
function remove(row: FinanceBill) { ElMessageBox.confirm('确定删除单据吗？', '提示', { type: 'warning' }).then(() => deleteFinanceBill(module.value, row.id!)).then(() => { ElMessage.success('删除成功'); getList() }) }
function handleDelete() {
  ElMessageBox.confirm('确定删除选中的单据吗？', '提示', { type: 'warning' })
    .then(() => deleteFinanceBill(module.value, ids.value))
    .then(() => { ElMessage.success('删除成功'); getList() })
}
watch(() => route.path, () => { loadOptions(); getList() })
onMounted(() => { loadOptions(); getList() })
</script>
<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
