<template>
  <view class="page">
    <view class="nav">
      <text class="title">订单</text>
    </view>
    <scroll-view class="content" scroll-y>
      <view v-for="item in orders" :key="item.id" class="card">
        <view class="row">
          <text class="order-no">订单号 {{ item.orderNo || item.id }}</text>
          <text class="status">{{ item.status || '-' }}</text>
        </view>
        <view class="row">
          <text class="amount">¥ {{ item.totalAmount ?? '-' }}</text>
          <button class="btn" @click="goDetail(item)">查看</button>
        </view>
      </view>
      <view v-if="orders.length === 0" class="empty">暂无订单</view>
    </scroll-view>
    <TabBar :current="3"/>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import TabBar from '@/components/TabBar.vue'
import {getOrders} from '@/api/business'

const orders = ref<any[]>([])
const currentUser = uni.getStorageSync('userInfo') || null

const loadData = async () => {
  if (!currentUser?.userId) {
    orders.value = []
    return
  }
  try {
    const res = await getOrders(currentUser.userId)
    orders.value = res?.data || []
  } catch (e) {
    console.error(e)
  }
}

const goDetail = (item: any) => {
  uni.showToast({title: '订单详情暂未接入', icon: 'none'})
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
  height: calc(100vh - 180rpx);
  padding: 0 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx;
  margin-bottom: 16rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.order-no {
  color: #666;
  font-size: 24rpx;
}

.status {
  color: #ff6f61;
}

.amount {
  font-size: 28rpx;
  font-weight: 600;
}

.btn {
  background: #ff6f61;
  color: #fff;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.empty {
  text-align: center;
  color: #999;
  margin-top: 80rpx;
}
</style>
