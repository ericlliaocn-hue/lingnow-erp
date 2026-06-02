<template>
  <div class="app-container erp-config-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryRef">
        <el-form-item label="编码" prop="templateCode"><el-input v-model="query.templateCode" placeholder="请输入模板编码" clearable /></el-form-item>
        <el-form-item label="名称" prop="templateName"><el-input v-model="query.templateName" placeholder="请输入模板名称" clearable /></el-form-item>
        <el-form-item label="类型" prop="billType"><el-input v-model="query.billType" placeholder="请输入单据类型" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="getList">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-header"><strong>打印模板</strong><el-button type="primary" :icon="Plus" @click="openAdd">新增</el-button></div></template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="templateCode" label="模板编码" min-width="150" />
        <el-table-column prop="templateName" label="模板名称" min-width="160" />
        <el-table-column prop="billType" label="单据类型" min-width="130" />
        <el-table-column prop="paperType" label="纸张" width="100" />
        <el-table-column prop="isDefault" label="默认" width="90"><template #default="{ row }"><el-tag :type="row.isDefault === 1 ? 'success' : 'info'">{{ row.isDefault === 1 ? '默认' : '普通' }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">修改</el-button><el-button link type="primary" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="open" :title="title" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板编码" prop="templateCode"><el-input v-model="form.templateCode" /></el-form-item>
        <el-form-item label="模板名称" prop="templateName"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="单据类型" prop="billType"><el-input v-model="form.billType" placeholder="如 SALE" /></el-form-item>
        <el-form-item label="纸张"><el-select v-model="form.paperType"><el-option label="A4" value="A4" /><el-option label="A5" value="A5" /><el-option label="小票58mm" value="POS58" /><el-option label="小票80mm" value="POS80" /></el-select></el-form-item>
        <el-form-item label="默认"><el-radio-group v-model="form.isDefault"><el-radio :value="1">是</el-radio><el-radio :value="0">否</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="模板内容"><el-input v-model="form.contentJson" type="textarea" :rows="8" placeholder="JSON模板内容" /></el-form-item>
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
import { addConfig, deleteConfig, getConfig, listConfig, updateConfig, type PrintTemplate } from '@/api/erp/config'
const loading = ref(false), open = ref(false), title = ref('')
const total = ref(0), queryRef = ref(), formRef = ref()
const list = ref<PrintTemplate[]>([])
const query = reactive({ current: 1, size: 10, templateCode: '', templateName: '', billType: '' })
const form = ref<PrintTemplate>({ templateCode: '', templateName: '', billType: '', paperType: 'A4', contentJson: '{}', isDefault: 0, status: 1 })
const rules = { templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }], templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }], billType: [{ required: true, message: '单据类型不能为空', trigger: 'blur' }] }
function getList() { loading.value = true; listConfig<PrintTemplate>('print-template', query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false) }
function resetQuery() { queryRef.value?.resetFields(); getList() }
function openAdd() { form.value = { templateCode: '', templateName: '', billType: '', paperType: 'A4', contentJson: '{}', isDefault: 0, status: 1 }; title.value = '新增打印模板'; open.value = true }
function openEdit(row: PrintTemplate) { getConfig<PrintTemplate>('print-template', row.id!).then(res => { form.value = res; title.value = '修改打印模板'; open.value = true }) }
function save() { formRef.value?.validate((valid: boolean) => { if (!valid) return; const action = form.value.id ? updateConfig('print-template', form.value) : addConfig('print-template', form.value); action.then(() => { ElMessage.success('保存成功'); open.value = false; getList() }) }) }
function remove(row: PrintTemplate) { ElMessageBox.confirm('确定删除该打印模板吗？', '提示', { type: 'warning' }).then(() => deleteConfig('print-template', row.id!)).then(() => { ElMessage.success('删除成功'); getList() }) }
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
