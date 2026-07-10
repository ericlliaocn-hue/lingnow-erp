<template>
  <main class="page order-page">
    <header class="topbar">
      <div class="topbar-inner">
        <div>
          <h1>填写订单</h1>
          <p>确认收货信息和商品备注</p>
        </div>
        <button class="plain-button" @click="addItem()">加商品</button>
      </div>
    </header>

    <section class="page-inner">
      <div class="card receiver-card">
        <div class="card-title">收货信息</div>
        <label class="field">
          <span>收货人</span>
          <input v-model.trim="form.receiverName" class="input" placeholder="请输入收货人姓名" />
        </label>
        <label class="field">
          <span>联系电话</span>
          <input v-model.trim="form.receiverPhone" class="input" inputmode="tel" placeholder="请输入联系电话" />
        </label>
        <label class="field">
          <span>收货地址</span>
          <textarea v-model.trim="form.receiverAddress" class="textarea" placeholder="请输入详细收货地址"></textarea>
        </label>
        <label class="field">
          <span>给商家留言</span>
          <textarea v-model.trim="form.remark" class="textarea" placeholder="选填，备注订单信息"></textarea>
        </label>
      </div>

      <article v-for="(item, index) in items" :key="item.key" class="card order-item">
        <div class="item-head">
          <strong>商品 {{ index + 1 }}</strong>
          <button v-if="items.length > 1" class="remove-button" @click="removeItem(index)">移除</button>
        </div>

        <div class="field">
          <span>选择商品</span>
          <ProductPicker v-model="item.productId" :products="products" @update:model-value="onProductChange(item)" />
        </div>

        <div v-if="productOf(item)" class="product-summary">
          <img v-if="productOf(item)?.imageUrl" :src="productOf(item)?.imageUrl" alt="" />
          <div v-else class="summary-img-empty">荣时</div>
          <div>
            <strong>{{ productOf(item)?.name }}</strong>
            <p>{{ productSummary(productOf(item)) }}</p>
            <span class="price">￥{{ itemPrice(item).toFixed(2) }}</span>
          </div>
        </div>

        <div v-for="group in attributeGroups(item)" :key="group.id" class="attribute-group">
          <div class="attribute-title">{{ displayShopLabel(group.name) }}</div>
          <div class="attribute-options">
            <button
              v-for="option in group.options"
              :key="option.id"
              type="button"
              :class="['option-button', item.selectedOptions[group.id] === option.id ? 'active' : '']"
              @click="selectOption(item, group.id, option.id)"
            >
              {{ displayShopLabel(option.name) }}<span v-if="Number(option.extraAmount || 0) > 0"> +￥{{ money(option.extraAmount) }}</span>
            </button>
          </div>
        </div>

        <label class="field">
          <span>数量</span>
          <input v-model.number="item.qty" class="input" type="number" min="1" step="1" inputmode="decimal" />
        </label>
        <label class="field">
          <span>Logo / 图案参考</span>
          <input class="input file-input" type="file" accept="image/*" @change="uploadLogo($event, item)" />
        </label>
        <div v-if="item.logoImageUrl" class="logo-preview">
          <img :src="item.logoImageUrl" alt="" />
          <button class="remove-button" @click="item.logoImageUrl = ''">移除图片</button>
        </div>
        <label class="field">
          <span>给商家留言</span>
          <textarea v-model.trim="item.remark" class="textarea" placeholder="选填，说明定制需求"></textarea>
        </label>
        <div class="line-total">小计：<strong class="price">￥{{ itemAmount(item).toFixed(2) }}</strong></div>
      </article>

      <div class="card summary">
        <div><span>共 N 件</span><strong>{{ totalQty }} 件</strong></div>
        <div><span>合计</span><strong class="price">￥{{ totalAmount.toFixed(2) }}</strong></div>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <div class="submit-bar">
        <button class="secondary-button" @click="addItem()">继续加商品</button>
        <button class="primary-button" :disabled="saving" @click="submit">{{ saving ? '提交中...' : '提交订单' }}</button>
      </div>
    </section>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAttributes, listProducts, submitOrder, uploadImage } from '@/api/shop'
import { displayShopLabel } from '@/utils/label'
import type { OrderSubmitPayload, ShopAttribute, ShopProduct } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'
import ProductPicker from './components/ProductPicker.vue'

interface DraftItem {
  key: string
  productId: string
  qty: number
  selectedOptions: Record<string, string>
  logoImageUrl: string
  remark: string
}

const route = useRoute()
const router = useRouter()
const products = ref<ShopProduct[]>([])
const attributes = ref<ShopAttribute[]>([])
const items = ref<DraftItem[]>([])
const saving = ref(false)
const error = ref('')
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: ''
})
const productMap = computed(() => new Map(products.value.map(item => [item.id, item])))
const attributeMap = computed(() => new Map(attributes.value.map(item => [item.id, item])))
const totalQty = computed(() => items.value.reduce((sum, item) => sum + Number(item.qty || 0), 0))
const totalAmount = computed(() => items.value.reduce((sum, item) => sum + itemAmount(item), 0))

function createItem(productId = ''): DraftItem {
  return { key: `${Date.now()}-${Math.random()}`, productId, qty: 1, selectedOptions: {}, logoImageUrl: '', remark: '' }
}

function productOf(item: DraftItem) {
  return productMap.value.get(item.productId)
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
  item.selectedOptions[groupId] = item.selectedOptions[groupId] === optionId ? '' : optionId
}

function onProductChange(item: DraftItem) {
  item.selectedOptions = {}
}

function addItem(productId = '') {
  items.value.push(createItem(productId))
}

function removeItem(index: number) {
  items.value.splice(index, 1)
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
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传失败'
  }
}

function validate() {
  const validItems = items.value.filter(item => item.productId && Number(item.qty || 0) > 0)
  if (validItems.length === 0) {
    return '请至少选择一个商品并填写数量'
  }
  if (items.value.some(item => item.productId && Number(item.qty || 0) <= 0)) {
    return '商品数量必须大于 0'
  }
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) {
    return
  }
  const payload: OrderSubmitPayload = {
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    receiverAddress: form.receiverAddress,
    remark: form.remark,
    items: items.value
      .filter(item => item.productId)
      .map(item => ({
        productId: item.productId,
        optionAttributeIds: selectedOptionIds(item).join(','),
        logoImageUrl: item.logoImageUrl,
        qty: Number(item.qty || 0),
        remark: item.remark
      }))
  }
  saving.value = true
  try {
    const id = await submitOrder(payload)
    router.replace(`/orders/${id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交失败'
  } finally {
    saving.value = false
  }
}

async function loadData() {
  const [productRes, attributeRes] = await Promise.all([
    listProducts({ current: 1, size: 1000 }),
    listAttributes()
  ])
  products.value = productRes.records
  attributes.value = attributeRes
  const productIds = typeof route.query.productIds === 'string'
    ? route.query.productIds.split(',').map(item => item.trim()).filter(Boolean)
    : []
  const productId = typeof route.query.productId === 'string' ? route.query.productId : ''
  const ids = productIds.length ? productIds : [productId].filter(Boolean)
  items.value = ids.length ? ids.map(id => createItem(id)) : [createItem()]
}

onMounted(loadData)
</script>

<style scoped>
.order-page {
  background: var(--bg-page);
}

.receiver-card {
  border: 1px solid var(--border-soft);
}

.card-title {
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-soft);
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
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
  grid-template-columns: 72px 1fr;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: var(--bg-cream);
}

.product-summary img,
.summary-img-empty {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: #fff;
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

.attribute-group {
  margin-bottom: 12px;
}

.attribute-title {
  margin-bottom: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
