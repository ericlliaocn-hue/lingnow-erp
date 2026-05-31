<template>
  <view :class="themeClass" class="container">
    <view class="list-menu">
      <view class="list-item" v-for="item in socialAccounts" :key="item.type">
        <text>{{ item.name }}</text>
        <view v-if="item.isBind">
           <text class="status bind">已绑定</text>
           <text class="unbind-btn" @click="handleUnbind(item)">解绑</text>
        </view>
        <text v-else class="status unbind" @click="handleBind(item)">去绑定</text>
      </view>
    </view>
    <GlobalLoginPopup/>
  </view>
</template>

<script setup lang="ts">
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {useTheme} from '@/utils/theme'

const {themeClass, updateNavigationBar} = useTheme()

onShow(() => {
  updateNavigationBar()
})

const socialAccounts = ref([
  { type: 'wechat', name: '微信', isBind: false },
  { type: 'weibo', name: '微博', isBind: false },
  { type: 'xiaohongshu', name: '小红书', isBind: false },
  { type: 'douyin', name: '抖音', isBind: false }
])

const handleBind = (item: any) => {
  uni.showToast({ title: `绑定${item.name}暂未接入`, icon: 'none' })
}

const handleUnbind = (item: any) => {
  uni.showToast({ title: `解绑${item.name}暂未接入`, icon: 'none' })
}
</script>

<style lang="scss">
.container {
  background-color: var(--bg-color);
  min-height: 100vh;
}
.section-title {
  padding: 20rpx 30rpx;
  color: var(--sub-text);
  font-size: 28rpx;
}
.list-menu {
  background-color: var(--card-bg);
  .list-item {
    padding: 30rpx;
    border-bottom: 1rpx solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 30rpx;
    color: var(--text-color);

    .status {
      font-size: 28rpx;
      &.unbind {
        color: var(--sub-text);
      }
      &.bind {
        color: var(--primary-color);
      }
    }
  }
}
</style>
