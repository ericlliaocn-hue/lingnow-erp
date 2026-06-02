<template>
  <div class="app-container erp-config-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="query" ref="queryRef">
        <el-form-item label="模块" prop="moduleCode"><el-input v-model="query.moduleCode" placeholder="请输入模块编码" clearable /></el-form-item>
        <el-form-item label="字段" prop="fieldKey"><el-input v-model="query.fieldKey" placeholder="请输入字段键" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="getList">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-header"><strong>字段设置</strong><el-button type="primary" :icon="Plus" @click="openAdd">新增</el-button></div></template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="moduleCode" label="模块编码" min-width="140" />
        <el-table-column prop="fieldKey" label="字段键" min-width="140" />
        <el-table-column prop="fieldLabel" label="字段名称" min-width="160" />
        <el-table-column prop="visible" label="显示" width="90"><template #default="{ row }"><el-tag :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag></template></el-table-column>
        <el-table-column prop="required" label="必填" width="90"><template #default="{ row }"><el-tag :type="row.required === 1 ? 'warning' : 'info'">{{ row.required === 1 ? '必填' : '选填' }}</el-tag></template></el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="width" label="宽度" width="90" />
        <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">修改</el-button><el-button link type="primary" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>
    <el-dialog v-model="open" :title="title" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模块编码" prop="moduleCode"><el-input v-model="form.moduleCode" /></el-form-item>
        <el-form-item label="字段键" prop="fieldKey"><el-input v-model="form.fieldKey" /></el-form-item>
        <el-form-item label="字段名称" prop="fieldLabel"><el-input v-model="form.fieldLabel" /></el-form-item>
        <el-form-item label="显示"><el-radio-group v-model="form.visible"><el-radio :value="1">显示</el-radio><el-radio :value="0">隐藏</el-radio></el-radio-group></el-form-item>
        <el-form-item label="必填"><el-radio-group v-model="form.required"><el-radio :value="1">必填</el-radio><el-radio :value="0">选填</el-radio></el-radio-group></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="宽度"><el-input-number v-model="form.width" :min="0" /></el-form-item>
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
import { addConfig, deleteConfig, getConfig, listConfig, updateConfig, type FieldSetting } from '@/api/erp/config'
const loading = ref(false), open = ref(false), title = ref('')
const total = ref(0), queryRef = ref(), formRef = ref()
const list = ref<FieldSetting[]>([])
const query = reactive({ current: 1, size: 10, moduleCode: '', fieldKey: '' })
const form = ref<FieldSetting>({ moduleCode: '', fieldKey: '', fieldLabel: '', visible: 1, required: 0, sortOrder: 0, width: 120 })
const rules = { moduleCode: [{ required: true, message: '模块编码不能为空', trigger: 'blur' }], fieldKey: [{ required: true, message: '字段键不能为空', trigger: 'blur' }], fieldLabel: [{ required: true, message: '字段名称不能为空', trigger: 'blur' }] }
function getList() { loading.value = true; listConfig<FieldSetting>('field-setting', query).then(res => { list.value = res.records; total.value = Number(res.total) }).finally(() => loading.value = false) }
function resetQuery() { queryRef.value?.resetFields(); getList() }
function openAdd() { form.value = { moduleCode: '', fieldKey: '', fieldLabel: '', visible: 1, required: 0, sortOrder: 0, width: 120 }; title.value = '新增字段设置'; open.value = true }
function openEdit(row: FieldSetting) { getConfig<FieldSetting>('field-setting', row.id!).then(res => { form.value = res; title.value = '修改字段设置'; open.value = true }) }
function save() { formRef.value?.validate((valid: boolean) => { if (!valid) return; const action = form.value.id ? updateConfig('field-setting', form.value) : addConfig('field-setting', form.value); action.then(() => { ElMessage.success('保存成功'); open.value = false; getList() }) }) }
function remove(row: FieldSetting) { ElMessageBox.confirm('确定删除该字段设置吗？', '提示', { type: 'warning' }).then(() => deleteConfig('field-setting', row.id!)).then(() => { ElMessage.success('删除成功'); getList() }) }
onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
