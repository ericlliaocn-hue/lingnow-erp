<template>
  <view class="page">
    <view class="nav"><text class="title">商品</text></view>
    <scroll-view class="content" scroll-y>
      <view v-for="item in products" :key="item.id" class="card" @click="goDetail(item)">
        <view class="thumb"></view>
        <view class="info">
          <text class="name">{{ item.name || '未命名商品' }}</text>
          <text class="line">编号：{{ item.code || '-' }}</text>
          <text class="line">规格：{{ item.spec || '-' }}</text>
          <text class="line">库存：{{ item.stockQty ?? '-' }}</text>
        </view>
      </view>
      <view v-if="loadError" class="empty error">{{ loadError }}</view>
      <view v-else-if="products.length === 0" class="empty">暂无商品资料</view>
      <view class="safe-area"></view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {getMobileProducts} from '@/api/business'

const products = ref<any[]>([])
const loadError = ref('')

const loadData = async () => {
  loadError.value = ''
  try {
    const res = await getMobileProducts()
    products.value = res?.data || []
  } catch (e) {
    loadError.value = '商品资料加载失败，请登录后重试'
  }
}

const goDetail = (item: any) => {
  uni.navigateTo({url: '/pages/business/product/detail?id=' + (item.id || '')})
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { height: 100vh; background: #f5f7f6; }
.nav { padding: 28rpx 24rpx; background: #fff; border-bottom: 1rpx solid #e5ebe7; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2d2a; }
.content { height: calc(100vh - 180rpx); padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 18rpx; display: flex; gap: 18rpx; border: 1rpx solid #e5ebe7; margin-bottom: 16rpx; }
.thumb { width: 112rpx; height: 112rpx; border-radius: 14rpx; background: #e8f2ec; }
.info { flex: 1; }
.name { display: block; font-size: 28rpx; font-weight: 700; color: #1f2d2a; margin-bottom: 8rpx; }
.line { display: block; color: #66736e; font-size: 24rpx; margin-top: 6rpx; }
.empty { text-align: center; color: #9aa4a0; margin-top: 80rpx; }
.error { color: #c2410c; }
.safe-area { height: 140rpx; }
</style>
