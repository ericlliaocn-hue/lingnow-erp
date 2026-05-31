<template>
  <view class="page">
    <image v-if="product?.image" :src="product.image" class="banner" mode="aspectFill"></image>
    <view v-else class="banner"></view>
    <view class="content">
      <text class="title">{{ product?.name || '商品' }}</text>
      <text class="price">¥ {{ product?.price || 0 }}</text>
      <text class="desc">商品详情、规格和库存信息可按业务扩展。</text>
    </view>
    <view class="actions">
      <button class="btn-outline" @click="goCart">加入购物车</button>
      <button class="btn" @click="goCheckout">立即购买</button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {addCartItem, getProducts} from '@/api/business'

const product = ref<any>(null)

const loadData = async () => {
  try {
    const res = await getProducts()
    const id = Number((uni as any).getCurrentPages?.()?.slice(-1)?.[0]?.$page?.options?.id || 0)
    product.value = (res?.data || []).find((p: any) => Number(p.id) === id) || null
  } catch (e) {
    console.error(e)
  }
}

const goCart = async () => {
  const currentUser = uni.getStorageSync('userInfo') || null
  if (!currentUser?.userId) {
    uni.showToast({title: '请先登录', icon: 'none'})
    return
  }
  try {
    if (!product.value) return
    await addCartItem({
      userId: currentUser.userId,
      productId: product.value.id,
      quantity: 1,
      price: product.value.price || 0
    })
    uni.showToast({title: '已加入购物车', icon: 'success'})
    uni.switchTab({url: '/pages/business/cart/index'})
  } catch (e) {
    console.error(e)
    uni.showToast({title: '加入失败', icon: 'none'})
  }
}

const goCheckout = () => {
  uni.navigateTo({url: '/pages/business/checkout/index'})
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  background: #fff;
}

.banner {
  height: 360rpx;
  background: linear-gradient(135deg, #ffd3c2, #ffb6a5);
}

.content {
  padding: 24rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.price {
  margin-top: 12rpx;
  color: #ff6f61;
  font-size: 30rpx;
}

.desc {
  display: block;
  margin-top: 12rpx;
  color: #666;
}

.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx 40rpx;
  background: #fff;
  box-shadow: 0 -8rpx 20rpx rgba(0, 0, 0, 0.08);
}

.btn-outline {
  flex: 1;
  background: #fff3f0;
  color: #ff6f61;
  border-radius: 999rpx;
}

.btn {
  flex: 1;
  background: #ff6f61;
  color: #fff;
  border-radius: 999rpx;
}
</style>
