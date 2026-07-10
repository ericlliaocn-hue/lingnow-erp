<template>
  <div class="app-container erp-master-page">
    <el-card v-if="!isAccountMaster" shadow="never" class="search-wrapper">
      <el-form :inline="true" :model="queryParams" ref="queryFormRef">
        <el-form-item label="编码" prop="code">
          <el-input v-model="queryParams.code" :placeholder="`请输入${config.name}编码`" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="queryParams.name" :placeholder="`请输入${config.name}名称`" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item v-if="config.hasContact" label="联系人" prop="contact">
          <el-input v-model="queryParams.contact" placeholder="请输入联系人" clearable @keyup.enter="handleQuery" />
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

    <el-card shadow="never" class="table-wrapper" :class="{ 'account-table-wrapper': isAccountMaster }">
      <template #header>
        <div class="card-header" :class="{ 'account-card-header': isAccountMaster }">
          <div v-if="!isAccountMaster">
            <strong>{{ config.title }}</strong>
            <span class="muted">{{ isAttributeMaster ? '按规格组维护平铺选项' : '真实数据库资料维护' }}</span>
          </div>
          <div>
            <el-button type="primary" :icon="Plus" :disabled="isAttributeMaster && !activeAttributeGroupId" @click="handleAdd">{{ isAttributeMaster ? '新增选项' : '新增' }}</el-button>
            <el-button v-if="isTreeMaster" type="primary" plain :icon="Plus" :disabled="single" @click="handleAddChild()">新增下级</el-button>
            <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate()">修改</el-button>
            <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-if="isAttributeMaster" v-model="activeAttributeGroupId" class="attribute-tabs">
        <el-tab-pane v-for="group in attributeGroups" :key="group.id" :label="group.name" :name="String(group.id)" />
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        border
        height="100%"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="!isAccountMaster" prop="code" label="编码" min-width="140" />
        <el-table-column v-if="isAttributeMaster" label="属性组" min-width="120">
          <template #default="{ row }">{{ attributeGroupName(row.parentId) }}</template>
        </el-table-column>
        <el-table-column v-if="!isAccountMaster" prop="name" label="名称" min-width="160" />
        <el-table-column v-if="isAttributeMaster" label="额外加钱" width="120" align="right">
          <template #default="{ row }">{{ Number(row.extraAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column v-if="config.hasContact" prop="contact" label="联系人" min-width="120" />
        <el-table-column v-if="config.hasContact" prop="phone" label="联系电话" min-width="140" />
        <el-table-column v-if="config.hasAddress" prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="type === 'account'" prop="accountType" label="账户类型" min-width="120" />
        <el-table-column v-if="type === 'account'" prop="openingBalance" label="期初余额" min-width="120" align="right" />
        <el-table-column v-if="type === 'agent-level'" prop="discountRate" label="折扣率" min-width="120" align="right" />
        <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleUpdate(row)">修改</el-button>
            <el-button v-if="isTreeMaster" link type="primary" :icon="Plus" @click="handleAddChild(row)">下级</el-button>
            <el-button link type="primary" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0 && !isTreeMaster && !isAttributeMaster"
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="open" :title="dialogTitle" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :placeholder="`请输入${config.name}编码`" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" :placeholder="`请输入${config.name}名称`" />
        </el-form-item>
        <el-form-item v-if="isAttributeMaster" label="属性组" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择属性组" style="width: 100%">
            <el-option v-for="group in attributeGroups" :key="group.id" :label="group.name" :value="String(group.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAttributeMaster" label="额外加钱" prop="extraAmount">
          <el-input-number v-model="form.extraAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else-if="isTreeMaster" label="上级分类" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="treeParentOptions"
            check-strictly
            clearable
            filterable
            node-key="id"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            style="width: 100%"
            placeholder="不选则作为顶级分类"
          />
        </el-form-item>
        <el-form-item v-if="config.hasContact" label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item v-if="config.hasContact" label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item v-if="config.hasAddress" label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址">
            <template #append>
              <el-button :icon="Aim" @click="openAddressParse">识别</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="type === 'account'" label="账户类型" prop="accountType">
          <el-select v-model="form.accountType" placeholder="请选择账户类型" style="width: 100%">
            <el-option label="现金" value="cash" />
            <el-option label="银行" value="bank" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="type === 'account'" label="期初余额" prop="openingBalance">
          <el-input-number v-model="form.openingBalance" :precision="2" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="type === 'agent-level'" label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :precision="2" :min="0" :max="100" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addressOpen" title="地址识别" width="640px" append-to-body>
      <el-form label-width="92px">
        <el-form-item label="粘贴内容">
          <el-input v-model="addressRawText" type="textarea" :rows="5" placeholder="粘贴姓名、手机号、完整地址" />
        </el-form-item>
        <el-form-item v-if="addressResult" label="识别结果">
          <el-descriptions :column="1" border style="width: 100%">
            <el-descriptions-item label="联系人">{{ addressResult.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ addressResult.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ addressResult.normalizedAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ addressResult.confidence ?? 0 }}%</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-alert v-if="addressResult?.warnings?.length" :title="addressResult.warnings.join('，')" type="warning" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="addressOpen = false">取消</el-button>
        <el-button :loading="addressLoading" @click="doParseAddress">识别</el-button>
        <el-button type="primary" :disabled="!addressResult" @click="applyAddress">确认回填</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination/index.vue'
import { addMaster, deleteMaster, getMaster, listMaster, updateMaster, type ErpMasterForm, type ErpMasterQuery, type ErpMasterVO, type MasterType } from '@/api/erp/master'
import { parseAddress, type AddressParseResult } from '@/api/erp/address'

const route = useRoute()
const pathTypeMap: Record<string, MasterType> = {
  '/erp/product/category': 'product-category',
  '/erp/product/unit': 'unit',
  '/erp/product/brand': 'product-brand',
  '/erp/product/attribute': 'product-attribute',
  '/erp/setting/customer': 'customer',
  '/erp/setting/supplier': 'supplier',
  '/erp/setting/warehouse': 'warehouse',
  '/erp/setting/account': 'account',
  '/erp/setting/agent-level': 'agent-level'
}
const type = computed(() => pathTypeMap[route.path] || 'unit')

const configs: Record<MasterType, { title: string; name: string; hasContact?: boolean; hasAddress?: boolean }> = {
  'product-category': { title: '商品分类', name: '分类' },
  unit: { title: '单位管理', name: '单位' },
  'product-brand': { title: '商品品牌', name: '品牌' },
  'product-attribute': { title: '商品属性', name: '属性' },
  customer: { title: '客户管理', name: '客户', hasContact: true, hasAddress: true },
  supplier: { title: '供应商管理', name: '供应商', hasContact: true, hasAddress: true },
  warehouse: { title: '仓库管理', name: '仓库', hasContact: true, hasAddress: true },
  account: { title: '账户管理', name: '账户' },
  'agent-level': { title: '代理等级', name: '代理等级' }
}
const config = computed(() => configs[type.value] || configs.unit)
const isAccountMaster = computed(() => type.value === 'account')
const isAttributeMaster = computed(() => type.value === 'product-attribute')

const queryFormRef = ref()
const formRef = ref()
const loading = ref(false)
const total = ref(0)
const list = ref<ErpMasterVO[]>([])
const open = ref(false)
const dialogTitle = ref('')
const ids = ref<string[]>([])
const single = ref(true)
const multiple = ref(true)
const addressOpen = ref(false)
const addressRawText = ref('')
const addressResult = ref<AddressParseResult>()
const addressLoading = ref(false)
const activeAttributeGroupId = ref('')

const state = reactive({
  queryParams: {
    current: 1,
    size: 10,
    code: undefined,
    name: undefined,
    status: undefined,
    contact: undefined,
    phone: undefined
  } as ErpMasterQuery,
  form: {} as ErpMasterForm,
  rules: {
    code: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(state)
const isProductCategory = computed(() => type.value === 'product-category')
const isTreeMaster = computed(() => isProductCategory.value)
const attributeGroups = computed(() => list.value.filter(item => String(item.parentId || '0') === '0'))
const attributeOptions = computed(() => {
  const groupId = String(activeAttributeGroupId.value || '')
  if (!groupId) return list.value.filter(item => String(item.parentId || '0') !== '0')
  return list.value.filter(item => String(item.parentId || '0') === groupId)
})
const tableData = computed(() => {
  if (isAttributeMaster.value) return attributeOptions.value
  return isTreeMaster.value ? buildTree(list.value) : list.value
})
const treeParentOptions = computed(() => [{ id: '0', code: 'ROOT', name: isProductCategory.value ? '商品' : '根节点', sortOrder: 0, status: 1, children: buildTree(list.value) } as ErpMasterVO])

function getList() {
  loading.value = true
  const params = (isTreeMaster.value || isAttributeMaster.value) ? { ...queryParams.value, current: 1, size: 1000 } : queryParams.value
  listMaster(type.value, params).then(res => {
    list.value = res.records
    total.value = Number(res.total)
    const groupId = String(activeAttributeGroupId.value || '')
    if (isAttributeMaster.value && !attributeGroups.value.some(item => String(item.id) === groupId)) {
      activeAttributeGroupId.value = String(attributeGroups.value[0]?.id || '')
    }
  }).finally(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: undefined,
    code: '',
    name: '',
    parentId: '0',
    contact: '',
    phone: '',
    address: '',
    levelId: undefined,
    accountType: type.value === 'account' ? 'cash' : undefined,
    openingBalance: type.value === 'account' ? 0 : undefined,
    discountRate: type.value === 'agent-level' ? 100 : undefined,
    extraAmount: isAttributeMaster.value ? 0 : undefined,
    sortOrder: 0,
    status: 1,
    remark: ''
  }
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

function handleSelectionChange(selection: ErpMasterVO[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

function handleAdd() {
  reset()
  if (isAttributeMaster.value) {
    form.value.parentId = activeAttributeGroupId.value
  }
  dialogTitle.value = `新增${config.value.name}`
  open.value = true
}

function handleAddChild(row?: ErpMasterVO) {
  const parent = row || list.value.find(item => item.id === ids.value[0])
  reset()
  form.value.parentId = parent?.id || '0'
  dialogTitle.value = `新增${config.value.name}`
  open.value = true
}

function openAddressParse() {
  addressRawText.value = [form.value.contact, form.value.phone, form.value.address].filter(Boolean).join(' ')
  addressResult.value = undefined
  addressOpen.value = true
}

function doParseAddress() {
  if (!addressRawText.value.trim()) {
    ElMessage.warning('请先粘贴需要识别的地址内容')
    return
  }
  addressLoading.value = true
  parseAddress(addressRawText.value).then(res => {
    addressResult.value = res
  }).finally(() => addressLoading.value = false)
}

function applyAddress() {
  if (!addressResult.value) return
  if (addressResult.value.contactName) form.value.contact = addressResult.value.contactName
  if (addressResult.value.phone) form.value.phone = addressResult.value.phone
  if (addressResult.value.normalizedAddress) form.value.address = addressResult.value.normalizedAddress
  addressOpen.value = false
}

function handleUpdate(row?: ErpMasterVO) {
  reset()
  const id = row?.id || ids.value[0]
  getMaster(type.value, id).then(res => {
    form.value = { ...res, parentId: res.parentId || '0', extraAmount: isAttributeMaster.value ? Number(res.extraAmount || 0) : res.extraAmount }
    dialogTitle.value = `修改${config.value.name}`
    open.value = true
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (isAttributeMaster.value && !form.value.parentId) {
      ElMessage.warning('请选择属性组')
      return
    }
    if (!form.value.parentId) {
      form.value.parentId = '0'
    }
    if (form.value.id && form.value.parentId === form.value.id) {
      ElMessage.warning('上级节点不能选择自己')
      return
    }
    const action = form.value.id ? updateMaster(type.value, form.value) : addMaster(type.value, form.value)
    action.then(() => {
      ElMessage.success('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row?: ErpMasterVO) {
  const deleteIds = row?.id ? [row.id] : ids.value
  ElMessageBox.confirm(`确定删除选中的${config.value.name}吗？已被业务引用的数据不能删除，可改为停用。`, '提示', { type: 'warning' })
    .then(() => deleteMaster(type.value, deleteIds))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
}

function cancel() {
  open.value = false
  reset()
}

watch(() => route.path, () => {
  queryParams.value.current = 1
  ids.value = []
  single.value = true
  multiple.value = true
  activeAttributeGroupId.value = ''
  getList()
})

watch(activeAttributeGroupId, () => {
  ids.value = []
  single.value = true
  multiple.value = true
})

onMounted(getList)

function buildTree(records: ErpMasterVO[]) {
  const map = new Map<string, ErpMasterVO & { children: ErpMasterVO[] }>()
  records.forEach(item => map.set(item.id, { ...item, children: [] }))
  const roots: (ErpMasterVO & { children: ErpMasterVO[] })[] = []
  map.forEach(item => {
    const parentId = String(item.parentId || '0')
    const parent = map.get(parentId)
    if (parent && parent.id !== item.id) {
      parent.children.push(item)
    } else {
      roots.push(item)
    }
  })
  const sort = (items: (ErpMasterVO & { children: ErpMasterVO[] })[]) => {
    items.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
    items.forEach(item => sort(item.children as (ErpMasterVO & { children: ErpMasterVO[] })[]))
  }
  sort(roots)
  return roots
}

function attributeGroupName(parentId?: string) {
  const groupId = String(parentId || '0')
  return attributeGroups.value.find(item => String(item.id) === groupId)?.name || '-'
}
</script>

<style scoped>
.erp-master-page {
  min-height: 100%;
}

.search-wrapper {
  margin-bottom: 16px;
}

.table-wrapper {
  height: calc(100vh - 230px);
}

.account-table-wrapper {
  height: calc(100vh - 150px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.account-card-header {
  justify-content: flex-end;
}

.muted {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 400;
}

.attribute-tabs {
  margin-bottom: 12px;
}
</style>
