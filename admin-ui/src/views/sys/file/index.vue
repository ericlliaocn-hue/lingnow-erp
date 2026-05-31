<template>
  <div class="app-container">
    <el-container class="file-layout">
      <!-- 左侧分类 -->
      <el-aside width="220px" class="file-aside">
        <el-card class="box-card aside-card" shadow="never" :body-style="{ padding: '10px 0' }">
          <el-menu
            :default-active="activeMenu"
            class="file-menu"
            @select="handleMenuSelect"
          >
            <el-menu-item index="ALL">
              <el-icon><Folder /></el-icon>
              <span>全部文件</span>
            </el-menu-item>
            <el-menu-item-group title="存储位置">
              <el-menu-item v-for="dict in sys_file_storage_type" :key="dict.value" :index="dict.value">
                <el-icon><Cloudy /></el-icon>{{ dict.label }}
              </el-menu-item>
            </el-menu-item-group>
          </el-menu>
        </el-card>
      </el-aside>

      <!-- 右侧列表 -->
      <el-main class="file-main">
        <el-card shadow="never" class="search-wrapper">
          <div class="file-header">
            <div class="left-panel">
               <el-input
                 v-model="queryParams.fileName"
                 placeholder="搜索文件名"
                 style="width: 240px"
                 clearable
                 @keyup.enter="handleSearch"
                 @clear="handleSearch"
               >
                  <template #prefix><el-icon><Search /></el-icon></template>
               </el-input>
               <el-button type="primary" @click="handleSearch" style="margin-left: 10px">搜索</el-button>
            </div>
            <div class="right-panel">
              <el-button type="primary" @click="handleUpload">
                <el-icon style="margin-right: 4px"><Upload /></el-icon> 上传文件
              </el-button>
              <el-button @click="handleConfig">
                <el-icon style="margin-right: 4px"><Setting /></el-icon> 存储配置
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="table-wrapper">
          <div style="flex: 1; overflow: hidden;">
            <el-table
               v-loading="loading"
               :data="tableData"
               style="width: 100%"
               height="100%"
               border
            >
              <el-table-column prop="id" label="ID" width="80" align="center" />
              <el-table-column label="预览" width="100" align="center">
                 <template #default="scope">
                   <el-image
                     v-if="isImage(scope.row.fileSuffix)"
                     style="width: 50px; height: 50px; border-radius: 4px"
                     :src="scope.row.fileUrl"
                     :preview-src-list="[scope.row.fileUrl]"
                     fit="cover"
                     preview-teleported
                   />
                   <el-icon v-else style="font-size: 40px; color: #909399"><Document /></el-icon>
                 </template>
              </el-table-column>
              <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
              <el-table-column prop="fileSize" label="大小" width="120" align="center">
                 <template #default="scope">
                   {{ formatSize(scope.row.fileSize) }}
                 </template>
              </el-table-column>
              <el-table-column prop="storageType" label="存储类型" width="100" align="center">
                <template #default="scope">
                  <template v-for="(dict, index) in sys_file_storage_type" :key="index">
                    <el-tag v-if="dict.value === scope.row.storageType" :type="dict.elTagType" effect="dark">{{ dict.label }}</el-tag>
                  </template>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="上传时间" width="180" align="center" />
              <el-table-column label="操作" width="180" align="center" fixed="right">
                <template #default="scope">
                  <el-button link type="primary" @click="handleCopy(scope.row.fileUrl)">
                     <el-icon><CopyDocument /></el-icon> 复制
                  </el-button>
                  <el-button link type="danger" @click="handleDelete(scope.row)">
                     <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <pagination
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="total"
            @pagination="getList"
          />
        </el-card>
      </el-main>
    </el-container>

    <!-- 配置弹窗 -->
    <el-dialog
      v-model="configVisible"
      title="存储配置"
      width="40%"
      align-center
      destroy-on-close
      class="config-dialog"
    >
      <el-table :data="configList" border style="width: 100%">
        <el-table-column prop="platform" label="平台" width="150" align="center" font-weight="bold" />
        <el-table-column prop="isActive" label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'" effect="dark">
              {{ scope.row.isActive === 1 ? '启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" align="center" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button type="primary" link @click="handleEditConfig(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              type="success"
              link
              v-if="scope.row.isActive === 0"
              @click="handleEnableConfig(scope.row)"
            >
              <el-icon><VideoPlay /></el-icon> 启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 编辑配置弹窗 -->
    <el-dialog v-model="editConfigVisible" title="编辑配置" width="600px" align-center>
      <el-form :model="configForm" label-width="100px" style="padding: 20px 0">
        <el-form-item label="平台">
          <el-input v-model="configForm.platform" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="configForm.remark" placeholder="请输入备注信息" />
        </el-form-item>
        <el-form-item label="配置(JSON)">
          <el-input
            v-model="configForm.configJson"
            type="textarea"
            :rows="8"
            placeholder="请输入JSON格式配置"
            style="font-family: monospace"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editConfigVisible = false">取消</el-button>
          <el-button type="primary" @click="submitConfig">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传文件" width="500px" align-center destroy-on-close>
      <el-upload
        class="file-upload"
        drag
        action="#"
        :http-request="customUpload"
        :show-file-list="false"
        multiple
        :auto-upload="true"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持任意文件格式，大文件自动分片上传
          </div>
        </template>
      </el-upload>
      <div v-if="uploadProgress > 0 && uploadProgress < 100" style="margin-top: 20px">
        <el-progress :percentage="uploadProgress" :status="uploadStatus === '上传失败' ? 'exception' : ''" />
        <div style="text-align: center; margin-top: 5px; color: var(--el-text-color-secondary)">{{ uploadStatus }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getFileList, deleteFile, getFileConfigList, saveFileConfig, uploadFile, uploadChunk, mergeChunks } from '@/api/sys/file'
import type { FileQuery, FileVO, FileConfigVO, FileConfigUpdate } from '@/api/sys/file'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useClipboard } from '@vueuse/core'
import { UploadFilled, Folder, Monitor, Cloudy, Connection, Search, Upload, Setting, Document, CopyDocument, Delete, Edit, VideoPlay } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { useDict } from '@/hooks/web/useDict'

const { copy } = useClipboard()

const { sys_file_storage_type } = useDict('sys_file_storage_type')

const loading = ref(false)
const total = ref(0)
const tableData = ref<FileVO[]>([])
const configVisible = ref(false)
const editConfigVisible = ref(false)
const uploadVisible = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')
const configList = ref<FileConfigVO[]>([])
const activeMenu = ref('ALL')

const configForm = reactive<FileConfigUpdate>({
  id: undefined,
  platform: '',
  configJson: '',
  isActive: 0,
  remark: ''
})

const queryParams = reactive<FileQuery>({
  pageNum: 1,
  pageSize: 10,
  fileName: '',
  storageType: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getFileList(queryParams)
    const data = res as any
    if (data && data.records) {
      tableData.value = data.records
      total.value = data.total
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error(error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  queryParams.pageNum = 1
  if (index === 'ALL') {
    queryParams.storageType = ''
  } else {
    queryParams.storageType = index
  }
  getList()
}

const handleSearch = () => {
  queryParams.pageNum = 1
  getList()
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

const handleDelete = (row: FileVO) => {
  ElMessageBox.confirm('确认删除该文件吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFile(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}

const handleCopy = async (url: string) => {
  try {
    await copy(url)
    ElMessage.success('复制成功')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const isImage = (suffix: string) => {
  if (!suffix) return false
  const imgTypes = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  return imgTypes.includes(suffix.toLowerCase())
}

const formatSize = (size: number) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / 1024 / 1024).toFixed(2) + ' MB'
  }
}

// Config logic
const handleConfig = async () => {
  configVisible.value = true
  loadConfigList()
}

const loadConfigList = async () => {
  try {
    const res = await getFileConfigList()
    const data = res as any
    // 兼容多种返回结构
    if (Array.isArray(data)) {
      configList.value = data
    } else if (data && Array.isArray(data.data)) {
      configList.value = data.data
    } else if (data && Array.isArray(data.records)) {
      configList.value = data.records
    } else {
      configList.value = []
    }
  } catch (e) {
    console.error(e)
    configList.value = []
  }
}

const handleEditConfig = (row: FileConfigVO) => {
  configForm.id = row.id
  configForm.platform = row.platform
  configForm.configJson = row.configJson
  configForm.isActive = row.isActive
  configForm.remark = row.remark
  editConfigVisible.value = true
}

const handleEnableConfig = (row: FileConfigVO) => {
  ElMessageBox.confirm(`确认启用 ${row.platform} 存储吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await saveFileConfig({
        id: row.id,
        platform: row.platform,
        configJson: row.configJson,
        isActive: 1,
        remark: row.remark
      })
      ElMessage.success('启用成功')
      loadConfigList()
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}

const submitConfig = async () => {
  try {
    // Validate JSON
    try {
      JSON.parse(configForm.configJson)
    } catch (e) {
      ElMessage.error('配置格式必须为有效JSON')
      return
    }

    await saveFileConfig(configForm)
    ElMessage.success('保存成功')
    editConfigVisible.value = false
    loadConfigList()
  } catch (error) {
    console.error(error)
  }
}

const handleUpload = () => {
  uploadVisible.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
}

const customUpload = async (options: any) => {
  const file = options.file
  const chunkSize = 2 * 1024 * 1024 // 2MB

  // 如果文件小于5MB，直接上传
  if (file.size < 5 * 1024 * 1024) {
    uploadStatus.value = '正在上传...'
    try {
      const formData = new FormData()
      formData.append('file', file)
      await uploadFile(formData)
      uploadStatus.value = '上传成功'
      uploadProgress.value = 100
      ElMessage.success('上传成功')
      uploadVisible.value = false
      getList()
    } catch (error) {
      console.error(error)
      uploadStatus.value = '上传失败'
    }
    return
  }

  // 分片上传
  const chunks = Math.ceil(file.size / chunkSize)
  const identifier = file.name + '-' + file.size + '-' + Date.now()

  for (let i = 0; i < chunks; i++) {
    const start = i * chunkSize
    const end = Math.min(file.size, start + chunkSize)
    const chunk = file.slice(start, end)

    const formData = new FormData()
    formData.append('chunk', chunk)
    formData.append('chunkNumber', (i + 1).toString())
    formData.append('totalChunks', chunks.toString())
    formData.append('identifier', identifier)
    formData.append('filename', file.name)

    try {
      await uploadChunk(formData)
      uploadProgress.value = Math.floor(((i + 1) / chunks) * 100)
      uploadStatus.value = `正在上传分片 ${i + 1}/${chunks}`
    } catch (error) {
      console.error(error)
      uploadStatus.value = '上传失败'
      return
    }
  }

  // 合并分片
  uploadStatus.value = '正在合并文件...'
  try {
    await mergeChunks({ identifier, filename: file.name })
    uploadStatus.value = '上传成功'
    ElMessage.success('上传成功')
    uploadVisible.value = false
    getList()
  } catch (error) {
    console.error(error)
    uploadStatus.value = '合并失败'
  }
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.file-layout {
  height: 100%;
  display: flex;

  .file-aside {
    background-color: var(--el-bg-color);
    border-right: 1px solid var(--el-border-color-light);
    margin-right: 16px;

    .aside-card {
      height: 100%;
      border: none;

      :deep(.el-card__body) {
        padding: 0;
        height: 100%;
      }
    }

    .file-menu {
      border-right: none;
      height: 100%;
    }
  }

  .file-main {
    padding: 0;
    height: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .table-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  }
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* 配合全局 search-wrapper 样式，保持 el-form-item 的间距一致 */
  margin-bottom: 18px;
}

:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>

<style lang="scss">
.config-dialog {
  height: 50vh;
  display: flex;
  flex-direction: column;

  .el-dialog__body {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    padding: 20px;

    .el-table {
      flex: 1;
    }
  }
}
</style>
