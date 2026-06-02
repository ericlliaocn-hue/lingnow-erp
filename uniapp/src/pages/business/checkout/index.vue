<template>
  <view class="page">
    <view class="nav"><text class="title">新建单据</text></view>
    <view class="content">
      <view class="card">
        <text class="label">单据类型</text>
        <picker :range="billTypes" range-key="label" @change="changeType">
          <view class="picker">{{ currentType.label }}</view>
        </picker>
      </view>
      <view class="card">
        <text class="label">往来单位</text>
        <text class="value">移动端 v1 暂不创建正式单据</text>
      </view>
      <view class="card">
        <text class="label">商品明细</text>
        <text class="value">请在管理端完成开单、审核和库存处理</text>
      </view>
      <button class="btn" @click="goBills">查看业务单据</button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'

const billTypes = [
  {label: '销售单', value: 'SALE'},
  {label: '销售退货单', value: 'SALE_RETURN'},
  {label: '进货单', value: 'PURCHASE'},
  {label: '进货退货单', value: 'PURCHASE_RETURN'}
]
const typeIndex = ref(0)
const currentType = computed(() => billTypes[typeIndex.value])
const changeType = (e: any) => { typeIndex.value = Number(e.detail.value || 0) }
const goBills = () => uni.switchTab({url: '/pages/business/order/index'})
</script>

<style lang="scss" scoped>
.page { height: 100vh; background: #f5f7f6; }
.nav { padding: 28rpx 24rpx; background: #fff; border-bottom: 1rpx solid #e5ebe7; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2d2a; }
.content { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 22rpx; border: 1rpx solid #e5ebe7; margin-bottom: 16rpx; }
.label { display: block; color: #7a8580; font-size: 24rpx; margin-bottom: 10rpx; }
.value, .picker { color: #1f2d2a; font-size: 28rpx; }
.btn { margin-top: 24rpx; background: #2f7d57; color: #fff; border-radius: 999rpx; }
</style>
