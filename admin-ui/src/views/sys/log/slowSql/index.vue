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
          <el-form-item label="最小执行时长(ms)" prop="minExecutionTime">
            <el-input-number
              v-model="queryParams.minExecutionTime"
              placeholder="请输入时长"
              :min="0"
              controls-position="right"
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
          <el-table-column label="执行时长(ms)" align="center" prop="executionTime" width="120">
            <template #default="scope">
              <el-tag type="danger">{{ scope.row.executionTime }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="SQL语句" align="center" prop="sqlStatement" show-overflow-tooltip />
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

    <!-- 慢SQL详情对话框 -->
    <el-dialog :title="detail.title" v-model="detail.open" width="800px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="追踪ID">{{ detail.form.traceId }}</el-descriptions-item>
        <el-descriptions-item label="用户名称">{{ detail.form.userName }}</el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ detail.form.executionTime }} ms</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.form.createTime }}</el-descriptions-item>
        <el-descriptions-item label="SQL语句">
          <div class="code-block sql-statement">{{ detail.form.sqlStatement }}</div>
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
import { getSlowSqlLogList, type SlowSqlLogVO, type SlowSqlLogQueryBO } from '@/api/sys/log'

defineOptions({ name: 'SlowSqlLog' })

const loading = ref(true)
const total = ref(0)
const logList = ref<SlowSqlLogVO[]>([])

const queryParams = reactive<SlowSqlLogQueryBO>({
  current: 1,
  size: 10,
  traceId: undefined,
  userName: undefined,
  minExecutionTime: undefined
})

const detail = reactive({
  open: false,
  title: '慢SQL日志详情',
  form: {} as SlowSqlLogVO
})

/** 查询慢SQL日志列表 */
function getList() {
  loading.value = true
  getSlowSqlLogList(queryParams).then(response => {
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
  queryParams.minExecutionTime = undefined
  handleQuery()
}

/** 详细按钮操作 */
function handleDetail(row: SlowSqlLogVO) {
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
  max-height: 400px;
  overflow-y: auto;
}

.sql-statement {
  color: #e6a23c;
}
</style>
