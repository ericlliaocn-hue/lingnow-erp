<template>
  <div v-if="open && product" class="config-mask" @click="close">
    <section class="config-sheet" @click.stop>
      <header>
        <div class="product-head">
          <img v-if="product.imageUrl" :src="product.imageUrl" alt="" />
          <div>
            <span class="product-role">主商品 · 多配置组合</span>
            <h2>{{ product.name }}</h2>
            <p>{{ product.spec || '按下列选项配置' }}</p>
            <div v-if="priceValid" class="price-breakdown">
              <span>基础价 ￥{{ basePrice.toFixed(2) }}</span>
              <strong>合计 ￥{{ totalAmount.toFixed(2) }}</strong>
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
            <span :class="{ invalid: !quantityBalanced }">已分配 {{ assignedQty }} / {{ totalQty }} 件</span>
          </div>
          <div class="summary-line main-line">
            <span>{{ product.name }}</span>
            <b>× {{ totalQty }}</b>
          </div>
          <div v-for="(configuration, index) in configurations" :key="configuration.key" class="summary-line option-line">
            <span>配置 {{ index + 1 }}：{{ configurationSummary(configuration) }}</span>
            <b>× {{ configuration.qty }}</b>
          </div>
          <p v-if="!quantityBalanced" class="quantity-warning">各配置数量合计必须等于主商品数量。</p>
        </section>

        <section v-for="(configuration, index) in configurations" :key="configuration.key" class="configuration-card">
          <div class="configuration-head">
            <div>
              <strong>配置 {{ index + 1 }}</strong>
              <span>单价 ￥{{ configurationPrice(configuration).toFixed(2) }}</span>
            </div>
            <div class="configuration-actions">
              <div class="qty-stepper compact">
                <button type="button" @click="changeConfigurationQty(configuration, -1)">−</button>
                <input
                  v-model.number="configuration.qty"
                  type="number"
                  min="1"
                  step="1"
                  inputmode="numeric"
                  @input="normalizeConfigurationQty(configuration)"
                />
                <button type="button" @click="changeConfigurationQty(configuration, 1)">＋</button>
              </div>
              <button v-if="configurations.length > 1" type="button" class="remove-configuration" @click="removeConfiguration(index)">删除</button>
            </div>
          </div>

          <section v-for="group in groups" :key="`${configuration.key}-${group.id}`" class="option-group">
            <h3>{{ displayShopLabel(group.name) }} <small>当前配置单选</small></h3>
            <div class="option-grid">
              <button
                v-for="option in group.options"
                :key="option.id"
                type="button"
                :class="configuration.selections[String(group.id)] === String(option.id) ? 'active' : ''"
                @click="select(configuration, String(group.id), String(option.id))"
              >
                <span>{{ displayShopLabel(option.name) }}</span>
                <em v-if="Number(option.extraAmount || 0)">每件 +￥{{ Number(option.extraAmount).toFixed(2) }}</em>
              </button>
            </div>
          </section>

          <label class="logo-field">
            <span>Logo / 定制图案（当前配置选填）</span>
            <input type="file" accept="image/*" @change="uploadLogo($event, configuration)" />
          </label>
          <div v-if="configuration.logoImageUrl" class="logo-preview">
            <img :src="configuration.logoImageUrl" alt="Logo预览" />
            <button type="button" @click="configuration.logoImageUrl = ''">移除</button>
          </div>
        </section>

        <button type="button" class="add-configuration" :disabled="configurations.length >= totalQty" @click="addConfiguration">
          ＋ 增加另一种配置
        </button>
      </div>

      <footer>
        <div class="main-qty">
          <span>主商品总数</span>
          <div class="qty-stepper">
            <button type="button" @click="changeTotalQty(-1)">−</button>
            <input v-model.number="totalQty" type="number" min="1" step="1" inputmode="numeric" @input="normalizeTotalQty" />
            <button type="button" @click="changeTotalQty(1)">＋</button>
          </div>
        </div>
        <button type="button" class="add-button" :disabled="uploading || !priceValid || !quantityBalanced" @click="confirm">
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
  attributeExtraAmount: number
  logoImageUrl?: string
  qty: number
}

interface ConfigurationDraft {
  key: string
  selections: Record<string, string>
  logoImageUrl: string
  qty: number
  uploading: boolean
}

const props = withDefaults(defineProps<{ open: boolean; product?: ShopProduct | null; initialQty?: number }>(), {
  initialQty: 1
})
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'confirm', configurations: ProductConfiguration[]): void
}>()
const attributes = ref<ShopAttribute[]>([])
const totalQty = ref(1)
const configurations = ref<ConfigurationDraft[]>([])

const groups = computed(() => {
  const groupIds = splitIds(props.product?.attributeIds)
  return groupIds.map(groupId => {
    const group = attributes.value.find(item => String(item.id) === groupId)
    if (!group) return null
    return { ...group, options: attributes.value.filter(item => String(item.parentId || '') === groupId) }
  }).filter(Boolean) as Array<ShopAttribute & { options: ShopAttribute[] }>
})
const attributeMap = computed(() => new Map(attributes.value.map(item => [String(item.id), item])))
const basePrice = computed(() => Number(props.product?.salePrice || 0))
const priceValid = computed(() => basePrice.value > 0)
const assignedQty = computed(() => configurations.value.reduce((sum, item) => sum + normalizePositive(item.qty), 0))
const quantityBalanced = computed(() => assignedQty.value === totalQty.value)
const uploading = computed(() => configurations.value.some(item => item.uploading))
const totalAmount = computed(() => configurations.value.reduce((sum, item) => sum + configurationPrice(item) * normalizePositive(item.qty), 0))
const confirmButtonText = computed(() => {
  if (uploading.value) return '图片上传中'
  if (!priceValid.value) return '销售价未维护，暂不能加入'
  if (!quantityBalanced.value) return `已分配 ${assignedQty.value} / ${totalQty.value} 件`
  return `加入 ${totalQty.value} 件主商品 · ￥${totalAmount.value.toFixed(2)}`
})

function splitIds(value?: string) {
  return (value || '').split(',').map(item => item.trim()).filter(Boolean)
}

function normalizePositive(value: number) {
  return Math.max(1, Math.floor(Number(value || 1)))
}

function createConfiguration(qty = 1, source?: ConfigurationDraft): ConfigurationDraft {
  return {
    key: `${Date.now()}-${Math.random()}`,
    selections: { ...(source?.selections || {}) },
    logoImageUrl: source?.logoImageUrl || '',
    qty: normalizePositive(qty),
    uploading: false
  }
}

function selectedOptionIds(configuration: ConfigurationDraft) {
  return groups.value.map(group => configuration.selections[String(group.id)]).filter((id): id is string => Boolean(id))
}

function configurationExtraAmount(configuration: ConfigurationDraft) {
  return selectedOptionIds(configuration).reduce((sum, id) => sum + Number(attributeMap.value.get(id)?.extraAmount || 0), 0)
}

function configurationPrice(configuration: ConfigurationDraft) {
  return basePrice.value + configurationExtraAmount(configuration)
}

function configurationTextParts(configuration: ConfigurationDraft) {
  return groups.value.map(group => {
    const optionId = configuration.selections[String(group.id)]
    const option = group.options.find(item => String(item.id) === optionId)
    return option ? `${displayShopLabel(group.name)}：${displayShopLabel(option.name)}` : ''
  }).filter(Boolean)
}

function configurationSummary(configuration: ConfigurationDraft) {
  return configurationTextParts(configuration).join(' / ') || '标准配置'
}

function select(configuration: ConfigurationDraft, groupId: string, optionId: string) {
  configuration.selections[groupId] = configuration.selections[groupId] === optionId ? '' : optionId
}

function close() {
  emit('close')
}

function changeTotalQty(delta: number) {
  totalQty.value = Math.max(configurations.value.length || 1, normalizePositive(totalQty.value) + delta)
  balanceConfigurationsToTotal()
}

function normalizeTotalQty() {
  totalQty.value = Math.max(configurations.value.length || 1, normalizePositive(totalQty.value))
  balanceConfigurationsToTotal()
}

function balanceConfigurationsToTotal() {
  if (!configurations.value.length) return
  let difference = totalQty.value - assignedQty.value
  if (difference > 0) {
    configurations.value[0]!.qty += difference
    return
  }
  for (let index = configurations.value.length - 1; index >= 0 && difference < 0; index -= 1) {
    const configuration = configurations.value[index]!
    const reducible = Math.min(configuration.qty - 1, Math.abs(difference))
    configuration.qty -= reducible
    difference += reducible
  }
}

function changeConfigurationQty(configuration: ConfigurationDraft, delta: number) {
  configuration.qty = Math.max(1, normalizePositive(configuration.qty) + delta)
}

function normalizeConfigurationQty(configuration: ConfigurationDraft) {
  configuration.qty = normalizePositive(configuration.qty)
}

function addConfiguration() {
  if (configurations.value.length >= totalQty.value) return
  const source = configurations.value[0]
  if (source && source.qty > 1) source.qty -= 1
  configurations.value.push(createConfiguration(1, source))
}

function removeConfiguration(index: number) {
  const [removed] = configurations.value.splice(index, 1)
  if (removed && configurations.value[0]) configurations.value[0].qty += normalizePositive(removed.qty)
}

function confirm() {
  if (!priceValid.value || !quantityBalanced.value) return
  emit('confirm', configurations.value.map(configuration => ({
    optionAttributeIds: selectedOptionIds(configuration).join(','),
    optionAttributeText: configurationTextParts(configuration).join(' / '),
    attributeExtraAmount: configurationExtraAmount(configuration),
    logoImageUrl: configuration.logoImageUrl,
    qty: normalizePositive(configuration.qty)
  })))
}

async function uploadLogo(event: Event, configuration: ConfigurationDraft) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  configuration.uploading = true
  try {
    configuration.logoImageUrl = await uploadImage(file)
  } finally {
    configuration.uploading = false
  }
}

function reset() {
  totalQty.value = normalizePositive(props.initialQty)
  configurations.value = [createConfiguration(totalQty.value)]
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
.product-role { display: inline-flex; margin-bottom: 4px; padding: 2px 7px; border-radius: 999px; color: var(--brand-teal); background: #e6f2ef; font-size: 11px; font-weight: 800; }
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
.summary-heading span { color: var(--brand-teal); font-size: 11px; font-weight: 800; }
.summary-heading span.invalid, .quantity-warning { color: #b54708; }
.summary-line { padding: 6px 0; border-top: 1px dashed #d2e4df; font-size: 13px; }
.summary-line span { min-width: 0; line-height: 1.4; }
.summary-line b { flex: none; color: var(--brand-teal); }
.main-line { font-weight: 800; }
.option-line { color: var(--text-sub); }
.purchase-summary p { margin: 7px 0 0; font-size: 12px; }
.configuration-card { margin-top: 12px; padding: 12px; border: 1px solid var(--border-soft); border-radius: var(--radius); background: #fff; box-shadow: 0 6px 18px rgba(67,47,31,.05); }
.configuration-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding-bottom: 8px; }
.configuration-head > div:first-child { display: grid; gap: 3px; }
.configuration-head strong { color: var(--text-main); }
.configuration-head span { color: var(--brand-orange); font-size: 12px; font-weight: 800; }
.configuration-actions { display: flex; align-items: center; gap: 7px; }
.remove-configuration { color: #9b2c2c; font-size: 12px; }
.option-group { padding: 12px 0; border-top: 1px solid var(--border-soft); }
.option-group h3 { margin: 0 0 10px; font-size: 15px; }
.option-group h3 small { margin-left: 5px; color: var(--text-sub); font-weight: 400; }
.option-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
.option-grid button { min-height: 48px; padding: 8px 10px; display: grid; gap: 3px; border: 1px solid var(--border-line); border-radius: var(--radius-sm); background: var(--bg-muted); text-align: left; }
.option-grid button.active { border-color: var(--brand-teal); background: #e6f2ef; color: var(--brand-teal); }
.option-grid em { color: var(--brand-orange); font-size: 11px; font-style: normal; }
.logo-field { display: grid; gap: 8px; padding-top: 12px; border-top: 1px solid var(--border-soft); color: var(--text-main); font-size: 14px; font-weight: 700; }
.logo-preview { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.logo-preview img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; }
.logo-preview button { color: #9b2c2c; }
.add-configuration { width: 100%; min-height: 42px; margin-top: 12px; border: 1px dashed var(--brand-teal); border-radius: var(--radius); color: var(--brand-teal); background: #f2f8f6; font-weight: 900; }
.add-configuration:disabled { opacity: .45; }
.config-sheet footer { padding: 10px 16px calc(10px + env(safe-area-inset-bottom)); display: grid; gap: 8px; border-top: 1px solid var(--border-soft); background: #fff; }
.main-qty { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.main-qty > span { color: var(--text-main); font-size: 13px; font-weight: 800; }
.qty-stepper { display: flex; align-items: center; border: 1px solid var(--border-line); border-radius: 999px; overflow: hidden; }
.qty-stepper button { width: 36px; height: 36px; background: var(--bg-muted); }
.qty-stepper.compact button { width: 30px; height: 32px; }
.qty-stepper input { width: 42px; height: 32px; padding: 0; border: 0; text-align: center; background: #fff; }
.add-button { width: 100%; min-height: 44px; border-radius: 999px; color: #fff; background: var(--brand-teal); font-weight: 900; }
.add-button:disabled { opacity: .55; }
</style>
