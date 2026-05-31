<template>
  <view :class="themeClass" class="register-container">
    <!-- 顶部装饰区域 -->
    <view class="top-decoration">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <view class="mascot-area">
        <view class="mascot-circle">
          <text class="mascot-emoji">💌</text>
        </view>
      </view>
    </view>

    <!-- 注册卡片 -->
    <view class="register-card">
      <view class="header-text">
        <text class="title">创建账号</text>
        <text class="subtitle">注册业务端账号</text>
      </view>

      <view class="form-area">
        <view class="input-wrapper">
          <image :src="icons.phone" class="input-icon-img" mode="aspectFit"/>
          <input
              v-model="form.phone"
              class="custom-input"
              placeholder="请输入手机号"
              placeholder-class="placeholder"
          />
        </view>

        <view class="input-wrapper">
          <image :src="icons.code" class="input-icon-img" mode="aspectFit"/>
          <input
              v-model="form.code"
              class="custom-input"
              placeholder="验证码"
              placeholder-class="placeholder"
              type="number"
          />
          <text class="code-btn" @click="handleGetCode">{{ codeText }}</text>
        </view>

        <view class="input-wrapper">
          <image :src="icons.lock" class="input-icon-img" mode="aspectFit"/>
          <input
              v-model="form.password"
              class="custom-input"
              placeholder="设置密码"
              placeholder-class="placeholder"
              type="password"
          />
        </view>

        <view class="input-wrapper">
          <image :src="icons.lock" class="input-icon-img" mode="aspectFit"/>
          <input
              v-model="form.confirmPassword"
              class="custom-input"
              placeholder="确认密码"
              placeholder-class="placeholder"
              type="password"
          />
        </view>

        <button class="register-btn" hover-class="btn-hover" @click="handleRegister">
          立即注册
        </button>

        <view class="login-link">
          <text>已有账号？</text>
          <text class="link-text" @click="goToLogin">去登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import {computed, onUnmounted, reactive, ref} from 'vue'
import {register, sendCode} from '@/api/user'
import {icons} from '@/utils/icons'
import {useTheme} from '@/utils/theme'

const {themeClass} = useTheme()

const form = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const countdown = ref(0)
let timer: any = null

const codeText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s` : '获取'
})

const handleGetCode = async () => {
  if (countdown.value > 0) return
  if (!form.phone) return uni.showToast({title: '请输入手机号', icon: 'none'})

  try {
    const res = await sendCode(form.phone)
    if (res.code === 200) {
      uni.showToast({title: '验证码已发送'})
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    }
  } catch (e) {
    console.error(e)
  }
}

const handleRegister = async () => {
  if (!form.phone) return uni.showToast({ title: '请输入手机号', icon: 'none' })
  if (!form.code) return uni.showToast({ title: '请输入验证码', icon: 'none' })
  if (!form.password) return uni.showToast({ title: '请输入密码', icon: 'none' })
  if (form.password !== form.confirmPassword) return uni.showToast({ title: '两次密码不一致', icon: 'none' })

  try {
    const res = await register({
      phone: form.phone,
      code: form.code,
      password: form.password
    })

    if (res.code === 200) {
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('userInfo', res.data)
      uni.showToast({ title: '注册成功' })
      setTimeout(() => {
        uni.switchTab({url: '/pages/business/home/index'})
      }, 1000)
    }
  } catch (e) {
    console.error(e)
  }
}

const goToLogin = () => {
  uni.navigateBack()
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss">
.register-container {
  min-height: 100vh;
  background-color: var(--bg-color-soft);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.top-decoration {
  height: 40vh; /* Slightly smaller than login to fit more inputs */
  background: linear-gradient(180deg, var(--bg-color-soft) 0%, var(--bg-color) 100%);
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;

  .circle-1 {
    position: absolute;
    top: -80rpx;
    left: -50rpx;
    width: 450rpx;
    height: 450rpx;
    background: radial-gradient(circle, var(--warning-color) 0%, rgba(255, 255, 255, 0) 70%);
    opacity: 0.3;
    filter: blur(50rpx);
  }

  .circle-2 {
    position: absolute;
    top: 50rpx;
    right: -50rpx;
    width: 350rpx;
    height: 350rpx;
    background: radial-gradient(circle, var(--primary-color) 0%, rgba(255, 255, 255, 0) 70%);
    opacity: 0.3;
    filter: blur(40rpx);
  }

  .mascot-area {
    position: relative;
    z-index: 10;
    margin-top: -40rpx;

    .mascot-circle {
      width: 180rpx;
      height: 180rpx;
      background: var(--card-bg);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10rpx 30rpx rgba(255, 183, 77, 0.2);
      border: 8rpx solid var(--bg-color-soft, #f5f5f5);

      .mascot-emoji {
        font-size: 80rpx;
      }
    }
  }
}

.register-card {
  flex: 1;
  background: var(--card-bg);
  border-radius: 60rpx 60rpx 0 0;
  padding: 50rpx 50rpx;
  box-shadow: 0 -10rpx 40rpx rgba(0, 0, 0, 0.03);
  position: relative;
  margin-top: -50rpx;
  z-index: 20;

  .header-text {
    text-align: center;
    margin-bottom: 40rpx;

    .title {
      display: block;
      font-size: 44rpx;
      font-weight: bold;
      color: var(--text-color);
      margin-bottom: 8rpx;
    }

    .subtitle {
      font-size: 24rpx;
      color: var(--sub-text);
    }
  }

  .form-area {
    .input-wrapper {
      background: var(--input-bg);
      border-radius: 40rpx;
      padding: 24rpx 30rpx;
      margin-bottom: 24rpx;
      display: flex;
      align-items: center;
      transition: all 0.3s;
      border: 2rpx solid transparent;

      &:focus-within {
        background: var(--card-bg);
        border-color: var(--warning-color); /* Use warning color for register focus */
        box-shadow: 0 4rpx 12rpx rgba(255, 183, 77, 0.15);
      }

      .input-icon-img {
        width: 40rpx;
        height: 40rpx;
        margin-right: 20rpx;
      }

      .custom-input {
        flex: 1;
        font-size: 28rpx;
        color: var(--text-color);
      }

      .placeholder {
        color: var(--sub-text);
      }

      .code-btn {
        font-size: 24rpx;
        color: var(--warning-color);
        font-weight: bold;
        padding: 10rpx 20rpx;
        background: var(--card-bg);
        border-radius: 20rpx;
        margin-left: 10rpx;
        box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.05);
      }
    }

    .register-btn {
      margin-top: 50rpx;
      background: linear-gradient(135deg, var(--warning-color), var(--primary-color));
      color: var(--text-color-inverse);
      font-size: 34rpx;
      font-weight: bold;
      border-radius: 50rpx;
      height: 100rpx;
      line-height: 100rpx;
      box-shadow: 0 8rpx 20rpx rgba(var(--warning-color-rgb), 0.3);
      letter-spacing: 4rpx;

      &::after {
        border: none;
      }
    }

    .btn-hover {
      transform: scale(0.98);
      opacity: 0.9;
    }

    .login-link {
      margin-top: 30rpx;
      text-align: center;
      font-size: 26rpx;
      color: var(--sub-text);

      .link-text {
        color: var(--primary-color);
        font-weight: bold;
        margin-left: 10rpx;
      }
    }
  }
}
</style>
