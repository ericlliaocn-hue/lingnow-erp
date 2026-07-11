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
            <button v-if="item.productImageUrl" class="item-image-button" type="button" @click="previewImage(item.productImageUrl)">
              <img :src="item.productImageUrl" alt="" />
            </button>
            <div v-else class="item-img-empty">荣时</div>
            <div>
              <strong>{{ item.productName }}</strong>
              <p v-if="item.spec">{{ item.spec }}</p>
              <div v-if="formatAttributeLines(item.optionAttributeText).length" class="attr-lines">
                <div v-for="(line, index) in formatAttributeLines(item.optionAttributeText)" :key="index" class="attr-line">
                  <span v-if="line.label">{{ displayShopLabel(line.label) }}：</span>
                  <strong>{{ displayShopLabel(line.value) }}</strong>
                </div>
              </div>
            </div>
          </div>
          <div v-if="item.logoImageUrl" class="logo-line">
            <span>Logo / 图案参考</span>
            <button class="logo-image-button" type="button" @click="previewImage(item.logoImageUrl)">
              <img :src="item.logoImageUrl" alt="" />
            </button>
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
    <div v-if="previewImageUrl" class="image-viewer" @click="closeImagePreview">
      <button type="button" class="image-viewer-close" @click.stop="closeImagePreview">关闭</button>
      <img :src="previewImageUrl" alt="图片预览" @click.stop />
    </div>
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
const previewImageUrl = ref('')

type AttributeLine = { label: string, value: string }

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function formatAttributeLines(value?: string): AttributeLine[] {
  return String(value || '')
    .split(/\s*\/\s*|[；;]\s*/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(item => {
      const match = item.match(/^([^:：]+)[:：]\s*(.+)$/)
      return match ? { label: (match[1] || '').trim(), value: (match[2] || '').trim() } : { label: '', value: item }
    })
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

.item-image-button,
.item-img-empty {
  width: 82px;
  height: 82px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.item-image-button {
  padding: 0;
  overflow: hidden;
}
.item-image-button img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.attr-lines {
  display: grid;
  gap: 3px;
  margin-top: 6px;
}
.attr-line span {
  color: var(--text-sub);
  font-size: 13px;
  font-weight: 400;
}
.attr-line strong {
  color: var(--brand-brown-soft);
  font-size: 13px;
  font-weight: 800;
}

.logo-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.logo-image-button {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  padding: 0;
  overflow: hidden;
}
.logo-image-button img {
  width: 100%;
  height: 100%;
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
