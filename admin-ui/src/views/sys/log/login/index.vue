<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">

      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="登录地址">
          <el-input v-model="queryParams.ipaddr" placeholder="请输入登录地址" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="用户名称">
          <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="登录状态" clearable style="width: 120px">
            <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-wrapper">
      <div style="flex: 1; overflow: hidden;">
        <el-table v-loading="loading" :data="logList" border style="width: 100%" height="100%">
          <el-table-column prop="infoId" label="访问编号" align="center" width="100" />
          <el-table-column prop="userName" label="用户名称" align="center" width="150" />
          <el-table-column prop="ipaddr" label="登录地址" align="center" width="130" />
          <el-table-column prop="loginLocation" label="登录地点" align="center" width="150" />
          <el-table-column prop="browser" label="浏览器" align="center" width="120" />
          <el-table-column prop="os" label="操作系统" align="center" width="120" />
          <el-table-column prop="status" label="登录状态" align="center" width="100">
            <template #default="scope">
              <template v-for="(dict, index) in sys_common_status" :key="index">
                <el-tag v-if="dict.value == '' + scope.row.status" :type="dict.elTagType">{{ dict.label }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="msg" label="操作信息" align="center" />
          <el-table-column prop="loginTime" label="登录时间" align="center" width="180" />
        </el-table>
      </div>

      <pagination
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getLoginLogList, type LoginLogVO, type LoginLogQueryBO } from '@/api/sys/log'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_common_status } = useDict('sys_common_status')

const loading = ref(false)
const total = ref(0)
const logList = ref<LoginLogVO[]>([])
const dateRange = ref<[string, string] | []>([])

const queryParams = reactive<LoginLogQueryBO>({
  current: 1,
  size: 10,
  ipaddr: undefined,
  userName: undefined,
  status: undefined
})

const getList = () => {
  loading.value = true
  // 处理日期范围
  const params = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }

  getLoginLogList(params).then((res: any) => {
    logList.value = res.records
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

const handleQuery = () => {
  queryParams.current = 1
  getList()
}

const resetQuery = () => {
  queryParams.ipaddr = undefined
  queryParams.userName = undefined
  queryParams.status = undefined
  dateRange.value = []
  handleQuery()
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


