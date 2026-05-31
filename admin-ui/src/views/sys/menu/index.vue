<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="模糊搜索">
          <el-input v-model="queryParams.keyword" placeholder="请输入模糊搜索" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
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
          <div>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
            <el-button type="info" plain @click="toggleExpandAll">
              <el-icon><Sort /></el-icon>
              展开/折叠
            </el-button>
          </div>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table
          v-if="refreshTable"
          :data="menuList"
          row-key="menuId"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children' }"
          border
          style="width: 100%"
          height="100%"
        >
          <el-table-column prop="menuId" label="菜单ID" width="200" show-overflow-tooltip align="center">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.menuId)" title="点击复制">{{ row.menuId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="menuName" label="菜单名称" width="200" show-overflow-tooltip align="center">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.menuName)" title="点击复制">{{ row.menuName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column prop="permission" label="权限标识" show-overflow-tooltip align="center">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.permission)" title="点击复制">{{ row.permission }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="component" label="组件路径" show-overflow-tooltip align="center">
            <template #default="{ row }">
               <span class="copy-text" @click.stop="handleCopy(row.component)" title="点击复制">{{ row.component }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="isCache" label="缓存" width="80" align="center">
            <template #default="{ row }">
              <template v-if="row.menuType === 1">
                <template v-for="(dict, index) in sys_yes_no" :key="index">
                  <el-tag v-if="dict.value == row.isCache" :type="dict.elTagType">{{ dict.label }}</el-tag>
                </template>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="visible" label="显示设置" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.visible"
                :active-value="1"
                :inactive-value="0"
                @change="handleVisibleChange(row)"
              />
            </template>
          </el-table-column>
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
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(row)" />
              </el-tooltip>
              <el-tooltip content="新增子菜单" placement="top">
                <el-button link type="primary" :icon="Plus" @click="handleAddChild(row)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio v-for="dict in sys_menu_type" :key="dict.value" :label="Number(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="父级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', value: 'id' }"
            placeholder="请选择父级菜单"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 User" />
        </el-form-item>
        <el-form-item label="路由地址" prop="path" v-if="form.menuType !== 2">
          <el-input v-model="form.path" placeholder="请输入路由地址" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component" v-if="form.menuType === 1">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="显示设置" prop="visible">
           <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
           <span style="margin-left: 10px; color: #909399; font-size: 12px">是否在菜单栏显示</span>
        </el-form-item>
        <el-form-item label="是否缓存" prop="isCache" v-if="form.menuType === 1">
           <el-radio-group v-model="form.isCache">
             <el-radio v-for="dict in sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
           </el-radio-group>
           <span style="margin-left: 10px; color: #909399; font-size: 12px">切换页面时是否缓存状态</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Sort, Search, Refresh } from '@element-plus/icons-vue'
import { getAllMenuTree, createMenu, updateMenu, deleteMenu, type MenuItem } from '@/api/sys/menu.ts'
import { useDict } from '@/hooks/web/useDict'
import { useClipboard } from '@vueuse/core'

const { sys_normal_disable, sys_yes_no, sys_menu_type } = useDict('sys_normal_disable', 'sys_yes_no', 'sys_menu_type')
const { copy } = useClipboard()

const menuList = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const formRef = ref()
const refreshTable = ref(true)
const isExpandAll = ref(false)

const queryParams = ref({
  keyword: '',
  status: undefined as number | undefined
})

const form = ref<Partial<MenuItem>>({
  parentId: 0,
  menuName: '',
  menuType: 1,
  icon: '',
  path: '',
  component: '',
  permission: '',
  sortOrder: 0,
  visible: 1,
  isCache: 'N',
  status: 1,
  remark: ''
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// 菜单树选项（用于选择父级菜单）
const menuTreeOptions = computed(() => {
  return [{ id: 0, menuName: '顶级菜单', children: menuList.value }]
})

// 加载菜单列表
const loadMenuList = async () => {
  try {
    const res = (await getAllMenuTree(queryParams.value)) as unknown as MenuItem[]
    menuList.value = res || []
  } catch (error) {
    console.error('加载菜单列表失败:', error)
  }
}

// 搜索
const handleQuery = () => {
  loadMenuList()
}

// 重置
const resetQuery = () => {
  queryParams.value.keyword = ''
  queryParams.value.status = undefined
  handleQuery()
}

// 展开/折叠
const toggleExpandAll = () => {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

// 显示状态切换
const handleVisibleChange = async (row: MenuItem) => {
  try {
    await updateMenu(row)
    ElMessage.success('更新显示状态成功')
  } catch (error) {
    // 恢复状态
    row.visible = row.visible === 1 ? 0 : 1
    console.error(error)
  }
}

// 状态切换确认
const confirmStatusChange = (row: MenuItem): Promise<boolean> => {
  return new Promise((resolve, reject) => {
    const text = row.status === 1 ? '禁用' : '启用'
    ElMessageBox.confirm(`确认要${text}该菜单吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true
    }).then(async () => {
      try {
        const newStatus = row.status === 1 ? 0 : 1
        // 这里需要注意：switch 的 before-change 如果返回 true，会自动切换 UI 状态
        // 我们需要先调用 API，成功后再允许切换
        // 或者我们手动调用 API 更新，然后返回 true
        // 由于 updateMenu 接口需要完整的对象，我们复制一份并修改 status
        await updateMenu({ ...row, status: newStatus })
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

// 新增菜单
const handleAdd = () => {
  dialogTitle.value = '新增菜单'
  form.value = {
    parentId: 0,
    menuName: '',
    menuType: 1,
    icon: '',
    path: '',
    component: '',
    permission: '',
    sortOrder: 0,
    visible: 1,
    status: 1,
    remark: ''
  }
  dialogVisible.value = true
}

// 新增子菜单
const handleAddChild = (row: MenuItem) => {
  dialogTitle.value = '新增子菜单'
  form.value = {
    parentId: row.menuId,
    menuName: '',
    menuType: 1,
    icon: '',
    path: '',
    component: '',
    permission: '',
    sortOrder: 0,
    visible: 1,
    status: 1,
    remark: ''
  }
  dialogVisible.value = true
}

// 编辑菜单
const handleEdit = (row: MenuItem) => {
  dialogTitle.value = '编辑菜单'
  form.value = { ...row }
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = () => {
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (form.value.menuId) {
          await updateMenu(form.value as MenuItem)
          ElMessage.success('更新成功')
        } else {
          await createMenu(form.value as MenuItem)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadMenuList()
      } catch (error) {
        console.error('保存菜单失败:', error)
      }
    }
  })
}

// 删除菜单
const handleDelete = (row: MenuItem) => {
  ElMessageBox.confirm('确定要删除该菜单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMenu(row.menuId)
      ElMessage.success('删除成功')
      loadMenuList()
    } catch (error) {
      console.error('删除菜单失败:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  loadMenuList()
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
