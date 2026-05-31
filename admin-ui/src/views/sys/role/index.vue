<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
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
          <el-button type="primary" @click="handleAdd" v-permission="'sys:role:add'">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table :data="roleList" border style="width: 100%" height="100%">
          <el-table-column prop="roleId" label="角色ID" align="center">
            <template #default="{ row }">
              <span class="copy-text" @click="handleCopy(row.roleId)" title="点击复制">{{ row.roleId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="roleName" label="角色名称" align="center">
            <template #default="{ row }">
              <span class="copy-text" @click="handleCopy(row.roleName)" title="点击复制">{{ row.roleName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="roleKey" label="权限字符" align="center">
            <template #default="{ row }">
              <span class="copy-text" @click="handleCopy(row.roleKey)" title="点击复制">{{ row.roleKey }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :before-change="() => confirmStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(row)" v-permission="'sys:role:edit'" />
              </el-tooltip>
              <el-tooltip content="数据权限" placement="top">
                <el-button link type="primary" :icon="Setting" @click="handleDataScope(row)" v-permission="'sys:role:edit'" />
              </el-tooltip>
              <el-tooltip content="分配用户" placement="top">
                <el-button link type="primary" :icon="UserIcon" @click="handleAuthUser(row)" v-permission="'sys:role:edit'" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" v-permission="'sys:role:remove'" />
              </el-tooltip>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1500px" top="5vh">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
        </el-form-item>
        <el-form-item label="角色顺序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand">展开/折叠</el-checkbox>
          <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll">全选/全不选</el-checkbox>
          <el-checkbox v-model="menuCheckStrictly" @change="handleCheckedTreeConnect">父子联动</el-checkbox>
          <el-tree
            class="tree-border"
            :data="menuOptions"
            show-checkbox
            ref="menuRef"
            node-key="menuId"
            :check-strictly="!menuCheckStrictly"
            empty-text="加载中，请稍后"
            :props="{ label: 'menuName', children: 'children' }"
          ></el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配数据权限对话框 -->
    <el-dialog :title="dataScopeTitle" v-model="dataScopeVisible" width="500px">
      <el-form :model="dataScopeForm" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="dataScopeForm.roleName" :disabled="true" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="dataScopeForm.roleKey" :disabled="true" />
        </el-form-item>
        <el-form-item label="权限范围">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%">
            <el-option
              v-for="dict in sys_data_scope"
              :key="dict.value"
              :label="dict.label"
              :value="Number(dict.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据权限" v-show="dataScopeForm.dataScope === 2">
          <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpandDept">展开/折叠</el-checkbox>
          <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAllDept">全选/全不选</el-checkbox>
          <el-checkbox v-model="deptCheckStrictly" @change="handleCheckedTreeConnectDept">父子联动</el-checkbox>
          <el-tree
            class="tree-border"
            :data="deptOptions"
            show-checkbox
            default-expand-all
            ref="deptRef"
            node-key="deptId"
            :check-strictly="!deptCheckStrictly"
            empty-text="加载中，请稍候"
            :props="{ label: 'deptName', children: 'children' }"
          ></el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitDataScope">确 定</el-button>
          <el-button @click="dataScopeVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分配用户组件 -->
    <auth-user ref="authUserRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Plus, Edit, Delete, Setting, Search, Refresh, User as UserIcon } from '@element-plus/icons-vue'
import { getRoleList, addRole, updateRole, deleteRole, getRoleDetail, dataScope, type Role } from '@/api/sys/role.ts'
import { getMenuTree } from '@/api/sys/menu.ts'
import { listDept } from '@/api/system/dept.ts'
import { handleTree } from '@/utils/tree'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import AuthUser from './components/AuthUser.vue'
import { useDict } from '@/hooks/web/useDict'
import { useClipboard } from '@vueuse/core'

const { sys_normal_disable, sys_data_scope } = useDict('sys_normal_disable', 'sys_data_scope')
const { copy } = useClipboard()

const roleList = ref<Role[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const queryParams = ref({
  current: 1,
  size: 10,
  roleName: '',
  status: undefined as string | undefined
})

const form = ref<any>({
  roleName: '',
  roleKey: '',
  sortOrder: 0,
  status: '1',
  remark: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入角色顺序', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const menuRef = ref()
const menuOptions = ref<any[]>([])
const menuExpand = ref(false)
const menuNodeAll = ref(false)
const menuCheckStrictly = ref(true)
const deptOptions = ref<any[]>([])
const deptExpand = ref(true)
const deptNodeAll = ref(false)
const deptCheckStrictly = ref(true)
const deptRef = ref<InstanceType<typeof ElTree>>()

const getMenuTreeselect = async () => {
  const res: any = await getMenuTree()
  menuOptions.value = res
}

const handleCheckedTreeExpand = (value: any) => {
  let treeList = menuOptions.value
  for (let i = 0; i < treeList.length; i++) {
    menuRef.value.store.nodesMap[treeList[i].menuId].expanded = value
  }
}

const handleCheckedTreeNodeAll = (value: any) => {
  menuRef.value.setCheckedNodes(value ? menuOptions.value : [])
}

const handleCheckedTreeConnect = (value: any) => {
  menuCheckStrictly.value = value ? true : false
}

const getMenuAllCheckedKeys = () => {
  // 目前被选中的菜单节点
  let checkedKeys = menuRef.value.getCheckedKeys()
  // 半选中的菜单节点
  let halfCheckedKeys = menuRef.value.getHalfCheckedKeys()
  // 使用展开运算符而非 apply，更加安全且符合 modern JS
  checkedKeys.push(...halfCheckedKeys)
  return checkedKeys
}

const getList = async () => {
  try {
    const res: any = await getRoleList(queryParams.value)
    roleList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error(error)
  }
}

const handleQuery = () => {
  queryParams.value.current = 1
  getList()
}

const resetQuery = () => {
  queryParams.value.roleName = ''
  queryParams.value.status = undefined
  handleQuery()
}

const handleAdd = async () => {
  dialogTitle.value = '新增角色'
  form.value = {
    roleName: '',
    roleKey: '',
    sortOrder: 0,
    status: 1,
    remark: '',
    menuIds: []
  }
  dialogVisible.value = true
  // 确保弹窗打开后再获取数据，或者并行处理但要确保ref存在
  await getMenuTreeselect()
}

const handleEdit = async (row: Role) => {
  dialogTitle.value = '编辑角色'
  const roleId = row.roleId
  dialogVisible.value = true

  // 先加载菜单树
  await getMenuTreeselect()

  // 获取角色详情
  getRoleDetail(roleId).then((response: any) => {
    form.value = { ...response, status: String(response.status) }
    // 确保DOM更新后设置选中状态
    nextTick(() => {
        if (response.menuIds && menuRef.value) {
            // 清空当前选中状态
            menuRef.value.setCheckedKeys([])
            response.menuIds.forEach((v: any) => {
                 // 只有当节点是叶子节点时才选中，避免父节点被选中导致所有子节点被选中
                 const node = menuRef.value.getNode(v)
                 if (node && node.isLeaf) {
                    menuRef.value.setChecked(v, true, false)
                 }
            })
        }
    })
  })
}

const dataScopeVisible = ref(false)
const dataScopeTitle = ref('分配数据权限')
const dataScopeForm = ref({
  roleId: '',
  roleName: '',
  roleKey: '',
  dataScope: 1
})
const authUserRef = ref()

const handleDataScope = async (row: Role) => {
  const roleId = row.roleId
  const response: any = await getRoleDetail(roleId)
  dataScopeForm.value.roleId = response.roleId
  dataScopeForm.value.roleName = response.roleName
  dataScopeForm.value.roleKey = response.roleKey
  dataScopeForm.value.dataScope = response.dataScope
  
  // 获取部门树
  const deptResponse: any = await listDept({})
  deptOptions.value = handleTree(deptResponse, 'deptId')
  
  dataScopeVisible.value = true
  
  nextTick(() => {
    if (deptRef.value) {
      deptRef.value.setCheckedKeys(response.deptIds || [])
    }
  })
}

const submitDataScope = async () => {
  if (dataScopeForm.value.roleId) {
    (dataScopeForm.value as any).deptIds = getDeptAllCheckedKeys()
    await dataScope(dataScopeForm.value)
    ElMessage.success('分配数据权限成功')
    dataScopeVisible.value = false
    getList()
  }
}

/** 所有部门节点数据 */
function getDeptAllCheckedKeys() {
  if (!deptRef.value) return []
  // 目前被选中的部门节点
  const checkedKeys = deptRef.value.getCheckedKeys()
  // 半选中的部门节点
  const halfCheckedKeys = deptRef.value.getHalfCheckedKeys()
  checkedKeys.push(...halfCheckedKeys)
  return checkedKeys
}

/** 树权限（展开/折叠）*/
function handleCheckedTreeExpandDept(value: any) {
  if (!deptRef.value) return
  const treeList = deptOptions.value
  for (let i = 0; i < treeList.length; i++) {
    (deptRef.value as any).store.nodesMap[treeList[i].deptId].expanded = value
  }
}

/** 树权限（全选/全不选）*/
function handleCheckedTreeNodeAllDept(value: any) {
  deptRef.value!.setCheckedNodes(value ? deptOptions.value : [])
}

/** 树权限（父子联动）*/
function handleCheckedTreeConnectDept(value: any) {
  deptCheckStrictly.value = value ? true : false
}

const handleAuthUser = (row: Role) => {
  authUserRef.value.show(row.roleId)
}

const confirmStatusChange = (row: Role): Promise<boolean> => {
  return new Promise((resolve, reject) => {
    const text = row.status === 1 ? '停用' : '启用'
    ElMessageBox.confirm(`确认要${text}该角色吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true
    }).then(async () => {
      try {
        const newStatus = row.status === 1 ? 0 : 1
        await updateRole({ ...row, status: newStatus })
        ElMessage.success('操作成功')
        resolve(true)
      } catch (error) {
        console.error(error)
        reject()
      }
    }).catch(() => {
      reject()
    })
  })
}

const handleDelete = (row: Role) => {
  ElMessageBox.confirm('是否确认删除该角色？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteRole(row.roleId)
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

const submitForm = () => {
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        form.value.menuIds = getMenuAllCheckedKeys()
        if (form.value.roleId) {
          await updateRole(form.value as Role)
          ElMessage.success('修改成功')
        } else {
          await addRole(form.value)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        getList()
      } catch (error) {
        console.error('提交失败', error)
        ElMessage.error('提交失败，请重试')
      }
    }
  })
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
.copy-text {
  cursor: pointer;
  transition: color 0.2s;
}
.copy-text:hover {
  color: var(--el-color-primary);
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tree-border {
  margin-top: 5px;
  border: 1px solid #e5e6e7;
  background: transparent;
  border-radius: 4px;
  width: 100%;
  min-height: 400px;
}

:deep(.el-tree) {
  background: transparent;
}

/* 确保树形控件复选框使用主题色 - 强制覆盖 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner),
:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: var(--el-color-primary) !important;
  border-color: var(--el-color-primary) !important;
}
:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--el-color-primary) !important;
}
</style>
