<template>
  <div v-if="open && product" class="config-mask" @click="close">
    <section class="config-sheet" @click.stop>
      <header>
        <div class="product-head">
          <img v-if="product.imageUrl" :src="product.imageUrl" alt="" />
          <div>
            <h2>{{ product.name }}</h2>
            <p>{{ product.spec || '按下列选项配置' }}</p>
            <div v-if="priceValid" class="price-breakdown">
              <span>基础价 ￥{{ basePrice.toFixed(2) }}</span>
              <strong>合计 ￥{{ totalAmount.toFixed(2) }}</strong>
            </div>
            <strong v-else class="price-warning">询价</strong>
          </div>
        </div>
        <button type="button" class="close" @click="close">×</button>
      </header>

      <div class="config-content">
        <section class="purchase-summary">
          <div class="summary-heading">
            <strong>本次购买</strong>
          </div>
          <div class="summary-line main-line">
            <span>{{ product.name }}</span>
            <div class="qty-stepper" :aria-label="`${product.name}数量`">
              <button type="button" @click="changeTotalQty(-1)">−</button>
              <input v-model.number="totalQty" type="number" min="1" step="1" inputmode="numeric" @input="normalizeTotalQty" />
              <button type="button" @click="changeTotalQty(1)">＋</button>
            </div>
          </div>
          <div v-for="item in selectedOptions" :key="item.id" class="summary-line option-line">
            <span>{{ item.groupName }}：{{ item.name }}</span>
            <div class="qty-stepper compact" :aria-label="`${item.name}数量`">
              <button type="button" :disabled="item.qty <= 0" @click="changeOptionQty(item.id, -1)">−</button>
              <input :value="item.qty" type="number" min="0" step="1" inputmode="numeric" @input="setOptionQty(item.id, ($event.target as HTMLInputElement).value)" />
              <button type="button" @click="changeOptionQty(item.id, 1)">＋</button>
            </div>
          </div>
          <p v-if="!selectedOptions.length" class="standard-hint">未选择额外选配项</p>
        </section>

        <section v-for="group in groups" :key="group.id" class="configuration-card option-group-card">
          <button class="configuration-head" type="button" :aria-expanded="isGroupExpanded(group.id)" @click="toggleGroup(group.id)">
            <strong>{{ displayShopLabel(group.name) }}</strong>
            <span>{{ isGroupExpanded(group.id) ? '收起⌃' : '展开⌄' }}</span>
          </button>
          <div v-if="isGroupExpanded(group.id)" class="option-quantity-list">
            <button v-for="option in group.options" :key="option.id" type="button" :class="['option-quantity-row', optionQty(option.id) > 0 ? 'active' : '']" @click="toggleOption(option.id)">
              <span class="option-name">
                <span>{{ displayShopLabel(option.name) }}</span>
                <em v-if="Number(option.extraAmount || 0)">每件 +￥{{ Number(option.extraAmount).toFixed(2) }}</em>
              </span>
              <span class="option-selected-mark">{{ optionQty(option.id) > 0 ? '已选' : '选择' }}</span>
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
        <button type="button" class="add-button" :disabled="uploading || !priceValid" @click="confirm">
          {{ confirmButtonText }}
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
  optionAttributeQuantityJson: string
  optionQuantities: Record<string, number>
  attributeExtraAmount: number
  logoImageUrl?: string
  qty: number
}

const props = withDefaults(defineProps<{ open: boolean; product?: ShopProduct | null; initialQty?: number }>(), {
  initialQty: 1
})
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'confirm', configuration: ProductConfiguration): void
}>()
const attributes = ref<ShopAttribute[]>([])
const totalQty = ref(1)
const optionQuantities = ref<Record<string, number>>({})
const logoImageUrl = ref('')
const uploading = ref(false)
const expandedGroups = ref<Record<string, boolean>>({})

const groups = computed(() => {
  const groupIds = splitIds(props.product?.attributeIds)
  const optionIds = new Set(splitIds(props.product?.optionAttributeIds))
  const hasSpecificOptions = optionIds.size > 0
  return groupIds.map(groupId => {
    const group = attributes.value.find(item => String(item.id) === groupId)
    if (!group) return null
    const options = attributes.value.filter(item => String(item.parentId || '') === groupId
      && (!hasSpecificOptions || optionIds.has(String(item.id))))
    return group && options.length ? { ...group, options } : null
  }).filter(Boolean) as Array<ShopAttribute & { options: ShopAttribute[] }>
})
const basePrice = computed(() => Number(props.product?.salePrice || 0))
const priceValid = computed(() => basePrice.value > 0)
const selectedOptions = computed(() => groups.value.flatMap(group => group.options.map(option => ({
  id: String(option.id),
  groupName: displayShopLabel(group.name),
  name: displayShopLabel(option.name),
  qty: optionQty(option.id),
  extraAmount: Number(option.extraAmount || 0)
})).filter(option => option.qty > 0)))
const optionExtraTotal = computed(() => selectedOptions.value.reduce((sum, item) => sum + item.extraAmount * item.qty, 0))
const totalAmount = computed(() => basePrice.value * totalQty.value + optionExtraTotal.value)
const confirmButtonText = computed(() => {
  if (uploading.value) return '图片上传中'
  if (!priceValid.value) return '询价商品暂不可加入'
  return `加入 ${totalQty.value} 件主商品 · ￥${totalAmount.value.toFixed(2)}`
})

function splitIds(value?: string) {
  return (value || '').split(',').map(item => item.trim()).filter(Boolean)
}

function normalizePositive(value: number) {
  return Math.max(1, Math.floor(Number(value || 1)))
}

function optionQty(id: string | number) {
  return Math.max(0, Math.floor(Number(optionQuantities.value[String(id)] || 0)))
}

function isGroupExpanded(groupId: string | number) {
  return expandedGroups.value[String(groupId)] === true
}

function toggleGroup(groupId: string | number) {
  const key = String(groupId)
  expandedGroups.value[key] = !isGroupExpanded(key)
}

function toggleOption(id: string | number) {
  const key = String(id)
  if (optionQuantities.value[key]) delete optionQuantities.value[key]
  else optionQuantities.value[key] = 1
}

function setOptionQty(id: string | number, value: string | number) {
  const key = String(id)
  const qty = Math.max(0, Math.floor(Number(value || 0)))
  if (qty > 0) optionQuantities.value[key] = qty
  else delete optionQuantities.value[key]
}

function changeOptionQty(id: string | number, delta: number) {
  setOptionQty(id, optionQty(id) + delta)
}

function close() {
  emit('close')
}

function changeTotalQty(delta: number) {
  totalQty.value = Math.max(1, normalizePositive(totalQty.value) + delta)
}

function normalizeTotalQty() {
  totalQty.value = normalizePositive(totalQty.value)
}

function confirm() {
  if (!priceValid.value) return
  const quantities = Object.fromEntries(selectedOptions.value.map(item => [item.id, item.qty]))
  emit('confirm', {
    optionAttributeIds: selectedOptions.value.map(item => item.id).join(','),
    optionAttributeText: selectedOptions.value.map(item => `${item.groupName}：${item.name} × ${item.qty}`).join(' / '),
    optionAttributeQuantityJson: JSON.stringify(quantities),
    optionQuantities: quantities,
    attributeExtraAmount: optionExtraTotal.value,
    logoImageUrl: logoImageUrl.value,
    qty: normalizePositive(totalQty.value)
  })
}

async function uploadLogo(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    logoImageUrl.value = await uploadImage(file)
  } finally {
    uploading.value = false
  }
}

function reset() {
  totalQty.value = normalizePositive(props.initialQty)
  optionQuantities.value = {}
  logoImageUrl.value = ''
  expandedGroups.value = {}
}

watch(() => [props.open, props.product?.id], ([isOpen]) => {
  if (isOpen) reset()
})
watch(() => props.open, value => { document.body.style.overflow = value ? 'hidden' : '' })
onMounted(async () => { attributes.value = await listAttributes() })
</script>

<style scoped>
.config-mask { position: fixed; inset: 0; z-index: 90; display: flex; align-items: flex-end; background: rgba(36,27,22,.52); }
.config-sheet { width: 100%; max-width: 640px; max-height: 92vh; margin: 0 auto; display: grid; grid-template-rows: auto minmax(0,1fr) auto; border-radius: 22px 22px 0 0; background: #fff; overflow: hidden; }
.config-sheet header { position: relative; padding: 16px; border-bottom: 1px solid var(--border-soft); }
.product-head { display: grid; grid-template-columns: 76px 1fr; gap: 12px; align-items: center; }
.product-head img { width: 76px; height: 76px; object-fit: contain; border-radius: var(--radius); background: var(--bg-muted); }
.product-head h2 { margin: 0; padding-right: 30px; font-size: 17px; }
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
.summary-line { padding: 6px 0; border-top: 1px dashed #d2e4df; font-size: 13px; }
.summary-line span { min-width: 0; line-height: 1.4; }
.summary-line .qty-stepper { flex: none; }
.main-line { font-weight: 800; }
.option-line { color: var(--text-sub); }
.purchase-summary p { margin: 7px 0 0; font-size: 12px; }
.standard-hint { color: var(--text-muted); }
.configuration-card { margin-top: 12px; padding: 12px; border: 1px solid var(--border-soft); border-radius: var(--radius); background: #fff; box-shadow: 0 6px 18px rgba(67,47,31,.05); }
.configuration-head { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 0 0 8px; border: 0; background: transparent; text-align: left; }
.configuration-head strong { color: var(--text-main); }
.configuration-head > span { color: var(--brand-teal); font-size: 12px; font-weight: 700; }
.configuration-actions { display: flex; align-items: center; gap: 7px; }
.remove-configuration { color: #9b2c2c; font-size: 12px; }
.option-group { padding: 12px 0; border-top: 1px solid var(--border-soft); }
.option-group h3 { margin: 0 0 10px; font-size: 15px; }
.option-group h3 small { margin-left: 5px; color: var(--text-sub); font-weight: 400; }
.option-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
.option-grid button { min-height: 48px; padding: 8px 10px; display: grid; gap: 3px; border: 1px solid var(--border-line); border-radius: var(--radius-sm); background: var(--bg-muted); text-align: left; }
.option-grid button.active { border-color: var(--brand-teal); background: #e6f2ef; color: var(--brand-teal); }
.option-grid em { color: var(--brand-orange); font-size: 11px; font-style: normal; }
.option-quantity-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; padding-top: 10px; border-top: 1px solid var(--border-soft); }
.option-quantity-row { width: 100%; display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 9px 10px; border: 1px solid var(--border-line); border-radius: var(--radius-sm); background: var(--bg-muted); text-align: left; }
.option-quantity-row.active { border-color: var(--brand-teal); background: #e6f2ef; }
.option-name { min-width: 0; display: grid; gap: 3px; text-align: left; }
.option-name span { color: var(--text-main); font-weight: 800; }
.option-name em { color: var(--brand-orange); font-size: 11px; font-style: normal; }
.option-selected-mark { flex: none; color: var(--brand-teal); font-size: 12px; font-weight: 800; }
.logo-field { display: grid; gap: 8px; padding-top: 12px; border-top: 1px solid var(--border-soft); color: var(--text-main); font-size: 14px; font-weight: 700; }
.logo-preview { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.logo-preview img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; }
.logo-preview button { color: #9b2c2c; }
.add-configuration { width: 100%; min-height: 42px; margin-top: 12px; border: 1px dashed var(--brand-teal); border-radius: var(--radius); color: var(--brand-teal); background: #f2f8f6; font-weight: 900; }
.add-configuration:disabled { opacity: .45; }
.config-sheet footer { padding: 10px 16px calc(10px + env(safe-area-inset-bottom)); display: grid; gap: 8px; border-top: 1px solid var(--border-soft); background: #fff; }
.qty-stepper { display: flex; align-items: center; border: 1px solid var(--border-line); border-radius: 999px; overflow: hidden; }
.qty-stepper button { width: 36px; height: 36px; background: var(--bg-muted); }
.qty-stepper.compact button { width: 30px; height: 32px; }
.qty-stepper input { width: 42px; height: 32px; padding: 0; border: 0; text-align: center; background: #fff; }
.add-button { width: 100%; min-height: 44px; border-radius: 999px; color: #fff; background: var(--brand-teal); font-weight: 900; }
.add-button:disabled { opacity: .55; }
@media (max-width: 520px) {
  .option-quantity-list { grid-template-columns: 1fr; }
}
</style>
