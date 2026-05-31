<template>
  <view class="page">
    <view class="nav">
      <view class="logo">业务下单</view>
      <view class="search" @click="goSearch">
        <text class="search-text">搜索商品</text>
      </view>
    </view>

    <scroll-view class="content" scroll-y>
      <view class="section">
        <view class="section-title">热门分类</view>
        <view class="pill-row">
          <view v-for="item in categories" :key="item.id" class="pill" @click="goCategory(item)">
            {{ item.name || '分类' }}
          </view>
          <view v-if="categories.length === 0" class="pill empty">暂无分类</view>
        </view>
      </view>

      <view class="section">
        <view class="section-title">推荐商品</view>
        <view class="card-grid">
          <view v-for="item in products" :key="item.id" class="card" @click="goDetail(item)">
            <image v-if="item.image" :src="item.image" class="cover" mode="aspectFill"></image>
            <view v-else class="cover"></view>
            <view class="card-title">{{ item.name || '商品' }}</view>
            <view class="card-sub">
              <text>¥ {{ item.price || 0 }}</text>
              <view class="quick-add" @click.stop="addToCart(item)">+</view>
            </view>
          </view>
          <view v-if="products.length === 0" class="empty-tip">暂无商品</view>
        </view>
      </view>

      <view class="safe-area"></view>
    </scroll-view>

    <TabBar :current="0"/>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import TabBar from '@/components/TabBar.vue'
import {addCartItem, getProductCategories, getProducts} from '@/api/business'

const categories = ref<any[]>([])
const products = ref<any[]>([])
const currentUser = uni.getStorageSync('userInfo') || null

const loadData = async () => {
  try {
    const [catRes, prodRes] = await Promise.all([getProductCategories(), getProducts()])
    categories.value = catRes?.data || []
    products.value = prodRes?.data || []
  } catch (e) {
    console.error(e)
  }
}

const goCategory = (item: any) => {
  uni.switchTab({url: '/pages/business/category/index'})
}

const goDetail = (item: any) => {
  uni.navigateTo({url: '/pages/business/product/detail?id=' + (item.id || '')})
}

const addToCart = async (item: any) => {
  if (!currentUser?.userId) {
    uni.showToast({title: '请先登录', icon: 'none'})
    return
  }
  try {
    await addCartItem({
      userId: currentUser.userId,
      productId: item.id,
      quantity: 1,
      price: item.price || 0
    })
    uni.showToast({title: '已加入购物车', icon: 'success'})
  } catch (e) {
    console.error(e)
    uni.showToast({title: '加入失败', icon: 'none'})
  }
}

const goSearch = () => {
  uni.showToast({title: '请输入关键词', icon: 'none'})
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
  padding: 24rpx 24rpx 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.logo {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f1f1f;
}

.search {
  flex: 1;
  background: #ffffff;
  border-radius: 999rpx;
  padding: 16rpx 24rpx;
  box-shadow: 0 10rpx 20rpx rgba(0, 0, 0, 0.04);
}

.search-text {
  color: #9aa0a6;
  font-size: 24rpx;
}

.content {
  height: calc(100vh - 160rpx);
  padding: 0 24rpx;
}

.section {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 10rpx 20rpx rgba(0, 0, 0, 0.03);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.pill {
  background: #f2f2f2;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: #333;
}

.pill.empty {
  color: #999;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.card {
  background: #fff7f5;
  border-radius: 20rpx;
  padding: 16rpx;
}

.cover {
  height: 160rpx;
  width: 100%;
  display: block;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #ffd7c2, #ffb7a1);
  margin-bottom: 12rpx;
}

.card-title {
  font-size: 26rpx;
  font-weight: 600;
}

.card-sub {
  color: #ff6f61;
  margin-top: 6rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quick-add {
  width: 44rpx;
  height: 44rpx;
  border-radius: 22rpx;
  background: #ff6f61;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.empty-tip {
  grid-column: span 2;
  color: #999;
  text-align: center;
  padding: 24rpx 0;
}

.safe-area {
  height: 140rpx;
}
</style>
