<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="系统模块">
          <el-input v-model="queryParams.title" placeholder="请输入系统模块" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="queryParams.operName" placeholder="请输入操作人员" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.businessType" placeholder="业务类型" clearable style="width: 120px">
            <el-option v-for="dict in sys_oper_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="操作状态" clearable style="width: 120px">
            <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
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
          <el-table-column prop="operId" label="日志编号" align="center" width="100" />
          <el-table-column prop="title" label="系统模块" align="center" min-width="150" />
          <el-table-column prop="businessType" label="业务类型" align="center" width="100">
            <template #default="scope">
              <template v-for="(dict, index) in sys_oper_type" :key="index">
                <el-tag v-if="dict.value == '' + scope.row.businessType" :type="dict.elTagType">{{ dict.label }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="requestMethod" label="请求方式" align="center" width="100" />
          <el-table-column prop="operUrl" label="请求地址" align="center" min-width="150" />
          <el-table-column prop="operName" label="操作人员" align="center" min-width="120" />
          <el-table-column prop="operIp" label="主机" align="center" width="130" />
          <el-table-column prop="operLocation" label="操作地点" align="center" min-width="150" />
          <el-table-column prop="status" label="状态" align="center" width="100">
            <template #default="scope">
              <template v-for="(dict, index) in sys_common_status" :key="index">
                <el-tag v-if="dict.value == '' + scope.row.status" :type="dict.elTagType">{{ dict.label }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="operTime" label="操作时间" align="center" width="180" />
          <el-table-column prop="costTime" label="消耗时间" align="center" width="100">
             <template #default="scope">
                {{ scope.row.costTime }}ms
             </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="100" align="center">
            <template #default="scope">
              <el-button link type="primary" :icon="View" @click="handleDetail(scope.row)">详情</el-button>
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

    <!-- 操作日志详情 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px" append-to-body>
      <el-form :model="currentLog" label-width="100px" v-if="currentLog">
        <el-row>
          <el-col :span="12">
            <el-form-item label="操作模块：">{{ currentLog.title }} / {{ typeFormat(currentLog) }}</el-form-item>
            <el-form-item label="登录信息：">{{ currentLog.operName }} / {{ currentLog.operIp }} / {{ currentLog.operLocation }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求地址：">{{ currentLog.operUrl }}</el-form-item>
            <el-form-item label="请求方式：">{{ currentLog.requestMethod }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="操作方法：">{{ currentLog.method }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求参数：">{{ currentLog.operParam }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="返回参数：">{{ currentLog.jsonResult }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作状态：">
              <template v-for="(dict, index) in sys_common_status" :key="index">
                <div v-if="dict.value == '' + currentLog.status">{{ dict.label }}</div>
              </template>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消耗时间：">{{ currentLog.costTime }}毫秒</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间：">{{ currentLog.operTime }}</el-form-item>
          </el-col>
          <el-col :span="24" v-if="currentLog.status === 0">
            <el-form-item label="异常信息：">{{ currentLog.errorMsg }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { getOperLogList, type OperLogVO, type OperLogQueryBO } from '@/api/sys/log'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { sys_oper_type, sys_common_status } = useDict('sys_oper_type', 'sys_common_status')

const loading = ref(false)
const total = ref(0)
const logList = ref<OperLogVO[]>([])
const dateRange = ref<[string, string] | []>([])
const detailVisible = ref(false)
const currentLog = ref<OperLogVO | null>(null)

const queryParams = reactive<OperLogQueryBO>({
  current: 1,
  size: 10,
  title: undefined,
  operName: undefined,
  businessType: undefined,
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

  getOperLogList(params).then((res: any) => {
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
  queryParams.title = undefined
  queryParams.operName = undefined
  queryParams.businessType = undefined
  queryParams.status = undefined
  dateRange.value = []
  handleQuery()
}

const handleDetail = (row: OperLogVO) => {
  currentLog.value = row
  detailVisible.value = true
}

const typeFormat = (row: OperLogVO) => {
  return selectDictLabel(sys_oper_type.value || [], row.businessType)
}

const selectDictLabel = (datas: any, value: any) => {
  if (value === undefined) {
    return ''
  }
  const actions: string[] = []
  Object.keys(datas).some((key) => {
    if (datas[key].value == '' + value) {
      actions.push(datas[key].label)
      return true
    }
  })
  return actions.join('')
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
