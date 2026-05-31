<template>
  <view :class="themeClass" class="forget-container">
    <!-- 顶部装饰区域 -->
    <view class="top-decoration">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <view class="mascot-area">
        <view class="mascot-circle">
          <text class="mascot-emoji">🔑</text>
        </view>
      </view>
    </view>

    <!-- 重置卡片 -->
    <view class="forget-card">
      <view class="header-text">
        <text class="title">找回密码</text>
        <text class="subtitle">通过手机号验证重置密码</text>
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
          <text class="code-btn" @click="handleSendCode">{{ timer > 0 ? `${timer}s` : '获取' }}</text>
        </view>

        <view class="input-wrapper">
          <image :src="icons.lock" class="input-icon-img" mode="aspectFit"/>
          <input
              v-model="form.password"
              class="custom-input"
              placeholder="设置新密码"
              placeholder-class="placeholder"
              type="password"
          />
        </view>

        <button class="reset-btn" hover-class="btn-hover" @click="handleForget">
          重置密码
        </button>

        <view class="login-link">
          <text class="link-text" @click="goToLogin">返回登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue'
import {forgetPassword, sendCode} from '@/api/user'
import {icons} from '@/utils/icons'
import {useTheme} from '@/utils/theme'

const {themeClass} = useTheme()

const form = reactive({
  phone: '',
  code: '',
  password: ''
})

const timer = ref(0)
let timerId: any = null

const handleSendCode = async () => {
  if (timer.value > 0) return
  if (!form.phone) return uni.showToast({title: '请输入手机号', icon: 'none'})
  if (!/^1[3-9]\d{9}$/.test(form.phone)) return uni.showToast({title: '手机号格式错误', icon: 'none'})

  try {
    await sendCode(form.phone)
    uni.showToast({title: '发送成功', icon: 'none'})
    timer.value = 60
    timerId = setInterval(() => {
      timer.value--
      if (timer.value <= 0) {
        clearInterval(timerId)
      }
    }, 1000)
  } catch (e) {
    // ignore
  }
}

const handleForget = async () => {
  if (!form.phone) return uni.showToast({ title: '请输入手机号', icon: 'none' })
  if (!form.code) return uni.showToast({ title: '请输入验证码', icon: 'none' })
  if (!form.password) return uni.showToast({ title: '请输入新密码', icon: 'none' })

  try {
    const res = await forgetPassword(form)

    if (res.code === 200) {
      uni.showToast({ title: '重置成功' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/index' })
      }, 1000)
    }
  } catch (e) {
    console.error(e)
  }
}

const goToLogin = () => {
  uni.navigateTo({url: '/pages/login/index'})
}
</script>

<style lang="scss">
.forget-container {
  min-height: 100vh;
  background-color: var(--bg-color);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.top-decoration {
  height: 40vh;
  background: linear-gradient(180deg, var(--bg-color) 0%, var(--card-bg) 100%);
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;

  .circle-1 {
    position: absolute;
    top: -80rpx;
    right: -50rpx;
    width: 400rpx;
    height: 400rpx;
    background: radial-gradient(circle, var(--success-color) 0%, rgba(255, 255, 255, 0) 70%);
    opacity: 0.3;
    filter: blur(40rpx);
  }

  .circle-2 {
    position: absolute;
    top: 50rpx;
    left: -50rpx;
    width: 300rpx;
    height: 300rpx;
    background: radial-gradient(circle, var(--secondary-color) 0%, rgba(255, 255, 255, 0) 70%);
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
      box-shadow: 0 10rpx 30rpx rgba(var(--success-color-rgb), 0.2);
      border: 8rpx solid var(--bg-color);

      .mascot-emoji {
        font-size: 80rpx;
      }
    }
  }
}

.forget-card {
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
        border-color: var(--success-color);
        box-shadow: 0 4rpx 12rpx rgba(129, 199, 132, 0.15);
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
        color: var(--success-color);
        font-weight: bold;
        padding: 10rpx 20rpx;
        background: var(--card-bg);
        border-radius: 20rpx;
        margin-left: 10rpx;
        box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.05);
      }
    }

    .reset-btn {
      margin-top: 50rpx;
      background: linear-gradient(135deg, var(--success-color), var(--primary-color));
      color: var(--text-color-inverse);
      font-size: 34rpx;
      font-weight: bold;
      border-radius: 50rpx;
      height: 100rpx;
      line-height: 100rpx;
      box-shadow: 0 8rpx 20rpx rgba(var(--success-color-rgb), 0.3);
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

      .link-text {
        color: var(--sub-text);
        font-weight: bold;
      }
    }
  }
}
</style>
