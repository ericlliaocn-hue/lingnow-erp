<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="参数名称" prop="configName">
          <el-input
            v-model="queryParams.configName"
            placeholder="请输入参数名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input
            v-model="queryParams.configKey"
            placeholder="请输入参数键名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="系统内置" prop="configType">
          <el-select v-model="queryParams.configType" placeholder="系统内置" clearable style="width: 120px">
            <el-option
              v-for="dict in sys_yes_no"
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
          <el-button type="primary" plain @click="handleAdd" v-permission="'system:config:add'">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="danger" plain :disabled="ids.length === 0" @click="handleDelete" v-permission="'system:config:remove'">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
          <el-button type="warning" plain @click="handleRefreshCache" v-permission="'system:config:remove'">
            <el-icon><Refresh /></el-icon>
            刷新缓存
          </el-button>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table :data="configList" border style="width: 100%" height="100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="参数主键" align="center" prop="configId" />
          <el-table-column label="参数名称" align="center" prop="configName" :show-overflow-tooltip="true" />
          <el-table-column label="参数键名" align="center" prop="configKey" :show-overflow-tooltip="true" />
          <el-table-column label="参数键值" align="center" prop="configValue" />
          <el-table-column label="系统内置" align="center" prop="configType">
            <template #default="{ row }">
               <template v-for="(dict, index) in sys_yes_no" :key="index">
                  <el-tag v-if="dict.value == row.configType" :type="dict.elTagType">{{ dict.label }}</el-tag>
               </template>
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" :icon="Edit" @click="handleUpdate(scope.row)" v-permission="'system:config:edit'">修改</el-button>
              <el-button link type="primary" :icon="Delete" @click="handleDelete(scope.row)" v-permission="'system:config:remove'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-card>

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="configFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="系统内置" prop="configType">
          <el-radio-group v-model="form.configType">
            <el-radio v-for="dict in sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
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
import { ref, reactive, toRefs, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from '@/api/system/config'
import type { ConfigVO, ConfigForm, ConfigQuery } from '@/api/system/config'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_yes_no } = useDict('sys_yes_no')

const queryFormRef = ref()
const configFormRef = ref()
const loading = ref(true)
const total = ref(0)
const configList = ref<ConfigVO[]>([])
const open = ref(false)
const title = ref('')
const ids = ref<string[]>([])

const data = reactive({
  form: {} as ConfigForm,
  queryParams: {
    current: 1,
    size: 10,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  } as ConfigQuery,
  rules: {
    configName: [{ required: true, message: '参数名称不能为空', trigger: 'blur' }],
    configKey: [{ required: true, message: '参数键名不能为空', trigger: 'blur' }],
    configValue: [{ required: true, message: '参数键值不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询参数列表 */
function getList() {
  loading.value = true
  listConfig(queryParams.value).then((response: any) => {
    configList.value = response.records
    total.value = response.total
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
    configId: undefined,
    configName: '',
    configKey: '',
    configValue: '',
    configType: 'Y',
    remark: ''
  }
  if (configFormRef.value) {
    configFormRef.value.resetFields()
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

/** 多选框选中数据 */
function handleSelectionChange(selection: ConfigVO[]) {
  ids.value = selection.map(item => item.configId!)
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加参数'
}

/** 修改按钮操作 */
function handleUpdate(row: ConfigVO) {
  reset()
  const configId = row.configId || ids.value[0] || ''
  getConfig(configId).then(response => {
    form.value = response
    open.value = true
    title.value = '修改参数'
  })
}

/** 提交按钮 */
function submitForm() {
  configFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.configId) {
        updateConfig(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addConfig(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: ConfigVO) {
  const configIds = row.configId || ids.value.join(',')
  ElMessageBox.confirm('是否确认删除参数编号为"' + configIds + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delConfig(configIds)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    ElMessage.success('刷新成功')
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
  padding-right: 20px;
}
.search-wrapper {
  margin-bottom: 0;
  flex-shrink: 0;
}
.table-wrapper {
  margin-top: 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 4px;
}
:deep(.el-table__cell) {
  padding: 20px 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
