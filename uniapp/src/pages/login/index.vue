<template>
  <view :class="themeClass" class="login-container">
    <!-- 头部 Logo/Slogan -->
    <view class="header-section">
      <view class="logo-box">
        <view class="grapefruit">
          <view class="g-inner"></view>
          <view class="g-leaf"></view>
        </view>
      </view>
      <text class="app-name">业务端</text>
    </view>

    <!-- 登录表单 -->
    <view class="form-card">
      <!-- 登录方式切换 -->
      <view class="login-tabs">
        <view
            class="tab-item"
            :class="{ active: loginType === 'code' }"
            @click="loginType = 'code'"
        >
          验证码登录
          <view v-if="loginType === 'code'" class="active-line"></view>
        </view>
        <view
            class="tab-item"
            :class="{ active: loginType === 'password' }"
            @click="loginType = 'password'"
        >
          密码登录
          <view v-if="loginType === 'password'" class="active-line"></view>
        </view>
      </view>

      <view class="input-group">
        <view class="input-item">
          <input
              v-model="form.account"
              class="input"
              placeholder="请输入手机号/账号"
              placeholder-class="input-placeholder"
          />
        </view>

        <view v-if="loginType === 'password'" class="input-item">
          <input
              v-model="form.password"
              class="input"
              type="password"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
          />
        </view>

        <view v-if="loginType === 'code'" class="input-item row-between">
          <input
              v-model="form.code"
              class="input"
              type="number"
              placeholder="请输入验证码"
              placeholder-class="input-placeholder"
          />
          <text :class="{ disabled: countdown > 0 }" class="code-text" @click="handleGetCode">
            {{ codeText }}
          </text>
        </view>
      </view>

      <button class="submit-btn" hover-class="btn-hover" @click="handleLogin">
        登 录
      </button>

      <view class="footer-actions">
        <text class="action-text" @click="goToRegister">注册新账号</text>
        <text class="divider">|</text>
        <text class="action-text" @click="goToForget">忘记密码</text>
      </view>
    </view>

    <!-- 底部装饰 -->
    <view class="bottom-decoration">
      <text class="copyright">LingNow Business Base</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import {useTheme} from '@/utils/theme'
import {computed, onUnmounted, reactive, ref} from 'vue'
import {onLoad, onShow} from '@dcloudio/uni-app'
import {login, sendCode} from '@/api/user'

const {themeClass, updateNavigationBar} = useTheme()

const redirectUrl = ref('')

onLoad((options: any) => {
  if (options.redirect) {
    redirectUrl.value = decodeURIComponent(options.redirect)
  }
})

onShow(() => {
  updateNavigationBar()
})

const loginType = ref<'password' | 'code'>('code')

const form = reactive({
  account: '',
  password: '',
  code: ''
})

const countdown = ref(0)
let timer: any = null

const codeText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s 后重试` : '获取验证码'
})

const handleGetCode = async () => {
  if (countdown.value > 0) return
  if (!form.account) return uni.showToast({title: '请输入手机号', icon: 'none'})

  // Simple phone validation
  if (!/^1[3-9]\d{9}$/.test(form.account)) {
    return uni.showToast({title: '手机号格式错误', icon: 'none'})
  }

  try {
    const res = await sendCode(form.account)
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

const handleSwitchTab = (path: string) => {
  uni.switchTab({
    url: path,
    fail: (err) => console.error('switchTab fail', err)
  })
}

const handleLogin = async () => {
  if (!form.account) {
    return uni.showToast({ title: '请输入账号', icon: 'none' })
  }

  const params = {
    account: form.account,
    type: loginType.value,
    credential: loginType.value === 'password' ? form.password : form.code
  }

  try {
    const res = await login(params)
    if (res.code === 200) {
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('userInfo', res.data)
      uni.showToast({ title: '登录成功' })
      setTimeout(() => {
        if (redirectUrl.value) {
          // Determine if it's a tab bar page or normal page
          const tabPages = [
            '/pages/business/home/index',
            '/pages/business/category/index',
            '/pages/business/cart/index',
            '/pages/business/order/index',
            '/pages/business/mine/index'
          ]
          // Check if it matches exactly or starts with (for query params)
          // Note: switchTab url cannot carry query params usually, but let's try
          const urlPath = redirectUrl.value.split('?')[0]
          const isTab = tabPages.some(path => urlPath === path)

          // Check if we can just navigateBack
          const pages = getCurrentPages()
          if (pages.length > 1) {
            const prevPage = pages[pages.length - 2]
            // Compare routes (ensure leading slash consistency)
            const prevRoute = prevPage.route?.startsWith('/') ? prevPage.route : `/${prevPage.route}`
            if (prevRoute === urlPath) {
              uni.navigateBack({
                fail: (err) => {
                  console.error('navigateBack fail, falling back to redirect/switch', err)
                  // Fallback logic below
                  if (isTab) {
                    handleSwitchTab(urlPath)
                  } else {
                    uni.redirectTo({url: redirectUrl.value})
                  }
                }
              })
              return
            }
          }

          if (isTab) {
            handleSwitchTab(urlPath)
          } else {
            uni.redirectTo({
              url: redirectUrl.value,
              fail: (err) => {
                console.error('redirectTo fail', err)
                uni.switchTab({url: '/pages/business/home/index'})
              }
            })
          }
        } else {
          uni.switchTab({url: '/pages/business/home/index'})
        }
      }, 1000)
    }
  } catch (error) {
    console.error(error)
  }
}

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/index' })
}

const goToForget = () => {
  uni.navigateTo({ url: '/pages/forget/index' })
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss">
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--bg-color) 0%, var(--card-bg) 100%);
  background-color: var(--bg-color);
  padding: 0 60rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.header-section {
  margin-bottom: 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .logo-box {
    width: 160rpx;
    height: 160rpx;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30rpx;
    box-shadow: none;

    .grapefruit {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      background: var(--secondary-color);
      padding: 8rpx;
      box-sizing: border-box;
      position: relative;
      box-shadow: 0 10rpx 30rpx rgba(var(--primary-color-rgb), 0.3);
    }

    .g-inner {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: repeating-conic-gradient(
              from 0deg,
              var(--primary-color) 0deg 50deg,
              var(--card-bg) 50deg 60deg
      );
      border: 4rpx solid var(--card-bg);
    }

    .g-leaf {
      position: absolute;
      top: -10rpx;
      right: 10rpx;
      width: 36rpx;
      height: 20rpx;
      background: var(--success-color);
      border-radius: 20rpx 0 20rpx 0;
    }
  }

  .app-name {
    font-size: 48rpx;
    font-weight: bold;
    color: var(--text-color);
    letter-spacing: 4rpx;
    margin-bottom: 16rpx;
    font-family: serif;
  }

  .app-slogan {
    font-size: 26rpx;
    color: var(--sub-text);
    letter-spacing: 8rpx;
    text-transform: uppercase;
  }
}

.form-card {
  width: 100%;
}

.login-tabs {
  display: flex;
  margin-bottom: 60rpx;

  .tab-item {
    margin-right: 40rpx;
    font-size: 32rpx;
    color: var(--sub-text);
    font-weight: 500;
    position: relative;
    padding-bottom: 12rpx;
    transition: all 0.3s;

    &.active {
      color: var(--text-color);
      font-weight: bold;
      font-size: 36rpx;
    }

    .active-line {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 40rpx;
      height: 6rpx;
      background: var(--primary-color);
      border-radius: 4rpx;
    }
  }
}

.input-group {
  margin-bottom: 60rpx;

  .input-item {
    background: var(--input-bg);
    border-radius: 12rpx;
    padding: 24rpx 30rpx;
    margin-bottom: 30rpx;
    border: 2rpx solid transparent;
    transition: all 0.3s;

    &.row-between {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &:focus-within {
      background: var(--card-bg);
      border-color: var(--primary-color);
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
    }

    .input {
      font-size: 30rpx;
      color: var(--text-color);
      width: 100%;
    }

    .input-placeholder {
      color: var(--sub-text);
    }

    .code-text {
      font-size: 28rpx;
      color: var(--primary-color);
      font-weight: 500;
      padding-left: 20rpx;
      white-space: nowrap;

      &.disabled {
        color: var(--sub-text);
      }
    }
  }
}

.submit-btn {
  background: var(--primary-color);
  color: var(--text-color-inverse);
  font-size: 32rpx;
  font-weight: 600;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 12rpx;
  letter-spacing: 4rpx;
  box-shadow: 0 10rpx 20rpx rgba(var(--primary-color-rgb), 0.4);
  margin-bottom: 40rpx;

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.9;
  transform: scale(0.99);
}

.footer-actions {
  display: flex;
  justify-content: center;
  align-items: center;

  .action-text {
    font-size: 26rpx;
    color: var(--sub-text);
    padding: 10rpx;
  }

  .divider {
    margin: 0 20rpx;
    color: var(--sub-text);
    font-size: 24rpx;
  }
}

.bottom-decoration {
  position: absolute;
  bottom: 40rpx;
  width: 100%;
  left: 0;
  text-align: center;

  .copyright {
    font-size: 20rpx;
    color: var(--sub-text);
    letter-spacing: 2rpx;
    text-transform: uppercase;
  }
}
</style>
