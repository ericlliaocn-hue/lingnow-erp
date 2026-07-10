<template>
  <main class="page address-edit-page">
    <header class="edit-header">
      <button class="back-button" type="button" @click="goBack">‹</button>
      <h1>{{ isEdit ? '编辑收货地址' : '新增收货地址' }}</h1>
      <span></span>
    </header>

    <section class="edit-content">
      <section class="card parse-card">
        <div class="card-title">地址识别</div>
        <textarea
          v-model.trim="rawText"
          class="textarea"
          placeholder="粘贴收货信息，自动识别姓名、手机号和地址，例如：张三 13800000000 广东省广州市海珠区..."
        ></textarea>
        <button class="secondary-button" type="button" :disabled="parsing" @click="parseRawAddress">
          {{ parsing ? '识别中...' : '识别并填入' }}
        </button>
      </section>

      <section class="card form-card">
        <label class="field">
          <span>收货人</span>
          <input v-model.trim="form.receiverName" class="input" placeholder="请输入收货人姓名" />
        </label>
        <label class="field">
          <span>手机号</span>
          <input v-model.trim="form.receiverPhone" class="input" inputmode="tel" placeholder="请输入手机号" />
        </label>
        <label class="field">
          <span>所在地区</span>
          <AddressRegionPicker v-model:path="addressPath" v-model:path-names="addressPathNames" />
        </label>
        <label class="field">
          <span>详细地址</span>
          <textarea v-model.trim="form.detailAddress" class="textarea" placeholder="门牌号、楼栋、公司、房间号等"></textarea>
        </label>

        <div class="field">
          <span>标签</span>
          <div class="label-grid">
            <button
              v-for="item in labelOptions"
              :key="item"
              type="button"
              :class="['label-chip', currentLabel === item ? 'active' : '']"
              @click="chooseLabel(item)"
            >
              {{ item }}
            </button>
            <button type="button" :class="['label-chip', customLabelMode ? 'active' : '']" @click="chooseCustomLabel">自定义</button>
          </div>
          <input v-if="customLabelMode" v-model.trim="customLabel" class="input" placeholder="请输入标签" maxlength="12" />
        </div>

        <label class="default-row">
          <input v-model="form.defaultFlag" type="checkbox" />
          <span>设为默认地址</span>
        </label>

        <div v-if="fullAddressPreview" class="address-preview">{{ fullAddressPreview }}</div>
      </section>

      <p v-if="error" class="error-text">{{ error }}</p>
    </section>

    <footer class="save-bar">
      <button class="primary-button" type="button" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存地址' }}</button>
    </footer>

    <div v-if="confirmOpen && pendingParse" class="confirm-mask">
      <div class="confirm-sheet">
        <h2>请确认识别结果</h2>
        <p v-if="pendingParse.warnings?.length" class="confirm-warning">{{ pendingParse.warnings.join('，') }}</p>
        <div v-if="candidateOptions.length > 1" class="candidate-box">
          <span>收货人可能是</span>
          <div class="candidate-list">
            <button
              v-for="item in candidateOptions"
              :key="item"
              type="button"
              :class="selectedCandidate === item ? 'active' : ''"
              @click="selectedCandidate = item"
            >
              {{ item }}
            </button>
          </div>
        </div>
        <div class="confirm-lines">
          <div><span>手机号</span><strong>{{ pendingParse.phone || '-' }}</strong></div>
          <div><span>地区</span><strong>{{ parseRegionText(pendingParse) || '-' }}</strong></div>
          <div><span>详细地址</span><strong>{{ pendingParse.detailAddress || '-' }}</strong></div>
        </div>
        <div class="confirm-actions">
          <button type="button" @click="confirmOpen = false">取消</button>
          <button type="button" class="primary" @click="applyPendingParse">确认填入</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createAddress, getAddress, parseAddress, updateAddress } from '@/api/shop'
import type { AddressParseResult, ShopAddressPayload } from '@/types/shop'
import AddressRegionPicker from './components/AddressRegionPicker.vue'

const SELECTED_ADDRESS_KEY = 'rs-checkout-address-id'
const labelOptions = ['家', '公司', '学校', '父母', '朋友']

const route = useRoute()
const router = useRouter()
const rawText = ref('')
const parsing = ref(false)
const saving = ref(false)
const error = ref('')
const addressPath = ref<string[]>([])
const addressPathNames = ref<string[]>([])
const currentLabel = ref('')
const customLabel = ref('')
const customLabelMode = ref(false)
const pendingParse = ref<AddressParseResult>()
const confirmOpen = ref(false)
const selectedCandidate = ref('')
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  detailAddress: '',
  defaultFlag: false
})

const id = computed(() => typeof route.params.id === 'string' ? route.params.id : '')
const isEdit = computed(() => Boolean(id.value))
const selectMode = computed(() => route.query.select === '1')
const redirectPath = computed(() => typeof route.query.redirect === 'string' ? route.query.redirect : '/orders/new')
const candidateOptions = computed(() => {
  const values = pendingParse.value?.contactCandidates || []
  return Array.from(new Set(values.map(item => String(item || '').trim()).filter(Boolean)))
})
const fullAddressPreview = computed(() => {
  const regionText = visibleRegionNames(addressPathNames.value).join('')
  return `${regionText}${form.detailAddress}`.trim()
})

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/addresses')
  }
}

async function loadAddress() {
  if (!id.value) {
    return
  }
  const data = await getAddress(id.value)
  form.receiverName = data.receiverName || ''
  form.receiverPhone = data.receiverPhone || ''
  form.detailAddress = data.detailAddress || ''
  form.defaultFlag = Boolean(data.defaultFlag)
  addressPath.value = [...(data.regionPath || [])]
  addressPathNames.value = [...(data.regionPathNames || [])]
  setLabel(data.addressLabel || '')
}

async function parseRawAddress() {
  if (!rawText.value.trim()) {
    error.value = '请先粘贴需要识别的地址'
    return
  }
  parsing.value = true
  error.value = ''
  try {
    const result = await parseAddress(rawText.value)
    pendingParse.value = result
    selectedCandidate.value = result.contactName || candidateOptions.value[0] || ''
    if (needsConfirm(result)) {
      confirmOpen.value = true
    } else {
      applyParseResult(result)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '识别失败'
  } finally {
    parsing.value = false
  }
}

function needsConfirm(result: AddressParseResult) {
  return candidateOptions.value.length > 1 || Boolean(result.warnings?.length) || Number(result.confidence || 0) < 80
}

function applyPendingParse() {
  if (!pendingParse.value) {
    return
  }
  applyParseResult({ ...pendingParse.value, contactName: selectedCandidate.value || pendingParse.value.contactName })
  confirmOpen.value = false
}

function applyParseResult(result: AddressParseResult) {
  if (result.contactName) form.receiverName = result.contactName
  if (result.phone) form.receiverPhone = result.phone
  addressPath.value = [...(result.regionPath || [])]
  addressPathNames.value = [...(result.regionPathNames || visibleRegionNames([
    result.province,
    result.city,
    result.district,
    result.street,
    result.village
  ]))]
  if (result.detailAddress) form.detailAddress = result.detailAddress
}

function chooseLabel(label: string) {
  customLabelMode.value = false
  customLabel.value = ''
  currentLabel.value = currentLabel.value === label ? '' : label
}

function chooseCustomLabel() {
  customLabelMode.value = true
  currentLabel.value = ''
}

function setLabel(label: string) {
  if (!label) {
    currentLabel.value = ''
    customLabel.value = ''
    customLabelMode.value = false
  } else if (labelOptions.includes(label)) {
    currentLabel.value = label
    customLabel.value = ''
    customLabelMode.value = false
  } else {
    currentLabel.value = ''
    customLabel.value = label
    customLabelMode.value = true
  }
}

function currentAddressLabel() {
  return customLabelMode.value ? customLabel.value : currentLabel.value
}

function validate() {
  if (!form.receiverName.trim()) return '请填写收货人'
  if (!form.receiverPhone.trim()) return '请填写手机号'
  if (addressPath.value.length < 3) return '请选择省市区'
  if (!form.detailAddress.trim()) return '请填写详细地址'
  return ''
}

async function save() {
  error.value = validate()
  if (error.value) {
    return
  }
  const payload: ShopAddressPayload = {
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    regionPath: addressPath.value,
    regionPathNames: addressPathNames.value,
    detailAddress: form.detailAddress,
    addressLabel: currentAddressLabel(),
    defaultFlag: form.defaultFlag
  }
  saving.value = true
  try {
    const saved = isEdit.value ? await updateAddress(id.value, payload) : await createAddress(payload)
    if (selectMode.value) {
      sessionStorage.setItem(SELECTED_ADDRESS_KEY, saved.id)
      router.replace(redirectPath.value)
    } else {
      router.replace('/addresses')
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function visibleRegionNames(values: Array<string | undefined>) {
  return values.filter(Boolean).filter((item, index) => !(index === 1 && ['市辖区', '县'].includes(String(item)))) as string[]
}

function parseRegionText(result: AddressParseResult) {
  return visibleRegionNames([result.province, result.city, result.district, result.street, result.village]).join(' / ')
}

onMounted(loadAddress)
</script>

<style scoped>
.address-edit-page {
  padding-bottom: calc(86px + env(safe-area-inset-bottom));
  background: var(--bg-page);
}

.edit-header {
  position: sticky;
  top: 0;
  z-index: 15;
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  gap: 8px;
  margin: -14px -14px 12px;
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px;
  border-bottom: 1px solid var(--border-soft);
  background: rgba(255, 247, 235, 0.96);
  backdrop-filter: blur(14px);
}

.edit-header h1 {
  margin: 0;
  text-align: center;
  font-size: 18px;
}

.back-button {
  min-height: 36px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.edit-content {
  display: grid;
  gap: 12px;
}

.card-title {
  margin-bottom: 10px;
  color: var(--text-main);
  font-weight: 900;
}

.parse-card {
  display: grid;
  gap: 10px;
}

.field > span {
  color: var(--text-sub);
  font-size: 13px;
}

.label-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.label-chip {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-pill);
  color: var(--text-main);
  background: #fff;
}

.label-chip.active {
  border-color: var(--brand-teal);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-weight: 900;
}

.default-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  color: var(--text-main);
  font-weight: 800;
}

.default-row input {
  width: 18px;
  height: 18px;
}

.address-preview {
  padding: 10px;
  border-radius: var(--radius-sm);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-size: 13px;
  line-height: 1.45;
}

.error-text {
  margin: 0;
  color: #c0392b;
}

.save-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
}

.save-bar button {
  width: min(100%, 612px);
  display: block;
  margin: 0 auto;
}

.confirm-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: flex-end;
  background: rgba(36, 27, 22, 0.48);
}

.confirm-sheet {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  padding: 18px 16px calc(16px + env(safe-area-inset-bottom));
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  background: #fff;
  box-shadow: var(--shadow-deep);
}

.confirm-sheet h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.confirm-warning {
  margin: 0 0 12px;
  color: #9b5a13;
  font-size: 13px;
  line-height: 1.45;
}

.candidate-box {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.candidate-box span,
.confirm-lines span {
  color: var(--text-sub);
  font-size: 13px;
}

.candidate-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.candidate-list button {
  min-height: 34px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  color: var(--text-main);
  background: var(--bg-muted);
}

.candidate-list button.active {
  color: #fff;
  background: var(--brand-teal);
  font-weight: 900;
}

.confirm-lines {
  display: grid;
  gap: 8px;
}

.confirm-lines div {
  display: grid;
  gap: 3px;
}

.confirm-lines strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.45;
}

.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.confirm-actions button {
  min-height: 42px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-weight: 900;
}

.confirm-actions .primary {
  color: #fff;
  background: var(--brand-teal);
}
</style>
