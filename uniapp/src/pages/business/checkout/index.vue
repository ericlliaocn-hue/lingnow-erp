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
        <view class="card-title">
          <text class="label">地址识别</text>
          <button class="mini-btn" :disabled="parsing" @click="parseAddressText">{{ parsing ? '识别中' : '识别' }}</button>
        </view>
        <textarea v-model="rawAddress" class="address-input" placeholder="粘贴姓名、手机号、完整地址" />
        <view v-if="addressResult" class="result">
          <text class="line">姓名：{{ addressResult.contactName || '-' }}</text>
          <text class="line">电话：{{ addressResult.phone || '-' }}</text>
          <text class="line">地址：{{ addressResult.normalizedAddress || '-' }}</text>
          <text class="line">置信度：{{ addressResult.confidence || 0 }}%</text>
        </view>
        <text v-if="addressTip" class="tip">{{ addressTip }}</text>
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
import {onShow} from '@dcloudio/uni-app'
import {parseMobileAddress} from '@/api/business'
import {requireLogin} from '@/utils/auth'

const billTypes = [
  {label: '销售单', value: 'SALE'},
  {label: '销售退货单', value: 'SALE_RETURN'},
  {label: '进货单', value: 'PURCHASE'},
  {label: '进货退货单', value: 'PURCHASE_RETURN'}
]
const typeIndex = ref(0)
const rawAddress = ref('')
const parsing = ref(false)
const addressResult = ref<any>()
const addressTip = ref('')
const currentType = computed(() => billTypes[typeIndex.value])
const changeType = (e: any) => { typeIndex.value = Number(e.detail.value || 0) }

onShow(() => {
  requireLogin('/pages/business/checkout/index')
})

const parseAddressText = async () => {
  if (!requireLogin('/pages/business/checkout/index')) return
  addressTip.value = ''
  addressResult.value = undefined
  if (!rawAddress.value.trim()) {
    addressTip.value = '请先粘贴地址内容'
    return
  }
  parsing.value = true
  try {
    const res: any = await parseMobileAddress(rawAddress.value)
    addressResult.value = res?.data || res
    const warnings = addressResult.value?.warnings || []
    addressTip.value = warnings.length ? warnings.join('，') : '识别完成'
  } catch (e) {
    addressTip.value = '地址识别失败，请登录后重试'
  } finally {
    parsing.value = false
  }
}
const goBills = () => {
  if (!requireLogin('/pages/business/order/index')) return
  uni.switchTab({url: '/pages/business/order/index'})
}
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
.card-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.mini-btn { margin: 0; background: #2f7d57; color: #fff; border-radius: 999rpx; font-size: 22rpx; line-height: 52rpx; padding: 0 24rpx; }
.address-input { width: 100%; min-height: 150rpx; box-sizing: border-box; border: 1rpx solid #d8e2dc; border-radius: 12rpx; padding: 16rpx; font-size: 26rpx; color: #1f2d2a; background: #fbfdfc; }
.result { margin-top: 14rpx; }
.line { display: block; color: #66736e; font-size: 24rpx; margin-top: 8rpx; }
.tip { display: block; margin-top: 12rpx; color: #c2410c; font-size: 24rpx; }
</style>
