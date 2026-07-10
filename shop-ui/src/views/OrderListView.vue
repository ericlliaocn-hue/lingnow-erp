<template>
  <main class="page">
    <header class="topbar">
      <div class="topbar-inner">
        <div>
          <h1>我的订单</h1>
          <p>查看购买记录</p>
        </div>
        <button class="plain-button" @click="reload">刷新</button>
      </div>
    </header>

    <section class="page-inner">
      <div v-if="loading && orders.length === 0" class="empty">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty">还没有订单，去首页逛逛吧</div>
      <RouterLink v-for="item in orders" :key="item.id" class="order-card" :to="`/orders/${item.id}`">
        <div class="order-head">
          <strong>订单号 {{ item.orderNo }}</strong>
          <span :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
        </div>
        <div class="order-meta">{{ item.orderTime || '-' }}</div>
        <div class="order-foot">
          <span>共 {{ Number(item.totalQty || 0).toFixed(0) }} 件</span>
          <strong class="price">￥{{ money(item.totalAmount) }}</strong>
        </div>
      </RouterLink>

      <button v-if="hasMore" class="secondary-button" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </section>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listOrders } from '@/api/shop'
import { statusText, statusClass } from '@/utils/label'
import type { CustomerOrder } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'

const current = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const orders = ref<CustomerOrder[]>([])
const hasMore = computed(() => orders.value.length < total.value)

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

async function fetchList(reset = false) {
  loading.value = true
  try {
    const res = await listOrders({ current: current.value, size })
    orders.value = reset ? res.records : orders.value.concat(res.records)
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  current.value = 1
  fetchList(true)
}

function loadMore() {
  current.value += 1
  fetchList()
}

onMounted(() => fetchList(true))
</script>

<style scoped>
.order-card {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.order-head,
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.order-head strong {
  color: var(--text-main);
  font-size: 14px;
}

.order-meta {
  color: var(--text-sub);
  font-size: 13px;
}

.order-foot span {
  color: var(--text-sub);
  font-size: 13px;
}
</style>
