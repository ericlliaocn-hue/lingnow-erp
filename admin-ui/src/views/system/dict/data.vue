<template>
  <div class="dict-data-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input
            v-model="queryParams.dictLabel"
            placeholder="请输入字典标签"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
            <el-option
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.label"
              :value="Number(dict.value)"
            />
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
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="dataList" style="width: 100%; flex: 1; overflow: auto;">
        <el-table-column type="index" width="75" align="center" label="序号" />
        <el-table-column label="字典标签" align="center" prop="dictLabel" />
        <el-table-column label="字典键值" align="center" prop="dictValue" />
        <el-table-column label="字典排序" align="center" prop="dictSort" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <template v-for="(dict, index) in sys_normal_disable" :key="index">
              <el-tag v-if="dict.value == '' + scope.row.status" :type="dict.elTagType">{{ dict.label }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" :icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="primary" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 10px;">
        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.current"
          v-model:limit="queryParams.size"
          @pagination="getList"
        />
      </div>
    </el-card>

    <!-- 添加或修改字典数据对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="dataFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="字典类型">
          <el-input v-model="form.dictType" disabled />
        </el-form-item>
        <el-form-item label="数据标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
        </el-form-item>
        <el-form-item label="样式属性" prop="cssClass">
          <el-input v-model="form.cssClass" placeholder="请输入样式属性" />
        </el-form-item>
        <el-form-item label="回显样式" prop="listClass">
          <el-select v-model="form.listClass">
            <el-option label="默认" value="" />
            <el-option label="主要" value="primary" />
            <el-option label="成功" value="success" />
            <el-option label="信息" value="info" />
            <el-option label="警告" value="warning" />
            <el-option label="危险" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="显示排序" prop="dictSort">
          <el-input-number v-model="form.dictSort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="系统默认">
            <el-radio-group v-model="form.isDefault">
              <el-radio v-for="dict in sys_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="Number(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, toRefs, watch, onMounted } from 'vue'
import { useDict } from '@/hooks/web/useDict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listDictData, getDictData, delDictData, addDictData, updateDictData } from '@/api/system/dict'
import type { DictDataVO, DictDataForm, DictDataQuery } from '@/api/system/dict'
import Pagination from '@/components/Pagination/index.vue'

const { sys_normal_disable, sys_yes_no } = useDict('sys_normal_disable', 'sys_yes_no')

const props = defineProps({
  dictType: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close'])

const queryFormRef = ref()
const dataFormRef = ref()
const loading = ref(true)
const total = ref(0)
const dataList = ref<DictDataVO[]>([])
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {} as DictDataForm,
  queryParams: {
    current: 1,
    size: 10,
    dictLabel: undefined,
    dictType: props.dictType,
    status: undefined
  } as DictDataQuery,
  rules: {
    dictLabel: [{ required: true, message: '数据标签不能为空', trigger: 'blur' }],
    dictValue: [{ required: true, message: '数据键值不能为空', trigger: 'blur' }],
    dictSort: [{ required: true, message: '数据顺序不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

watch(() => props.dictType, (val) => {
  queryParams.value.dictType = val
  getList()
})

/** 查询字典数据列表 */
function getList() {
  loading.value = true
  listDictData(queryParams.value).then(response => {
    const res = response as any
    if (Array.isArray(res)) {
      dataList.value = res
      total.value = res.length
    } else {
      dataList.value = res.records || []
      total.value = Number(res.total) || 0
    }
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    dictCode: undefined,
    dictLabel: '',
    dictValue: '',
    dictSort: 0,
    dictType: props.dictType,
    status: 1,
    isDefault: 'N',
    cssClass: '',
    listClass: '',
    remark: undefined
  }
  if (dataFormRef.value) {
    dataFormRef.value.resetFields()
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.current = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  if (queryFormRef.value) {
    queryFormRef.value.resetFields()
  }
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加字典数据'
}

/** 修改按钮操作 */
function handleUpdate(row: DictDataVO) {
  reset()
  getDictData(row.dictCode).then(response => {
    form.value = response
    open.value = true
    title.value = '修改字典数据'
  })
}

/** 提交按钮 */
function submitForm() {
  dataFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.dictCode) {
        updateDictData(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDictData(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          handleQuery()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: DictDataVO) {
  ElMessageBox.confirm('是否确认删除字典标签为"' + row.dictLabel + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delDictData(row.dictCode)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function handleClose() {
  emit('close')
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.dict-data-container {
  padding: 0 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.search-wrapper {
  margin-bottom: 20px;
}
.table-wrapper {
  margin-bottom: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.table-wrapper :deep(.el-card__body) {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
}
:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>
