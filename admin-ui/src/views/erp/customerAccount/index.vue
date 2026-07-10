<template>
  <div class="app-container customer-account-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form ref="queryFormRef" :inline="true" :model="queryParams">
        <el-form-item label="账号" prop="username">
          <el-input v-model="queryParams.username" clearable placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="客户" prop="customerName">
          <el-input v-model="queryParams.customerName" clearable placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="请选择状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
          <strong>客户账号</strong>
          <el-button type="primary" :icon="Plus" @click="openAdd" v-permission="'erp:customer-account:add'">新增账号</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%">
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="username" label="登录账号" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="170">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)" v-permission="'erp:customer-account:edit'">修改</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)" v-permission="'erp:customer-account:edit'">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <el-dialog v-model="open" :title="title" width="520px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="form.customerId" filterable style="width: 100%" placeholder="请选择客户">
            <el-option v-for="item in customers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" maxlength="64" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.id ? undefined : 'password'">
          <el-input v-model="form.password" show-password maxlength="32" :placeholder="form.id ? '不填则不修改密码' : '请输入初始密码'" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="64" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="32" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { addCustomerAccount, listCustomerAccount, updateCustomerAccount, updateCustomerAccountStatus, type CustomerAccount, type CustomerAccountQuery } from '@/api/erp/customerAccount'
import { listMaster, type ErpMasterVO } from '@/api/erp/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<CustomerAccount[]>([])
const total = ref(0)
const queryFormRef = ref()
const formRef = ref()
const open = ref(false)
const title = ref('新增客户账号')
const customers = ref<ErpMasterVO[]>([])
const queryParams = reactive<CustomerAccountQuery>({ current: 1, size: 10 })
const form = reactive<CustomerAccount>({ customerId: '', username: '', password: '', nickname: '', phone: '', status: 1, remark: '' })
const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listCustomerAccount(queryParams).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}
function loadCustomers() {
  listMaster('customer', { current: 1, size: 1000, status: 1 }).then(res => customers.value = res.records)
}
function handleQuery() { queryParams.current = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function resetForm() {
  Object.assign(form, { id: undefined, customerId: '', username: '', password: '', nickname: '', phone: '', status: 1, remark: '' })
}
function openAdd() {
  resetForm()
  title.value = '新增客户账号'
  open.value = true
}
function openEdit(row: CustomerAccount) {
  resetForm()
  Object.assign(form, { ...row, password: '' })
  title.value = '修改客户账号'
  open.value = true
}
function submit() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    saving.value = true
    const action = form.id ? updateCustomerAccount(form) : addCustomerAccount(form)
    action.then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    }).finally(() => saving.value = false)
  })
}
function toggleStatus(row: CustomerAccount) {
  const nextStatus = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(`确定${nextStatus === 1 ? '启用' : '禁用'}账号“${row.username}”吗？`, '提示', { type: 'warning' })
    .then(() => updateCustomerAccountStatus(row.id!, nextStatus))
    .then(() => { ElMessage.success('操作成功'); getList() })
}
onMounted(() => { loadCustomers(); getList() })
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
</style>
