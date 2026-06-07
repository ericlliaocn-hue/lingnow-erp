<template>
  <view class="page">
    <view class="header" @click="goUser">
      <view class="avatar"></view>
      <view class="info">
        <text class="name">{{ displayName }}</text>
        <text class="sub">手机号 {{ user?.phone || '未绑定' }}</text>
      </view>
    </view>
    <view class="panel">
      <view class="item" @click="goProfile">个人资料</view>
      <view class="item" @click="goBills">我的单据</view>
      <view class="item" @click="goSettings">账号设置</view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {getProfile} from '@/api/user'
import {goLogin, goProtectedPage} from '@/utils/auth'

const user = ref<any>(null)
const displayName = computed(() => user.value?.nickname || user.value?.username || '未登录')

const loadData = async () => {
  user.value = uni.getStorageSync('userInfo') || null
  if (!uni.getStorageSync('token')) {
    return
  }
  try {
    const res = await getProfile()
    if (res?.code === 200 && res.data) {
      user.value = res.data
      uni.setStorageSync('userInfo', res.data)
    }
  } catch (e) {
  }
}

const goProfile = () => goProtectedPage('/pages/settings/profile-edit')
const goBills = () => goProtectedPage('/pages/business/order/index')
const goSettings = () => goProtectedPage('/pages/settings/index')
const goUser = () => {
  if (!uni.getStorageSync('token')) {
    goLogin('/pages/business/mine/index')
  }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  background: #f7f7f8;
}

.header {
  padding: 40rpx 24rpx;
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd1c4, #ffb6a5);
}

.name {
  font-size: 30rpx;
  font-weight: 700;
}

.sub {
  color: #999;
  font-size: 24rpx;
}

.panel {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.item {
  padding: 28rpx 24rpx;
  border-bottom: 1px solid #f0f0f0;
}

.item:last-child {
  border-bottom: none;
}
</style>
