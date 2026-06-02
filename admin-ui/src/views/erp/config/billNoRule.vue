<template>
  <div class="app-container erp-config-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryRef">
        <el-form-item label="类型" prop="billType"><el-input v-model="query.billType" placeholder="请输入单据类型" clearable /></el-form-item>
        <el-form-item label="名称" prop="billName"><el-input v-model="query.billName" placeholder="请输入规则名称" clearable /></el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-select v-model="query.enabled" placeholder="请选择" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="getList">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><strong>单号规则</strong><el-button type="primary" :icon="Plus" @click="openAdd">新增</el-button></div></template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="billType" label="单据类型" min-width="140" />
        <el-table-column prop="billName" label="规则名称" min-width="160" />
        <el-table-column prop="prefix" label="前缀" width="100" />
        <el-table-column prop="datePattern" label="日期格式" width="120" />
        <el-table-column prop="serialLength" label="流水长度" width="100" />
        <el-table-column prop="nextSerial" label="下一流水" width="110" />
        <el-table-column prop="enabled" label="状态" width="90"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button link type="primary" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>

    <el-dialog v-model="open" :title="title" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="单据类型" prop="billType"><el-input v-model="form.billType" placeholder="如 SALE" /></el-form-item>
        <el-form-item label="规则名称" prop="billName"><el-input v-model="form.billName" /></el-form-item>
        <el-form-item label="前缀" prop="prefix"><el-input v-model="form.prefix" /></el-form-item>
        <el-form-item label="日期格式"><el-input v-model="form.datePattern" placeholder="yyyyMMdd" /></el-form-item>
        <el-form-item label="流水长度" prop="serialLength"><el-input-number v-model="form.serialLength" :min="1" :max="12" /></el-form-item>
        <el-form-item label="下一流水" prop="nextSerial"><el-input-number v-model="form.nextSerial" :min="1" /></el-form-item>
        <el-form-item label="重置周期"><el-select v-model="form.resetCycle"><el-option label="每日" value="DAY" /><el-option label="每月" value="MONTH" /><el-option label="不重置" value="NONE" /></el-select></el-form-item>
        <el-form-item label="启用"><el-radio-group v-model="form.enabled"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="open = false">取消</el-button><el-button type="primary" @click="save">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { addConfig, deleteConfig, getConfig, listConfig, updateConfig, type BillNoRule } from '@/api/erp/config'

const loading = ref(false)
const open = ref(false)
const title = ref('')
const total = ref(0)
const list = ref<BillNoRule[]>([])
const queryRef = ref()
const formRef = ref()
const query = reactive({ current: 1, size: 10, billType: '', billName: '', enabled: undefined as number | undefined })
const form = ref<BillNoRule>({ billType: '', billName: '', prefix: '', datePattern: 'yyyyMMdd', serialLength: 4, nextSerial: 1, resetCycle: 'DAY', enabled: 1 })
const rules = { billType: [{ required: true, message: '单据类型不能为空', trigger: 'blur' }], billName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }], prefix: [{ required: true, message: '前缀不能为空', trigger: 'blur' }] }

function getList() { loading.value = true; listConfig<BillNoRule>('bill-no-rule', query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false) }
function resetQuery() { queryRef.value?.resetFields(); getList() }
function openAdd() { form.value = { billType: '', billName: '', prefix: '', datePattern: 'yyyyMMdd', serialLength: 4, nextSerial: 1, resetCycle: 'DAY', enabled: 1 }; title.value = '新增单号规则'; open.value = true }
function openEdit(row: BillNoRule) { getConfig<BillNoRule>('bill-no-rule', row.id!).then(res => { form.value = res; title.value = '修改单号规则'; open.value = true }) }
function save() { formRef.value?.validate((valid: boolean) => { if (!valid) return; const action = form.value.id ? updateConfig('bill-no-rule', form.value) : addConfig('bill-no-rule', form.value); action.then(() => { ElMessage.success('保存成功'); open.value = false; getList() }) }) }
function remove(row: BillNoRule) { ElMessageBox.confirm('确定删除该规则吗？', '提示', { type: 'warning' }).then(() => deleteConfig('bill-no-rule', row.id!)).then(() => { ElMessage.success('删除成功'); getList() }) }
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
