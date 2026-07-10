<template>
  <main class="page address-list-page">
    <header class="address-header">
      <div class="header-row">
        <button class="back-button" type="button" @click="goBack">‹</button>
        <h1>收货地址</h1>
        <button class="manage-button" type="button" @click="manageMode = !manageMode">{{ manageMode ? '完成' : '管理' }}</button>
      </div>
      <div class="search-box">
        <input v-model.trim="keyword" class="input" placeholder="搜索姓名、手机号、地址" inputmode="search" />
      </div>
    </header>

    <section class="address-content">
      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="!addresses.length" class="address-empty">
        <strong>还没有收货地址</strong>
        <p>新增一个地址，下单时就能直接选择。</p>
        <button type="button" @click="addAddress">新增地址</button>
      </div>

      <template v-else>
        <article
          v-for="address in addresses"
          :key="address.id"
          :class="['address-card', selectedId === address.id ? 'active' : '']"
          @click="selectAddress(address)"
        >
          <div class="address-main">
            <div class="address-title">
              <strong>{{ address.receiverName }}</strong>
              <span>{{ address.receiverPhone }}</span>
            </div>
            <p>{{ address.fullAddress || address.detailAddress }}</p>
            <div class="tag-row">
              <em v-if="address.addressLabel">{{ address.addressLabel }}</em>
              <em v-if="address.defaultFlag" class="default-tag">默认</em>
            </div>
          </div>
          <div class="address-actions" @click.stop>
            <button type="button" @click="editAddress(address)">编辑</button>
            <button v-if="manageMode && !address.defaultFlag" type="button" @click="makeDefault(address)">设默认</button>
          </div>
        </article>
      </template>
    </section>

    <footer class="add-bar">
      <button type="button" @click="addAddress">新增收货地址</button>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAddresses, setDefaultAddress } from '@/api/shop'
import type { ShopAddress } from '@/types/shop'

const SELECTED_ADDRESS_KEY = 'rs-checkout-address-id'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const addresses = ref<ShopAddress[]>([])
const manageMode = ref(false)
const selectedId = ref(sessionStorage.getItem(SELECTED_ADDRESS_KEY) || '')
let searchTimer: ReturnType<typeof window.setTimeout> | undefined

const selectMode = computed(() => route.query.select === '1')
const redirectPath = computed(() => typeof route.query.redirect === 'string' ? route.query.redirect : '/orders/new')

watch(keyword, () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(loadAddresses, 220)
})

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/mine')
  }
}

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await listAddresses(keyword.value)
  } finally {
    loading.value = false
  }
}

function selectAddress(address: ShopAddress) {
  if (!selectMode.value) {
    return
  }
  sessionStorage.setItem(SELECTED_ADDRESS_KEY, address.id)
  selectedId.value = address.id
  router.replace(redirectPath.value)
}

function addAddress() {
  router.push({
    path: '/addresses/new',
    query: selectMode.value ? { select: '1', redirect: redirectPath.value } : undefined
  })
}

function editAddress(address: ShopAddress) {
  router.push({
    path: `/addresses/${address.id}/edit`,
    query: selectMode.value ? { select: '1', redirect: redirectPath.value } : undefined
  })
}

async function makeDefault(address: ShopAddress) {
  await setDefaultAddress(address.id)
  await loadAddresses()
}

onMounted(loadAddresses)
</script>

<style scoped>
.address-list-page {
  padding-bottom: calc(86px + env(safe-area-inset-bottom));
  background: var(--bg-page);
}

.address-header {
  position: sticky;
  top: 0;
  z-index: 15;
  margin: -14px -14px 12px;
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px;
  border-bottom: 1px solid var(--border-soft);
  background: rgba(255, 247, 235, 0.96);
  backdrop-filter: blur(14px);
}

.header-row {
  display: grid;
  grid-template-columns: 44px 1fr 52px;
  align-items: center;
  gap: 8px;
}

.header-row h1 {
  margin: 0;
  text-align: center;
  font-size: 18px;
}

.back-button,
.manage-button {
  min-height: 36px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-weight: 800;
}

.back-button {
  font-size: 24px;
  line-height: 1;
}

.manage-button {
  font-size: 13px;
}

.search-box {
  margin-top: 10px;
}

.search-box .input {
  border-radius: var(--radius-pill);
  background: #fff;
}

.address-content {
  display: grid;
  gap: 10px;
}

.address-card,
.address-empty {
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.address-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 14px;
}

.address-card.active {
  border-color: var(--brand-teal);
  box-shadow: 0 0 0 3px rgba(29, 109, 95, 0.12);
}

.address-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.address-title strong {
  color: var(--text-main);
  font-size: 16px;
}

.address-title span {
  color: var(--text-sub);
  font-size: 13px;
}

.address-main p {
  margin: 8px 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.45;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-row em {
  padding: 3px 8px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 12px;
  font-style: normal;
}

.tag-row .default-tag {
  color: var(--brand-teal);
  background: #e6f2ef;
}

.address-actions {
  display: grid;
  align-content: start;
  gap: 8px;
}

.address-actions button {
  min-width: 54px;
  min-height: 32px;
  border-radius: var(--radius-pill);
  color: var(--brand-teal);
  background: #e6f2ef;
  font-size: 12px;
  font-weight: 800;
}

.address-empty {
  padding: 32px 18px;
  text-align: center;
}

.address-empty strong {
  font-size: 18px;
}

.address-empty p {
  color: var(--text-sub);
}

.address-empty button,
.add-bar button {
  min-height: 44px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 900;
}

.address-empty button {
  padding: 0 22px;
}

.add-bar {
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

.add-bar button {
  width: min(100%, 612px);
  display: block;
  margin: 0 auto;
}
</style>
