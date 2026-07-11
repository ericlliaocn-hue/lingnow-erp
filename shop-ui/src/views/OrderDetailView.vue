<template>
  <main class="page">
    <header class="topbar">
      <div class="topbar-inner">
        <div>
          <h1>订单详情</h1>
          <p>{{ order?.orderNo || '订单详情' }}</p>
        </div>
        <button class="plain-button" @click="router.back()">返回</button>
      </div>
    </header>

    <section class="page-inner">
      <div v-if="loading" class="empty">加载中...</div>
      <template v-else-if="order">
        <div class="card detail-card">
          <div class="detail-line">
            <span>订单状态</span>
            <strong :class="statusClass(order.status)">{{ statusText(order.status) }}</strong>
          </div>
          <div class="detail-line"><span>下单时间</span><strong>{{ order.orderTime || '-' }}</strong></div>
          <div class="detail-line"><span>收货人</span><strong>{{ order.receiverName || '-' }}</strong></div>
          <div class="detail-line"><span>联系电话</span><strong>{{ order.receiverPhone || '-' }}</strong></div>
          <div class="detail-block"><span>收货地址</span><p>{{ order.receiverAddress || '-' }}</p></div>
          <div v-if="order.remark" class="detail-block"><span>订单留言</span><p>{{ order.remark }}</p></div>
        </div>

        <article v-for="item in order.items || []" :key="item.id || item.productId" class="card item-card">
          <div class="item-main">
            <img v-if="item.productImageUrl" :src="item.productImageUrl" alt="" />
            <div v-else class="item-img-empty">荣时</div>
            <div>
              <strong>{{ item.productName }}</strong>
              <p v-if="item.spec">{{ item.spec }}</p>
              <p v-if="item.optionAttributeText" class="attr-text">{{ displayAttributeText(item.optionAttributeText) }}</p>
            </div>
          </div>
          <div v-if="item.logoImageUrl" class="logo-line">
            <span>Logo / 图案参考</span>
            <img :src="item.logoImageUrl" alt="" />
          </div>
          <div class="item-total">
            <span>{{ Number(item.qty || 0).toFixed(0) }} × ￥{{ money(item.price) }}</span>
            <strong class="price">￥{{ money(item.amount) }}</strong>
          </div>
        </article>

        <div class="card summary">
          <div><span>共 {{ Number(order.totalQty || 0).toFixed(0) }} 件</span><strong>{{ Number(order.totalQty || 0).toFixed(0) }} 件</strong></div>
          <div><span>合计</span><strong class="price">￥{{ money(order.totalAmount) }}</strong></div>
        </div>
      </template>
      <div v-else class="empty">订单不存在</div>
    </section>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder } from '@/api/shop'
import { displayShopLabel, statusText, statusClass } from '@/utils/label'
import type { CustomerOrder } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'

const route = useRoute()
const router = useRouter()
const order = ref<CustomerOrder>()
const loading = ref(false)

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function displayAttributeText(value?: string) {
  return String(value || '')
    .split(/[\/,，]/)
    .map(item => displayShopLabel(item))
    .filter(Boolean)
    .join(' / ')
}

async function load() {
  const id = String(route.params.id || '')
  if (!id) {
    return
  }
  loading.value = true
  try {
    order.value = await getOrder(id)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-card,
.item-card,
.summary {
  display: grid;
  gap: 10px;
}

.detail-line,
.item-total,
.summary div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-line span,
.detail-block span,
.logo-line span {
  color: var(--text-sub);
  font-size: 13px;
}

.detail-line strong {
  text-align: right;
}

.detail-block p {
  margin: 6px 0 0;
  word-break: break-word;
}

.item-main {
  display: grid;
  grid-template-columns: 82px 1fr;
  gap: 10px;
}

.item-main img,
.item-img-empty {
  width: 82px;
  height: 82px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--bg-muted);
}

.item-img-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 11px;
  font-weight: 800;
}

.item-main strong {
  color: var(--text-main);
}

.item-main p {
  margin: 5px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.attr-text {
  color: var(--brand-brown-soft);
}

.logo-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.logo-line img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  border: 1px solid var(--border-line);
}

.item-total {
  padding-top: 8px;
  border-top: 1px solid var(--border-soft);
}

.item-total span {
  color: var(--text-sub);
  font-size: 13px;
}

.summary span {
  color: var(--text-sub);
}
</style>
