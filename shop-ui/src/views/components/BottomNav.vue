<template>
  <nav class="bottom-nav">
    <div class="bottom-nav-inner">
      <RouterLink class="nav-item" to="/home">
        <span class="nav-icon">⌂</span>
        <span class="nav-label">首页</span>
      </RouterLink>
      <RouterLink class="nav-item" to="/categories">
        <span class="nav-icon">▦</span>
        <span class="nav-label">分类</span>
      </RouterLink>
      <RouterLink class="nav-item cart-nav" to="/cart">
        <span class="nav-icon">⌕</span>
        <span class="nav-label">购物车</span>
        <em v-if="cart.totalQty">{{ cart.totalQty > 99 ? '99+' : cart.totalQty }}</em>
      </RouterLink>
      <RouterLink class="nav-item" to="/mine">
        <span class="nav-icon">☺</span>
        <span class="nav-label">我的</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useCartStore } from '@/stores/cart'

const cart = useCartStore()
</script>

<style scoped>
.nav-item {
  display: grid;
  gap: 2px;
  place-items: center;
  min-height: 48px;
}

.nav-icon {
  font-size: 20px;
  line-height: 1;
}

.nav-label {
  font-size: 12px;
}
</style>
