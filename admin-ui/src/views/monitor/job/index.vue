<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="queryParams.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="任务组" prop="jobGroup">
          <el-input v-model="queryParams.jobGroup" placeholder="请输入任务组" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="任务状态" clearable style="width: 130px">
            <el-option
              v-for="dict in sys_job_status"
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
          <el-button type="primary" plain @click="handleAdd" v-permission="'monitor:job:add'">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleDelete()" v-permission="'monitor:job:remove'">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </template>

      <div style="flex: 1; overflow: hidden;">
        <el-table
          v-loading="loading"
          :data="jobList"
          border
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column prop="jobId" label="任务ID" width="170" align="center" show-overflow-tooltip />
          <el-table-column prop="jobName" label="任务名称" min-width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="jobGroup" label="任务组" width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="invokeTarget" label="调用目标" min-width="240" show-overflow-tooltip />
          <el-table-column prop="cronExpression" label="Cron表达式" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="misfirePolicy" label="错过策略" width="130" align="center">
            <template #default="{ row }">
              {{ dictLabel(sys_job_misfire_policy, row.misfirePolicy) }}
            </template>
          </el-table-column>
          <el-table-column prop="concurrent" label="并发" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.concurrent === 'Y' ? 'success' : 'warning'">
                {{ dictLabel(sys_job_concurrent, row.concurrent) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :before-change="() => confirmStatusChange(row)"
                v-permission="'monitor:job:changeStatus'"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
          <el-table-column label="操作" width="260" fixed="right" align="center">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(row)" v-permission="'monitor:job:edit'" />
              </el-tooltip>
              <el-tooltip content="执行一次" placement="top">
                <el-button link type="primary" :icon="VideoPlay" @click="handleRun(row)" v-permission="'monitor:job:run'" />
              </el-tooltip>
              <el-tooltip content="查看日志" placement="top">
                <el-button link type="primary" :icon="Tickets" @click="openLogDrawer(row)" v-permission="'monitor:job:log'" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" v-permission="'monitor:job:remove'" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务组" prop="jobGroup">
          <el-input v-model="form.jobGroup" placeholder="请输入任务组" />
        </el-form-item>
        <el-form-item label="调用目标" prop="invokeTarget">
          <el-input v-model="form.invokeTarget" placeholder="例如 sysConfigManager.refreshConfigCache" />
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="例如 0 0/10 * * * ?" />
        </el-form-item>
        <el-form-item label="错过策略" prop="misfirePolicy">
          <el-select v-model="form.misfirePolicy" style="width: 100%">
            <el-option
              v-for="dict in sys_job_misfire_policy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="并发执行" prop="concurrent">
          <el-radio-group v-model="form.concurrent">
            <el-radio v-for="dict in sys_job_concurrent" :key="dict.value" :label="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_job_status" :key="dict.value" :label="Number(dict.value)">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawerVisible" title="任务日志" size="70%" append-to-body>
      <el-form :inline="true" :model="logQueryParams" class="drawer-search">
        <el-form-item label="任务名称">
          <el-input v-model="logQueryParams.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleLogQuery" />
        </el-form-item>
        <el-form-item label="任务组">
          <el-input v-model="logQueryParams.jobGroup" placeholder="请输入任务组" clearable @keyup.enter="handleLogQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="logQueryParams.status" placeholder="执行状态" clearable style="width: 130px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleLogQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetLogQuery">重置</el-button>
          <el-button type="danger" plain :disabled="selectedLogIds.length === 0" :icon="Delete" @click="handleDeleteLog()" v-permission="'monitor:job:logRemove'">删除</el-button>
          <el-button type="warning" plain :icon="DeleteFilled" @click="handleCleanLog" v-permission="'monitor:job:logClean'">清空</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="logLoading"
        :data="jobLogList"
        border
        height="calc(100vh - 270px)"
        style="width: 100%"
        @selection-change="handleLogSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="jobLogId" label="日志ID" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="jobName" label="任务名称" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="jobGroup" label="任务组" width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="invokeTarget" label="调用目标" min-width="220" show-overflow-tooltip />
        <el-table-column prop="jobMessage" label="日志信息" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="100" align="center">
          <template #default="{ row }">{{ row.durationMs || 0 }}ms</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" align="center" />
        <el-table-column prop="endTime" label="结束时间" width="180" align="center" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="异常详情" placement="top" v-if="row.exceptionInfo">
              <el-button link type="primary" :icon="View" @click="showException(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click="handleDeleteLog(row)" v-permission="'monitor:job:logRemove'" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        :total="logTotal"
        v-model:page="logQueryParams.current"
        v-model:limit="logQueryParams.size"
        @pagination="getLogList"
      />
    </el-drawer>

    <el-dialog v-model="exceptionVisible" title="异常详情" width="760px" append-to-body>
      <pre class="exception-box">{{ currentException }}</pre>
      <template #footer>
        <el-button @click="exceptionVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, toRefs, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, DeleteFilled, Edit, Plus, Refresh, Search, Tickets, VideoPlay, View } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'
import {
  addJob,
  changeJobStatus,
  cleanJobLog,
  deleteJob,
  deleteJobLog,
  getJob,
  listJob,
  listJobLog,
  runJob,
  updateJob,
  type JobLogVO,
  type JobQueryBO,
  type JobVO
} from '@/api/monitor/job'

const { sys_job_status, sys_job_concurrent, sys_job_misfire_policy } = useDict(
  'sys_job_status',
  'sys_job_concurrent',
  'sys_job_misfire_policy'
)

const loading = ref(false)
const total = ref(0)
const jobList = ref<JobVO[]>([])
const selectedIds = ref<string[]>([])
const queryFormRef = ref()
const formRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)

const data = reactive({
  queryParams: {
    current: 1,
    size: 10,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined
  } as JobQueryBO,
  form: {} as JobVO,
  rules: {
    jobName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
    jobGroup: [{ required: true, message: '任务组不能为空', trigger: 'blur' }],
    invokeTarget: [{ required: true, message: '调用目标不能为空', trigger: 'blur' }],
    cronExpression: [{ required: true, message: 'Cron表达式不能为空', trigger: 'blur' }],
    misfirePolicy: [{ required: true, message: '错过策略不能为空', trigger: 'change' }],
    concurrent: [{ required: true, message: '并发策略不能为空', trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const logDrawerVisible = ref(false)
const logLoading = ref(false)
const logTotal = ref(0)
const jobLogList = ref<JobLogVO[]>([])
const selectedLogIds = ref<string[]>([])
const exceptionVisible = ref(false)
const currentException = ref('')

const logQueryParams = reactive({
  current: 1,
  size: 10,
  jobId: undefined as string | undefined,
  jobName: undefined as string | undefined,
  jobGroup: undefined as string | undefined,
  status: undefined as number | string | undefined
})

const dictLabel = (dicts: any[], value: string | number) => {
  const item = (dicts || []).find((dict) => String(dict.value) === String(value))
  return item?.label || value
}

const resetForm = () => {
  form.value = {
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: '',
    cronExpression: '',
    misfirePolicy: 'DO_NOTHING',
    concurrent: 'N',
    status: 0,
    remark: ''
  }
  formRef.value?.resetFields()
}

const getList = () => {
  loading.value = true
  listJob(queryParams.value).then((res: any) => {
    jobList.value = res.records || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

const handleQuery = () => {
  queryParams.value.current = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleSelectionChange = (selection: JobVO[]) => {
  selectedIds.value = selection.map((item) => item.jobId!).filter(Boolean)
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增任务'
  dialogVisible.value = true
}

const handleEdit = (row: JobVO) => {
  resetForm()
  getJob(row.jobId!).then((res) => {
    form.value = res
    dialogTitle.value = '编辑任务'
    dialogVisible.value = true
  })
}

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (form.value.jobId) {
        await updateJob(form.value)
        ElMessage.success('修改成功')
      } else {
        await addJob(form.value)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    } finally {
      submitLoading.value = false
    }
  })
}

const confirmStatusChange = async (row: JobVO) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '暂停'
  await ElMessageBox.confirm(`确认${action}任务“${row.jobName}”吗？`, '系统提示', { type: 'warning' })
  await changeJobStatus(row.jobId!, nextStatus)
  ElMessage.success(`${action}成功`)
  return true
}

const handleRun = async (row: JobVO) => {
  await ElMessageBox.confirm(`确认立即执行一次任务“${row.jobName}”吗？`, '系统提示', { type: 'warning' })
  await runJob(row.jobId!)
  ElMessage.success('执行完成')
  if (logDrawerVisible.value) {
    getLogList()
  }
}

const handleDelete = async (row?: JobVO) => {
  const ids = row?.jobId ? [row.jobId] : selectedIds.value
  if (ids.length === 0) return
  await ElMessageBox.confirm('确认删除选中的任务吗？', '系统提示', { type: 'warning' })
  await deleteJob(ids)
  ElMessage.success('删除成功')
  getList()
}

const openLogDrawer = (row: JobVO) => {
  logQueryParams.current = 1
  logQueryParams.size = 10
  logQueryParams.jobId = row.jobId
  logQueryParams.jobName = undefined
  logQueryParams.jobGroup = undefined
  logQueryParams.status = undefined
  logDrawerVisible.value = true
  getLogList()
}

const getLogList = () => {
  logLoading.value = true
  listJobLog(logQueryParams).then((res: any) => {
    jobLogList.value = res.records || []
    logTotal.value = res.total || 0
  }).finally(() => {
    logLoading.value = false
  })
}

const handleLogQuery = () => {
  logQueryParams.current = 1
  getLogList()
}

const resetLogQuery = () => {
  logQueryParams.jobName = undefined
  logQueryParams.jobGroup = undefined
  logQueryParams.status = undefined
  handleLogQuery()
}

const handleLogSelectionChange = (selection: JobLogVO[]) => {
  selectedLogIds.value = selection.map((item) => item.jobLogId!).filter(Boolean)
}

const handleDeleteLog = async (row?: JobLogVO) => {
  const ids = row?.jobLogId ? [row.jobLogId] : selectedLogIds.value
  if (ids.length === 0) return
  await ElMessageBox.confirm('确认删除选中的任务日志吗？', '系统提示', { type: 'warning' })
  await deleteJobLog(ids)
  ElMessage.success('删除成功')
  getLogList()
}

const handleCleanLog = async () => {
  await ElMessageBox.confirm('确认清空全部任务日志吗？', '系统提示', { type: 'warning' })
  await cleanJobLog()
  ElMessage.success('清空成功')
  getLogList()
}

const showException = (row: JobLogVO) => {
  currentException.value = row.exceptionInfo || ''
  exceptionVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-search {
  margin-bottom: 12px;
}

.exception-box {
  max-height: 520px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
