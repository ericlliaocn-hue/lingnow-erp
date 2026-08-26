<template>
  <section class="customer-picker">
    <button type="button" class="customer-trigger" @click="open = true">
      <span>
        <small>本次代客下单</small>
        <strong>{{ selectedName || '请选择客户' }}</strong>
      </span>
      <em>切换 ›</em>
    </button>

    <div v-if="open" class="picker-mask" @click="open = false">
      <div class="picker-sheet" @click.stop>
        <header>
          <h2>选择客户</h2>
          <button type="button" @click="open = false">关闭</button>
        </header>
        <input v-model.trim="keyword" class="input" placeholder="搜索客户名称、联系人、手机号" />
        <div class="customer-list">
          <button
            v-for="item in filtered"
            :key="item.id"
            type="button"
            :class="['customer-row', String(item.id) === selectedId ? 'active' : '']"
            @click="select(item)"
          >
            <span><strong>{{ item.name }}</strong><small>{{ [item.contact, item.phone].filter(Boolean).join(' · ') || item.code }}</small></span>
            <em>{{ String(item.id) === selectedId ? '已选择' : '选择' }}</em>
          </button>
          <p v-if="!loading && !filtered.length" class="empty">没有找到客户</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listCustomers } from '@/api/shop'
import type { ShopCustomer } from '@/types/shop'

const emit = defineEmits<{ (e: 'change', customer: ShopCustomer): void }>()
const open = ref(false)
const loading = ref(false)
const keyword = ref('')
const customers = ref<ShopCustomer[]>([])
const selectedId = ref(localStorage.getItem('sales-customer-id') || '')
const selectedName = ref(localStorage.getItem('sales-customer-name') || '')
const filtered = computed(() => {
  const value = keyword.value.toLowerCase()
  if (!value) return customers.value
  return customers.value.filter(item => `${item.name} ${item.contact || ''} ${item.phone || ''}`.toLowerCase().includes(value))
})

async function load() {
  loading.value = true
  try {
    customers.value = await listCustomers()
    if (selectedId.value && !customers.value.some(item => String(item.id) === selectedId.value)) {
      clearSelection()
    }
  } finally {
    loading.value = false
  }
}

function select(customer: ShopCustomer) {
  selectedId.value = String(customer.id)
  selectedName.value = customer.name
  localStorage.setItem('sales-customer-id', String(customer.id))
  localStorage.setItem('sales-customer-name', customer.name)
  sessionStorage.removeItem('rs-checkout-address-id')
  open.value = false
  emit('change', customer)
}

function clearSelection() {
  selectedId.value = ''
  selectedName.value = ''
  localStorage.removeItem('sales-customer-id')
  localStorage.removeItem('sales-customer-name')
}

watch(open, value => {
  document.body.style.overflow = value ? 'hidden' : ''
})
onMounted(load)
</script>

<style scoped>
.customer-trigger { width: 100%; min-height: 58px; padding: 10px 14px; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--border-soft); border-radius: var(--radius); background: #fff; text-align: left; box-shadow: var(--shadow-card); }
.customer-trigger span { display: grid; gap: 3px; }
.customer-trigger small { color: var(--text-sub); font-size: 11px; }
.customer-trigger strong { color: var(--text-main); font-size: 15px; }
.customer-trigger em { color: var(--brand-teal); font-size: 12px; font-style: normal; font-weight: 800; }
.picker-mask { position: fixed; inset: 0; z-index: 80; display: flex; align-items: flex-end; background: rgba(36,27,22,.48); }
.picker-sheet { width: 100%; max-width: 640px; max-height: 78vh; margin: 0 auto; padding: 16px; border-radius: 20px 20px 0 0; background: #fff; display: grid; gap: 12px; }
.picker-sheet header { display: flex; align-items: center; justify-content: space-between; }
.picker-sheet h2 { margin: 0; font-size: 18px; }
.picker-sheet header button { color: var(--text-sub); }
.customer-list { overflow-y: auto; display: grid; gap: 8px; }
.customer-row { width: 100%; padding: 12px; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--border-soft); border-radius: var(--radius); background: var(--bg-muted); text-align: left; }
.customer-row.active { border-color: var(--brand-teal); background: #e6f2ef; }
.customer-row span { display: grid; gap: 4px; }
.customer-row small { color: var(--text-sub); }
.customer-row em { color: var(--brand-teal); font-style: normal; font-size: 12px; font-weight: 800; }
.empty { text-align: center; color: var(--text-sub); }
</style>
