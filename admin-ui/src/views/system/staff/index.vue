<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 部门树 -->
      <el-col :span="4" :xs="24">
        <el-card shadow="never" class="box-card">
          <div class="head-container">
            <el-input
              v-model="deptName"
              placeholder="请输入部门名称"
              clearable
              prefix-icon="Search"
              style="margin-bottom: 20px"
            />
          </div>
          <div class="head-container">
            <el-tree
              ref="deptTreeRef"
              :data="deptOptions"
              :props="{ label: 'deptName', children: 'children' }"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              node-key="deptId"
              default-expand-all
              highlight-current
              @node-click="handleNodeClick"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 用户数据 -->
      <el-col :span="20" :xs="24">
        <el-card shadow="never" class="search-wrapper">
          <el-form :inline="true" :model="queryParams" ref="queryFormRef">
            <el-form-item label="用户名称" prop="username">
              <el-input
                v-model="queryParams.username"
                placeholder="请输入用户名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="手机号码" prop="phone">
              <el-input
                v-model="queryParams.phone"
                placeholder="请输入手机号码"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 120px">
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
              <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
            </div>
          </template>

          <el-table v-loading="loading" :data="staffList" @selection-change="handleSelectionChange" border height="100%">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column label="用户ID" align="center" prop="userId" width="100" />
            <el-table-column label="用户名称" align="center" prop="username" show-overflow-tooltip />
            <el-table-column label="用户昵称" align="center" prop="nickname" show-overflow-tooltip />
            <el-table-column label="部门" align="center" prop="dept.deptName" show-overflow-tooltip />
            <el-table-column label="手机号码" align="center" prop="phone" width="120" />
            <el-table-column label="状态" align="center" prop="status">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleStatusChange(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" width="160">
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

          <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.current"
            v-model:limit="queryParams.size"
            @pagination="getList"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="staffFormRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
                value-key="deptId"
                placeholder="请选择归属部门"
                check-strictly
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名称" maxlength="30" :disabled="!!form.userId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户密码" prop="password" v-if="!form.userId">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.gender" placeholder="请选择">
                <el-option
                  v-for="dict in sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="Number(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
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
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status === 0"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status === 0"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
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
import { ref, reactive, toRefs, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listStaff, getStaff, delStaff, addStaff, updateStaff, updateStaffStatus } from '@/api/system/staff'
import { listDept } from '@/api/system/dept'
import { listPost } from '@/api/system/post'
import { getActiveRoles } from '@/api/sys/role'
import type { StaffVO, StaffForm, StaffQuery } from '@/api/system/staff'
import type { DeptVO } from '@/api/system/dept'
import type { PostVO } from '@/api/system/post'
import type { Role } from '@/api/sys/role'
import { handleTree } from '@/utils/tree'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_normal_disable, sys_user_sex } = useDict('sys_normal_disable', 'sys_user_sex')

const deptTreeRef = ref()
const queryFormRef = ref()
const staffFormRef = ref()
const loading = ref(true)
const total = ref(0)
const staffList = ref<StaffVO[]>([])
const deptOptions = ref<DeptVO[]>([])
const postOptions = ref<PostVO[]>([])
const roleOptions = ref<Role[]>([])
const ids = ref<string[]>([])
const multiple = ref(true)
const open = ref(false)
const title = ref('')
const deptName = ref('')

const data = reactive({
  form: {} as StaffForm,
  queryParams: {
    current: 1,
    size: 10,
    username: undefined,
    phone: undefined,
    status: undefined,
    deptId: undefined
  } as StaffQuery,
  rules: {
    username: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
    nickname: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
    password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

watch(deptName, (val) => {
  if (deptTreeRef.value) {
    deptTreeRef.value.filter(val)
  }
})

/** 查询用户列表 */
function getList() {
  loading.value = true
  listStaff(queryParams.value).then(response => {
    staffList.value = response.records
    total.value = Number(response.total)
    loading.value = false
  })
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  listDept().then(response => {
    deptOptions.value = handleTree(response, 'deptId')
  })
}

/** 筛选节点 */
function filterNode(value: string, data: DeptVO) {
  if (!value) return true
  return data.deptName.includes(value)
}

/** 节点单击事件 */
function handleNodeClick(data: DeptVO) {
  queryParams.value.deptId = data.deptId
  handleQuery()
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
  queryParams.value.deptId = undefined
  if (deptTreeRef.value) {
    deptTreeRef.value.setCurrentKey(null)
  }
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: StaffVO[]) {
  ids.value = selection.map(item => item.userId)
  multiple.value = !selection.length
}

/** 状态修改 */
function handleStatusChange(row: StaffVO) {
  let text = row.status === 1 ? '启用' : '停用'
  ElMessageBox.confirm('确认要"' + text + '""' + row.username + '"用户吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return updateStaffStatus(row.userId, row.status)
  }).then(() => {
    ElMessage.success(text + '成功')
  }).catch(function() {
    row.status = row.status === 1 ? 0 : 1
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
    userId: undefined,
    deptId: undefined,
    username: '',
    nickname: '',
    password: '',
    email: undefined,
    phone: undefined,
    gender: 1,
    status: 1,
    remark: undefined,
    postIds: [],
    roleIds: []
  }
  if (staffFormRef.value) {
    staffFormRef.value.resetFields()
  }
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  getDeptTree()
  // 加载岗位和角色
  listPost({ current: 1, size: 999 }).then(res => postOptions.value = res.records)
  getActiveRoles().then((res: any) => roleOptions.value = res) // res might be Result<Role[]> or Role[] depending on role.ts

  open.value = true
  title.value = '添加用户'
}

/** 修改按钮操作 */
function handleUpdate(row: StaffVO) {
  reset()
  getDeptTree()
  const userId = row.userId || ids.value[0] || ''
  
  // 加载岗位和角色
  listPost({ current: 1, size: 999 }).then(res => postOptions.value = res.records)
  getActiveRoles().then((res: any) => roleOptions.value = res)

  getStaff(userId).then(response => {
    form.value = response as any // casting to any because form has password but response doesn't
    form.value.password = '' // don't show password
    // Ensure postIds/roleIds are arrays
    if (!form.value.postIds) form.value.postIds = []
    if (!form.value.roleIds) form.value.roleIds = []
    
    open.value = true
    title.value = '修改用户'
  })
}

/** 提交按钮 */
function submitForm() {
  staffFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.userId) {
        updateStaff(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addStaff(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  const userIds = row?.userId ? [row.userId] : ids.value
  ElMessageBox.confirm('是否确认删除用户编号为"' + userIds + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delStaff(userIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  getList()
  getDeptTree()
})
</script>

<style scoped>
.app-container {
  height: 100%;
  padding: 20px;
}
.el-row {
  height: 100%;
}
.el-col {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.box-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 4px;
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
:deep(.el-tree-node__content) {
  height: 46px;
}
:deep(.el-tree) {
  font-size: 14px;
}
:deep(.el-table__cell) {
  padding: 20px 0;
}
.head-container {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
