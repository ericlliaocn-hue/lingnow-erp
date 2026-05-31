<template>
  <view v-if="visible" class="login-popup-mask" @click="close"></view>
  <view v-if="visible" class="login-popup">
    <view class="popup-content">
      <text class="popup-title">温馨提示</text>
      <text class="popup-desc">您还没有登录，登录后即可畅享更多精彩剧集</text>
      <button class="login-btn" @click="goToLogin">立即登录</button>
      <text class="close-text" @click="close">暂不登录</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {ref, onMounted, onUnmounted} from 'vue'

const visible = ref(false)

const open = () => {
  visible.value = true
}

const close = () => {
  visible.value = false
}

const goToLogin = () => {
  visible.value = false

  // Capture current page for redirect
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1]
    const url = currentPage.route
    const options = (currentPage as any).options || {}

    let queryString = Object.keys(options).map(key => {
      return `${key}=${encodeURIComponent(options[key])}`
    }).join('&')

    const fullPath = queryString ? `/${url}?${queryString}` : `/${url}`

    uni.navigateTo({
      url: `/pages/login/index?redirect=${encodeURIComponent(fullPath)}`
    })
  } else {
    uni.navigateTo({
      url: '/pages/login/index'
    })
  }
}

onMounted(() => {
  uni.$on('showGlobalLoginPopup', open)
})

onUnmounted(() => {
  uni.$off('showGlobalLoginPopup', open)
})

defineExpose({
  open,
  close
})
</script>

<style lang="scss" scoped>
.login-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  z-index: 9999;
}

.login-popup {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 280px;
  background: var(--card-bg, #1c1c1e);
  border-radius: 16px;
  z-index: 10000;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);

  .popup-content {
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;

    .popup-title {
      font-size: 18px;
      font-weight: bold;
      color: var(--text-color, #fff);
      margin-bottom: 12px;
    }

    .popup-desc {
      font-size: 14px;
      color: var(--sub-text, #8e8e93);
      text-align: center;
      margin-bottom: 24px;
      line-height: 1.5;
    }

    .login-btn {
      width: 100%;
      height: 40px;
      line-height: 40px;
      background: var(--primary-color, #ff2442);
      color: #fff;
      font-size: 16px;
      font-weight: 500;
      border-radius: 20px;
      margin-bottom: 12px;
    }

    .close-text {
      font-size: 14px;
      color: var(--sub-text, #8e8e93);
      padding: 8px;
    }
  }
}
</style>
