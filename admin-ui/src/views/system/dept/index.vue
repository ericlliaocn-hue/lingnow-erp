<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="部门名称" prop="deptName">
          <el-input
            v-model="queryParams.deptName"
            placeholder="请输入部门名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="部门状态" clearable style="width: 120px">
            <el-option
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
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
          <el-button type="info" plain :icon="Sort" @click="toggleExpandAll">展开/折叠</el-button>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table
          v-if="refreshTable"
          v-loading="loading"
          :data="deptList"
          row-key="deptId"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          border
          height="100%"
        >
          <el-table-column prop="deptName" label="部门名称" min-width="160">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.deptName)" title="点击复制">{{ row.deptName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deptId" label="部门ID" min-width="100" align="center">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.deptId)" title="点击复制">{{ row.deptId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderNum" label="排序" min-width="80" align="center" />
          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template #default="scope">
              <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                @change="handleStatusChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" min-width="200">
            <template #default="scope">
              <span>{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button type="success" :icon="Edit" size="small" @click="handleUpdate(scope.row)" />
              <el-button type="primary" :icon="Plus" size="small" @click="handleAdd(scope.row)" />
              <el-button type="danger" :icon="Delete" size="small" v-if="scope.row.parentId !== '0'" @click="handleDelete(scope.row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 添加或修改部门对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="deptFormRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="24" v-if="form.parentId !== '0'">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                v-model="form.parentId"
                :data="deptOptions"
                :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
                value-key="deptId"
                placeholder="选择上级部门"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :value="Number(dict.value)"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh, Sort } from '@element-plus/icons-vue'
import { listDept, getDept, delDept, addDept, updateDept } from '@/api/system/dept'
import type { DeptVO, DeptForm, DeptQuery } from '@/api/system/dept'
import { handleTree } from '@/utils/tree'
import { useDict } from '@/hooks/web/useDict'
import { useClipboard } from '@vueuse/core'

const { sys_normal_disable } = useDict('sys_normal_disable')
const { copy } = useClipboard()

const queryFormRef = ref()
const deptFormRef = ref()
const loading = ref(true)
const deptList = ref<DeptVO[]>([])
const deptOptions = ref<DeptVO[]>([])
const open = ref(false)
const title = ref('')
const isExpandAll = ref(true)
const refreshTable = ref(true)

const data = reactive({
  form: {} as DeptForm,
  queryParams: {
    deptName: undefined,
    status: undefined
  } as DeptQuery,
  rules: {
    parentId: [{ required: true, message: '上级部门不能为空', trigger: 'blur' }],
    deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
    orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询部门列表 */
function getList() {
  loading.value = true
  listDept(queryParams.value).then(response => {
    // 处理树形结构
    deptList.value = handleTree(response, 'deptId')
    loading.value = false
  })
}

/** 部门状态修改 */
function handleStatusChange(row: DeptVO) {
  let text = row.status === 1 ? "启用" : "停用";
  ElMessageBox.confirm('确认要"' + text + '""' + row.deptName + '"部门吗?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(function() {
    return updateDept(row as unknown as DeptForm);
  }).then(() => {
    ElMessage.success(text + "成功");
  }).catch(function() {
    row.status = row.status === 1 ? 0 : 1;
  });
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
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
    deptId: undefined,
    parentId: undefined,
    deptName: '',
    orderNum: 0,
    leader: undefined,
    phone: undefined,
    email: undefined,
    status: 1
  }
  if (deptFormRef.value) {
    deptFormRef.value.resetFields()
  }
}

/** 搜索按钮操作 */
function handleQuery() {
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
function handleAdd(row?: DeptVO) {
  reset()
  listDept().then(response => {
    deptOptions.value = handleTree(response, 'deptId')
  })
  if (row != null && row.deptId) {
    form.value.parentId = row.deptId
  } else {
    form.value.parentId = '0'
  }
  open.value = true
  title.value = '添加部门'
}

/** 修改按钮操作 */
function handleUpdate(row: DeptVO) {
  reset()
  listDept().then(response => {
    deptOptions.value = handleTree(response, 'deptId')
  })
  getDept(row.deptId).then(response => {
    form.value = response
    open.value = true
    title.value = '修改部门'
  })
}

/** 提交按钮 */
function submitForm() {
  deptFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.deptId) {
        updateDept(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDept(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: DeptVO) {
  ElMessageBox.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delDept(row.deptId)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  getList()
})

const handleCopy = (text: string | number | undefined) => {
  if (!text) return
  copy(String(text))
  ElMessage.success('复制成功')
}
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
:deep(.el-table__expand-icon) {
  margin-right: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.copy-text {
  cursor: pointer;
  transition: color 0.2s;
}
.copy-text:hover {
  color: var(--el-color-primary);
}
</style>
