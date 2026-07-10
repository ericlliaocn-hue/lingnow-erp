import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import HomeView from '@/views/HomeView.vue'
import ProductListView from '@/views/ProductListView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import CategoryView from '@/views/CategoryView.vue'
import CartView from '@/views/CartView.vue'
import MineView from '@/views/MineView.vue'
import OrderCreateView from '@/views/OrderCreateView.vue'
import OrderListView from '@/views/OrderListView.vue'
import OrderDetailView from '@/views/OrderDetailView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { guest: true } },
    { path: '/', redirect: '/home' },
    { path: '/home', name: 'home', component: HomeView },
    { path: '/products', name: 'products', component: ProductListView },
    { path: '/products/:id', name: 'productDetail', component: ProductDetailView },
    { path: '/categories', name: 'categories', component: CategoryView },
    { path: '/cart', name: 'cart', component: CartView },
    { path: '/mine', name: 'mine', component: MineView },
    { path: '/orders/new', name: 'orderCreate', component: OrderCreateView, meta: { requiresAuth: true } },
    { path: '/orders', name: 'orders', component: OrderListView, meta: { requiresAuth: true } },
    { path: '/orders/:id', name: 'orderDetail', component: OrderDetailView, meta: { requiresAuth: true } }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.token && !auth.userLoaded) {
    await auth.fetchMe().catch(() => auth.clear())
  }
  if (to.meta.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.token) {
    return '/home'
  }
  return true
})

export default router
