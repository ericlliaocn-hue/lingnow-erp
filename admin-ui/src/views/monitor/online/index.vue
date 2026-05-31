<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="登录名称" prop="username">
          <el-input v-model="queryParams.username" placeholder="请输入登录名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="主机地址" prop="ipaddr">
          <el-input v-model="queryParams.ipaddr" placeholder="请输入主机地址" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-wrapper">
      <div style="flex: 1; overflow: hidden;">
        <el-table :data="tableData" border height="100%" style="width: 100%" v-loading="loading">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="sessionId" label="会话编号" show-overflow-tooltip />
          <el-table-column prop="username" label="登录名称" align="center" />
          <el-table-column prop="ipaddr" label="主机" align="center" />
          <el-table-column prop="loginLocation" label="登录地点" align="center" />
          <el-table-column prop="browser" label="浏览器" align="center" />
          <el-table-column prop="os" label="操作系统" align="center" />
          <el-table-column prop="loginTime" label="登录时间" width="180" align="center" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-popconfirm title="确定要强制退出该用户吗？" @confirm="handleForceLogout(row)">
                <template #reference>
                  <el-button link type="danger" size="small" icon="Delete">强退</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 注意：在线用户通常是内存分页，或者直接返回全部，这里简单处理 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOnlineUserList, forceLogout } from '@/api/monitor/online'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive({
  current: 1,
  size: 10,
  username: '',
  ipaddr: ''
})

const tableData = ref([])

const getList = async () => {
  loading.value = true
  try {
    const res = await getOnlineUserList(queryParams)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.current = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.username = ''
  queryParams.ipaddr = ''
  handleQuery()
}

const handleForceLogout = async (row: any) => {
  try {
    await forceLogout(row.sessionId)
    ElMessage.success('强退成功')
    getList()
  } catch (error) {
    // ignore
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>
