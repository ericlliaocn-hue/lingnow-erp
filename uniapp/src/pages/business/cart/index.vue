<template>
  <view class="page">
    <view class="nav">
      <text class="title">购物车</text>
    </view>
    <scroll-view class="content" scroll-y>
      <view v-for="item in cartItems" :key="item.id" class="card">
        <image v-if="item.image" :src="item.image" class="thumb" mode="aspectFill"></image>
        <view v-else class="thumb"></view>
        <view class="info">
          <text class="name">{{ item.productName || '商品' }}</text>
          <text class="price">¥ {{ item.price || 0 }}</text>
        </view>
        <text class="qty">× {{ item.quantity || 1 }}</text>
      </view>
      <view v-if="cartItems.length === 0" class="empty">购物车暂无商品</view>
    </scroll-view>
    <view class="footer">
      <button class="btn" @click="goCheckout">去结算</button>
    </view>
    <TabBar :current="2"/>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import TabBar from '@/components/TabBar.vue'
import {getCartItems, getProducts} from '@/api/business'

const cartItems = ref<any[]>([])
const currentUser = uni.getStorageSync('userInfo') || null

const loadData = async () => {
  if (!currentUser?.userId) {
    cartItems.value = []
    return
  }
  try {
    const [cartRes, productRes] = await Promise.all([getCartItems(currentUser.userId), getProducts()])
    const products = productRes?.data || []
    cartItems.value = (cartRes?.data || []).map((item: any) => {
      const prod = products.find((p: any) => Number(p.id) === Number(item.productId))
      return {
        ...item,
        productName: prod?.name || item.productName,
        image: prod?.image
      }
    })
  } catch (e) {
    console.error(e)
  }
}

const goCheckout = () => {
  if (cartItems.value.length === 0) {
    uni.showToast({title: '购物车暂无商品', icon: 'none'})
    return
  }
  uni.navigateTo({url: '/pages/business/checkout/index'})
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
}

.content {
  height: calc(100vh - 260rpx);
  padding: 0 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx;
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #ffe2b8, #ffd1c4);
}

.info {
  flex: 1;
}

.qty {
  color: #666;
  font-size: 24rpx;
}

.name {
  font-size: 26rpx;
  font-weight: 600;
}

.price {
  color: #ff6f61;
  margin-top: 8rpx;
}

.empty {
  text-align: center;
  color: #999;
  margin-top: 80rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 110rpx;
  padding: 0 24rpx 24rpx;
}

.btn {
  background: #ff6f61;
  color: #fff;
  border-radius: 999rpx;
}
</style>
