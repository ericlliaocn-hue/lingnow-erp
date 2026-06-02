<template>
  <div>
    <el-card shadow="never" class="search-wrapper">
      <el-form ref="queryFormRef" :inline="true" :model="query">
        <el-form-item label="单据类型" prop="bizType">
          <el-select v-model="query.bizType" clearable style="width: 180px">
            <el-option v-for="item in approvalBizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单号" prop="billNo">
          <el-input v-model="query.billNo" clearable placeholder="请输入单号" />
        </el-form-item>
        <el-form-item v-if="showStatusFilter" label="审批状态" prop="approvalStatus">
          <el-select v-model="query.approvalStatus" clearable style="width: 140px">
            <el-option label="审批中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="REVOKED" />
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
          <strong>{{ title }}</strong>
          <el-button :icon="Refresh" @click="getList">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border height="100%">
        <el-table-column prop="bizName" label="类型" min-width="110" />
        <el-table-column prop="billNo" label="单号" min-width="150" />
        <el-table-column prop="amount" label="金额" min-width="100" align="right" />
        <el-table-column prop="nodeName" label="当前节点" min-width="110" />
        <el-table-column prop="approvalStatus" label="审批状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="approvalStatusTag[row.approvalStatus] || 'info'">{{ approvalStatusText[row.approvalStatus] || row.approvalStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitBy" label="发起人" width="110" />
        <el-table-column prop="submitTime" label="发起时间" min-width="170" />
        <el-table-column v-if="mode !== 'mine'" prop="createTime" label="任务时间" min-width="170" />
        <el-table-column label="操作" :width="actionWidth" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewBill(row)">查看单据</el-button>
            <el-button link type="primary" @click="openHistory(row)">记录</el-button>
            <el-button v-if="mode === 'todo'" link type="success" @click="openHandle(row, 'pass')" v-permission="'erp:approval:approve'">通过</el-button>
            <el-button v-if="mode === 'todo'" link type="danger" @click="openHandle(row, 'reject')" v-permission="'erp:approval:reject'">驳回</el-button>
            <el-button v-if="mode === 'todo'" link type="warning" @click="openTransfer(row)" v-permission="'erp:approval:transfer'">转交</el-button>
            <el-button v-if="mode === 'mine' && row.approvalStatus === 'PENDING'" link type="warning" @click="revoke(row)" v-permission="'erp:approval:revoke'">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.current" v-model:limit="query.size" @pagination="getList" />
    </el-card>

    <el-dialog v-model="handleOpen" :title="handleTitle" width="520px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="handleComment" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferOpen" title="转交审批" width="520px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="接收人">
          <el-input v-model="transferUserId" placeholder="请输入接收人用户ID" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="handleComment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmTransfer">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyOpen" title="审批记录" width="760px" append-to-body>
      <el-table :data="historyList" border>
        <el-table-column prop="nodeName" label="节点" min-width="120" />
        <el-table-column prop="approver" label="审批人" width="100" />
        <el-table-column prop="skipType" label="动作" width="100" />
        <el-table-column prop="message" label="意见" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="处理时间" min-width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import {
  approvalBizTypeOptions,
  approvalHistory,
  approvalStatusTag,
  approvalStatusText,
  listApprovalDone,
  listApprovalMine,
  listApprovalTodo,
  passApproval,
  rejectApproval,
  revokeApproval,
  transferApproval,
  type ApprovalHistory,
  type ApprovalQuery,
  type ApprovalTask
} from '@/api/erp/approval'

const props = defineProps<{ mode: 'todo' | 'mine' | 'done'; title: string }>()
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<ApprovalTask[]>([])
const queryFormRef = ref()
const query = reactive<ApprovalQuery>({ current: 1, size: 10 })
const currentRow = ref<ApprovalTask>()
const handleOpen = ref(false)
const transferOpen = ref(false)
const historyOpen = ref(false)
const handleAction = ref<'pass' | 'reject'>('pass')
const handleComment = ref('')
const transferUserId = ref('')
const historyList = ref<ApprovalHistory[]>([])
const showStatusFilter = computed(() => props.mode !== 'todo')
const handleTitle = computed(() => handleAction.value === 'pass' ? '审批通过' : '审批驳回')
const actionWidth = computed(() => props.mode === 'todo' ? 300 : 220)

function getList() {
  loading.value = true
  const api = props.mode === 'todo' ? listApprovalTodo : props.mode === 'mine' ? listApprovalMine : listApprovalDone
  api(query).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}

function handleQuery() {
  query.current = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields()
  handleQuery()
}

function openHandle(row: ApprovalTask, action: 'pass' | 'reject') {
  currentRow.value = row
  handleAction.value = action
  handleComment.value = ''
  handleOpen.value = true
}

function confirmHandle() {
  if (!currentRow.value?.taskId) return
  const api = handleAction.value === 'pass' ? passApproval : rejectApproval
  api(currentRow.value.taskId, handleComment.value).then(() => {
    ElMessage.success(handleAction.value === 'pass' ? '审批通过成功' : '审批驳回成功')
    handleOpen.value = false
    getList()
  })
}

function openTransfer(row: ApprovalTask) {
  currentRow.value = row
  transferUserId.value = ''
  handleComment.value = ''
  transferOpen.value = true
}

function confirmTransfer() {
  if (!currentRow.value?.taskId || !transferUserId.value) {
    ElMessage.warning('请输入接收人用户ID')
    return
  }
  transferApproval(currentRow.value.taskId, transferUserId.value, handleComment.value).then(() => {
    ElMessage.success('转交成功')
    transferOpen.value = false
    getList()
  })
}

function revoke(row: ApprovalTask) {
  ElMessageBox.confirm('确定撤回这条审批吗？', '提示', { type: 'warning' })
    .then(() => revokeApproval(row.bizType, row.bizId))
    .then(() => {
      ElMessage.success('撤回成功')
      getList()
    })
}

function openHistory(row: ApprovalTask) {
  approvalHistory(row.bizType, row.bizId).then(res => {
    historyList.value = res
    historyOpen.value = true
  })
}

function viewBill(row: ApprovalTask) {
  if (row.actionUrl) {
    router.push(row.actionUrl)
  }
}

onMounted(getList)
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
</style>
