<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="公告标题" prop="noticeTitle">
          <el-input
            v-model="queryParams.noticeTitle"
            placeholder="请输入公告标题"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="操作人员" prop="createBy">
          <el-input
            v-model="queryParams.createBy"
            placeholder="请输入操作人员"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable style="width: 120px">
            <el-option
              v-for="dict in sys_notice_type"
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
          <el-button type="primary" @click="handleAdd" v-permission="'system:notice:add'">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table v-loading="loading" :data="noticeList" border style="width: 100%" height="100%">
          <el-table-column type="index" width="75" align="center" label="序号" />
          <el-table-column label="公告标题" align="center" prop="noticeTitle" show-overflow-tooltip />
          <el-table-column label="公告类型" align="center" prop="noticeType" width="100">
            <template #default="scope">
              <template v-for="(dict, index) in sys_notice_type" :key="index">
                <el-tag v-if="dict.value == '' + scope.row.noticeType" :type="dict.elTagType">{{ dict.label }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
              <template v-for="(dict, index) in sys_notice_status" :key="index">
                <el-tag v-if="dict.value == '' + scope.row.status" :type="dict.elTagType">{{ dict.label }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="创建者" align="center" prop="createBy" width="100" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
              <span>{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" :icon="Edit" @click="handleUpdate(scope.row)" v-permission="'system:notice:edit'">修改</el-button>
              <el-button link type="primary" :icon="Delete" @click="handleDelete(scope.row)" v-permission="'system:notice:remove'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-card>

    <!-- 添加或修改公告对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="noticeFormRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="公告标题" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告类型" prop="noticeType">
              <el-select v-model="form.noticeType" placeholder="请选择公告类型">
                <el-option v-for="dict in sys_notice_type" :key="dict.value" :label="dict.label" :value="parseInt(dict.value)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_notice_status" :key="dict.value" :value="parseInt(dict.value)">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
               <el-input
                 v-model="form.noticeContent"
                 type="textarea"
                 placeholder="请输入内容"
                 :rows="4"
               />
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
import { ref, reactive, toRefs, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { listNotice, getNotice, delNotice, addNotice, updateNotice } from '@/api/system/notice'
import type { NoticeVO, NoticeForm, NoticeQuery } from '@/api/system/notice'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_notice_type, sys_notice_status } = useDict('sys_notice_type', 'sys_notice_status')

const queryFormRef = ref()
const noticeFormRef = ref()
const loading = ref(true)
const total = ref(0)
const noticeList = ref<NoticeVO[]>([])
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {} as NoticeForm,
  queryParams: {
    current: 1,
    size: 10,
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined
  } as NoticeQuery,
  rules: {
    noticeTitle: [{ required: true, message: '公告标题不能为空', trigger: 'blur' }],
    noticeType: [{ required: true, message: '公告类型不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询公告列表 */
function getList() {
  loading.value = true
  listNotice(queryParams.value).then(response => {
    noticeList.value = response.records
    total.value = Number(response.total)
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
    noticeId: undefined,
    noticeTitle: '',
    noticeType: 1,
    noticeContent: '',
    status: 1
  }
  if (noticeFormRef.value) {
    noticeFormRef.value.resetFields()
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
  title.value = '添加公告'
}

/** 修改按钮操作 */
function handleUpdate(row: NoticeVO) {
  reset()
  getNotice(row.noticeId).then(response => {
    form.value = response
    open.value = true
    title.value = '修改公告'
  })
}

/** 提交按钮 */
function submitForm() {
  noticeFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.value.noticeId) {
        updateNotice(form.value).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addNotice(form.value).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: NoticeVO) {
  ElMessageBox.confirm('是否确认删除公告标题为"' + row.noticeTitle + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return delNotice(row.noticeId)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
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
.search-wrapper {
  margin-bottom: 20px;
  flex-shrink: 0;
}
.table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>
