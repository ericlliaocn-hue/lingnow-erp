<template>
  <view class="page">
    <view class="nav">
      <text class="title">单据</text>
      <button class="new-btn" @click="goCreate">新建</button>
    </view>
    <scroll-view class="content" scroll-y>
      <view v-for="item in bills" :key="item.id" class="card">
        <view class="row">
          <text class="bill-no">{{ item.billNo || item.id }}</text>
          <text class="status">{{ item.auditStatus === 1 ? '已审核' : '未审核' }}</text>
        </view>
        <text class="line">类型：{{ item.billType || '-' }}</text>
        <text class="line">客户/供应商：{{ item.partnerName || '-' }}</text>
        <text class="amount">金额：{{ item.payableAmount ?? item.amount ?? '-' }}</text>
      </view>
      <view v-if="loadError" class="empty error">{{ loadError }}</view>
      <view v-else-if="bills.length === 0" class="empty">暂无业务单据</view>
      <view class="safe-area"></view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {getMobileBills} from '@/api/business'
import {requireLogin} from '@/utils/auth'

const bills = ref<any[]>([])
const loadError = ref('')

const loadData = async () => {
  if (!requireLogin('/pages/business/order/index')) {
    return
  }
  loadError.value = ''
  try {
    const res = await getMobileBills()
    bills.value = res?.data || []
  } catch (e) {
    loadError.value = '业务单据加载失败，请登录后重试'
  }
}

const goCreate = () => {
  if (!requireLogin('/pages/business/checkout/index')) return
  uni.navigateTo({url: '/pages/business/checkout/index'})
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { height: 100vh; background: #f5f7f6; }
.nav { padding: 24rpx; display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1rpx solid #e5ebe7; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2d2a; }
.new-btn { background: #2f7d57; color: #fff; border-radius: 999rpx; font-size: 24rpx; padding: 0 28rpx; }
.content { height: calc(100vh - 180rpx); padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 22rpx; border: 1rpx solid #e5ebe7; margin-bottom: 16rpx; }
.row { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.bill-no { font-size: 28rpx; font-weight: 700; color: #1f2d2a; }
.status { color: #2f7d57; font-size: 24rpx; }
.line, .amount { display: block; color: #66736e; font-size: 24rpx; margin-top: 8rpx; }
.empty { text-align: center; color: #9aa4a0; margin-top: 80rpx; }
.error { color: #c2410c; }
.safe-area { height: 140rpx; }
</style>
