<template>
  <view class="page">
    <view class="nav">
      <view>
        <view class="logo">ERP业务端</view>
        <view class="sub">客户、商品、单据移动工作台</view>
      </view>
    </view>

    <scroll-view class="content" scroll-y>
      <view class="metric-grid">
        <view class="metric">
          <text class="metric-value">{{ loadError ? '-' : (dashboard.customerCount || 0) }}</text>
          <text class="metric-label">客户数</text>
        </view>
        <view class="metric">
          <text class="metric-value">{{ loadError ? '-' : (dashboard.productCount || 0) }}</text>
          <text class="metric-label">商品数</text>
        </view>
        <view class="metric">
          <text class="metric-value">{{ loadError ? '-' : (dashboard.billCount || 0) }}</text>
          <text class="metric-label">单据数</text>
        </view>
        <view class="metric">
          <text class="metric-value">{{ loadError ? '-' : (dashboard.receivableAmount || 0) }}</text>
          <text class="metric-label">应收余额</text>
        </view>
      </view>
      <view v-if="loadError" class="error-tip">{{ loadError }}</view>

      <view class="section">
        <view class="section-title">快捷入口</view>
        <view class="action-grid">
          <view class="action" @click="go('/pages/business/category/index')">客户档案</view>
          <view class="action" @click="go('/pages/business/cart/index')">商品资料</view>
          <view class="action" @click="go('/pages/business/order/index')">业务单据</view>
          <view class="action" @click="go('/pages/business/checkout/index')">新建单据</view>
        </view>
      </view>

      <view class="section">
        <view class="section-title">待办提醒</view>
        <view class="empty-tip">暂无待处理事项</view>
      </view>
      <view class="safe-area"></view>
    </scroll-view>

  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {getMobileDashboard} from '@/api/business'

const dashboard = ref<any>({})
const loadError = ref('')

const loadData = async () => {
  loadError.value = ''
  try {
    const res = await getMobileDashboard()
    dashboard.value = res?.data || {}
  } catch (e) {
    loadError.value = '数据加载失败，请登录后重试'
  }
}

const go = (url: string) => {
  if (url.includes('/category') || url.includes('/cart') || url.includes('/order')) {
    uni.switchTab({url})
    return
  }
  uni.navigateTo({url})
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { height: 100vh; background: #f5f7f6; }
.nav { padding: 32rpx 24rpx 8rpx; display: flex; align-items: center; justify-content: space-between; }
.logo { font-size: 34rpx; font-weight: 700; color: #1f2d2a; }
.sub { margin-top: 8rpx; color: #7a8580; font-size: 24rpx; }
.content { height: calc(100vh - 150rpx); padding: 0 24rpx; }
.metric-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; margin-top: 24rpx; }
.metric { background: #fff; border-radius: 16rpx; padding: 24rpx; border: 1rpx solid #e5ebe7; }
.metric-value { display: block; font-size: 36rpx; font-weight: 700; color: #2f7d57; }
.metric-label { display: block; margin-top: 8rpx; color: #7a8580; font-size: 24rpx; }
.section { margin-top: 24rpx; background: #fff; border-radius: 16rpx; padding: 24rpx; border: 1rpx solid #e5ebe7; }
.section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 18rpx; color: #1f2d2a; }
.action-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; }
.action { background: #eef7f1; color: #2f7d57; border-radius: 12rpx; padding: 24rpx; text-align: center; font-size: 26rpx; }
.empty-tip { color: #9aa4a0; text-align: center; padding: 32rpx 0; }
.error-tip { margin-top: 18rpx; color: #c2410c; background: #fff7ed; border: 1rpx solid #fed7aa; border-radius: 12rpx; padding: 18rpx 20rpx; font-size: 24rpx; }
.safe-area { height: 140rpx; }
</style>
