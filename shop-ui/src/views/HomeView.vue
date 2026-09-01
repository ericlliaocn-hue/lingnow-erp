<template>
  <main class="page home-page">
    <!-- 顶部固定栏 -->
    <header class="home-header">
      <div class="page-inner header-stack">
        <div class="header-inner">
          <p class="brand-mark">荣时衣架</p>
        </div>
        <button class="search-entry" type="button" @click="goSearch">
          <span class="search-icon">⌕</span>
          <span class="search-placeholder">搜索木衣架、裤架、尺寸</span>
        </button>
      </div>
    </header>

    <section class="page-inner home-content">
      <!-- Banner 轮播 -->
      <section v-if="banners.length" class="banner-section">
        <div class="banner-track" :style="{ transform: `translateX(-${bannerIndex * 100}%)` }">
          <article
            v-for="(item, index) in banners"
            :key="item.id"
            class="banner-slide"
            :style="{ background: bannerTheme(index) }"
            @click="goDetail(item.id)"
          >
            <div class="banner-copy">
              <span>{{ bannerTag(index) }}</span>
              <h2>{{ bannerTitle(item) }}</h2>
              <p>{{ bannerSubtitle(item) }}</p>
              <strong v-if="item.salePrice">{{ priceLabel(item.salePrice) }}</strong>
            </div>
            <div class="banner-media">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" loading="lazy" />
              <div v-else class="banner-empty">荣时</div>
            </div>
          </article>
        </div>
        <div class="banner-dots">
          <button
            v-for="(item, index) in banners"
            :key="item.id"
            type="button"
            :class="['banner-dot', index === bannerIndex ? 'active' : '']"
            :aria-label="`切换到第 ${index + 1} 张`"
            @click="bannerIndex = index"
          />
        </div>
      </section>

      <!-- 精选推荐 -->
      <section v-if="recommended.length" class="recommend-section">
        <div class="section-head">
          <div>
            <h2>精选推荐</h2>
            <p>适合服装店陈列和品牌搭配</p>
          </div>
          <RouterLink to="/categories">全部分类</RouterLink>
        </div>
        <div class="recommend-scroll">
          <article
            v-for="item in recommended"
            :key="item.id"
            class="recommend-card"
            @click="goDetail(item.id)"
          >
            <div class="recommend-image">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" loading="lazy" />
              <div v-else class="image-empty">荣时衣架</div>
            </div>
            <h3>{{ item.name }}</h3>
            <p>{{ item.spec || '多规格可选' }}</p>
            <strong>{{ priceLabel(item.salePrice) }}</strong>
          </article>
        </div>
      </section>

      <!-- 热卖商品 -->
      <section class="product-section">
        <div class="section-head">
          <div>
            <h2>热卖商品</h2>
            <p>{{ products.length }} 款好物</p>
          </div>
          <RouterLink to="/products">查看全部</RouterLink>
        </div>

        <div v-if="loading && products.length === 0" class="product-grid">
          <div v-for="n in 4" :key="n" class="skeleton product-skeleton"></div>
        </div>
        <div v-else-if="products.length === 0" class="empty">暂时没有找到相关商品</div>
        <div v-else class="product-grid">
          <article v-for="item in products" :key="item.id" class="product-card">
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listProducts } from '@/api/shop'
import { priceLabel, productSubtitle, productTags } from '@/utils/label'
import type { ShopProduct } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'

const router = useRouter()
const current = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const products = ref<ShopProduct[]>([])

// Banner 轮播
const banners = computed(() => products.value.filter(item => item.imageUrl).slice(0, 5))
const bannerIndex = ref(0)
let bannerTimer: ReturnType<typeof setInterval> | undefined

const hasMore = computed(() => products.value.length < total.value)
const recommended = computed(() => products.value.filter(item => item.imageUrl).slice(0, 6))

const bannerThemes = [
  '#fff4e4',
  '#e7f3ef',
  '#fff0e8',
  '#f1ebe2',
  '#edf4f4'
]
function bannerTheme(index: number) {
  return bannerThemes[index % bannerThemes.length]
}
function bannerTag(index: number) {
  return ['本周推荐', '新品上架', '热卖好物', '门店精选', '品牌搭配'][index % 5]
}
function bannerTitle(item: ShopProduct) {
  return item.name || '门店陈列衣架'
}
function bannerSubtitle(item: ShopProduct) {
  return item.spec || '木质衣架、裤架和陈列配件按材质、颜色、尺寸分类展示'
}

function startBannerAuto() {
  if (banners.value.length <= 1) return
  stopBannerAuto()
  bannerTimer = setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % banners.value.length
  }, 4000)
}
function stopBannerAuto() {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = undefined
  }
}

async function fetchList(reset = false) {
  loading.value = true
  try {
    const res = await listProducts({ current: current.value, size, keyword: undefined })
    products.value = reset ? res.records : products.value.concat(res.records)
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (loading.value || !hasMore.value) return
  current.value += 1
  fetchList()
}

function goDetail(id: string) {
  router.push(`/products/${id}`)
}

function goSearch() {
  router.push('/products')
}

function addToCart(item: ShopProduct) {
  router.push(`/products/${item.id}`)
}

onMounted(async () => {
  await fetchList(true)
  startBannerAuto()
})
onUnmounted(stopBannerAuto)
</script>

<style scoped>
.home-page {
  padding-top: calc(104px + env(safe-area-inset-top));
  background: var(--bg-page);
}

.home-header {
  position: fixed;
  top: 0;
  left: 50%;
  width: min(100vw, 640px);
  transform: translateX(-50%);
  z-index: 30;
  margin: 0;
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px;
  background: rgba(255, 247, 235, 0.94);
  border-bottom: 1px solid var(--border-soft);
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 18px rgba(71, 49, 31, 0.04);
}

.header-stack {
  display: grid;
  gap: 10px;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.brand-mark {
  margin: 0;
  color: var(--brand-brown-soft);
  font-size: 12px;
  font-weight: 800;
}

.home-content {
  display: grid;
  gap: 12px;
}

/* 搜索入口 */
.search-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--border-line);
  border-radius: var(--radius-pill);
  color: var(--text-sub);
  background: #fffaf4;
  text-align: left;
}

.search-icon {
  color: var(--brand-teal);
  font-size: 18px;
  font-weight: 900;
}

.search-placeholder {
  font-size: 14px;
}

/* Banner 轮播 */
.banner-section {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-pop);
}

.banner-track {
  display: flex;
  transition: transform 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.banner-slide {
  flex: 0 0 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px;
  gap: 12px;
  padding: 18px 16px;
  cursor: pointer;
}

.banner-copy span {
  color: var(--brand-teal);
  font-size: 12px;
  font-weight: 900;
}

.banner-copy h2 {
  margin: 8px 0 8px;
  color: var(--text-main);
  font-size: 22px;
  line-height: 1.18;
}

.banner-copy p {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.45;
}

.banner-copy strong {
  display: inline-block;
  margin-top: 12px;
  color: var(--brand-orange);
  font-size: 18px;
}

.banner-media {
  align-self: center;
}

.banner-media img,
.banner-empty {
  width: 130px;
  height: 130px;
  border-radius: var(--radius);
  object-fit: contain;
  background: #f3eee7;
  box-shadow: var(--shadow-deep);
}

.banner-empty {
  display: grid;
  place-items: center;
  color: #9b826b;
  font-size: 13px;
  font-weight: 800;
}

.banner-dots {
  position: absolute;
  bottom: 8px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 5px;
}

.banner-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-pill);
  background: rgba(122, 63, 24, 0.25);
  transition: all 0.2s;
}

.banner-dot.active {
  width: 18px;
  background: var(--brand-brown);
}

/* 区块标题 */
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

.section-head a {
  flex: 0 0 auto;
  color: var(--brand-teal);
  font-size: 13px;
  font-weight: 800;
}

/* 精选推荐 */
.recommend-section {
  padding: 13px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.recommend-scroll {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  overflow: visible;
}

.recommend-card {
  min-width: 0;
  display: grid;
  gap: 6px;
  cursor: pointer;
}

.recommend-image {
  width: 100%;
  aspect-ratio: 1 / 0.76;
  height: auto;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg-muted);
}

.recommend-image img,
.image-empty {
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

.recommend-card h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.25;
  min-height: 32px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommend-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend-card strong {
  color: var(--brand-orange);
  font-size: 15px;
}

/* 热卖商品 */
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

@media (max-width: 370px) {
  .banner-slide {
    grid-template-columns: minmax(0, 1fr) 98px;
    padding: 14px;
  }

  .banner-copy h2 {
    font-size: 20px;
  }

  .banner-media img,
  .banner-empty {
    width: 96px;
    height: 112px;
  }

  .product-image {
    height: 128px;
  }

  .recommend-scroll {
    gap: 8px;
  }
}
</style>
