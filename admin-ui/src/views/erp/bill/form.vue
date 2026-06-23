<template>
  <div class="app-container bill-form-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>{{ title }}</strong>
          <div>
            <el-button @click="back">返回</el-button>
            <el-button type="primary" :disabled="readonly" @click="submit(false)" v-permission="form.id ? `erp:${module}:edit` : `erp:${module}:add`">保存</el-button>
            <el-button type="warning" :disabled="readonly" @click="submit(true)" v-permission="`erp:${module}:audit`">保存并提交</el-button>
          </div>
        </div>
      </template>
        <el-alert v-if="readonly" title="已审核单据只能查看，需反审核后才能修改。" type="warning" :closable="false" show-icon class="readonly-alert" />
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" :disabled="readonly">
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="单号"><el-input v-model="form.billNo" placeholder="自动生成" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="日期" prop="billDate"><el-date-picker v-model="form.billDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item :label="partnerLabel" prop="partnerId">
              <el-select v-model="form.partnerId" filterable style="width: 100%" :placeholder="`请选择${partnerLabel}`">
                <el-option v-for="item in partners" :key="item.id" :label="item.name" :value="item.id" />
                <template v-if="showReceiver" #empty>
                  <div class="select-empty-action">
                    <span>暂无客户</span>
                    <el-button link type="primary" :icon="Plus" @click.stop="openQuickCustomer">新增客户</el-button>
                  </div>
                </template>
                <template v-if="showReceiver" #footer>
                  <el-button link type="primary" :icon="Plus" @click.stop="openQuickCustomer">新增客户</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" filterable style="width: 100%" placeholder="请选择仓库">
                <el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" />
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无仓库</span>
                    <el-button link type="primary" :icon="Plus" @click.stop="openQuickWarehouse">新增仓库</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" @click.stop="openQuickWarehouse">新增仓库</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="账户"><el-select v-model="form.accountId" clearable filterable style="width: 100%"><el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="付款方式" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" clearable style="width: 100%" placeholder="请选择付款方式">
                <el-option v-for="item in paymentMethods" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="付款金额"><el-input-number v-model="form.paidAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="整单优惠"><el-input-number v-model="form.discountAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="其他费用"><el-input-number v-model="form.otherAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <template v-if="showReceiver">
          <el-divider />
          <div class="section-title">
            <strong>收货信息</strong>
            <el-button :icon="Aim" :disabled="readonly" @click="openAddressParse">地址识别</el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="6"><el-form-item label="收货人"><el-input v-model="form.receiverName" placeholder="请输入收货人" /></el-form-item></el-col>
            <el-col :span="6"><el-form-item label="收货电话"><el-input v-model="form.receiverPhone" placeholder="请输入收货电话" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="收货地址"><el-input v-model="form.receiverAddress" placeholder="请输入收货地址" /></el-form-item></el-col>
          </el-row>
        </template>
        <el-divider />
        <div class="toolbar"><el-button type="primary" :disabled="readonly" @click="addRow">添加商品</el-button></div>
        <div class="bill-item-list">
          <div v-for="(row, index) in form.items" :key="row.id || index" class="bill-item-card">
            <div class="bill-item-head">
              <span>商品 {{ index + 1 }}</span>
              <el-button link type="primary" :disabled="readonly" @click="removeRow(index)">删除</el-button>
            </div>
            <div class="bill-item-product">
              <el-image
                v-if="row.productImageUrl"
                class="bill-product-image"
                :src="row.productImageUrl"
                :preview-src-list="[row.productImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else class="bill-product-placeholder">无图</span>
              <el-form-item label="商品" class="bill-item-product-select" required>
                <el-select
                  v-model="row.productId"
                  filterable
                  remote
                  reserve-keyword
                  :remote-method="(keyword: string) => loadRowProducts(row, keyword)"
                  placeholder="搜索商品编号 / 名称 / 条码"
                  style="width: 100%"
                  @visible-change="(visible: boolean) => visible && loadRowProducts(row)"
                  @change="productChanged(row)"
                >
                  <el-option v-for="item in rowProductOptions(row)" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id">
                    <div class="product-option">
                      <img v-if="item.imageUrl" :src="item.imageUrl" class="product-option-image" />
                      <span v-else class="product-option-empty">无图</span>
                      <div class="product-option-text">
                        <strong>{{ item.code }} {{ item.name }}</strong>
                        <span>{{ [item.spec, item.attributeText].filter(Boolean).join(' / ') || '暂无规格属性' }}</span>
                      </div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </div>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="商品分类">
                  <el-tree-select
                    v-model="row.categoryPickerId"
                    :data="categoryPickerTree"
                    clearable
                    filterable
                    check-strictly
                    node-key="id"
                    :props="{ label: 'name', value: 'id', children: 'children' }"
                    placeholder="选择商品分类"
                    style="width: 100%"
                    @change="categoryPathChanged(row)"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="6"><el-form-item label="规格"><el-input v-model="row.spec" disabled placeholder="自动带出" /></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="数量"><el-input-number v-model="row.qty" :min="0" :precision="2" style="width: 100%" @change="calc" /></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="单价"><el-input-number v-model="row.price" :min="0" :precision="2" style="width: 100%" @change="calc" /></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="金额"><el-input :model-value="Number(row.amount || 0).toFixed(2)" disabled /></el-form-item></el-col>
            </el-row>
            <div v-if="rowLinePath(row)" class="bill-item-path">{{ rowLinePath(row) }}</div>
          </div>
          <el-empty v-if="!form.items.length" description="暂无商品明细" />
        </div>
        <div class="totals">
          <span>总数量：{{ totalQty }}</span>
          <span>总金额：{{ totalAmount }}</span>
          <span>应收/应付：{{ payableAmount }}</span>
          <span>付款金额：{{ Number(form.paidAmount || 0).toFixed(2) }}</span>
          <span>欠款：{{ debtAmount }}</span>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="addressOpen" title="地址识别" width="640px" append-to-body>
      <el-form label-width="92px">
        <el-form-item label="粘贴内容">
          <el-input v-model="addressRawText" type="textarea" :rows="5" placeholder="粘贴姓名、手机号、完整地址" />
        </el-form-item>
        <el-form-item v-if="addressResult" label="识别结果">
          <el-descriptions :column="1" border style="width: 100%">
            <el-descriptions-item label="收货人">{{ addressResult.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货电话">{{ addressResult.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址">{{ addressResult.normalizedAddress || '-' }}</el-descriptions-item>
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

    <el-dialog v-model="quickCustomerOpen" title="新增客户" width="720px" append-to-body destroy-on-close>
      <el-form ref="quickCustomerFormRef" :model="quickCustomerForm" :rules="quickCustomerRules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="编码" prop="code">
              <el-input v-model="quickCustomerForm.code" placeholder="请输入客户编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="quickCustomerForm.name" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="quickCustomerForm.contact" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="quickCustomerForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="quickCustomerForm.address" placeholder="请输入地址">
                <template #append>
                  <el-button :icon="Aim" @click="openQuickCustomerAddressParse">识别</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="quickCustomerForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="quickCustomerForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="quickCustomerForm.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="quickCustomerOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickCustomerSaving" @click="submitQuickCustomer">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickCustomerAddressOpen" title="地址识别" width="640px" append-to-body>
      <el-form label-width="92px">
        <el-form-item label="粘贴内容">
          <el-input v-model="quickCustomerAddressRawText" type="textarea" :rows="5" placeholder="粘贴姓名、手机号、完整地址" />
        </el-form-item>
        <el-form-item v-if="quickCustomerAddressResult" label="识别结果">
          <el-descriptions :column="1" border style="width: 100%">
            <el-descriptions-item label="联系人">{{ quickCustomerAddressResult.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ quickCustomerAddressResult.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ quickCustomerAddressResult.normalizedAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ quickCustomerAddressResult.confidence ?? 0 }}%</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-alert v-if="quickCustomerAddressResult?.warnings?.length" :title="quickCustomerAddressResult.warnings.join('，')" type="warning" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="quickCustomerAddressOpen = false">取消</el-button>
        <el-button :loading="quickCustomerAddressLoading" @click="doParseQuickCustomerAddress">识别</el-button>
        <el-button type="primary" :disabled="!quickCustomerAddressResult" @click="applyQuickCustomerAddress">确认回填</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickWarehouseOpen" title="新增仓库" width="420px" append-to-body destroy-on-close>
      <el-form ref="quickWarehouseFormRef" :model="quickWarehouseForm" :rules="quickWarehouseRules" label-width="76px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickWarehouseForm.name" placeholder="请输入仓库名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickWarehouseOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickWarehouseSaving" @click="submitQuickWarehouse">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Plus } from '@element-plus/icons-vue'
import { addBill, getBill, listBill, nextBillNo, updateBill, type BillItem, type BillModule, type ErpBill } from '@/api/erp/bill'
import { submitApproval, type ApprovalBizType } from '@/api/erp/approval'
import { productOptions, type ErpProduct } from '@/api/erp/product'
import { addMaster, listMaster, type ErpMasterForm, type ErpMasterVO } from '@/api/erp/master'
import { parseAddress, type AddressParseResult } from '@/api/erp/address'

const route = useRoute()
const router = useRouter()
const module = computed<BillModule>(() => {
  if (route.path.includes('/sale-return')) return 'sale-return'
  if (route.path.includes('/purchase-return')) return 'purchase-return'
  return route.path.includes('/purchase') ? 'purchase' : 'sale'
})
const titleMap: Record<BillModule, string> = { sale: '新增销售单', 'sale-return': '新增销售退货', purchase: '新增进货单', 'purchase-return': '新增进货退货' }
const title = computed(() => titleMap[module.value])
const partnerLabel = computed(() => module.value.startsWith('sale') ? '客户' : '供应商')
const showReceiver = computed(() => module.value.startsWith('sale'))
const bizType = computed<ApprovalBizType>(() => {
  const map: Record<BillModule, ApprovalBizType> = {
    sale: 'SALE',
    'sale-return': 'SALE_RETURN',
    purchase: 'PURCHASE',
    'purchase-return': 'PURCHASE_RETURN'
  }
  return map[module.value]
})
const formRef = ref()
const products = ref<ErpProduct[]>([])
const categories = ref<ErpMasterVO[]>([])
const partners = ref<ErpMasterVO[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const addressOpen = ref(false)
const addressRawText = ref('')
const addressResult = ref<AddressParseResult>()
const addressLoading = ref(false)
const quickCustomerOpen = ref(false)
const quickCustomerSaving = ref(false)
const quickCustomerFormRef = ref()
const quickCustomerAddressOpen = ref(false)
const quickCustomerAddressRawText = ref('')
const quickCustomerAddressResult = ref<AddressParseResult>()
const quickCustomerAddressLoading = ref(false)
const quickWarehouseOpen = ref(false)
const quickWarehouseSaving = ref(false)
const quickWarehouseFormRef = ref()
const quickCustomerForm = reactive<ErpMasterForm>({
  code: '',
  name: '',
  parentId: '0',
  contact: '',
  phone: '',
  address: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})
const quickCustomerRules = {
  code: [{ required: true, message: '客户编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }]
}
const quickWarehouseForm = reactive<ErpMasterForm>({
  code: '',
  name: '',
  parentId: '0',
  sortOrder: 0,
  status: 1,
  remark: ''
})
const quickWarehouseRules = {
  name: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
}
const paymentMethods = ['淘宝', '1688', '小红书', '微信', '支付宝']
const readonly = computed(() => form.value.auditStatus === 1)
const state = reactive({
  form: { billDate: new Date().toISOString().slice(0, 10), partnerId: '', warehouseId: '', paymentMethod: '', paidAmount: 0, discountAmount: 0, otherAmount: 0, items: [] } as ErpBill,
  rules: {
    billDate: [{ required: true, message: '日期不能为空', trigger: 'change' }],
    partnerId: [{ required: true, message: '往来单位不能为空', trigger: 'change' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'change' }],
    paymentMethod: [{
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (Number(form.value.paidAmount || 0) > 0 && !value) {
          callback(new Error('请选择付款方式'))
          return
        }
        callback()
      },
      trigger: 'change'
    }]
  }
})
const { form, rules } = toRefs(state)
const totalQty = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.qty || 0), 0).toFixed(2))
const totalAmount = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.amount || 0), 0).toFixed(2))
const payableAmount = computed(() => (Number(totalAmount.value) - Number(form.value.discountAmount || 0) + Number(form.value.otherAmount || 0)).toFixed(2))
const debtAmount = computed(() => (Number(payableAmount.value) - Number(form.value.paidAmount || 0)).toFixed(2))
const categoryTree = computed(() => buildTree(categories.value))
const visibleCategoryTree = computed(() => {
  const productRoot = categoryTree.value.find(item => item.name === '商品')
  return productRoot?.children?.length ? productRoot.children : categoryTree.value
})
const categoryPickerTree = computed(() => visibleCategoryTree.value)

function loadOptions() {
  productOptions().then(res => products.value = res)
  listMaster('product-category', { current: 1, size: 1000 }).then(res => categories.value = res.records)
  loadPartners()
  loadWarehouses()
  listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records)
}
function loadPartners() {
  return listMaster(module.value.startsWith('sale') ? 'customer' : 'supplier', { current: 1, size: 200 }).then(res => {
    partners.value = res.records
    return res.records
  })
}
function loadWarehouses() {
  return listMaster('warehouse', { current: 1, size: 200 }).then(res => {
    warehouses.value = res.records
    return res.records
  })
}
function loadData() {
  const id = route.query.id as string
  if (id) {
    getBill(module.value, id).then(res => {
      form.value = { ...res, items: (res.items || []).map(prepareBillItem) }
      form.value.items.forEach(row => loadRowProducts(row))
    })
  } else {
    nextBillNo(module.value).then(no => form.value.billNo = no)
  }
}
function addRow() {
  const row = prepareBillItem({ productId: '', qty: 1, price: 0 } as BillItem)
  form.value.items.push(row)
  loadRowProducts(row)
}
function prepareBillItem(row: BillItem) {
  row.categoryPickerId = splitIds(row.optionAttributeIds).slice(-1)[0] || row.categoryLevel2Id || row.categoryLevel1Id
  if (!row.categoryLevel1Id && row.categoryLevel2Id) {
    const parent = parentCategory(row.categoryLevel2Id)
    row.categoryLevel1Id = parent?.id
    row.categoryLevel1Name = parent?.name
  }
  return row
}
function rowProductOptions(row: BillItem) {
  return Array.isArray(row.optionProducts) ? row.optionProducts : products.value
}
function loadRowProducts(row: BillItem, keyword = '') {
  const params = {
    ...(keyword ? { keyword } : {})
  }
  productOptions(params).then(res => row.optionProducts = res)
}
function clearRowProduct(row: BillItem) {
  row.productId = ''
  row.productCode = undefined
  row.productName = undefined
  row.productImageUrl = undefined
  row.spec = undefined
  row.unitId = undefined
  row.unitName = undefined
  row.price = 0
  row.amount = 0
  calc()
}
function productChanged(row: BillItem) {
  const product = rowProductOptions(row).find(item => item.id === row.productId) || products.value.find(item => item.id === row.productId)
  if (!product) return
  row.productCode = product.code
  row.productName = product.name
  row.productImageUrl = product.imageUrl
  row.spec = product.spec
  row.unitId = product.unitId
  row.price = module.value.startsWith('sale') ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0)
  applyProductDefaultCategory(row, product)
  syncRowSnapshot(row)
  calc()
}
function removeRow(index: number) {
  form.value.items.splice(index, 1)
  calc()
}
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
function parentCategory(categoryId: string) {
  const current = categories.value.find(item => item.id === categoryId)
  if (!current) return undefined
  return categories.value.find(item => item.id === String(current.parentId || '0'))
}
function categoryPath(categoryId?: string) {
  const byId = new Map(categories.value.map(item => [item.id, item]))
  const path: ErpMasterVO[] = []
  let current = categoryId ? byId.get(categoryId) : undefined
  while (current) {
    path.unshift(current)
    const parentId = String(current.parentId || '0')
    current = byId.get(parentId)
  }
  return path[0]?.name === '商品' ? path.slice(1) : path
}
function applyProductDefaultCategory(row: BillItem, product: ErpProduct) {
  if (!product.categoryId) return
  const path = categoryPath(product.categoryId)
  if (!path.length) return
  row.categoryLevel1Id = path[0]?.id
  row.categoryLevel1Name = path[0]?.name
  row.categoryLevel2Id = path[1]?.id
  row.categoryLevel2Name = path[1]?.name
  row.categoryPickerId = product.categoryId
}
function categoryPathChanged(row: BillItem) {
  const path = categoryPath(row.categoryPickerId)
  row.categoryLevel1Id = path[0]?.id
  row.categoryLevel1Name = path[0]?.name
  row.categoryLevel2Id = path[1]?.id
  row.categoryLevel2Name = path[1]?.name
  syncRowSnapshot(row)
}
function syncRowSnapshot(row: BillItem) {
  row.categoryLevel1Name = categories.value.find(item => item.id === row.categoryLevel1Id)?.name || row.categoryLevel1Name
  row.categoryLevel2Name = categories.value.find(item => item.id === row.categoryLevel2Id)?.name || row.categoryLevel2Name
  const path = categoryPath(row.categoryPickerId)
  const optionIds = path.slice(2).map(item => item.id).filter(Boolean)
  row.optionAttributeIds = optionIds.join(',')
  row.optionAttributeText = path.slice(2).map(item => item.name).join(' / ')
  row.attributeText = rowLinePath(row)
}
function rowLinePath(row: BillItem) {
  return [row.categoryLevel1Name, row.categoryLevel2Name, row.optionAttributeText].filter(Boolean).join(' / ')
}
function splitIds(value?: string) {
  return value ? value.split(',').map(item => item.trim()).filter(Boolean) : []
}
function calc() {
  form.value.items.forEach(row => {
    row.amount = Number(row.qty || 0) * Number(row.price || 0)
  })
}
function openAddressParse() {
  addressRawText.value = [form.value.receiverName, form.value.receiverPhone, form.value.receiverAddress].filter(Boolean).join(' ')
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
  if (addressResult.value.contactName) form.value.receiverName = addressResult.value.contactName
  if (addressResult.value.phone) form.value.receiverPhone = addressResult.value.phone
  if (addressResult.value.normalizedAddress) form.value.receiverAddress = addressResult.value.normalizedAddress
  const matched = partners.value.find(item => item.phone && addressResult.value?.phone && item.phone === addressResult.value.phone)
  if (matched) {
    form.value.partnerId = matched.id
    ElMessage.success(`已匹配客户：${matched.name}`)
  }
  addressOpen.value = false
}
function resetQuickCustomer() {
  quickCustomerForm.code = `CUS_${Date.now().toString().slice(-8)}`
  quickCustomerForm.name = ''
  quickCustomerForm.parentId = '0'
  quickCustomerForm.contact = form.value.receiverName || ''
  quickCustomerForm.phone = form.value.receiverPhone || ''
  quickCustomerForm.address = form.value.receiverAddress || ''
  quickCustomerForm.sortOrder = 0
  quickCustomerForm.status = 1
  quickCustomerForm.remark = ''
  quickCustomerAddressRawText.value = ''
  quickCustomerAddressResult.value = undefined
}
function openQuickCustomer() {
  resetQuickCustomer()
  quickCustomerOpen.value = true
}
function openQuickCustomerAddressParse() {
  quickCustomerAddressRawText.value = [quickCustomerForm.contact, quickCustomerForm.phone, quickCustomerForm.address].filter(Boolean).join(' ')
  quickCustomerAddressResult.value = undefined
  quickCustomerAddressOpen.value = true
}
function doParseQuickCustomerAddress() {
  if (!quickCustomerAddressRawText.value.trim()) {
    ElMessage.warning('请先粘贴需要识别的地址内容')
    return
  }
  quickCustomerAddressLoading.value = true
  parseAddress(quickCustomerAddressRawText.value).then(res => {
    quickCustomerAddressResult.value = res
  }).finally(() => quickCustomerAddressLoading.value = false)
}
function applyQuickCustomerAddress() {
  if (!quickCustomerAddressResult.value) return
  if (quickCustomerAddressResult.value.contactName) quickCustomerForm.contact = quickCustomerAddressResult.value.contactName
  if (quickCustomerAddressResult.value.phone) quickCustomerForm.phone = quickCustomerAddressResult.value.phone
  if (quickCustomerAddressResult.value.normalizedAddress) quickCustomerForm.address = quickCustomerAddressResult.value.normalizedAddress
  if (!quickCustomerForm.name && quickCustomerAddressResult.value.contactName) quickCustomerForm.name = quickCustomerAddressResult.value.contactName
  quickCustomerAddressOpen.value = false
}
async function submitQuickCustomer() {
  const valid = await quickCustomerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickCustomerSaving.value = true
  try {
    await addMaster('customer', quickCustomerForm)
    const [latestPartners, createdPage] = await Promise.all([
      loadPartners(),
      listMaster('customer', { current: 1, size: 1, code: quickCustomerForm.code })
    ])
    const created = createdPage.records[0] || latestPartners.find(item => item.code === quickCustomerForm.code)
    if (created) {
      form.value.partnerId = created.id
    }
    if (!form.value.receiverName && quickCustomerForm.contact) form.value.receiverName = quickCustomerForm.contact
    if (!form.value.receiverPhone && quickCustomerForm.phone) form.value.receiverPhone = quickCustomerForm.phone
    if (!form.value.receiverAddress && quickCustomerForm.address) form.value.receiverAddress = quickCustomerForm.address
    ElMessage.success('客户新增成功')
    quickCustomerOpen.value = false
  } finally {
    quickCustomerSaving.value = false
  }
}
function resetQuickWarehouse() {
  quickWarehouseForm.code = `WH_${Date.now().toString().slice(-8)}`
  quickWarehouseForm.name = ''
  quickWarehouseForm.parentId = '0'
  quickWarehouseForm.sortOrder = 0
  quickWarehouseForm.status = 1
  quickWarehouseForm.remark = ''
}
function openQuickWarehouse() {
  resetQuickWarehouse()
  quickWarehouseOpen.value = true
}
async function submitQuickWarehouse() {
  const valid = await quickWarehouseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickWarehouseSaving.value = true
  try {
    await addMaster('warehouse', quickWarehouseForm)
    const [latestWarehouses, createdPage] = await Promise.all([
      loadWarehouses(),
      listMaster('warehouse', { current: 1, size: 1, code: quickWarehouseForm.code })
    ])
    const created = createdPage.records[0] || latestWarehouses.find(item => item.code === quickWarehouseForm.code)
    if (created) {
      form.value.warehouseId = created.id
    }
    ElMessage.success('仓库新增成功')
    quickWarehouseOpen.value = false
  } finally {
    quickWarehouseSaving.value = false
  }
}
function submit(needAudit: boolean) {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (!form.value.items.length) { ElMessage.warning('请添加商品明细'); return }
    calc()
    const payload = sanitizeBill()
    const action = payload.id ? updateBill(module.value, payload) : addBill(module.value, payload)
    action.then(() => {
      ElMessage.success('保存成功')
      if (needAudit) {
        auditSavedBill()
      } else {
        back()
      }
    })
  })
}
function sanitizeBill() {
  form.value.items.forEach(syncRowSnapshot)
  return {
    ...form.value,
    items: form.value.items.map(({ optionProducts, categoryPickerId, ...item }) => item)
  } as ErpBill
}
function auditSavedBill() {
  const id = form.value.id
  if (id) {
    submitApproval(bizType.value, id).then(() => {
      ElMessage.success('提交审批成功')
      back()
    })
    return
  }
  listBill(module.value, { current: 1, size: 1, billNo: form.value.billNo }).then(res => {
    const saved = res.records[0]
    if (!saved?.id) {
      back()
      return
    }
    submitApproval(bizType.value, saved.id).then(() => {
      ElMessage.success('提交审批成功')
      back()
    })
  })
}
function back() { router.push(`/erp/${module.value}/list`) }
onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.card-header, .toolbar, .totals, .section-title { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.toolbar { justify-content: flex-start; margin-bottom: 12px; }
.section-title { margin-bottom: 16px; }
.totals { justify-content: flex-end; padding: 16px 0; font-weight: 600; }
.readonly-alert { margin-bottom: 16px; }
.bill-item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bill-item-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 14px;
  background: var(--el-bg-color);
}
.bill-item-head,
.bill-item-product {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bill-item-head {
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}
.bill-item-product {
  margin-bottom: 12px;
}
.bill-item-product-select {
  flex: 1;
  margin-bottom: 0;
}
.bill-item-path {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.product-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
}
.product-option-image,
.product-option-empty,
.bill-product-image {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  flex: 0 0 auto;
}
.bill-product-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  flex: 0 0 auto;
}
.product-option-image,
.bill-product-image {
  object-fit: cover;
}
.product-option-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.product-option-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.product-option-text span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.image-empty {
  color: var(--el-text-color-placeholder);
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
</style>
