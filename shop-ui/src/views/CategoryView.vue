<template>
  <main class="page category-page">
    <header class="simple-header">
      <div class="page-inner">
        <h1>商品分类</h1>
        <p>选择分类快速找到商品</p>
      </div>
    </header>

    <section class="page-inner category-layout">
      <CustomerPicker class="customer-row" />
      <aside class="category-menu" aria-label="商品分类">
        <button :class="{ active: activeLevel1 === 'all' }" type="button" @click="selectLevel1('all')">全部商品</button>
        <button
          v-for="item in rootCategories"
          :key="item.id"
          :class="{ active: activeLevel1 === item.id }"
          type="button"
          @click="selectLevel1(item.id)"
        >
          {{ item.name }}
        </button>
      </aside>

      <section class="category-content">
        <div v-if="level2Categories.length" class="subcategory-panel">
          <div class="subcategory-list">
            <button :class="{ active: !activeLevel2 }" type="button" @click="selectLevel2('')">全部</button>
            <button
              v-for="item in level2Categories"
              :key="item.id"
              :class="{ active: activeLevel2 === item.id }"
              type="button"
              @click="selectLevel2(item.id)"
            >
              {{ item.name }}
            </button>
          </div>
        </div>

        <div v-if="level3Categories.length" class="subcategory-panel third-level">
          <div class="subcategory-list">
            <button :class="{ active: !activeLevel3 }" type="button" @click="selectLevel3('')">全部</button>
            <button
              v-for="item in level3Categories"
              :key="item.id"
              :class="{ active: activeLevel3 === item.id }"
              type="button"
              @click="selectLevel3(item.id)"
            >
              {{ item.name }}
            </button>
          </div>
        </div>

        <div class="category-banner">
          <div>
            <small v-if="activePathLabel">{{ activePathLabel }}</small>
            <span>{{ activeCategoryLabel }}</span>
          </div>
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
import { buildCategoryTree, categoryPath } from '@/utils/category'
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
const activeLevel1 = ref('all')
const activeLevel2 = ref('')
const activeLevel3 = ref('')
const configuringProduct = ref<ShopProduct | null>(null)

const categoryTree = computed(() => buildCategoryTree(categoryRecords.value))
const rootCategories = computed(() => categoryTree.value)
const activeRoot = computed(() => rootCategories.value.find(item => item.id === activeLevel1.value))
const level2Categories = computed(() => activeRoot.value?.children || [])
const activeSecond = computed(() => level2Categories.value.find(item => item.id === activeLevel2.value))
const level3Categories = computed(() => activeSecond.value?.children || [])
const selectedCategoryId = computed(() => activeLevel3.value || activeLevel2.value || (activeLevel1.value === 'all' ? '' : activeLevel1.value))
const activePath = computed(() => categoryPath(categoryRecords.value, selectedCategoryId.value))
const activeCategoryLabel = computed(() => activePath.value[activePath.value.length - 1]?.name || '全部商品')
const activePathLabel = computed(() => activePath.value.slice(0, -1).map(item => item.name).join(' / '))

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
    const res = await listProducts({ current: 1, size: 500, categoryId: selectedCategoryId.value || undefined })
    products.value = res.records
  } finally {
    loading.value = false
  }
}

async function selectLevel1(id: string) {
  activeLevel1.value = id
  activeLevel2.value = ''
  activeLevel3.value = ''
  await loadProducts()
}

async function selectLevel2(id: string) {
  activeLevel2.value = id
  activeLevel3.value = ''
  await loadProducts()
}

async function selectLevel3(id: string) {
  activeLevel3.value = id
  await loadProducts()
}

onMounted(async () => {
  categoryRecords.value = await listCategories()
  await loadProducts()
})
</script>

<style scoped>
.category-page { background: var(--bg-page); }
.simple-header { margin: -14px -14px 12px; padding: calc(16px + env(safe-area-inset-top)) 14px 14px; background: var(--bg-cream); border-bottom: 1px solid var(--border-soft); }
.simple-header h1 { margin: 0; color: var(--text-main); font-size: 22px; }
.simple-header p { margin: 4px 0 0; color: var(--text-sub); font-size: 13px; }
.category-layout { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: 10px; }
.customer-row { grid-column: 1 / -1; }
.category-menu { position: sticky; top: 12px; align-self: start; display: grid; gap: 8px; }
.category-menu button { min-height: 42px; padding: 8px; border-radius: var(--radius); color: #6d5b4c; background: var(--bg-card); font-size: 13px; font-weight: 800; }
.category-menu button.active { color: var(--brand-brown); background: var(--bg-cream-soft); box-shadow: inset 3px 0 0 var(--brand-orange); }
.category-content { min-width: 0; display: grid; gap: 10px; align-content: start; }
.subcategory-panel { padding: 10px; border: 1px solid var(--border-soft); border-radius: var(--radius); background: var(--bg-card); }
.subcategory-list { display: flex; gap: 7px; overflow-x: auto; padding-bottom: 2px; }
.subcategory-list button { flex: 0 0 auto; min-height: 32px; padding: 0 12px; border-radius: var(--radius-pill); color: var(--text-sub); background: var(--bg-muted); font-size: 12px; font-weight: 800; }
.subcategory-list button.active { color: #fff; background: var(--brand-teal); }
.third-level { background: #f7faf9; }
.category-banner, .category-product { border: 1px solid var(--border-soft); border-radius: var(--radius); background: var(--bg-card); box-shadow: var(--shadow-card); }
.category-banner { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px; color: var(--brand-brown); }
.category-banner > div { display: grid; gap: 3px; }
.category-banner small { color: var(--text-muted); font-size: 11px; }
.category-banner span { font-weight: 900; }
.category-banner strong { flex: none; }
.category-product { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 10px; padding: 10px; }
.category-product img, .image-empty { width: 92px; height: 92px; border-radius: var(--radius); object-fit: contain; background: var(--bg-muted); }
.image-empty { display: grid; place-items: center; color: #9b826b; font-size: 13px; font-weight: 800; }
.category-product h2 { margin: 0; color: var(--text-main); font-size: 14px; line-height: 1.3; cursor: pointer; }
.category-product p { margin: 5px 0; color: var(--text-sub); font-size: 12px; }
.category-product strong { color: var(--brand-orange); }
.product-btns { display: flex; gap: 8px; margin-top: 8px; }
.detail-btn, .cart-btn { min-height: 30px; padding: 0 12px; border-radius: var(--radius-pill); font-size: 12px; font-weight: 800; }
.detail-btn { color: var(--brand-brown); background: var(--bg-cream-soft); }
.cart-btn { color: #fff; background: var(--brand-teal); }
</style>
