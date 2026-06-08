<template>
  <div class="app-container product-page">
    <el-card shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="编号" prop="code">
          <el-input v-model="queryParams.code" placeholder="请输入商品编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="queryParams.name" placeholder="请输入商品名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="queryParams.barcode" placeholder="请输入条码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
          <strong>商品管理</strong>
          <div>
            <el-button type="primary" :icon="Plus" @click="handleAdd" v-permission="'erp:product:add'">新增</el-button>
            <el-button @click="handleImport" v-permission="'erp:product:import'">导入</el-button>
            <el-button @click="handleTemplate" v-permission="'erp:product:import'">模板下载</el-button>
            <el-button @click="handleExport" v-permission="'erp:product:export'">导出</el-button>
            <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate()" v-permission="'erp:product:edit'">修改</el-button>
            <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete()" v-permission="'erp:product:remove'">删除</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange" border height="100%">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="code" label="编号" min-width="120" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column prop="brandName" label="品牌" min-width="120" />
        <el-table-column prop="unitName" label="单位" min-width="90" />
        <el-table-column prop="barcode" label="条码" min-width="130" />
        <el-table-column prop="purchasePrice" label="采购价" min-width="100" align="right" />
        <el-table-column prop="salePrice" label="销售价" min-width="100" align="right" />
        <el-table-column prop="minStock" label="最低库存" min-width="100" align="right" />
        <el-table-column prop="maxStock" label="最高库存" min-width="100" align="right" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleUpdate(row)" v-permission="'erp:product:edit'">修改</el-button>
            <el-button link type="primary" :icon="Delete" @click="handleDelete(row)" v-permission="'erp:product:remove'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.current" v-model:limit="queryParams.size" @pagination="getList" />
    </el-card>

    <el-dialog v-model="open" :title="dialogTitle" width="840px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="编号" prop="code"><el-input v-model="form.code" placeholder="请输入商品编号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="请输入商品名称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.spec" placeholder="请输入规格" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="条码"><el-input v-model="form.barcode" placeholder="请输入条码" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" clearable filterable style="width: 100%" placeholder="请选择分类">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无分类</span>
                    <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('product-category')">新增分类</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('product-category')">新增分类</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌">
              <el-select v-model="form.brandId" clearable filterable style="width: 100%" placeholder="请选择品牌">
                <el-option v-for="item in brands" :key="item.id" :label="item.name" :value="item.id" />
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无品牌</span>
                    <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('product-brand')">新增品牌</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('product-brand')">新增品牌</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-select v-model="form.unitId" clearable filterable style="width: 100%" placeholder="请选择单位">
                <el-option v-for="item in units" :key="item.id" :label="item.name" :value="item.id" />
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无单位</span>
                    <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('unit')">新增单位</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" @click.stop="openQuickMaster('unit')">新增单位</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="辅助属性"><el-input v-model="form.attributeText" placeholder="如颜色、尺码" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="货位"><el-input v-model="form.location" placeholder="请输入货位" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="采购价"><el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="销售价"><el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="零售价"><el-input-number v-model="form.retailPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最低库存"><el-input-number v-model="form.minStock" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最高库存"><el-input-number v-model="form.maxStock" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="商品图片">
              <div class="product-image-field">
                <el-upload
                  ref="productUploadRef"
                  class="product-image-uploader"
                  accept="image/*"
                  :show-file-list="false"
                  :http-request="uploadProductImage"
                >
                  <div class="product-image-box" :class="{ 'has-image': !!form.imageUrl }">
                    <img v-if="form.imageUrl" :src="form.imageUrl" class="product-image-preview" />
                    <div v-else class="product-image-placeholder">
                      <el-icon><UploadFilled /></el-icon>
                      <span>上传图片</span>
                    </div>
                    <div v-if="form.imageUrl" class="product-image-overlay">
                      <el-tooltip content="预览" placement="top">
                        <el-button circle size="small" :icon="View" @click.stop="previewImage" />
                      </el-tooltip>
                      <el-tooltip content="替换" placement="top">
                        <el-button circle size="small" :icon="RefreshRight" @click.stop="triggerProductImageUpload" />
                      </el-tooltip>
                    </div>
                  </div>
                </el-upload>
                <el-tooltip content="添加图片" placement="top">
                  <el-button class="product-image-add" circle :icon="Plus" @click="triggerProductImageUpload" />
                </el-tooltip>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submitForm" v-permission="form.id ? 'erp:product:edit' : 'erp:product:add'">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickMasterOpen" :title="`新增${quickMasterMeta.name}`" width="420px" append-to-body destroy-on-close>
      <el-form ref="quickMasterFormRef" :model="quickMasterForm" :rules="quickMasterRules" label-width="76px">
        <el-form-item label="编号" prop="code">
          <el-input v-model="quickMasterForm.code" placeholder="请输入编号" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickMasterForm.name" :placeholder="`请输入${quickMasterMeta.name}名称`" @input="syncQuickMasterCode" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="quickMasterForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickMasterOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickMasterSaving" @click="submitQuickMaster">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importOpen" title="导入商品" width="560px" append-to-body>
      <el-upload drag :auto-upload="false" :limit="1" accept=".csv" :on-change="onImportFile" :on-remove="onRemoveImportFile">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 CSV 文件到此处，或点击选择</div>
        <template #tip><div class="el-upload__tip">请先下载模板，按字段填写后上传。</div></template>
      </el-upload>
      <el-alert v-if="importResult" class="import-result" :title="`成功 ${importResult.success} 条，失败 ${importResult.fail} 条`" type="info" :closable="false" />
      <ul v-if="importResult?.errors?.length" class="import-errors">
        <li v-for="item in importResult.errors" :key="item">{{ item }}</li>
      </ul>
      <template #footer>
        <el-button @click="importOpen = false">关闭</el-button>
        <el-button type="primary" :disabled="!importFile" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imagePreviewOpen" title="图片预览" width="720px" append-to-body destroy-on-close>
      <div class="product-image-preview-dialog">
        <img v-if="imagePreviewUrl" :src="imagePreviewUrl" alt="商品图片预览" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, RefreshRight, Search, UploadFilled, View } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { addProduct, deleteProduct, downloadProductTemplate, exportProduct, getProduct, importProduct, listProduct, updateProduct, type ErpProduct, type ErpProductQuery } from '@/api/erp/product'
import { addMaster, listMaster, type ErpMasterForm, type ErpMasterVO, type MasterType } from '@/api/erp/master'
import { uploadFile } from '@/api/sys/file'
import { downloadBlob } from '@/utils/download'

const queryFormRef = ref()
const formRef = ref()
const loading = ref(false)
const total = ref(0)
const list = ref<ErpProduct[]>([])
const open = ref(false)
const dialogTitle = ref('')
const ids = ref<string[]>([])
const single = ref(true)
const multiple = ref(true)
const categories = ref<ErpMasterVO[]>([])
const brands = ref<ErpMasterVO[]>([])
const units = ref<ErpMasterVO[]>([])
const importOpen = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<{ success: number; fail: number; errors: string[] } | null>(null)
const quickMasterOpen = ref(false)
const quickMasterSaving = ref(false)
const quickMasterType = ref<MasterType>('product-category')
const quickMasterFormRef = ref()
const quickMasterForm = reactive<ErpMasterForm>({ code: '', name: '', sortOrder: 0, status: 1 })
const quickMasterRules = {
  code: [{ required: true, message: '编号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
}
const quickMasterConfig = {
  'product-category': { name: '分类', prefix: 'CAT', target: 'categoryId', list: categories },
  'product-brand': { name: '品牌', prefix: 'BRAND', target: 'brandId', list: brands },
  unit: { name: '单位', prefix: 'UNIT', target: 'unitId', list: units }
} as const
const quickMasterMeta = computed(() => quickMasterConfig[quickMasterType.value as keyof typeof quickMasterConfig])
const productUploadRef = ref<any>()
const imagePreviewOpen = ref(false)
const imagePreviewUrl = ref('')

const state = reactive({
  queryParams: { current: 1, size: 10 } as ErpProductQuery,
  form: {} as ErpProduct,
  rules: {
    code: [{ required: true, message: '商品编号不能为空', trigger: 'blur' }],
    name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(state)

function getList() {
  loading.value = true
  listProduct(queryParams.value).then(res => {
    list.value = res.records
    total.value = Number(res.total)
  }).finally(() => loading.value = false)
}

function loadOptions() {
  return Promise.all([
    listMaster('product-category', { current: 1, size: 200 }).then(res => categories.value = res.records),
    listMaster('product-brand', { current: 1, size: 200 }).then(res => brands.value = res.records),
    listMaster('unit', { current: 1, size: 200 }).then(res => units.value = res.records)
  ])
}

function reset() {
  form.value = { code: '', name: '', purchasePrice: 0, salePrice: 0, retailPrice: 0, minStock: 0, maxStock: 0, sortOrder: 0, status: 1 }
  formRef.value?.resetFields()
}

function handleQuery() {
  queryParams.value.current = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields()
  handleQuery()
}

function handleSelectionChange(selection: ErpProduct[]) {
  ids.value = selection.map(item => item.id!)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

function handleAdd() {
  reset()
  dialogTitle.value = '新增商品'
  open.value = true
}

function handleUpdate(row?: ErpProduct) {
  reset()
  const id = row?.id || ids.value[0]
  getProduct(id!).then(res => {
    form.value = res
    dialogTitle.value = '修改商品'
    open.value = true
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const action = form.value.id ? updateProduct(form.value) : addProduct(form.value)
    action.then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

async function uploadProductImage(options: any) {
  const file = options.file as File
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    options.onError?.(new Error('请选择图片文件'))
    return
  }
  const data = new FormData()
  data.append('file', file)
  try {
    const url = await uploadFile(data) as string
    form.value.imageUrl = url
    ElMessage.success('图片上传成功')
    options.onSuccess?.(url)
  } catch (error) {
    options.onError?.(error)
  }
}

function previewImage() {
  if (!form.value.imageUrl) return
  imagePreviewUrl.value = form.value.imageUrl
  imagePreviewOpen.value = true
}

function triggerProductImageUpload() {
  const input = productUploadRef.value?.$el?.querySelector('input[type="file"]') as HTMLInputElement | null
  input?.click()
}

function openQuickMaster(type: 'product-category' | 'product-brand' | 'unit') {
  quickMasterType.value = type
  quickMasterForm.code = `${quickMasterConfig[type].prefix}_${Date.now().toString().slice(-8)}`
  quickMasterForm.name = ''
  quickMasterForm.sortOrder = 0
  quickMasterForm.status = 1
  quickMasterOpen.value = true
}

function syncQuickMasterCode() {
  if (quickMasterForm.code) return
  quickMasterForm.code = `${quickMasterMeta.value.prefix}_${Date.now().toString().slice(-8)}`
}

async function submitQuickMaster() {
  const valid = await quickMasterFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickMasterSaving.value = true
  try {
    await addMaster(quickMasterType.value, quickMasterForm)
    await loadOptions()
    const created = quickMasterMeta.value.list.value.find(item => item.code === quickMasterForm.code || item.name === quickMasterForm.name)
    if (created) {
      ;(form.value as any)[quickMasterMeta.value.target] = created.id
    }
    ElMessage.success(`${quickMasterMeta.value.name}新增成功`)
    quickMasterOpen.value = false
  } finally {
    quickMasterSaving.value = false
  }
}

function handleDelete(row?: ErpProduct) {
  const deleteIds = row?.id ? [row.id] : ids.value
  ElMessageBox.confirm('确定删除选中的商品吗？', '提示', { type: 'warning' })
    .then(() => deleteProduct(deleteIds))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
}

function handleExport() {
  exportProduct(queryParams.value).then(blob => downloadBlob(blob, '商品管理.csv'))
}

function handleTemplate() {
  downloadProductTemplate().then(blob => downloadBlob(blob, '商品导入模板.csv'))
}

function handleImport() {
  importOpen.value = true
  importFile.value = null
  importResult.value = null
}

function onImportFile(file: any) {
  importFile.value = file.raw
}

function onRemoveImportFile() {
  importFile.value = null
}

function submitImport() {
  if (!importFile.value) return
  const data = new FormData()
  data.append('file', importFile.value)
  importProduct(data).then(res => {
    importResult.value = res
    ElMessage.success('导入处理完成')
    getList()
  })
}

function cancel() {
  open.value = false
  reset()
}

onMounted(() => {
  loadOptions()
  getList()
})
</script>

<style scoped>
.search-wrapper { margin-bottom: 16px; }
.table-wrapper { height: calc(100vh - 230px); }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.product-image-field { display: flex; align-items: flex-start; gap: 14px; width: 100%; }
.product-image-uploader { flex: 0 0 auto; }
.product-image-uploader :deep(.el-upload) { display: block; }
.product-image-uploader :deep(.el-upload) {
  width: 86px;
  height: 86px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
}
.product-image-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}
.product-image-box {
  position: relative;
  width: 86px;
  height: 86px;
}
.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.product-image-placeholder .el-icon {
  font-size: 22px;
}
.product-image-preview {
  display: block;
  width: 86px;
  height: 86px;
  object-fit: cover;
}
.product-image-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.35);
  opacity: 0;
  transition: opacity 0.15s ease;
}
.product-image-box.has-image:hover .product-image-overlay {
  opacity: 1;
}
.product-image-add {
  margin-top: 26px;
}
.product-image-preview-dialog {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  max-height: 70vh;
  overflow: auto;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 12px;
}
.product-image-preview-dialog img {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
.select-empty-action {
  min-height: 42px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
}
.import-result { margin-top: 16px; }
.import-errors { margin: 12px 0 0; padding-left: 18px; color: var(--el-color-danger); max-height: 180px; overflow: auto; }
</style>
