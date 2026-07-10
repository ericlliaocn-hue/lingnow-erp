<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 120px">
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
      <el-alert v-if="userList.length === 0 && !loading" type="info" :closable="false" style="margin-top: 10px">
        <template #title>
          暂无用户数据
        </template>
      </el-alert>
    </el-card>

    <el-card shadow="never" class="table-wrapper">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      </div>
      <div style="flex: 1; overflow: hidden;">
        <el-table v-loading="loading" :data="userList" border style="width: 100%" height="100%">
          <el-table-column prop="userId" label="用户ID" width="160" align="center">
            <template #default="{ row }">
               <span class="copy-text" @click="handleCopy(row.userId)" title="点击复制">{{ row.userId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户信息" min-width="200" align="left">
            <template #default="{ row }">
              <div style="display: flex; align-items: center;">
                <el-avatar :size="40" :src="row.avatar" style="margin-right: 12px; flex-shrink: 0;">
                  {{ (row.nickname || row.username || '').charAt(0).toUpperCase() }}
                </el-avatar>
                <div style="display: flex; flex-direction: column; line-height: 1.4;">
                  <span class="copy-text" @click="handleCopy(row.nickname || row.username)" title="点击复制昵称" style="font-weight: 500; font-size: 14px;">{{ row.nickname || row.username }}</span>
                  <span class="copy-text" @click="handleCopy(row.phone)" title="点击复制手机号" style="font-size: 12px; color: #909399;">{{ row.phone || '-' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template #default="{ row }">
               <el-switch
                 v-model="row.status"
                 :active-value="1"
                 :inactive-value="0"
                 :before-change="() => confirmStatusChange(row)"
               />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" min-width="180" align="center" />
          <el-table-column label="操作" fixed="right" width="280" align="center">
             <template #default="scope">
                <template v-if="scope.row.userId !== '1' && scope.row.userId !== 1">
                  <el-tooltip content="详情" placement="top">
                    <el-button link type="primary" :icon="View" @click="openDetail(scope.row.userId)" />
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-button link type="primary" :icon="Edit" @click="handleEdit(scope.row.userId)" />
                  </el-tooltip>
                  <el-tooltip content="重置密码" placement="top">
                    <el-button link type="primary" :icon="Key" @click="handleResetPwd(scope.row)" />
                  </el-tooltip>
                  <el-tooltip content="分配角色" placement="top">
                    <el-button link type="primary" :icon="UserFilled" @click="handleAuthRole(scope.row)" />
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)" />
                  </el-tooltip>
                </template>
             </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="handleQuery"
      />
    </el-card>

    <!-- 新增用户弹框 -->
    <el-dialog v-model="addVisible" title="新增用户" width="700px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="addForm.username" placeholder="请输入用户名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="addForm.password" placeholder="请输入密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="addForm.nickname" placeholder="请输入昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="addForm.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="addForm.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="addForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.label"
                  :value="Number(dict.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色">
          <el-select v-model="addForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.roleId"
              :label="item.roleName"
              :value="item.roleId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情 / 编辑弹框 -->
    <el-dialog v-model="detailVisible" :title="isEdit ? '编辑用户' : '用户详情'" width="700px" :close-on-click-modal="false">
      <div v-loading="detailLoading" style="min-height: 200px;">
        <el-form v-if="currentUser" :model="editForm" label-width="100px" :disabled="!isEdit">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用户ID">
                <el-input :value="currentUser.userId" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input :value="currentUser.username" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="昵称">
                <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号">
                <el-input v-model="editForm.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="邮箱">
                <el-input v-model="editForm.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="性别">
                <el-select v-model="editForm.gender" placeholder="请选择性别" style="width: 100%">
                  <el-option
                    v-for="dict in sys_user_sex"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="头像">
            <div class="avatar-upload-field">
              <el-upload
                class="avatar-uploader"
                accept="image/*"
                :show-file-list="false"
                :http-request="uploadAvatar"
              >
                <el-avatar :size="72" :src="editForm.avatar">
                  {{ (editForm.nickname || editForm.username || '').charAt(0).toUpperCase() }}
                </el-avatar>
                <div class="avatar-upload-tip">
                  <el-icon><UploadFilled /></el-icon>
                  <span>上传头像</span>
                </div>
              </el-upload>
              <div class="avatar-upload-actions">
                <el-input v-model="editForm.avatar" placeholder="上传后自动回填，也可粘贴图片地址" clearable>
                  <template #append>
                    <el-button v-if="editForm.avatar" @click="previewAvatar">预览</el-button>
                  </template>
                </el-input>
                <div class="avatar-upload-extra">
                  <el-button v-if="editForm.avatar" link type="danger" @click="editForm.avatar = ''">清空</el-button>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="所在地区">
            <el-input v-model="editForm.region" placeholder="请输入地区" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="状态">
                <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
                  <el-option
                    v-for="dict in sys_normal_disable"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="分配角色">
             <el-select v-model="editForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                />
             </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">{{ isEdit ? '取消' : '关闭' }}</el-button>
          <el-button v-if="isEdit" type="primary" @click="handleSave" :loading="detailLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 头像预览弹框 -->
    <el-dialog v-model="avatarPreviewVisible" title="头像预览" width="400px">
      <div style="text-align: center;">
        <img :src="editForm.avatar" alt="Avatar" style="max-width: 100%; max-height: 400px;" />
      </div>
    </el-dialog>

    <!-- 重置密码弹框 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="500px">
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹框 -->
    <el-dialog v-model="authRoleVisible" title="分配角色" width="800px" append-to-body>
      <el-form :model="authRoleForm" label-width="80px">
        <h4 style="margin-top: 0; margin-bottom: 15px;">用户信息</h4>
        <el-row :gutter="20" style="margin-bottom: 10px; padding: 10px; border: 1px solid var(--el-border-color); border-radius: 4px;">
           <el-col :span="8">
             <span style="font-weight: bold;">用户ID：</span> {{ authRoleForm.userId }}
           </el-col>
           <el-col :span="8">
             <span style="font-weight: bold;">登录账号：</span> {{ authRoleForm.username }}
           </el-col>
           <el-col :span="8">
             <span style="font-weight: bold;">用户昵称：</span> {{ authRoleForm.nickname }}
           </el-col>
        </el-row>

        <h4 style="margin-top: 20px; margin-bottom: 15px;">角色信息</h4>
        <el-table
          ref="roleTableRef"
          :data="roleOptions"
          border
          style="width: 100%; margin-bottom: 20px;"
          height="300px"
          @selection-change="handleRoleSelectionChange"
          row-key="roleId"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="roleId" label="角色ID" width="100" align="center" />
          <el-table-column prop="roleName" label="角色名称" min-width="120" align="center" />
          <el-table-column prop="roleKey" label="权限字符" min-width="120" align="center" />
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="authRoleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAuthRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick } from 'vue'
import { getUserList, updateUserStatus, getUserDetail, updateUser, deleteUser, resetUserPassword, assignUserRoles, addUser } from '@/api/sys/user.ts'
import { getActiveRoles, assignRoles } from '@/api/sys/role.ts'
import type { User } from '@/api/sys/user.ts'
import { uploadFile } from '@/api/sys/file'
import { ElMessage, ElMessageBox, ElTable } from 'element-plus'
import { View, Search, Refresh, Edit, Delete, Key, UserFilled, UploadFilled, Plus } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'
import { useClipboard } from '@vueuse/core'

const { sys_normal_disable, sys_user_sex } = useDict('sys_normal_disable', 'sys_user_sex')
const { copy } = useClipboard()

const userList = ref<User[]>([])
const total = ref(0)
const loading = ref(false)

const queryParams = reactive({
  current: 1,
  size: 10,
  username: undefined,
  phone: undefined,
  status: undefined as string | undefined
})

const queryFormRef = ref()

// 详情/编辑相关
const detailVisible = ref(false)
const detailLoading = ref(false)
const isEdit = ref(false)
const currentUser = ref<User | null>(null)
const roleOptions = ref<any[]>([])

const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref()
const addForm = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  gender: 2,
  status: 1,
  roleIds: [] as string[],
  postIds: [] as string[]
})
const addRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }]
}

// 编辑表单数据
const editForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  gender: undefined as string | undefined,
  region: '',
  status: '1',
  roleIds: [] as string[]
})

// 头像预览
const avatarPreviewVisible = ref(false)
const previewAvatar = () => {
  avatarPreviewVisible.value = true
}

async function uploadAvatar(options: any) {
  const file = options.file as File
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    options.onError?.(new Error('请选择图片文件'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  try {
    const url = await uploadFile(formData) as string
    editForm.avatar = url
    ElMessage.success('头像上传成功')
    options.onSuccess?.(url)
  } catch (error) {
    options.onError?.(error)
  }
}

// 获取角色列表供选择
const getRoles = async () => {
  try {
    const res = await getActiveRoles() as any
    roleOptions.value = res || []
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res: any = await getUserList(queryParams as any)
    if (res && res.records) {
      userList.value = res.records
      total.value = res.total || 0
    } else {
      console.warn('响应数据格式异常:', res)
      userList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('查询用户列表失败:', error)
    userList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.username = undefined
  queryParams.phone = undefined
  queryParams.status = undefined
  queryParams.current = 1
  handleQuery()
}

const resetAddForm = () => {
  addForm.username = ''
  addForm.password = ''
  addForm.nickname = ''
  addForm.phone = ''
  addForm.email = ''
  addForm.gender = 2
  addForm.status = 1
  addForm.roleIds = []
  addForm.postIds = []
  addFormRef.value?.clearValidate?.()
}

const handleAdd = async () => {
  resetAddForm()
  if (roleOptions.value.length === 0) {
    await getRoles()
  }
  addVisible.value = true
}

const submitAdd = async () => {
  const valid = await addFormRef.value?.validate?.().catch(() => false)
  if (!valid) return
  addLoading.value = true
  try {
    await addUser({ ...addForm })
    ElMessage.success('新增成功')
    addVisible.value = false
    await handleQuery()
  } catch (error) {
    console.error(error)
    ElMessage.error('新增失败')
  } finally {
    addLoading.value = false
  }
}

const confirmStatusChange = (row: User): Promise<boolean> => {
  return new Promise((resolve, reject) => {
    const text = row.status === 1 ? '禁用' : '启用'
    ElMessageBox.confirm(`确认要${text}该用户吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true
    }).then(async () => {
      try {
        const newStatus = row.status === 1 ? 0 : 1
        await updateUserStatus(row.userId, newStatus)
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

const loadDetail = async (id: string) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res: any = await getUserDetail(id)
    currentUser.value = res
    // 填充表单数据
    editForm.nickname = res.nickname || ''
    editForm.phone = res.phone || ''
    editForm.email = res.email || ''
    editForm.avatar = res.avatar || ''
    editForm.gender = res.gender !== undefined ? String(res.gender) : undefined
    editForm.region = res.region || ''
    editForm.status = res.status !== undefined ? String(res.status) : '1'
    editForm.roleIds = res.roles ? res.roles.map((r: any) => r.roleId) : []
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const openDetail = (userId: string) => {
  isEdit.value = false
  loadDetail(userId)
}

const handleEdit = (userId: string) => {
  isEdit.value = true
  loadDetail(userId)
}

const handleSave = async () => {
  if (!currentUser.value) {
    ElMessage.warning('用户信息不存在')
    return
  }

  detailLoading.value = true
  try {
    await updateUser(currentUser.value.userId, editForm as any)
    // 保存角色关联
    if (editForm.roleIds) {
      await assignRoles(currentUser.value.userId, editForm.roleIds)
    }
    ElMessage.success('保存成功')
    detailVisible.value = false
    await handleQuery()
  } catch (error) {
    console.error('保存用户信息失败:', error)
    ElMessage.error('保存失败')
  } finally {
    detailLoading.value = false
  }
}

// 删除用户
const handleDelete = (row: User) => {
  ElMessageBox.confirm(`确认要删除用户 ${row.username} 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUser([row.userId])
      ElMessage.success('删除成功')
      await handleQuery()
    } catch (error) {
      console.error(error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 重置密码
const resetPwdVisible = ref(false)
const resetPwdForm = reactive({
  userId: '',
  password: ''
})
const handleResetPwd = (row: User) => {
  resetPwdForm.userId = row.userId
  resetPwdForm.password = ''
  resetPwdVisible.value = true
}
const submitResetPwd = async () => {
  if (!resetPwdForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  try {
    await resetUserPassword(resetPwdForm)
    ElMessage.success('重置密码成功')
    resetPwdVisible.value = false
  } catch (error) {
    console.error(error)
    ElMessage.error('重置密码失败')
  }
}

// 分配角色
const authRoleVisible = ref(false)
const roleTableRef = ref<InstanceType<typeof ElTable>>()
const authRoleForm = reactive({
  userId: '',
  username: '',
  nickname: '',
  roleIds: [] as string[]
})

const handleRoleSelectionChange = (selection: any[]) => {
  if (!selection) {
    authRoleForm.roleIds = []
    return
  }
  authRoleForm.roleIds = selection
    .filter((item: any) => item && (item.roleId !== undefined && item.roleId !== null))
    .map((item: any) => item.roleId)
}

const handleAuthRole = async (row: User) => {
  authRoleForm.userId = row.userId
  authRoleForm.username = row.username
  authRoleForm.nickname = row.nickname
  authRoleForm.roleIds = []

  // 确保角色列表已加载
  if (roleOptions.value.length === 0) {
    await getRoles()
  }

  authRoleVisible.value = true

  nextTick(() => {
    if (roleTableRef.value) {
      roleTableRef.value.clearSelection()
    }
  })

  // 加载用户当前角色
   try {
     const res: any = await getUserDetail(row.userId)
     if (res.roles) {
       const userRoleIds = res.roles.map((r: any) => r.roleId)
       authRoleForm.roleIds = userRoleIds

       nextTick(() => {
         nextTick(() => {
           if (roleTableRef.value) {
              roleOptions.value.forEach(role => {
                // 兼容 roleId 可能是数字或字符串的情况
                if (userRoleIds.some((id: any) => String(id) === String(role.roleId))) {
                   roleTableRef.value!.toggleRowSelection(role, true)
                }
              })
           }
         })
       })
     }
   } catch (error) {
    console.error(error)
  }
}
const submitAuthRole = async () => {
  try {
    await assignUserRoles({
      userId: authRoleForm.userId,
      roleIds: authRoleForm.roleIds
    })
    ElMessage.success('分配角色成功')
    authRoleVisible.value = false
  } catch (error) {
    console.error(error)
    ElMessage.error('分配角色失败')
  }
}

onMounted(() => {
  handleQuery()
  getRoles()
})

const handleCopy = (text: string | number | undefined) => {
  if (!text) return
  copy(String(text))
  ElMessage.success('复制成功')
}
</script>

<style scoped>
.avatar-upload-field {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.avatar-uploader {
  position: relative;
  width: 104px;
  height: 104px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--el-fill-color-lighter);
  overflow: hidden;
}

.avatar-uploader:hover {
  border-color: var(--el-color-primary);
}

.avatar-upload-tip {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  pointer-events: none;
  font-size: 12px;
}

.avatar-upload-actions {
  flex: 1;
  min-width: 280px;
}

.avatar-upload-extra {
  margin-top: 6px;
}
</style>

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
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 4px;
}
.copy-text {
  cursor: pointer;
  transition: color 0.2s;
}
.copy-text:hover {
  color: var(--el-color-primary);
}
</style>
