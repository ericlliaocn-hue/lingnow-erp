<template>
  <view class="page">
    <view class="nav">
      <text class="title">商品</text>
    </view>
    <view class="body">
      <scroll-view class="left" scroll-y>
        <view v-for="item in categories" :key="item.id" :class="['left-item', activeId === item.id ? 'active' : '']"
              @click="selectCategory(item)">
          {{ item.name || '分类' }}
        </view>
      </scroll-view>
      <scroll-view class="right" scroll-y>
        <view class="grid">
          <view v-for="item in filteredProducts" :key="item.id" class="product" @click="goDetail(item)">
            <image v-if="item.image" :src="item.image" class="thumb" mode="aspectFill"></image>
            <view v-else class="thumb"></view>
            <view class="info">
              <text class="name">{{ item.name || '商品' }}</text>
              <text class="sub">库存 {{ item.stock || 0 }}</text>
              <view class="price-row">
                <text class="price">¥ {{ item.price || 0 }}</text>
                <view class="quick-spec" @click.stop="addToCart(item)">加入</view>
              </view>
            </view>
          </view>
          <view v-if="filteredProducts.length === 0" class="empty">暂无商品</view>
        </view>
      </scroll-view>
    </view>
    <view class="cart-bar">
      <view class="cart-left" @click="toggleCart">
        <view class="cart-icon">
          <text v-if="cartCount > 0" class="cart-dot">{{ cartCount }}</text>
        </view>
        <view class="cart-info">
          <text class="cart-total">¥ {{ cartTotal }}</text>
          <text class="cart-tip">已选 {{ cartCount }} 件</text>
        </view>
      </view>
      <view class="cart-action" @click="goCheckout">去结算</view>
    </view>
    <TabBar :current="1"/>
    <view v-if="showCart" class="mask" @click="toggleCart">
      <view class="drawer" @click.stop>
        <view class="drawer-head">
          <text class="drawer-title">已购商品</text>
          <text class="drawer-clear" @click="clearAll">清空</text>
        </view>
        <scroll-view class="drawer-body" scroll-y>
          <view v-for="item in cartViewItems" :key="item.id" class="drawer-item">
            <image v-if="item.product?.image" :src="item.product.image" class="drawer-thumb" mode="aspectFill"></image>
            <view v-else class="drawer-thumb"></view>
            <view class="drawer-info">
              <text class="drawer-name">{{ item.product?.name || '商品' }}</text>
              <text class="drawer-price">¥ {{ item.price }}</text>
            </view>
            <view class="drawer-stepper">
              <view class="step-btn" @click="decCart(item)">-</view>
              <text class="step-count">{{ item.quantity }}</text>
              <view class="step-btn" @click="incCart(item)">+</view>
            </view>
          </view>
          <view v-if="cartViewItems.length === 0" class="empty">购物车为空</view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import TabBar from '@/components/TabBar.vue'
import {
  addCartItem,
  clearCart,
  getCartItems,
  getProductCategories,
  getProducts,
  removeCartItem,
  updateCartItem
} from '@/api/business'

const categories = ref<any[]>([])
const products = ref<any[]>([])
const activeId = ref<string | number | null>(null)
const activeCategoryName = ref<string>('')
const cartItems = ref<any[]>([])
const showCart = ref(false)
const currentUser = uni.getStorageSync('userInfo') || null

const loadData = async () => {
  try {
    const [catRes, prodRes] = await Promise.all([getProductCategories(), getProducts()])
    categories.value = catRes?.data || []
    products.value = prodRes?.data || []
    const first = categories.value[0]
    activeId.value = first?.id || null
    activeCategoryName.value = first?.name || ''
    await refreshCart()
  } catch (e) {
    console.error(e)
  }
}

const selectCategory = (item: any) => {
  activeId.value = item.id
  activeCategoryName.value = item?.name || ''
}

const filteredProducts = computed(() => {
  if (!activeCategoryName.value) return products.value
  return products.value.filter(p => p.category === activeCategoryName.value)
})

const cartViewItems = computed(() => {
  return cartItems.value.map((item: any) => {
    const product = products.value.find(p => p.id === item.productId)
    return {...item, product}
  })
})

const cartTotal = computed(() => {
  return cartViewItems.value.reduce((sum: number, item: any) => {
    return sum + (item.price || 0) * (item.quantity || 0)
  }, 0)
})

const cartCount = computed(() => {
  return cartViewItems.value.reduce((sum: number, item: any) => sum + (item.quantity || 0), 0)
})

const refreshCart = async () => {
  if (!currentUser?.userId) {
    cartItems.value = []
    return
  }
  const res = await getCartItems(currentUser.userId)
  cartItems.value = res?.data || []
}

const addToCart = async (item: any) => {
  if (!currentUser?.userId) {
    uni.showToast({title: '请先登录', icon: 'none'})
    return
  }
  try {
    const existing = cartItems.value.find(ci => ci.productId === item.id)
    if (existing) {
      await updateCartItem(existing.id, {quantity: (existing.quantity || 0) + 1, price: item.price || 0})
    } else {
      await addCartItem({
        userId: currentUser.userId,
        productId: item.id,
        quantity: 1,
        price: item.price || 0
      })
    }
    await refreshCart()
    uni.showToast({title: '已加入购物车', icon: 'success'})
  } catch (e) {
    console.error(e)
    uni.showToast({title: '加入失败', icon: 'none'})
  }
}

const incCart = async (item: any) => {
  await updateCartItem(item.id, {quantity: (item.quantity || 0) + 1, price: item.price || 0})
  await refreshCart()
}

const decCart = async (item: any) => {
  const next = (item.quantity || 0) - 1
  if (next <= 0) {
    await removeCartItem(item.id)
  } else {
    await updateCartItem(item.id, {quantity: next, price: item.price || 0})
  }
  await refreshCart()
}

const clearAll = async () => {
  if (!currentUser?.userId) return
  await clearCart(currentUser.userId)
  await refreshCart()
}

const toggleCart = () => {
  showCart.value = !showCart.value
}

const goCheckout = () => {
  if (cartItems.value.length === 0) {
    uni.showToast({title: '购物车为空', icon: 'none'})
    return
  }
  uni.navigateTo({url: '/pages/business/checkout/index'})
}

const goDetail = (item: any) => {
  uni.navigateTo({url: '/pages/business/product/detail?id=' + (item.id || '')})
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  background: #f7f7f8;
}

.nav {
  padding: 24rpx;
  font-size: 30rpx;
  font-weight: 700;
  background: #fff;
}

.body {
  display: flex;
  height: calc(100vh - 300rpx);
}

.left {
  width: 200rpx;
  background: #fff;
}

.left-item {
  padding: 24rpx 16rpx;
  font-size: 26rpx;
  color: #666;
}

.left-item.active {
  color: #ff6f61;
  font-weight: 600;
  background: #fff3f0;
}

.right {
  flex: 1;
  padding: 16rpx;
}

.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16rpx;
}

.product {
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx;
  display: flex;
  gap: 16rpx;
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.thumb {
  height: 140rpx;
  width: 140rpx;
  display: block;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #cfe2ff, #f7c8ff);
}

.name {
  font-size: 24rpx;
  font-weight: 600;
}

.sub {
  color: #999;
  font-size: 22rpx;
  margin-top: 4rpx;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10rpx;
}

.price {
  color: #ff6f61;
}

.quick-spec {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  background: #ffcc33;
  color: #3a2a00;
  font-size: 22rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.empty {
  grid-column: span 1;
  text-align: center;
  color: #999;
}

.cart-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 120rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 8;
}

.cart-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cart-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: #ffcc33;
  position: relative;
}

.cart-dot {
  position: absolute;
  right: -6rpx;
  top: -6rpx;
  background: #ff3b30;
  color: #fff;
  border-radius: 999rpx;
  font-size: 20rpx;
  padding: 2rpx 8rpx;
}

.cart-info {
  display: flex;
  flex-direction: column;
}

.cart-total {
  font-size: 28rpx;
  font-weight: 700;
}

.cart-tip {
  color: #999;
  font-size: 22rpx;
}

.cart-action {
  background: #ffcc33;
  color: #3a2a00;
  padding: 16rpx 32rpx;
  border-radius: 999rpx;
  font-weight: 700;
}

.mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 9;
  display: flex;
  align-items: flex-end;
}

.drawer {
  background: #fff;
  width: 100%;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  padding: 24rpx;
  max-height: 70vh;
  height: 70vh;
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.drawer-title {
  font-weight: 700;
  font-size: 28rpx;
}

.drawer-clear {
  color: #999;
  font-size: 24rpx;
}

.drawer-body {
  max-height: calc(70vh - 80rpx);
  height: calc(70vh - 80rpx);
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 0;
  border-bottom: 1px solid #f2f2f2;
}

.drawer-thumb {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #cfe2ff, #f7c8ff);
}

.drawer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.drawer-name {
  font-size: 24rpx;
}

.drawer-price {
  color: #ff6f61;
  margin-top: 6rpx;
}

.drawer-stepper {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.step-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 22rpx;
  border: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-count {
  min-width: 32rpx;
  text-align: center;
}

.spec-modal {
  background: #fff;
  width: 86%;
  margin: 0 auto;
  border-radius: 24rpx;
  padding: 28rpx;
  max-height: 70vh;
  align-self: center;
  position: relative;
  display: flex;
  flex-direction: column;
}

.spec-title {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}

.spec-close {
  position: absolute;
  right: 20rpx;
  top: 16rpx;
  font-size: 36rpx;
  color: #999;
}

.spec-group {
  margin-bottom: 16rpx;
}

.spec-label {
  font-size: 24rpx;
  color: #666;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 12rpx;
}

.spec-option {
  padding: 10rpx 20rpx;
  background: #f6f6f6;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.spec-option.active {
  background: #ffcc33;
  color: #3a2a00;
}

.spec-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20rpx;
}

.spec-price {
  font-size: 28rpx;
  font-weight: 700;
}

.spec-btn {
  background: #ffcc33;
  color: #3a2a00;
  padding: 16rpx 32rpx;
  border-radius: 999rpx;
  font-weight: 700;
}
</style>
