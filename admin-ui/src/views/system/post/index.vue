<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧部门树 -->
      <el-col :span="4" :xs="24">
        <el-card shadow="never" class="box-card">
          <div class="head-container">
            <el-input
              v-model="deptName"
              placeholder="请输入部门名称"
              clearable
              :prefix-icon="Search"
              style="margin-bottom: 20px"
            />
          </div>
          <div class="head-container">
            <el-tree
              :data="deptOptions"
              :props="{ label: 'deptName', children: 'children' }"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              ref="deptTreeRef"
              node-key="deptId"
              default-expand-all
              highlight-current
              @node-click="handleNodeClick"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧岗位数据 -->
      <el-col :span="20" :xs="24">
        <el-card shadow="never" class="search-wrapper">
          <el-form :inline="true" :model="queryParams" ref="queryFormRef">
            <el-form-item label="岗位编码" prop="postCode">
              <el-input
                v-model="queryParams.postCode"
                placeholder="请输入岗位编码"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="岗位名称" prop="postName">
              <el-input
                v-model="queryParams.postName"
                placeholder="请输入岗位名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="岗位状态" clearable style="width: 120px">
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
              <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
              <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
            </div>
          </template>

          <el-table v-loading="loading" :data="postList" @selection-change="handleSelectionChange" border height="100%">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="岗位名称" align="center" prop="postName" />
            <el-table-column label="岗位编码" align="center" prop="postCode" />
            <el-table-column label="所在部门" align="center" prop="deptName" />
            <el-table-column label="岗位排序" align="center" prop="postSort" />
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

    <!-- 添加或修改岗位对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="postFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="form.postName" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="form.postCode" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="岗位顺序" prop="postSort">
          <el-input-number v-model="form.postSort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="岗位状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :value="Number(dict.value)"
            >{{ dict.label }}</el-radio>
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
import { ref, reactive, toRefs, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listPost, getPost, delPost, addPost, updatePost } from '@/api/system/post'
import { listDept } from '@/api/system/dept'
import { handleTree } from '@/utils/tree'
import type { PostVO, PostForm, PostQuery } from '@/api/system/post'
import type { DeptVO } from '@/api/system/dept'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_normal_disable } = useDict('sys_normal_disable')

const queryFormRef = ref()
const postFormRef = ref()
const deptTreeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(true)
const total = ref(0)
const postList = ref<PostVO[]>([])
const deptOptions = ref<DeptVO[]>([])
const deptName = ref('')
const open = ref(false)
const title = ref('')
const ids = ref<string[]>([])
const single = ref(true)
const multiple = ref(true)

const data = reactive({
  form: {} as PostForm,
  queryParams: {
    current: 1,
    size: 10,
    postCode: undefined,
    postName: undefined,
    status: undefined,
    deptId: undefined
  } as PostQuery,
  rules: {
    postName: [{ required: true, message: '岗位名称不能为空', trigger: 'blur' }],
    postCode: [{ required: true, message: '岗位编码不能为空', trigger: 'blur' }],
    postSort: [{ required: true, message: '岗位顺序不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询岗位列表 */
function getList() {
  loading.value = true
  listPost(queryParams.value).then(response => {
    postList.value = response.records
    total.value = Number(response.total)
    loading.value = false
  })
}

/** 查询部门列表 */
function getDeptList() {
  listDept().then(response => {
    deptOptions.value = handleTree(response, 'deptId')
  })
}

/** 筛选节点 */
const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.deptName.includes(value)
}

/** 节点单击事件 */
function handleNodeClick(data: DeptVO) {
  queryParams.value.deptId = data.deptId
  handleQuery()
}

/** 监听部门名称查询 */
watch(deptName, (val) => {
  deptTreeRef.value!.filter(val)
})

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    postId: undefined,
    postCode: '',
    postName: '',
    postSort: 0,
    status: 1,
    remark: undefined
  }
  if (postFormRef.value) {
    postFormRef.value.resetFields()
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
  queryParams.value.deptId = undefined
  deptTreeRef.value?.setCurrentKey(undefined)
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: PostVO[]) {
  ids.value = selection.map(item => item.postId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 岗位状态修改 */
function handleStatusChange(row: PostVO) {
  let text = row.status === 1 ? "启用" : "停用";
  ElMessageBox.confirm('确认要"' + text + '""' + row.postName + '"岗位吗?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(function() {
    return updatePost(row);
  }).then(() => {
    ElMessage.success(text + "成功");
  }).catch(function() {
    row.status = row.status === 1 ? 0 : 1;
  });
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加岗位'
}

/** 修改按钮操作 */
function handleUpdate(row?: PostVO) {
  reset()
  const postId = row?.postId || ids.value[0]
  if (!postId) return
  getPost(postId).then(response => {
    form.value = response
    open.value = true
    title.value = '修改岗位'
  })
}

/** 提交按钮 */
function submitForm() {
  postFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.postId) {
        updatePost(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addPost(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row?: PostVO) {
  const postIds = row?.postId ? [row.postId] : ids.value
  ElMessageBox.confirm('是否确认删除岗位编号为"' + postIds + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delPost(postIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  getDeptList()
  getList()
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
