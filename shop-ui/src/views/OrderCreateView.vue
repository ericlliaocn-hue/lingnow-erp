<template>
  <main class="page order-page">
    <section class="page-inner">
      <CustomerPicker @change="onCustomerChange" />
      <div class="card receiver-card" @click="openAddressList">
        <div class="card-title-row">
          <span>收货地址</span>
          <button type="button">{{ selectedAddress ? '更换' : '新增' }}</button>
        </div>
        <template v-if="selectedAddress">
          <div class="receiver-title">
            <strong>{{ selectedAddress.receiverName }}</strong>
            <span>{{ selectedAddress.receiverPhone }}</span>
          </div>
          <p>{{ selectedAddress.fullAddress || selectedAddress.detailAddress }}</p>
          <em v-if="selectedAddress.addressLabel">{{ selectedAddress.addressLabel }}</em>
        </template>
        <div v-else class="receiver-empty">
          <strong>请选择收货地址</strong>
          <p>新增或选择一个地址后再提交订单。</p>
        </div>
      </div>

      <article v-for="(item, index) in items" :key="item.key" class="card order-item">
        <div class="item-head">
          <strong>{{ duplicateProductCount(item) > 1 ? `配置明细 ${configurationIndex(item)}` : `商品 ${index + 1}` }}</strong>
          <button v-if="items.length > 1" class="remove-button" @click="removeItem(index)">移除</button>
        </div>

        <div class="field">
          <span>选择商品</span>
          <ProductPicker v-model="item.productId" :products="products" @update:model-value="onProductChange(item)" />
        </div>

        <div v-if="productOf(item)" class="product-summary">
          <button v-if="productOf(item)?.imageUrl" class="summary-image-button" type="button" @click="previewImage(productOf(item)?.imageUrl)">
            <img :src="productOf(item)?.imageUrl" alt="" />
          </button>
          <div v-else class="summary-img-empty">荣时</div>
          <div class="product-summary-info">
            <span class="main-product-tag">主商品合计 × {{ productGroupTotal(item) }} · 当前配置 × {{ item.qty }}</span>
            <strong>{{ productOf(item)?.name }}</strong>
            <p>{{ productSummary(productOf(item)) }}</p>
            <span class="price">￥{{ itemPrice(item).toFixed(2) }}</span>
          </div>
          <div class="qty-stepper" aria-label="数量">
            <button type="button" @click="decreaseQty(item)">-</button>
            <input v-model.number="item.qty" type="number" min="1" step="1" inputmode="numeric" @blur="normalizeQty(item)" />
            <button type="button" @click="increaseQty(item)">+</button>
          </div>
        </div>

        <div v-for="group in attributeGroups(item)" :key="group.id" class="attribute-group">
          <div class="attribute-title">{{ displayShopLabel(group.name) }} <small>每件商品配置</small></div>
          <div class="attribute-options">
            <button
              v-for="option in group.options"
              :key="option.id"
              type="button"
              :class="['option-button', item.selectedOptions[String(group.id)] === String(option.id) ? 'active' : '']"
              @click="selectOption(item, String(group.id), String(option.id))"
            >
              {{ displayShopLabel(option.name) }}<span v-if="Number(option.extraAmount || 0) > 0"> +￥{{ money(option.extraAmount) }}</span>
            </button>
          </div>
        </div>

        <div v-if="productOf(item)" class="composition-summary">
          <div><strong>当前配置</strong><span>本配置数量可单独修改</span></div>
          <p><span>{{ productOf(item)?.name }}</span><b>× {{ item.qty }}</b></p>
          <p v-for="part in selectedConfiguration(item)" :key="part"><span>{{ part }}</span><b>× {{ item.qty }}</b></p>
        </div>

        <label class="field">
          <span>Logo / 图案参考</span>
          <input class="input file-input" type="file" accept="image/*" @change="uploadLogo($event, item)" />
        </label>
        <div v-if="item.logoImageUrl" class="logo-preview">
          <button class="logo-image-button" type="button" @click="previewImage(item.logoImageUrl)">
            <img :src="item.logoImageUrl" alt="" />
          </button>
          <button class="remove-button" @click="removeLogo(item)">移除图片</button>
        </div>
        <div class="line-total">小计：<strong class="price">￥{{ itemAmount(item).toFixed(2) }}</strong></div>
      </article>

      <div class="card remark-card">
        <label class="field">
          <span>订单留言</span>
          <textarea v-model.trim="form.remark" class="textarea" placeholder="选填，说明订单需求"></textarea>
        </label>
      </div>

      <div class="card summary">
        <div><span>共 {{ totalQty }} 件</span><strong>{{ totalQty }} 件</strong></div>
        <div><span>合计</span><strong class="price">￥{{ totalAmount.toFixed(2) }}</strong></div>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <div class="submit-bar">
        <div>
          <span>合计</span>
          <strong class="price">￥{{ totalAmount.toFixed(2) }}</strong>
        </div>
        <button class="primary-button" :disabled="saving" @click="submit">{{ saving ? '提交中...' : '提交订单' }}</button>
      </div>
    </section>
    <div v-if="previewImageUrl" class="image-viewer" @click="closeImagePreview">
      <button type="button" class="image-viewer-close" @click.stop="closeImagePreview">关闭</button>
      <img :src="previewImageUrl" alt="图片预览" @click.stop />
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAddress, listAddresses, listAttributes, listProducts, submitOrder, uploadImage } from '@/api/shop'
import { displayShopLabel } from '@/utils/label'
import type { OrderSubmitPayload, ShopAddress, ShopAttribute, ShopCustomer, ShopProduct } from '@/types/shop'
import ProductPicker from './components/ProductPicker.vue'
import CustomerPicker from './components/CustomerPicker.vue'
import { useCartStore } from '@/stores/cart'

interface DraftItem {
  key: string
  productId: string
  qty: number
  selectedOptions: Record<string, string>
  logoImageUrl: string
}

const SELECTED_ADDRESS_KEY = 'rs-checkout-address-id'
const CHECKOUT_DRAFT_KEY = 'rs-checkout-draft'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const products = ref<ShopProduct[]>([])
const attributes = ref<ShopAttribute[]>([])
const items = ref<DraftItem[]>([])
const selectedAddress = ref<ShopAddress>()
const saving = ref(false)
const error = ref('')
const previewImageUrl = ref('')
const form = reactive({
  remark: ''
})
const productMap = computed(() => new Map(products.value.map(item => [String(item.id), item])))
const attributeMap = computed(() => new Map(attributes.value.map(item => [String(item.id), item])))
const totalQty = computed(() => items.value.reduce((sum, item) => sum + Number(item.qty || 0), 0))
const totalAmount = computed(() => items.value.reduce((sum, item) => sum + itemAmount(item), 0))

function createItem(productId = ''): DraftItem {
  return { key: `${Date.now()}-${Math.random()}`, productId, qty: 1, selectedOptions: {}, logoImageUrl: '' }
}

function productOf(item: DraftItem) {
  return productMap.value.get(String(item.productId))
}

function sameProductItems(item: DraftItem) {
  return items.value.filter(record => record.productId && record.productId === item.productId)
}

function duplicateProductCount(item: DraftItem) {
  return sameProductItems(item).length
}

function configurationIndex(item: DraftItem) {
  return sameProductItems(item).findIndex(record => record.key === item.key) + 1
}

function productGroupTotal(item: DraftItem) {
  return sameProductItems(item).reduce((sum, record) => sum + Number(record.qty || 0), 0)
}

function productSummary(product?: ShopProduct) {
  if (!product) {
    return ''
  }
  return [product.spec, product.unitName || '件'].filter(Boolean).join(' · ')
}

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function splitIds(value?: string) {
  return (value || '').split(',').map(item => item.trim()).filter(Boolean)
}

function attributeGroups(item: DraftItem) {
  const product = productOf(item)
  if (!product?.attributeIds) {
    return []
  }
  return splitIds(product.attributeIds).map(groupId => {
    const group = attributeMap.value.get(groupId)
    const options = attributes.value.filter(attr => String(attr.parentId || '') === groupId)
    return group ? { ...group, options } : null
  }).filter(Boolean) as Array<ShopAttribute & { options: ShopAttribute[] }>
}

function selectedOptionIds(item: DraftItem) {
  return Object.values(item.selectedOptions).filter(Boolean)
}

function selectedConfiguration(item: DraftItem) {
  return attributeGroups(item).map(group => {
    const optionId = item.selectedOptions[String(group.id)]
    const option = group.options.find(record => String(record.id) === String(optionId || ''))
    return option ? `${displayShopLabel(group.name)}：${displayShopLabel(option.name)}` : ''
  }).filter(Boolean)
}

function itemPrice(item: DraftItem) {
  const product = productOf(item)
  const basePrice = Number(product?.salePrice || 0)
  const extra = selectedOptionIds(item).reduce((sum, optionId) => sum + Number(attributeMap.value.get(optionId)?.extraAmount || 0), 0)
  return basePrice + extra
}

function itemAmount(item: DraftItem) {
  return Number(item.qty || 0) * itemPrice(item)
}

function selectOption(item: DraftItem, groupId: string, optionId: string) {
  const normalizedGroupId = String(groupId)
  const normalizedOptionId = String(optionId)
  item.selectedOptions[normalizedGroupId] = item.selectedOptions[normalizedGroupId] === normalizedOptionId ? '' : normalizedOptionId
}

function normalizeQty(item: DraftItem) {
  item.qty = Math.max(1, Math.floor(Number(item.qty || 1)))
}

function decreaseQty(item: DraftItem) {
  item.qty = Math.max(1, Math.floor(Number(item.qty || 1)) - 1)
}

function increaseQty(item: DraftItem) {
  item.qty = Math.max(1, Math.floor(Number(item.qty || 1)) + 1)
}

function onProductChange(item: DraftItem) {
  item.selectedOptions = {}
}

function addItem(productId = '') {
  items.value.push(createItem(productId))
}

function removeItem(index: number) {
  items.value.splice(index, 1)
  saveDraft()
}

function previewImage(url?: string) {
  if (!url) {
    return
  }
  previewImageUrl.value = url
}

function closeImagePreview() {
  previewImageUrl.value = ''
}

function removeLogo(item: DraftItem) {
  item.logoImageUrl = ''
  saveDraft()
}

async function uploadLogo(event: Event, item: DraftItem) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    error.value = '图片不能超过 20MB'
    return
  }
  try {
    error.value = ''
    item.logoImageUrl = await uploadImage(file)
    saveDraft()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传失败'
  }
}

function validate() {
  if (!selectedAddress.value?.id) {
    return '请选择收货地址'
  }
  const validItems = items.value.filter(item => item.productId && Number(item.qty || 0) > 0)
  if (validItems.length === 0) {
    return '请至少选择一个商品并填写数量'
  }
  const invalidPriceItem = validItems.find(item => Number(productOf(item)?.salePrice || 0) <= 0)
  if (invalidPriceItem) {
    return `${productOf(invalidPriceItem)?.name || '所选商品'}销售价未维护，请先在商品管理中补全`
  }
  if (items.value.some(item => item.productId && Number(item.qty || 0) <= 0)) {
    return '商品数量必须大于 0'
  }
  return ''
}

async function submit() {
  const customerId = localStorage.getItem('sales-customer-id') || ''
  if (!customerId) {
    error.value = '请先选择客户'
    return
  }
  error.value = validate()
  if (error.value) {
    return
  }
  const payload: OrderSubmitPayload = {
    customerId,
    addressId: selectedAddress.value?.id,
    receiverName: selectedAddress.value?.receiverName,
    receiverPhone: selectedAddress.value?.receiverPhone,
    receiverAddress: selectedAddress.value?.fullAddress || selectedAddress.value?.detailAddress,
    remark: form.remark,
    items: items.value
      .filter(item => item.productId)
      .map(item => ({
        productId: item.productId,
        optionAttributeIds: selectedOptionIds(item).join(','),
        logoImageUrl: item.logoImageUrl,
        qty: Number(item.qty || 0)
      }))
  }
  saving.value = true
  try {
    const id = await submitOrder(payload)
    cart.clear()
    sessionStorage.removeItem(CHECKOUT_DRAFT_KEY)
    router.replace(`/orders/${id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交失败'
  } finally {
    saving.value = false
  }
}

function routeProductIds() {
  if (route.query.fromCart === '1') {
    return cart.items.map(item => item.productId)
  }
  const productIds = typeof route.query.productIds === 'string'
    ? route.query.productIds.split(',').map(item => item.trim()).filter(Boolean)
    : []
  const productId = typeof route.query.productId === 'string' ? route.query.productId : ''
  return productIds.length ? productIds : [productId].filter(Boolean)
}

async function loadData() {
  const [productRes, attributeRes] = await Promise.all([
    listProducts({ current: 1, size: 1000 }),
    listAttributes()
  ])
  products.value = productRes.records
  attributes.value = attributeRes
  const ids = routeProductIds()
  if (route.query.fromCart === '1' && cart.items.length) {
    items.value = cart.items.map(record => {
      const selectedOptions: Record<string, string> = {}
      splitIds(record.optionAttributeIds).forEach(optionId => {
        const option = attributes.value.find(item => String(item.id) === String(optionId))
        if (option?.parentId) selectedOptions[String(option.parentId)] = String(optionId)
      })
      return { ...createItem(record.productId), qty: record.qty, selectedOptions, logoImageUrl: record.logoImageUrl || '' }
    })
  } else {
    items.value = ids.length ? ids.map(id => createItem(id)) : [createItem()]
  }
  if (route.query.fromCart !== '1') restoreDraft(ids)
  if (localStorage.getItem('sales-customer-id')) {
    await loadSelectedAddress(await listAddresses())
  }
}

async function onCustomerChange(_customer: ShopCustomer) {
  selectedAddress.value = undefined
  await loadSelectedAddress(await listAddresses())
}

async function loadSelectedAddress(addressRecords: ShopAddress[]) {
  const storedId = sessionStorage.getItem(SELECTED_ADDRESS_KEY)
  if (storedId) {
    try {
      selectedAddress.value = await getAddress(storedId)
      return
    } catch {
      sessionStorage.removeItem(SELECTED_ADDRESS_KEY)
    }
  }
  selectedAddress.value = addressRecords.find(item => item.defaultFlag) || addressRecords[0]
  if (selectedAddress.value?.id) {
    sessionStorage.setItem(SELECTED_ADDRESS_KEY, selectedAddress.value.id)
  }
}

function openAddressList() {
  if (!localStorage.getItem('sales-customer-id')) {
    error.value = '请先选择客户'
    return
  }
  saveDraft()
  router.push({ path: '/addresses', query: { select: '1', redirect: route.fullPath } })
}

function saveDraft() {
  const ids = routeProductIds()
  sessionStorage.setItem(CHECKOUT_DRAFT_KEY, JSON.stringify({
    productIds: ids,
    remark: form.remark,
    items: items.value
  }))
}

function restoreDraft(ids: string[]) {
  const raw = sessionStorage.getItem(CHECKOUT_DRAFT_KEY)
  if (!raw) {
    return
  }
  try {
    const draft = JSON.parse(raw) as { productIds?: string[]; remark?: string; items?: DraftItem[] }
    if ((draft.productIds || []).join(',') !== ids.join(',')) {
      return
    }
    form.remark = draft.remark || ''
    if (Array.isArray(draft.items) && draft.items.length) {
      items.value = draft.items.map(item => ({
        ...createItem(item.productId),
        ...item,
        selectedOptions: item.selectedOptions || {}
      }))
    }
  } catch {
    sessionStorage.removeItem(CHECKOUT_DRAFT_KEY)
  }
}

onMounted(loadData)
</script>

<style scoped>
.order-page {
  background: var(--bg-page);
}

.receiver-card {
  border: 1px solid var(--border-soft);
  cursor: pointer;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 900;
}

.card-title-row button {
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--radius-pill);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-size: 12px;
  font-weight: 900;
}

.receiver-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.receiver-title strong {
  color: var(--text-main);
  font-size: 17px;
}

.receiver-title span {
  color: var(--text-sub);
  font-size: 13px;
}

.receiver-card p,
.receiver-empty p {
  margin: 8px 0 0;
  color: var(--text-main);
  line-height: 1.5;
}

.receiver-card em {
  display: inline-flex;
  margin-top: 8px;
  padding: 3px 8px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 12px;
  font-style: normal;
}

.receiver-empty strong {
  color: var(--text-main);
}

.receiver-empty p {
  color: var(--text-sub);
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-soft);
}

.item-head strong {
  color: var(--text-main);
}

.remove-button {
  min-height: 32px;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  color: #9b2c2c;
  background: #ffe9e7;
}

.product-summary {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: var(--bg-cream);
}

.summary-image-button,
.summary-img-empty {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  background: #fff;
}
.summary-image-button {
  padding: 0;
  overflow: hidden;
}
.summary-image-button img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.summary-img-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 11px;
  font-weight: 800;
}

.product-summary p {
  margin: 4px 0;
  color: var(--text-sub);
  font-size: 13px;
}

.product-summary-info {
  min-width: 0;
}

.main-product-tag { display: inline-flex; margin-bottom: 4px; padding: 2px 7px; border-radius: var(--radius-pill); color: var(--brand-teal); background: #e6f2ef; font-size: 11px; font-weight: 800; }

.product-summary-info strong {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-main);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.qty-stepper {
  display: grid;
  grid-template-columns: 30px 42px 30px;
  align-items: center;
  min-width: 102px;
  overflow: hidden;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-sm);
  background: #fff;
}

.qty-stepper button,
.qty-stepper input {
  width: 100%;
  height: 32px;
  border: 0;
  background: transparent;
  color: var(--text-main);
  text-align: center;
  font-weight: 800;
}

.qty-stepper button {
  color: var(--brand-teal);
}

.qty-stepper input {
  border-right: 1px solid var(--border-soft);
  border-left: 1px solid var(--border-soft);
  outline: none;
}

.qty-stepper input::-webkit-outer-spin-button,
.qty-stepper input::-webkit-inner-spin-button {
  margin: 0;
  -webkit-appearance: none;
}

.attribute-group {
  margin-bottom: 12px;
}

.attribute-title {
  margin-bottom: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

.attribute-title small { margin-left: 4px; color: var(--brand-teal); font-size: 11px; font-weight: 400; }

.composition-summary { margin-bottom: 12px; padding: 10px; border: 1px solid #c8dfd9; border-radius: var(--radius-sm); background: #f2f8f6; }
.composition-summary > div, .composition-summary p { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.composition-summary > div { margin-bottom: 6px; }
.composition-summary > div strong { color: var(--text-main); font-size: 13px; }
.composition-summary > div span { color: var(--brand-teal); font-size: 11px; }
.composition-summary p { margin: 0; padding: 5px 0; border-top: 1px dashed #d2e4df; color: var(--text-sub); font-size: 12px; }
.composition-summary b { flex: none; color: var(--brand-teal); }

.attribute-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-button {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-sm);
  color: var(--text-main);
  background: #fff;
}

.option-button.active {
  border-color: var(--brand-teal);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-weight: 700;
}

.file-input {
  padding: 8px;
}

.file-input::file-selector-button {
  min-height: 36px;
  margin-right: 10px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--radius-sm);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 900;
}

.file-input::-webkit-file-upload-button {
  min-height: 36px;
  margin-right: 10px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--radius-sm);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 900;
}

.logo-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.logo-preview img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  border: 1px solid var(--border-line);
}
.logo-image-button {
  width: 72px;
  height: 72px;
  padding: 0;
  overflow: hidden;
  border-radius: var(--radius-sm);
}
.logo-image-button img {
  width: 100%;
  height: 100%;
}

.line-total {
  text-align: right;
  color: var(--text-sub);
}

.summary {
  display: grid;
  gap: 8px;
}

.summary div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.summary span {
  color: var(--text-sub);
}

.error-text {
  color: #c0392b;
}

.submit-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
}

.submit-bar > div {
  display: grid;
  gap: 2px;
}

.submit-bar span {
  color: var(--text-sub);
  font-size: 12px;
}

.submit-bar button {
  width: 132px;
  border-radius: var(--radius-pill);
}
.image-viewer {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(0, 0, 0, 0.82);
}
.image-viewer img {
  max-width: 100%;
  max-height: 82vh;
  border-radius: var(--radius-sm);
  object-fit: contain;
}
.image-viewer-close {
  position: absolute;
  top: calc(12px + env(safe-area-inset-top));
  right: 14px;
  min-height: 34px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 800;
}
</style>
