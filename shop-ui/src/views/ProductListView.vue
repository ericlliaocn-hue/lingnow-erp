<template>
  <main class="page list-page">
    <!-- 顶部固定栏 -->
    <header class="list-header">
      <div class="page-inner header-inner">
        <RouterLink class="back-link" to="/home">‹ 返回</RouterLink>
        <h1>全部商品</h1>
        <button class="header-account" type="button" @click="goMine">
          {{ auth.token ? '我的' : '登录' }}
        </button>
      </div>
    </header>

    <section class="page-inner list-content">
      <!-- 搜索栏 -->
      <section class="search-panel">
        <div class="search-line">
          <input
            ref="searchInputRef"
            v-model.trim="keyword"
            class="input"
            placeholder="搜索木衣架、裤架、尺寸"
            @keyup.enter="reload"
          />
          <button v-if="keyword" class="clear-button" type="button" aria-label="清空搜索" @click="clearSearch">×</button>
          <button class="search-button" type="button" @click="reload">搜索</button>
        </div>
      </section>

      <!-- 商品分类 -->
      <section class="category-strip" aria-label="商品分类">
        <button
          v-for="item in rootCategories"
          :key="item.id"
          :class="['category-pill', activeCategory === item.id ? 'active' : '']"
          type="button"
          @click="selectCategory(item.id)"
        >
          <span>{{ item.name }}</span>
          <small>{{ categoryCount(item.id) }}</small>
        </button>
      </section>

      <section v-if="level2Categories.length" class="subcategory-strip" aria-label="子分类">
        <button :class="{ active: !activeLevel2 }" type="button" @click="selectCategory(activeLevel1)">全部</button>
        <button
          v-for="item in level2Categories"
          :key="item.id"
          :class="{ active: activeLevel2 === item.id }"
          type="button"
          @click="selectCategory(item.id)"
        >
          {{ item.name }} <small>{{ categoryCount(item.id) }}</small>
        </button>
      </section>

      <section v-if="level3Categories.length" class="subcategory-strip third-level" aria-label="子分类">
        <button :class="{ active: !activeLevel3 }" type="button" @click="selectCategory(activeLevel2)">全部</button>
        <button
          v-for="item in level3Categories"
          :key="item.id"
          :class="{ active: activeLevel3 === item.id }"
          type="button"
          @click="selectCategory(item.id)"
        >
          {{ item.name }} <small>{{ categoryCount(item.id) }}</small>
        </button>
      </section>

      <!-- 商品网格 -->
      <section class="product-section">
        <div class="section-head">
          <div>
            <h2>{{ activeCategory === 'all' ? '全部商品' : categoryLabel(activeCategory) }}</h2>
            <small v-if="activePathLabel" class="category-path">{{ activePathLabel }}</small>
            <p>{{ visibleProducts.length }} 款好物</p>
          </div>
          <button type="button" class="refresh-btn" @click="reload">换一批</button>
        </div>

        <div v-if="loading && products.length === 0" class="product-grid">
          <div v-for="n in 4" :key="n" class="skeleton product-skeleton"></div>
        </div>
        <div v-else-if="visibleProducts.length === 0" class="empty">暂时没有找到相关商品</div>
        <div v-else class="product-grid">
          <article v-for="item in visibleProducts" :key="item.id" class="product-card">
            <button class="product-image" type="button" @click="goDetail(item.id)">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" loading="lazy" />
              <div v-else class="image-empty">荣时衣架</div>
            </button>
            <div class="product-info">
              <h3 @click="goDetail(item.id)">{{ item.name }}</h3>
              <p>{{ productSubtitle(item) }}</p>
              <div class="product-tags">
                <span v-for="tag in productTags(item)" :key="tag">{{ tag }}</span>
              </div>
              <div class="product-foot">
                <strong>{{ priceLabel(item.salePrice) }}</strong>
                <button type="button" class="cart-btn" @click="addToCart(item)">加入购物车</button>
              </div>
            </div>
          </article>
        </div>

        <button v-if="hasMore" class="secondary-button load-more" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中...' : '查看更多商品' }}
        </button>
      </section>
    </section>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCategories, listProducts } from '@/api/shop'
import { useAuthStore } from '@/stores/auth'
import { buildCategoryTree, categoryPath, descendantCategoryIds } from '@/utils/category'
import { priceLabel, productSubtitle, productTags } from '@/utils/label'
import type { ShopCategory, ShopProduct } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const searchInputRef = ref<HTMLInputElement>()
const keyword = ref('')
const current = ref(1)
const size = 500
const total = ref(0)
const loading = ref(false)
const products = ref<ShopProduct[]>([])
const activeCategory = ref('all')
const categoryRecords = ref<ShopCategory[]>([])
const categoryTree = computed(() => buildCategoryTree(categoryRecords.value))
const selectedPath = computed(() => categoryPath(categoryRecords.value, activeCategory.value === 'all' ? '' : activeCategory.value))
const activeLevel1 = computed(() => String(selectedPath.value[0]?.id || 'all'))
const activeLevel2 = computed(() => String(selectedPath.value[1]?.id || ''))
const activeLevel3 = computed(() => String(selectedPath.value[2]?.id || ''))
const rootCategories = computed(() => [{ id: 'all', name: '全部', children: [] } as ReturnType<typeof buildCategoryTree>[number], ...categoryTree.value])
const level2Categories = computed(() => categoryTree.value.find(item => item.id === activeLevel1.value)?.children || [])
const level3Categories = computed(() => level2Categories.value.find(item => item.id === activeLevel2.value)?.children || [])
const activePathLabel = computed(() => selectedPath.value.slice(0, -1).map(item => item.name).join(' / '))

const hasMore = computed(() => products.value.length < total.value)
const visibleProducts = computed(() => {
  if (activeCategory.value === 'all') return products.value
  const ids = descendantCategoryIds(categoryRecords.value, activeCategory.value)
  return products.value.filter(item => item.categoryId && ids.has(item.categoryId))
})

function categoryLabel(value: string) {
  return selectedPath.value.find(item => String(item.id) === value)?.name || '全部商品'
}

function categoryCount(category: string) {
  if (category === 'all') return products.value.length
  const ids = descendantCategoryIds(categoryRecords.value, category)
  return products.value.filter(item => item.categoryId && ids.has(item.categoryId)).length
}

function selectCategory(id: string) {
  activeCategory.value = id || 'all'
}

async function fetchList(reset = false) {
  loading.value = true
  try {
    const res = await listProducts({ current: current.value, size, keyword: keyword.value || undefined })
    products.value = reset ? res.records : products.value.concat(res.records)
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  current.value = 1
  activeCategory.value = 'all'
  fetchList(true)
}

function clearSearch() {
  keyword.value = ''
  reload()
}

function loadMore() {
  if (loading.value || !hasMore.value) return
  current.value += 1
  fetchList()
}

function goDetail(id: string) {
  router.push(`/products/${id}`)
}

function addToCart(item: ShopProduct) {
  router.push(`/products/${item.id}`)
}

function goMine() {
  router.push(auth.token ? '/mine' : { path: '/login', query: { redirect: '/mine' } })
}

onMounted(() => {
  Promise.all([listCategories(), fetchList(true)]).then(([records]) => {
    categoryRecords.value = records
    const queryCategory = String(route.query.category || '')
    if (categoryRecords.value.some(item => item.id === queryCategory)) activeCategory.value = queryCategory
  })
})
</script>

<style scoped>
.list-page {
  background: var(--bg-page);
}

.list-header {
  position: sticky;
  top: 0;
  z-index: 10;
  margin: -14px -14px 10px;
  padding: calc(12px + env(safe-area-inset-top)) 14px 10px;
  background: rgba(255, 247, 235, 0.94);
  border-bottom: 1px solid var(--border-soft);
  backdrop-filter: blur(14px);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.back-link {
  color: var(--brand-brown);
  font-size: 14px;
  font-weight: 800;
}

.list-header h1 {
  margin: 0;
  color: var(--text-main);
  font-size: 19px;
  font-weight: 800;
}

.header-account {
  min-width: 58px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-brown);
  font-weight: 800;
}

.list-content {
  display: grid;
  gap: 12px;
}

.search-panel {
  position: sticky;
  top: calc(60px + env(safe-area-inset-top));
  z-index: 9;
  padding: 10px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.search-line {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 72px;
  gap: 8px;
}

.search-line .input {
  min-height: 42px;
  border-color: var(--border-line);
  border-radius: var(--radius-pill);
  background: #fffaf4;
  padding-right: 34px;
}

.clear-button {
  position: absolute;
  top: 6px;
  right: 80px;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-pill);
  color: #8a7a6b;
  background: transparent;
  font-size: 18px;
}

.search-button {
  min-height: 42px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 800;
}

.category-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 2px 1px;
}

.category-pill {
  flex: 0 0 auto;
  display: grid;
  gap: 2px;
  min-width: 76px;
  min-height: 52px;
  padding: 8px 12px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius);
  color: #4f4035;
  background: var(--bg-card);
  text-align: left;
}

.category-pill span {
  font-weight: 900;
}

.category-pill small {
  color: var(--text-muted);
}

.category-pill.active {
  border-color: #d9a067;
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
}

.subcategory-strip {
  display: flex;
  align-items: center;
  gap: 7px;
  overflow-x: auto;
  padding: 9px 10px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
}

.subcategory-strip button {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--radius-pill);
  color: var(--text-sub);
  background: var(--bg-muted);
  font-size: 12px;
  font-weight: 800;
}

.subcategory-strip button.active {
  color: #fff;
  background: var(--brand-teal);
}

.subcategory-strip button small { font-size: 10px; opacity: .78; }
.subcategory-strip.third-level { background: #f7faf9; }
.category-path { display: block; margin-top: 3px; color: var(--text-muted); font-size: 11px; }

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 18px;
}

.section-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 12px;
}

.refresh-btn {
  color: var(--brand-teal);
  background: transparent;
  font-size: 13px;
  font-weight: 800;
}

.product-section {
  display: grid;
  gap: 10px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.product-card {
  overflow: hidden;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.product-image {
  width: 100%;
  height: 142px;
  background: var(--bg-muted);
}

.product-image img,
.product-image .image-empty {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.image-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 13px;
  font-weight: 800;
}

.product-info {
  display: grid;
  gap: 6px;
  padding: 10px;
}

.product-info h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.28;
  min-height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
}

.product-info p {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-tags {
  display: flex;
  gap: 5px;
  overflow: hidden;
}

.product-tags span {
  flex: 0 0 auto;
  max-width: 88px;
  min-height: 22px;
  padding: 3px 7px;
  border-radius: var(--radius-pill);
  color: #76685d;
  background: var(--bg-muted);
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-foot {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin-top: 2px;
}

.product-foot strong {
  color: var(--brand-orange);
  font-size: 18px;
}

.cart-btn {
  min-height: 34px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-teal);
  font-size: 13px;
  font-weight: 800;
}

.product-skeleton {
  height: 242px;
}

.load-more {
  margin-top: 4px;
}
</style>
