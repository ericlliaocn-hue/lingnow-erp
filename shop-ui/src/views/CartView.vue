<template>
  <main class="page cart-page">
    <header class="simple-header">
      <div class="page-inner">
        <h1>购物车</h1>
        <p>确认想要的商品，再一起填写购买信息</p>
      </div>
    </header>

    <section class="page-inner cart-content">
      <div v-if="cart.items.length === 0" class="cart-empty">
        <strong>购物车还是空的</strong>
        <p>去首页挑选衣架、裤架和配件商品。</p>
        <RouterLink to="/home">去逛逛</RouterLink>
      </div>

      <article v-for="group in productGroups" :key="group.productId" class="cart-item">
        <img v-if="group.productImageUrl" :src="group.productImageUrl" alt="" loading="lazy" />
        <div v-else class="image-empty">荣时</div>
        <div class="cart-info">
          <h2 @click="goDetail(group.productId)">{{ group.productName }}</h2>
          <p>{{ group.spec || '多规格可选' }}</p>
          <p class="main-product-line"><strong>主商品合计</strong><span>× {{ group.totalQty }}</span></p>

          <section v-for="(item, index) in group.items" :key="item.key" class="configuration-row">
            <div class="configuration-row-head">
              <strong>配置 {{ index + 1 }}</strong>
              <span>{{ priceLabel(item.price) }} / 件</span>
            </div>
            <div v-if="item.optionAttributeText" class="configured-options">
              <p v-for="part in configurationParts(item.optionAttributeText)" :key="part">{{ part }}</p>
            </div>
            <p v-else class="standard-configuration">标准配置</p>
            <p v-if="item.logoImageUrl" class="logo-mark">已上传当前配置的 Logo / 图案</p>
            <p v-if="item.basePrice <= 0" class="price-warning">销售价未维护，暂不能提交订单</p>
            <p v-else class="price-detail">基础价 ￥{{ money(item.basePrice) }}<template v-if="item.attributeExtraAmount > 0"> ＋ 选配 ￥{{ money(item.attributeExtraAmount) }}</template></p>
            <div class="configuration-foot">
              <div class="qty-stepper">
                <button type="button" @click="cart.updateQty(item.key, item.qty - 1)">−</button>
                <span>{{ item.qty }}</span>
                <button type="button" @click="cart.updateQty(item.key, item.qty + 1)">＋</button>
              </div>
              <span v-if="item.price > 0" class="line-subtotal">小计 ￥{{ money(item.price * item.qty) }}</span>
              <button class="remove" type="button" @click="cart.remove(item.key)">删除</button>
            </div>
          </section>

          <div class="group-total">本商品合计：<strong>￥{{ money(group.totalAmount) }}</strong></div>
        </div>
      </article>
    </section>

    <footer v-if="cart.items.length" class="cart-bar">
      <div>
        <span>合计</span>
        <strong>{{ cart.totalAmount > 0 ? `￥${money(cart.totalAmount)}` : '询价' }}</strong>
      </div>
      <button type="button" :disabled="hasInvalidPrice" @click="checkout">{{ hasInvalidPrice ? '存在未维护价格' : '去填写订单' }}</button>
    </footer>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { priceLabel } from '@/utils/label'
import type { CartItem } from '@/stores/cart'
import BottomNav from './components/BottomNav.vue'

const router = useRouter()
const cart = useCartStore()
const hasInvalidPrice = computed(() => cart.items.some(item => Number(item.basePrice || 0) <= 0))
const productGroups = computed(() => {
  const groups = new Map<string, {
    productId: string
    productName: string
    productImageUrl?: string
    spec?: string
    items: CartItem[]
    totalQty: number
    totalAmount: number
  }>()
  cart.items.forEach(item => {
    const group = groups.get(item.productId) || {
      productId: item.productId,
      productName: item.productName,
      productImageUrl: item.productImageUrl,
      spec: item.spec,
      items: [],
      totalQty: 0,
      totalAmount: 0
    }
    group.items.push(item)
    group.totalQty += Number(item.qty || 0)
    group.totalAmount += Number(item.price || 0) * Number(item.qty || 0)
    groups.set(item.productId, group)
  })
  return [...groups.values()]
})

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function goDetail(id: string) {
  router.push(`/products/${id}`)
}

function configurationParts(value?: string) {
  return (value || '').split('/').map(item => item.trim()).filter(Boolean)
}

function checkout() {
  if (hasInvalidPrice.value) return
  router.push(cart.items.length ? '/orders/new?fromCart=1' : '/home')
}
</script>

<style scoped>
.cart-page {
  padding-bottom: calc(150px + env(safe-area-inset-bottom));
  background: var(--bg-page);
}

.simple-header {
  margin: -14px -14px 12px;
  padding: calc(16px + env(safe-area-inset-top)) 14px 14px;
  background: var(--bg-cream);
  border-bottom: 1px solid var(--border-soft);
}

.simple-header h1 {
  margin: 0;
  color: var(--text-main);
  font-size: 22px;
}

.simple-header p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.cart-content {
  display: grid;
  gap: 10px;
}

.cart-empty,
.cart-item {
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.cart-empty {
  padding: 28px 18px;
  text-align: center;
}

.cart-empty strong {
  color: var(--text-main);
  font-size: 18px;
}

.cart-empty p {
  color: var(--text-sub);
}

.cart-empty a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 800;
}

.cart-item {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 10px;
  padding: 10px;
}

.cart-item img,
.image-empty {
  width: 96px;
  height: 96px;
  border-radius: var(--radius);
  object-fit: contain;
  background: var(--bg-muted);
}

.image-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 13px;
  font-weight: 800;
}

.cart-info h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.3;
  cursor: pointer;
}

.cart-info > p {
  margin: 5px 0;
  color: var(--text-sub);
  font-size: 12px;
}

.main-product-line { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 8px; border-radius: 7px; color: var(--text-main) !important; background: var(--bg-cream-soft); }
.main-product-line strong { font-size: 12px; }
.main-product-line span { color: var(--brand-brown); font-weight: 800; }
.configuration-row { margin-top: 8px; padding: 9px; border: 1px solid #c8dfd9; border-radius: 9px; background: #f8fbfa; }
.configuration-row-head, .configuration-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.configuration-row-head strong { color: var(--brand-teal); font-size: 12px; }
.configuration-row-head span { color: var(--brand-orange); font-size: 12px; font-weight: 800; }
.configured-options { display: grid; gap: 3px; margin: 7px 0; padding: 7px 8px; border-radius: 7px; color: var(--brand-teal); background: #eef7f5; }
.configured-options p { margin: 0; color: var(--text-sub); font-size: 12px; line-height: 1.45; }
.standard-configuration, .logo-mark { margin: 7px 0 !important; color: var(--text-sub) !important; font-size: 12px !important; }
.logo-mark { color: var(--brand-teal) !important; }
.price-detail { color: var(--text-sub) !important; font-size: 11px !important; }
.price-warning { color: #9b2c2c !important; font-weight: 800; }
.line-subtotal {
  color: var(--text-sub);
  font-size: 12px;
}
.configuration-foot { margin-top: 8px; }
.group-total { margin-top: 9px; text-align: right; color: var(--text-sub); font-size: 12px; }
.group-total strong { color: var(--brand-orange); font-size: 15px; }

.qty-stepper {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.qty-stepper button {
  width: 32px;
  height: 30px;
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-weight: 900;
}

.qty-stepper span {
  min-width: 34px;
  text-align: center;
  border-left: 1px solid var(--border-line);
  border-right: 1px solid var(--border-line);
  line-height: 30px;
}

.remove {
  min-width: 48px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  color: #9b2c2c;
  background: #ffe9e7;
  font-size: 12px;
}

.cart-bar {
  position: fixed;
  right: 0;
  bottom: calc(65px + env(safe-area-inset-bottom));
  left: 0;
  z-index: 19;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-top: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
}

.cart-bar div {
  display: grid;
  gap: 2px;
}

.cart-bar span {
  color: var(--text-sub);
  font-size: 12px;
}

.cart-bar strong {
  color: var(--brand-orange);
  font-size: 20px;
}

.cart-bar button {
  min-width: 132px;
  min-height: 42px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-orange);
  font-weight: 900;
}

.cart-bar button:disabled { opacity: .55; }
</style>
