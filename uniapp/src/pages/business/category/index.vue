<template>
  <view class="page">
    <view class="nav"><text class="title">客户</text></view>
    <scroll-view class="content" scroll-y>
      <view class="search-card">
        <text class="search-text">客户名称、联系人、手机号</text>
      </view>
      <view v-for="item in customers" :key="item.id" class="card">
        <view class="row">
          <text class="name">{{ item.name || '未命名客户' }}</text>
          <text class="status">{{ item.status === 0 ? '停用' : '启用' }}</text>
        </view>
        <text class="line">联系人：{{ item.contact || '-' }}</text>
        <text class="line">电话：{{ item.phone || '-' }}</text>
        <text class="line">地址：{{ item.address || '-' }}</text>
      </view>
      <view v-if="loadError" class="empty error">{{ loadError }}</view>
      <view v-else-if="customers.length === 0" class="empty">暂无客户资料</view>
      <view class="safe-area"></view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {getMobileCustomers} from '@/api/business'

const customers = ref<any[]>([])
const loadError = ref('')

const loadData = async () => {
  loadError.value = ''
  try {
    const res = await getMobileCustomers()
    customers.value = res?.data || []
  } catch (e) {
    loadError.value = '客户资料加载失败，请登录后重试'
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { height: 100vh; background: #f5f7f6; }
.nav { padding: 28rpx 24rpx; background: #fff; border-bottom: 1rpx solid #e5ebe7; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2d2a; }
.content { height: calc(100vh - 180rpx); padding: 24rpx; }
.search-card { background: #fff; border-radius: 14rpx; padding: 20rpx 24rpx; color: #9aa4a0; border: 1rpx solid #e5ebe7; margin-bottom: 18rpx; }
.search-text { font-size: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 22rpx; border: 1rpx solid #e5ebe7; margin-bottom: 16rpx; }
.row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.name { font-size: 28rpx; font-weight: 700; color: #1f2d2a; }
.status { color: #2f7d57; font-size: 24rpx; }
.line { display: block; color: #66736e; font-size: 24rpx; margin-top: 8rpx; }
.empty { text-align: center; color: #9aa4a0; margin-top: 80rpx; }
.error { color: #c2410c; }
.safe-area { height: 140rpx; }
</style>
