<template>
  <div class="app-container">
    <div class="search-wrapper">
      <el-card shadow="never">
        <el-form :model="queryParams" ref="queryForm" :inline="true">
          <el-form-item label="追踪ID" prop="traceId">
            <el-input
              v-model="queryParams.traceId"
              placeholder="请输入追踪ID"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="用户名称" prop="userName">
            <el-input
              v-model="queryParams.userName"
              placeholder="请输入用户名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="请求URL" prop="requestUrl">
            <el-input
              v-model="queryParams.requestUrl"
              placeholder="请输入请求URL"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div class="table-wrapper">
      <el-card shadow="never" class="flex-card">
        <el-table v-loading="loading" :data="logList" border style="width: 100%; height: 100%;">
          <el-table-column label="追踪ID" align="center" prop="traceId" show-overflow-tooltip />
          <el-table-column label="用户名称" align="center" prop="userName" width="120" />
          <el-table-column label="请求方式" align="center" prop="requestMethod" width="100" />
          <el-table-column label="请求URL" align="center" prop="requestUrl" show-overflow-tooltip />
          <el-table-column label="IP地址" align="center" prop="ip" width="130" />
          <el-table-column label="错误信息" align="center" prop="errorMsg" show-overflow-tooltip />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-show="total > 0"
            v-model:current-page="queryParams.current"
            v-model:page-size="queryParams.size"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleQuery"
            @current-change="handleQuery"
          />
        </div>
      </el-card>
    </div>

    <!-- 错误详情对话框 -->
    <el-dialog :title="detail.title" v-model="detail.open" width="800px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="追踪ID">{{ detail.form.traceId }}</el-descriptions-item>
        <el-descriptions-item label="用户名称">{{ detail.form.userName }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.form.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ detail.form.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detail.form.ip }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <div class="code-block">{{ detail.form.requestParams }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ detail.form.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="堆栈信息">
          <div class="code-block error-stack">{{ detail.form.errorStack }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detail.open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getErrorLogList, type ErrorLogVO, type ErrorLogQueryBO } from '@/api/sys/log'

defineOptions({ name: 'ErrorLog' })

const loading = ref(true)
const total = ref(0)
const logList = ref<ErrorLogVO[]>([])

const queryParams = reactive<ErrorLogQueryBO>({
  current: 1,
  size: 10,
  traceId: undefined,
  userName: undefined,
  requestUrl: undefined
})

const detail = reactive({
  open: false,
  title: '错误日志详情',
  form: {} as ErrorLogVO
})

/** 查询错误日志列表 */
function getList() {
  loading.value = true
  getErrorLogList(queryParams).then(response => {
    logList.value = response.records
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.current = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.traceId = undefined
  queryParams.userName = undefined
  queryParams.requestUrl = undefined
  handleQuery()
}

/** 详细按钮操作 */
function handleDetail(row: ErrorLogVO) {
  detail.open = true
  detail.form = row
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
}

.table-wrapper {
  flex: 1;
  min-height: 0;
}

.flex-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.flex-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.pagination-container {
  padding: 15px 20px;
  border-top: 1px solid #ebeef5;
}

:deep(.el-table__cell) {
  padding: 20px 0;
}

.code-block {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.error-stack {
  color: #f56c6c;
  max-height: 400px;
}
</style>
