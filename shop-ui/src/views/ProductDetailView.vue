<template>
  <main class="page detail-page">
    <!-- 返回栏 -->
    <header class="detail-header">
      <div class="page-inner header-inner">
        <button class="back-btn" type="button" @click="goBack">‹</button>
        <h1>商品详情</h1>
        <RouterLink class="home-btn" to="/home">首页</RouterLink>
      </div>
    </header>

    <section class="page-inner detail-content">
      <div v-if="loading" class="skeleton gallery-skeleton"></div>
      <template v-else-if="product">
        <!-- 商品大图 -->
        <section class="gallery">
          <img v-if="product.imageUrl" :src="product.imageUrl" alt="" />
          <div v-else class="gallery-empty">荣时衣架</div>
        </section>

        <!-- 价格与标题 -->
        <section class="info-card">
          <div class="price-row">
            <strong class="price-main">{{ priceLabel(product.salePrice) }}</strong>
            <span v-if="product.unitName" class="price-unit">/ {{ product.unitName }}</span>
          </div>
          <h2 class="info-name">{{ product.name }}</h2>
          <p v-if="product.spec" class="info-spec">{{ product.spec }}</p>
          <div v-if="tags.length" class="info-tags">
            <span v-for="tag in tags" :key="tag" class="info-tag">{{ tag }}</span>
          </div>
        </section>

        <!-- 商品参数 -->
        <section class="param-card">
          <h3>商品参数</h3>
          <dl>
            <div class="param-row">
              <dt>规格</dt>
              <dd>{{ product.spec || '多规格可选' }}</dd>
            </div>
            <div v-if="product.unitName" class="param-row">
              <dt>计价单位</dt>
              <dd>{{ product.unitName }}</dd>
            </div>
            <div v-if="product.code" class="param-row">
              <dt>货号</dt>
              <dd>{{ product.code }}</dd>
            </div>
            <div class="param-row">
              <dt>材质</dt>
              <dd>{{ materialText }}</dd>
            </div>
          </dl>
        </section>

        <!-- 数量选择 -->
        <section class="qty-card">
          <span>数量</span>
          <div class="qty-stepper">
            <button type="button" @click="changeQty(-1)">－</button>
            <input v-model.number="qty" type="number" min="1" step="1" inputmode="numeric" />
            <button type="button" @click="changeQty(1)">＋</button>
          </div>
        </section>

        <!-- 商品描述 -->
        <section class="desc-card">
          <h3>商品描述</h3>
          <p>{{ descriptionText }}</p>
        </section>
      </template>
      <div v-else class="empty">商品不存在或已下架</div>
    </section>

    <!-- 底部固定操作栏 -->
    <footer v-if="product" class="action-bar">
      <div class="page-inner action-inner">
        <RouterLink class="action-icon" to="/cart">
          <span class="action-emoji">⌕</span>
          <small>购物车</small>
          <em v-if="cart.totalQty">{{ cart.totalQty > 99 ? '99+' : cart.totalQty }}</em>
        </RouterLink>
        <button class="action-btn cart" type="button" @click="addToCart">加入购物车</button>
        <button class="action-btn buy" type="button" @click="buyNow">立即购买</button>
      </div>
    </footer>
    <ProductConfigurator
      :open="configOpen"
      :product="product"
      :initial-qty="qty"
      @close="configOpen = false"
      @confirm="addConfigured"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduct } from '@/api/shop'
import { useCartStore } from '@/stores/cart'
import { priceLabel, productTags } from '@/utils/label'
import type { ShopProduct } from '@/types/shop'
import ProductConfigurator, { type ProductConfiguration } from './components/ProductConfigurator.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const product = ref<ShopProduct | null>(null)
const loading = ref(false)
const qty = ref(1)
const configOpen = ref(false)

const tags = computed(() => (product.value ? productTags(product.value) : []))
const materialText = computed(() => {
  if (!product.value) return '木质'
  const text = `${product.value.name || ''} ${product.value.spec || ''} ${product.value.attributeText || ''}`
  if (/榉木|橡胶木|荷木|实木|木质/.test(text)) {
    const m = text.match(/(榉木|橡胶木|荷木|实木|木质)/)
    return m ? m[1] : '木质'
  }
  return '木质'
})
const descriptionText = computed(() => {
  if (!product.value) return ''
  const p = product.value
  const parts = [
    p.name,
    p.spec ? `规格 ${p.spec}` : '',
    '采用优质木材精细打磨，适合服装门店陈列、品牌展示与日常收纳',
    p.unitName ? `按${p.unitName}计价` : ''
  ].filter(Boolean)
  return parts.join('；') + '。'
})

function changeQty(delta: number) {
  const next = Number(qty.value || 1) + delta
  qty.value = Math.max(1, next)
}

function addToCart() {
  configOpen.value = true
}

function buyNow() {
  configOpen.value = true
}

function addConfigured(configuration: ProductConfiguration) {
  if (!product.value) return
  cart.addConfigured(product.value, configuration, configuration.qty)
  configOpen.value = false
  router.push('/cart')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

async function loadProduct() {
  const id = String(route.params.id || '')
  if (!id) return
  loading.value = true
  try {
    product.value = await getProduct(id)
    configOpen.value = true
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadProduct)
watch(() => route.params.id, loadProduct)
</script>

<style scoped>
.detail-page {
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
  background: var(--bg-page);
}

.detail-header {
  position: sticky;
  top: 0;
  z-index: 10;
  margin: -14px -14px 12px;
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px;
  background: rgba(255, 247, 235, 0.94);
  border-bottom: 1px solid var(--border-soft);
  backdrop-filter: blur(14px);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.back-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 22px;
  line-height: 1;
}

.detail-header h1 {
  margin: 0;
  color: var(--text-main);
  font-size: 18px;
  font-weight: 800;
}

.home-btn {
  color: var(--brand-teal);
  font-size: 13px;
  font-weight: 800;
}

.detail-content {
  display: grid;
  gap: 12px;
}

.gallery-skeleton {
  height: 340px;
  border-radius: var(--radius-lg);
}

.gallery {
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.gallery img,
.gallery-empty {
  width: 100%;
  height: 340px;
  object-fit: contain;
  background: var(--bg-muted);
}

.gallery-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 18px;
  font-weight: 900;
}

.info-card {
  padding: 16px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

.price-main {
  color: var(--brand-orange);
  font-size: 28px;
  font-weight: 900;
}

.price-unit {
  color: var(--text-sub);
  font-size: 13px;
}

.info-name {
  margin: 0 0 6px;
  color: var(--text-main);
  font-size: 18px;
  line-height: 1.4;
}

.info-spec {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.info-tag {
  padding: 3px 9px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 12px;
  font-weight: 700;
}

.param-card,
.qty-card,
.desc-card {
  padding: 14px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.param-card h3,
.desc-card h3 {
  margin: 0 0 12px;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
}

.param-row {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-soft);
}

.param-row:last-child {
  border-bottom: 0;
}

.param-row dt {
  flex: 0 0 88px;
  color: var(--text-sub);
  font-size: 13px;
}

.param-row dd {
  margin: 0;
  color: var(--text-main);
  font-size: 13px;
}

.qty-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
}

.qty-stepper {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.qty-stepper button {
  width: 38px;
  height: 36px;
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 18px;
}

.qty-stepper input {
  width: 48px;
  height: 36px;
  border: 0;
  border-left: 1px solid var(--border-line);
  border-right: 1px solid var(--border-line);
  text-align: center;
  color: var(--text-main);
  background: #fff;
  outline: none;
}

.desc-card p {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 8px 14px calc(8px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border-line);
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(14px);
}

.action-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-icon {
  position: relative;
  display: grid;
  gap: 2px;
  place-items: center;
  padding: 2px 6px;
  color: var(--text-sub);
  text-align: center;
}

.action-emoji {
  font-size: 20px;
  line-height: 1;
}

.action-icon small {
  font-size: 11px;
}

.action-icon em {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-orange);
  font-style: normal;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.action-btn {
  flex: 1;
  min-height: 44px;
  border-radius: var(--radius-pill);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
}

.action-btn.cart {
  background: var(--brand-brown);
}

.action-btn.buy {
  background: var(--brand-orange);
}
</style>
