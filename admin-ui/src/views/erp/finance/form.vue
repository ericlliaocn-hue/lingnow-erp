<template>
  <div class="app-container finance-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <el-button type="primary" @click="openAdd" v-permission="`erp:finance:${module}:add`">新增</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="partnerName" :label="module === 'receipt' ? '客户' : '供应商'" />
        <el-table-column prop="accountName" label="账户" />
        <el-table-column prop="amount" label="金额" align="right" />
        <el-table-column prop="auditStatus" label="审核" width="90"><template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : 'info'">{{ row.auditStatus === 1 ? '已审核' : '未审核' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)" v-permission="`erp:finance:${module}:edit`">修改</el-button>
            <el-button v-if="row.auditStatus === 1" link type="primary" @click="unaudit(row)" v-permission="`erp:finance:${module}:unaudit`">反审核</el-button>
            <el-button v-else link type="primary" @click="audit(row)" v-permission="`erp:finance:${module}:audit`">审核</el-button>
            <el-button link type="primary" @click="remove(row)" v-permission="`erp:finance:${module}:remove`">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="open" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" label-width="96px">
        <el-form-item label="单号"><el-input v-model="form.billNo" placeholder="自动生成" /></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.billDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item :label="module === 'receipt' ? '客户' : '供应商'"><el-select v-model="form.partnerId" filterable style="width: 100%"><el-option v-for="item in partners" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="账户"><el-select v-model="form.accountId" filterable style="width: 100%"><el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="open = false">取消</el-button><el-button type="primary" @click="save">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { addFinanceBill, auditFinanceBill, deleteFinanceBill, getFinanceBill, listFinanceBill, nextFinanceNo, unauditFinanceBill, updateFinanceBill, type FinanceBill, type FinanceModule } from '@/api/erp/finance'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'
const route = useRoute()
const module = computed<FinanceModule>(() => route.path.includes('/payment') ? 'payment' : 'receipt')
const title = computed(() => module.value === 'receipt' ? '收款单' : '付款单')
const loading = ref(false)
const open = ref(false)
const dialogTitle = ref('')
const list = ref<FinanceBill[]>([])
const total = ref(0)
const partners = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const query = reactive({ current: 1, size: 10 })
const form = ref<FinanceBill>({ billDate: new Date().toISOString().slice(0, 10), partnerId: '', accountId: '', amount: 0 })
function getList() { loading.value = true; listFinanceBill(module.value, query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false) }
function loadOptions() {
  listMaster(module.value === 'receipt' ? 'customer' : 'supplier', { current: 1, size: 200 }).then(res => partners.value = res.records)
  listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records)
}
function openAdd() {
  form.value = { billDate: new Date().toISOString().slice(0, 10), partnerId: '', accountId: '', amount: 0 }
  nextFinanceNo(module.value).then(no => form.value.billNo = no)
  dialogTitle.value = `新增${title.value}`
  open.value = true
}
function edit(row: FinanceBill) { getFinanceBill(module.value, row.id!).then(res => { form.value = res; dialogTitle.value = `修改${title.value}`; open.value = true }) }
function save() { const action = form.value.id ? updateFinanceBill(module.value, form.value) : addFinanceBill(module.value, form.value); action.then(() => { ElMessage.success('保存成功'); open.value = false; getList() }) }
function audit(row: FinanceBill) { auditFinanceBill(module.value, row.id!).then(() => { ElMessage.success('审核成功'); getList() }) }
function unaudit(row: FinanceBill) { unauditFinanceBill(module.value, row.id!).then(() => { ElMessage.success('反审核成功'); getList() }) }
function remove(row: FinanceBill) { ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' }).then(() => deleteFinanceBill(module.value, row.id!)).then(() => { ElMessage.success('删除成功'); getList() }) }
watch(() => route.path, () => { loadOptions(); getList() })
onMounted(() => { loadOptions(); getList() })
</script>
<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
