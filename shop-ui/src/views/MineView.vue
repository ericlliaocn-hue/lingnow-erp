<template>
  <main class="page mine-page">
    <header class="profile-header">
      <div class="page-inner profile-card">
        <div class="avatar">{{ avatarText(auth.user) }}</div>
        <div>
          <h1>{{ auth.token ? displayName(auth.user) : '欢迎来到荣时衣架' }}</h1>
          <p>{{ auth.token ? '查看订单和常买商品' : '登录后可查看订单和常买商品' }}</p>
        </div>
      </div>
    </header>

    <section class="page-inner mine-content">
      <div v-if="!auth.token" class="login-panel">
        <strong>登录后体验更完整</strong>
        <p>保存收货信息，查看每一笔购买记录。</p>
        <div class="login-actions">
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink to="/login?mode=register">注册</RouterLink>
        </div>
      </div>

      <div class="action-grid">
        <RouterLink to="/orders">
          <strong>我的订单</strong>
          <span>查看全部订单</span>
        </RouterLink>
        <RouterLink to="/cart">
          <strong>购物车</strong>
          <span>继续编辑清单</span>
        </RouterLink>
      </div>

      <section v-if="auth.token" class="orders-panel">
        <div class="section-head">
          <h2>最近订单</h2>
          <RouterLink to="/orders">全部</RouterLink>
        </div>
        <div v-if="loading" class="empty">加载中...</div>
        <div v-else-if="orders.length === 0" class="empty">还没有订单，去首页逛逛吧</div>
        <RouterLink v-for="item in orders" :key="item.id" class="order-row" :to="`/orders/${item.id}`">
          <div>
            <strong>订单号 {{ item.orderNo }}</strong>
            <span>{{ item.orderTime || '-' }}</span>
          </div>
          <em :class="statusClass(item.status)">{{ statusText(item.status) }}</em>
        </RouterLink>
      </section>

      <button v-if="auth.token" class="logout-button" type="button" @click="logout">退出登录</button>
    </section>

    <BottomNav />
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api/shop'
import { useAuthStore } from '@/stores/auth'
import { avatarText, displayName, statusText, statusClass } from '@/utils/label'
import type { CustomerOrder } from '@/types/shop'
import BottomNav from './components/BottomNav.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const orders = ref<CustomerOrder[]>([])

async function loadOrders() {
  if (!auth.token) return
  loading.value = true
  try {
    const res = await listOrders({ current: 1, size: 3 })
    orders.value = res.records
  } finally {
    loading.value = false
  }
}

async function logout() {
  await auth.logout()
  router.replace('/home')
}

onMounted(loadOrders)
</script>

<style scoped>
.mine-page {
  background: var(--bg-page);
}

.profile-header {
  margin: -14px -14px 12px;
  padding: calc(18px + env(safe-area-inset-top)) 14px 18px;
  background: linear-gradient(135deg, var(--brand-brown), var(--brand-teal));
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.avatar {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: var(--radius-pill);
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
  font-size: 24px;
  font-weight: 900;
}

.profile-card h1 {
  margin: 0;
  font-size: 20px;
}

.profile-card p {
  margin: 5px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.mine-content {
  display: grid;
  gap: 12px;
}

.login-panel,
.action-grid a,
.orders-panel {
  border: 1px solid var(--border-soft);
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.login-panel {
  padding: 16px;
}

.login-panel strong {
  color: var(--text-main);
  font-size: 18px;
}

.login-panel p {
  color: var(--text-sub);
  font-size: 13px;
}

.login-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.login-actions a {
  display: grid;
  place-items: center;
  min-height: 40px;
  border-radius: var(--radius-pill);
  color: #fff;
  background: var(--brand-teal);
  font-weight: 900;
}

.login-actions a + a {
  color: var(--brand-brown);
  background: var(--bg-cream-soft);
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.action-grid a {
  display: grid;
  gap: 4px;
  padding: 14px;
}

.action-grid strong {
  color: var(--text-main);
}

.action-grid span {
  color: var(--text-sub);
  font-size: 12px;
}

.orders-panel {
  padding: 14px;
}

.section-head,
.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 17px;
}

.section-head a {
  color: var(--brand-teal);
  font-size: 13px;
  font-weight: 800;
}

.order-row {
  padding: 12px 0;
  border-top: 1px solid var(--border-soft);
}

.order-row strong {
  display: block;
  color: var(--text-main);
  font-size: 14px;
}

.order-row span {
  color: var(--text-sub);
  font-size: 12px;
}

.order-row em {
  flex: 0 0 auto;
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
}

.logout-button {
  min-height: 44px;
  border-radius: var(--radius-pill);
  color: #9b2c2c;
  background: #ffe9e7;
  font-weight: 900;
}
</style>
