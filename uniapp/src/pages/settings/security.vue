<template>
  <view :class="themeClass" class="container">
    <view class="list-menu">
      <view class="list-item" @click="handleChangePassword">
        <text>修改密码</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="handleChangePhone">
        <text>修改手机号</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="handleDeleteAccount">
        <text>注销账号</text>
        <text class="arrow">></text>
      </view>
    </view>
    <GlobalLoginPopup/>
  </view>
</template>

<script setup lang="ts">
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {useTheme} from '@/utils/theme'
import {onShow} from '@dcloudio/uni-app'
import {requireLogin} from '@/utils/auth'

const {themeClass, updateNavigationBar} = useTheme()

onShow(() => {
  updateNavigationBar()
  requireLogin('/pages/settings/security')
})

const handleChangePassword = () => {
  if (!requireLogin('/pages/settings/password')) return
  uni.navigateTo({url: '/pages/settings/password'})
}

const handleChangePhone = () => {
  if (!requireLogin('/pages/settings/mobile')) return
  uni.navigateTo({url: '/pages/settings/mobile'})
}

const handleDeleteAccount = () => {
  if (!requireLogin('/pages/settings/security')) return
  uni.showModal({
    title: '警告',
    content: '注销后账号将无法恢复，确定要继续吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '申请已提交' })
      }
    }
  })
}
</script>

<style lang="scss">
.container {
  background-color: var(--bg-color);
  min-height: 100vh;
}
.list-menu {
  background-color: var(--card-bg);
  .list-item {
    padding: 30rpx;
    border-bottom: 1rpx solid var(--border-color);
    display: flex;
    justify-content: space-between;
    font-size: 30rpx;
    color: var(--text-color);
    .arrow {
      color: var(--sub-text);
    }
  }
}
</style>
