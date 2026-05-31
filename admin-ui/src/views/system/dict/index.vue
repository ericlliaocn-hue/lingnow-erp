<template>
  <div class="app-container">
    <el-row :gutter="20" class="h-full">
      <!-- 左侧字典类型列表 -->
      <el-col :span="10" class="h-full">
        <el-card shadow="never" class="h-full flex-card">
          <template #header>
            <div class="card-header">
              <span>字典列表</span>
            </div>
          </template>
          
          <el-form :inline="true" :model="queryParams" ref="queryFormRef" class="mb-2">
            <el-form-item prop="dictName" style="margin-bottom: 10px; margin-right: 10px;">
              <el-input 
                v-model="queryParams.dictName" 
                placeholder="字典名称" 
                clearable 
                style="width: 200px" 
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item prop="dictType" style="margin-bottom: 10px; margin-right: 10px;">
              <el-input 
                v-model="queryParams.dictType" 
                placeholder="字典类型" 
                clearable 
                style="width: 200px" 
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item style="margin-bottom: 10px; margin-right: 0;">
              <el-button type="primary" :icon="Search" circle @click="handleQuery" />
              <el-button :icon="Refresh" circle @click="resetQuery" />
            </el-form-item>
          </el-form>

          <div class="mb-4">
             <el-button type="primary" :icon="Plus" plain @click="handleAdd" size="small">新增类型</el-button>
             <el-button type="danger" :icon="Refresh" plain @click="handleRefreshCache" size="small">刷新缓存</el-button>
          </div>

          <el-table
            ref="typeTableRef"
            v-loading="loading"
            :data="typeList"
            highlight-current-row
            @current-change="handleCurrentChange"
            border
            height="100%"
            style="width: 100%; flex: 1; overflow: auto;"
          >
            <el-table-column label="字典名称" align="center" prop="dictName" show-overflow-tooltip />
            <el-table-column label="字典类型" align="center" prop="dictType" show-overflow-tooltip />
            <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button link type="primary" :icon="Edit" @click.stop="handleUpdate(scope.row)" />
                <el-button link type="primary" :icon="Delete" @click.stop="handleDelete(scope.row)" />
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
              layout="prev, pager, next"
              small
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧字典数据列表 -->
      <el-col :span="14" class="h-full">
        <el-card shadow="never" class="h-full flex-card">
          <template #header>
            <div class="card-header">
              <span>字典数据</span>
              <span v-if="currentDictType" style="font-size: 16px; font-weight: bold; color: #409EFF; margin-left: 10px;">
                当前选中: {{ currentDictType }}
              </span>
            </div>
          </template>
          
          <dict-data 
            v-if="currentDictType" 
            :dict-type="currentDictType" 
          />
          <el-empty v-else description="请点击左侧字典列表选择" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加或修改字典类型对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="typeFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" placeholder="请输入字典类型" />
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
import { ref, reactive, toRefs, onMounted, nextTick } from 'vue'
import { useDict } from '@/hooks/web/useDict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listDictType, getDictType, delDictType, addDictType, updateDictType, refreshDictCache } from '@/api/system/dict'
import type { DictTypeVO, DictTypeForm, DictTypeQuery } from '@/api/system/dict'
import Pagination from '@/components/Pagination/index.vue'
import DictData from './data.vue'

const { sys_normal_disable } = useDict('sys_normal_disable')

const queryFormRef = ref()
const typeFormRef = ref()
const typeTableRef = ref()
const loading = ref(true)
const total = ref(0)
const typeList = ref<DictTypeVO[]>([])
const open = ref(false)
const title = ref('')
const currentDictType = ref('')

const data = reactive({
  form: {} as DictTypeForm,
  queryParams: {
    current: 1,
    size: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  } as DictTypeQuery,
  rules: {
    dictName: [{ required: true, message: '字典名称不能为空', trigger: 'blur' }],
    dictType: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询字典类型列表 */
function getList() {
  loading.value = true
  listDictType(queryParams.value).then(response => {
    typeList.value = response.records
    total.value = Number(response.total)
    loading.value = false
    
    // 默认选中第一条
    if (typeList.value.length > 0) {
      nextTick(() => {
        typeTableRef.value?.setCurrentRow(typeList.value[0])
        handleCurrentChange(typeList.value[0])
      })
    } else {
      currentDictType.value = ''
    }
  })
}

/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshDictCache().then(() => {
    ElMessage.success('刷新成功')
  })
}

/** 选中字典类型 */
function handleCurrentChange(val: DictTypeVO | undefined) {
  if (val) {
    currentDictType.value = val.dictType
  }
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    dictId: undefined,
    dictName: '',
    dictType: '',
    status: 1,
    remark: undefined
  }
  if (typeFormRef.value) {
    typeFormRef.value.resetFields()
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
  title.value = '添加字典类型'
}

/** 修改按钮操作 */
function handleUpdate(row: DictTypeVO) {
  reset()
  const dictId = row.dictId
  if (dictId) {
    getDictType(dictId).then(response => {
      form.value = response
      open.value = true
      title.value = '修改字典类型'
    })
  }
}

/** 提交按钮 */
function submitForm() {
  typeFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.dictId) {
        updateDictType(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDictType(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: DictTypeVO) {
  const dictIds = row.dictId
  ElMessageBox.confirm('是否确认删除字典编号为"' + dictIds + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delDictType(dictIds!)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
    if (currentDictType.value === row.dictType) {
        currentDictType.value = ''
    }
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.h-full {
    height: 100%;
}
.mb-2 {
    margin-bottom: 8px;
}
.mb-4 {
    margin-bottom: 16px;
}
.flex-card {
    display: flex;
    flex-direction: column;
}
.flex-card :deep(.el-card__body) {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
}
:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>
