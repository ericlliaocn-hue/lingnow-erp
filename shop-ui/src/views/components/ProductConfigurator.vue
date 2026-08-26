<template>
  <div v-if="open && product" class="config-mask" @click="close">
    <section class="config-sheet" @click.stop>
      <header>
        <div class="product-head">
          <img v-if="product.imageUrl" :src="product.imageUrl" alt="" />
          <div>
            <span class="product-role">主商品 · 已包含</span>
            <h2>{{ product.name }}</h2>
            <p>{{ product.spec || '按下列选项配置' }}</p>
            <div v-if="priceValid" class="price-breakdown">
              <span>基础价 ￥{{ basePrice.toFixed(2) }}</span>
              <span v-if="extraAmount > 0">选配 +￥{{ extraAmount.toFixed(2) }}</span>
              <strong>单价 ￥{{ totalPrice.toFixed(2) }}</strong>
            </div>
            <strong v-else class="price-warning">销售价未维护</strong>
          </div>
        </div>
        <button type="button" class="close" @click="close">×</button>
      </header>

      <div class="config-content">
        <section class="purchase-summary">
          <div class="summary-heading">
            <strong>本次购买</strong>
            <span>配件数量跟随主商品</span>
          </div>
          <div class="summary-line main-line">
            <span>{{ product.name }}</span>
            <b>× {{ qty }}</b>
          </div>
          <div v-for="item in selectedConfigurations" :key="item.groupId" class="summary-line option-line">
            <span>{{ item.groupName }}：{{ item.optionName }}</span>
            <b>× {{ qty }}</b>
          </div>
          <p v-if="selectedConfigurations.length === 0">请选择每件商品需要的款式、挂钩等配置。</p>
        </section>

        <section v-for="group in groups" :key="group.id" class="option-group">
          <h3>{{ displayShopLabel(group.name) }} <small>每件商品单选</small></h3>
          <div class="option-grid">
            <button
              v-for="option in group.options"
              :key="option.id"
              type="button"
              :class="selections[String(group.id)] === String(option.id) ? 'active' : ''"
              @click="select(group.id, option.id)"
            >
              <span>{{ displayShopLabel(option.name) }}</span>
              <em v-if="Number(option.extraAmount || 0)">每件 +￥{{ Number(option.extraAmount).toFixed(2) }}</em>
            </button>
          </div>
        </section>

        <label class="logo-field">
          <span>Logo / 定制图案（选填）</span>
          <input type="file" accept="image/*" @change="uploadLogo" />
        </label>
        <div v-if="logoImageUrl" class="logo-preview">
          <img :src="logoImageUrl" alt="Logo预览" />
          <button type="button" @click="logoImageUrl = ''">移除</button>
        </div>
      </div>

      <footer>
        <div class="qty-stepper">
          <button type="button" @click="qty = Math.max(1, qty - 1)">−</button>
          <span>{{ qty }}</span>
          <button type="button" @click="qty += 1">＋</button>
        </div>
        <button type="button" class="add-button" :disabled="uploading || !priceValid" @click="confirm">
          {{ uploading ? '图片上传中' : priceValid ? `加入 ${qty} 件主商品 · ￥${(totalPrice * qty).toFixed(2)}` : '销售价未维护，暂不能加入' }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listAttributes, uploadImage } from '@/api/shop'
import { displayShopLabel } from '@/utils/label'
import type { ShopAttribute, ShopProduct } from '@/types/shop'

export interface ProductConfiguration {
  optionAttributeIds: string
  optionAttributeText: string
  attributeExtraAmount: number
  logoImageUrl?: string
  qty: number
}

const props = defineProps<{ open: boolean; product?: ShopProduct | null }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'confirm', configuration: ProductConfiguration): void
}>()
const attributes = ref<ShopAttribute[]>([])
const selections = ref<Record<string, string>>({})
const qty = ref(1)
const logoImageUrl = ref('')
const uploading = ref(false)

const groups = computed(() => {
  const groupIds = splitIds(props.product?.attributeIds)
  return groupIds.map(groupId => {
    const group = attributes.value.find(item => String(item.id) === groupId)
    if (!group) return null
    return { ...group, options: attributes.value.filter(item => String(item.parentId || '') === groupId) }
  }).filter(Boolean) as Array<ShopAttribute & { options: ShopAttribute[] }>
})
const selectedOptions = computed(() => Object.values(selections.value).filter(Boolean)
  .map(id => attributes.value.find(item => String(item.id) === id)).filter(Boolean) as ShopAttribute[])
const extraAmount = computed(() => selectedOptions.value.reduce((sum, item) => sum + Number(item.extraAmount || 0), 0))
const basePrice = computed(() => Number(props.product?.salePrice || 0))
const priceValid = computed(() => basePrice.value > 0)
const totalPrice = computed(() => basePrice.value + extraAmount.value)
const selectedConfigurations = computed(() => groups.value.map(group => {
  const optionId = selections.value[String(group.id)]
  const option = group.options.find(item => String(item.id) === optionId)
  return option ? {
    groupId: String(group.id),
    groupName: displayShopLabel(group.name),
    optionName: displayShopLabel(option.name)
  } : null
}).filter(Boolean) as Array<{ groupId: string; groupName: string; optionName: string }>)

function splitIds(value?: string) {
  return (value || '').split(',').map(item => item.trim()).filter(Boolean)
}
function select(groupId: string, optionId: string) {
  const normalizedGroupId = String(groupId)
  const normalizedOptionId = String(optionId)
  selections.value[normalizedGroupId] = selections.value[normalizedGroupId] === normalizedOptionId ? '' : normalizedOptionId
}
function close() { emit('close') }
function confirm() {
  if (!priceValid.value) return
  const byId = new Map(attributes.value.map(item => [String(item.id), item]))
  const ids = groups.value.map(group => selections.value[String(group.id)]).filter((id): id is string => Boolean(id))
  const text = ids.map(id => {
    const option = byId.get(id)
    const group = option ? byId.get(String(option.parentId || '')) : undefined
    return option ? `${displayShopLabel(group?.name || '商品属性')}: ${displayShopLabel(option.name)}` : ''
  }).filter(Boolean).join(' / ')
  emit('confirm', {
    optionAttributeIds: ids.join(','),
    optionAttributeText: text,
    attributeExtraAmount: extraAmount.value,
    logoImageUrl: logoImageUrl.value,
    qty: qty.value
  })
}
async function uploadLogo(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploading.value = true
  try { logoImageUrl.value = await uploadImage(file) } finally { uploading.value = false }
}
function reset() {
  selections.value = {}
  qty.value = 1
  logoImageUrl.value = ''
}
watch(() => [props.open, props.product?.id], ([isOpen]) => {
  if (isOpen) reset()
})
watch(() => props.open, value => { document.body.style.overflow = value ? 'hidden' : '' })
onMounted(async () => { attributes.value = await listAttributes() })
</script>

<style scoped>
.config-mask { position: fixed; inset: 0; z-index: 90; display: flex; align-items: flex-end; background: rgba(36,27,22,.52); }
.config-sheet { width: 100%; max-width: 640px; max-height: 88vh; margin: 0 auto; display: grid; grid-template-rows: auto minmax(0,1fr) auto; border-radius: 22px 22px 0 0; background: #fff; overflow: hidden; }
.config-sheet header { position: relative; padding: 16px; border-bottom: 1px solid var(--border-soft); }
.product-head { display: grid; grid-template-columns: 76px 1fr; gap: 12px; align-items: center; }
.product-head img { width: 76px; height: 76px; object-fit: contain; border-radius: var(--radius); background: var(--bg-muted); }
.product-role { display: inline-flex; margin-bottom: 4px; padding: 2px 7px; border-radius: 999px; color: var(--brand-teal); background: #e6f2ef; font-size: 11px; font-weight: 800; }
.product-head h2 { margin: 0; font-size: 17px; }
.product-head p { margin: 5px 0; color: var(--text-sub); font-size: 12px; }
.price-breakdown { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 9px; }
.price-breakdown span { color: var(--text-sub); font-size: 11px; }
.price-breakdown strong { color: var(--brand-orange); font-size: 18px; }
.product-head .price-warning { color: #9b2c2c; font-size: 14px; }
.close { position: absolute; top: 10px; right: 12px; width: 32px; height: 32px; border-radius: 50%; background: var(--bg-muted); font-size: 22px; }
.config-content { overflow-y: auto; padding: 4px 16px 18px; }
.purchase-summary { margin-top: 12px; padding: 12px; border: 1px solid #c8dfd9; border-radius: var(--radius); background: #f2f8f6; }
.summary-heading, .summary-line { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.summary-heading { margin-bottom: 9px; }
.summary-heading strong { color: var(--text-main); font-size: 14px; }
.summary-heading span { color: var(--brand-teal); font-size: 11px; }
.summary-line { padding: 6px 0; border-top: 1px dashed #d2e4df; font-size: 13px; }
.summary-line span { min-width: 0; }
.summary-line b { flex: none; color: var(--brand-teal); }
.main-line { font-weight: 800; }
.option-line { color: var(--text-sub); }
.purchase-summary p { margin: 7px 0 0; color: var(--text-sub); font-size: 12px; }
.option-group { padding: 14px 0; border-bottom: 1px solid var(--border-soft); }
.option-group h3 { margin: 0 0 10px; font-size: 15px; }
.option-group h3 small { margin-left: 5px; color: var(--text-sub); font-weight: 400; }
.option-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
.option-grid button { min-height: 48px; padding: 8px 10px; display: grid; gap: 3px; border: 1px solid var(--border-line); border-radius: var(--radius-sm); background: var(--bg-muted); text-align: left; }
.option-grid button.active { border-color: var(--brand-teal); background: #e6f2ef; color: var(--brand-teal); }
.option-grid em { color: var(--brand-orange); font-size: 11px; font-style: normal; }
.logo-field { display: grid; gap: 8px; padding-top: 14px; color: var(--text-main); font-size: 14px; font-weight: 700; }
.logo-preview { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.logo-preview img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; }
.logo-preview button { color: #9b2c2c; }
.config-sheet footer { padding: 12px 16px calc(12px + env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 12px; border-top: 1px solid var(--border-soft); }
.qty-stepper { display: flex; align-items: center; border: 1px solid var(--border-line); border-radius: 999px; overflow: hidden; }
.qty-stepper button { width: 36px; height: 38px; background: var(--bg-muted); }
.qty-stepper span { min-width: 32px; text-align: center; }
.add-button { flex: 1; min-height: 44px; border-radius: 999px; color: #fff; background: var(--brand-teal); font-weight: 900; }
.add-button:disabled { opacity: .55; }
</style>
