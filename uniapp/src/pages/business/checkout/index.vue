<template>
  <view class="page">
    <view class="nav">
      <text class="title">确认下单</text>
    </view>
    <view class="content">
      <view class="card">
        <text class="label">收货地址</text>
        <text class="value">{{ addressText }}</text>
      </view>
      <view class="card">
        <text class="label">商品</text>
        <text class="value">{{ summaryText }}</text>
      </view>
    </view>
    <view class="footer">
      <text class="total">合计 ¥ {{ totalAmount }}</text>
      <button class="btn" @click="submitOrder">提交订单</button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {createOrder, getCartItems, getProducts} from '@/api/business'

const cartItems = ref<any[]>([])
const totalAmount = ref(0)
const currentUser = uni.getStorageSync('userInfo') || null
const addressText = ref('未选择收货地址')
const summaryText = computed(() => {
  if (cartItems.value.length === 0) return '暂无商品'
  return `共 ${cartItems.value.length} 件商品`
})

const loadCart = async () => {
  try {
    const userId = currentUser?.userId
    const [cartRes, productRes] = await Promise.all([getCartItems(userId), getProducts()])
    const products = productRes?.data || []
    cartItems.value = (cartRes?.data || []).map((item: any) => {
      const prod = products.find((p: any) => Number(p.id) === Number(item.productId))
      return {
        ...item,
        price: item.price || prod?.price || 0,
        quantity: item.quantity || 1,
        productName: prod?.name || item.productName
      }
    })
    totalAmount.value = cartItems.value.reduce((sum, item) => sum + (Number(item.price) * Number(item.quantity)), 0)
  } catch (error) {
    console.error(error)
    cartItems.value = []
    totalAmount.value = 0
  }
}

const submitOrder = async () => {
  if (!currentUser?.userId) {
    uni.showToast({title: '请先登录', icon: 'none'})
    return
  }
  if (cartItems.value.length === 0) {
    uni.showToast({title: '暂无可提交商品', icon: 'none'})
    return
  }
  try {
    const orderRes = await createOrder({
      userId: currentUser.userId,
      totalAmount: totalAmount.value,
      items: cartItems.value.map(item => ({
        productId: item.productId,
        productName: item.productName,
        price: item.price,
        quantity: item.quantity
      }))
    })
    if (orderRes?.code !== 200) {
      throw orderRes
    }
    uni.showToast({title: '下单成功', icon: 'success'})
  } catch (e) {
    console.error(e)
    uni.showToast({title: '下单失败', icon: 'none'})
  }
}

onMounted(() => {
  loadCart()
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
  padding: 0 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.label {
  color: #999;
  font-size: 24rpx;
}

.value {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx 40rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 -8rpx 20rpx rgba(0, 0, 0, 0.08);
}

.total {
  font-size: 30rpx;
  font-weight: 600;
  color: #ff6f61;
}

.btn {
  background: #ff6f61;
  color: #fff;
  border-radius: 999rpx;
  padding: 0 32rpx;
}
</style>
