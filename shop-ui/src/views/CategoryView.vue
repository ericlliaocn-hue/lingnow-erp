<template>
  <main class="page category-page">
    <header class="simple-header">
      <div class="page-inner">
        <h1>商品分类</h1>
        <p>按场景挑选适合的衣架商品</p>
      </div>
    </header>

    <section class="page-inner category-layout">
      <CustomerPicker class="customer-row" />
      <aside class="category-menu">
        <button
          v-for="item in categories"
          :key="item.id"
          :class="{ active: activeCategory === item.id }"
          type="button"
          @click="selectCategory(item.id)"
        >
          {{ item.name }}
        </button>
      </aside>

      <section class="category-content">
        <div class="category-banner">
          <span>{{ activeCategoryLabel }}</span>
          <strong>{{ products.length }} 件商品</strong>
        </div>

        <div v-if="loading" class="empty">加载中...</div>
        <div v-else-if="products.length === 0" class="empty">这个分类暂时没有商品</div>
        <article v-for="item in products" :key="item.id" class="category-product">
          <img v-if="item.imageUrl" :src="item.imageUrl" alt="" loading="lazy" />
          <div v-else class="image-empty">荣时</div>
          <div>
            <h2 @click="goDetail(item)">{{ item.name }}</h2>
            <p>{{ item.spec || '多规格可选' }}</p>
            <strong>{{ priceLabel(item.salePrice) }}</strong>
            <div class="product-btns">
              <button type="button" class="detail-btn" @click="goDetail(item)">看详情</button>
              <button type="button" class="cart-btn" @click="configure(item)">选规格</button>
            </div>
          </div>
        </article>
      </section>
    </section>

    <ProductConfigurator :open="Boolean(configuringProduct)" :product="configuringProduct" @close="configuringProduct = null" @confirm="addConfigured" />

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCategories, listProducts } from '@/api/shop'
import { useCartStore } from '@/stores/cart'
import { priceLabel } from '@/utils/label'
import type { ShopCategory, ShopProduct } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'
import CustomerPicker from './components/CustomerPicker.vue'
import ProductConfigurator, { type ProductConfiguration } from './components/ProductConfigurator.vue'

const router = useRouter()
const cart = useCartStore()
const loading = ref(false)
const products = ref<ShopProduct[]>([])
const categoryRecords = ref<ShopCategory[]>([])
const activeCategory = ref('all')
const configuringProduct = ref<ShopProduct | null>(null)
const categories = computed(() => [{ id: 'all', name: '全部商品' } as ShopCategory, ...categoryRecords.value])

const activeCategoryLabel = computed(() => categories.value.find(item => item.id === activeCategory.value)?.name || '全部商品')

function goDetail(item: ShopProduct) {
  router.push(`/products/${item.id}`)
}

function configure(item: ShopProduct) {
  configuringProduct.value = item
}

function addConfigured(configuration: ProductConfiguration) {
  if (!configuringProduct.value) return
  cart.addConfigured(configuringProduct.value, configuration, configuration.qty)
  configuringProduct.value = null
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await listProducts({ current: 1, size: 500, categoryId: activeCategory.value === 'all' ? undefined : activeCategory.value })
    products.value = res.records
  } finally {
    loading.value = false
  }
}

async function selectCategory(id: string) {
  activeCategory.value = id
  await loadProducts()
}

onMounted(async () => {
  categoryRecords.value = await listCategories()
  await loadProducts()
})
</script>

<style scoped>
.category-page {
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

.category-layout {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
}

.customer-row { grid-column: 1 / -1; }

.category-menu {
  position: sticky;
  top: 12px;
  align-self: start;
  display: grid;
  gap: 8px;
}

.category-menu button {
  min-height: 42px;
  border-radius: var(--radius);
  color: #6d5b4c;
  background: var(--bg-card);
  font-size: 13px;
  font-weight: 800;
}

.category-menu button.active {
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
}

.category-content {
  display: grid;
  gap: 10px;
}

.category-banner,
.category-product {
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.category-banner {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  color: var(--brand-brown);
}

.category-product {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
  padding: 10px;
}

.category-product img,
.image-empty {
  width: 92px;
  height: 92px;
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

.category-product h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.3;
  cursor: pointer;
}

.category-product p {
  margin: 5px 0;
  color: var(--text-sub);
  font-size: 12px;
}

.category-product strong {
  color: var(--brand-orange);
}

.product-btns {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.detail-btn,
.cart-btn {
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 800;
}

.detail-btn {
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
}

.cart-btn {
  color: #fff;
  background: var(--brand-teal);
}
</style>
