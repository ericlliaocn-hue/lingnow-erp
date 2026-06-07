<template>
  <view :class="themeClass" class="login-container">
    <view class="header-section">
      <view class="logo-box">
        <text>LN</text>
      </view>
      <view class="brand-copy">
        <text class="app-name">LingNow ERP</text>
        <text class="app-slogan">移动业务工作台</text>
      </view>
    </view>

    <view class="form-card">
      <view class="form-title">
        <text class="title">账号登录</text>
        <text class="desc">请输入企业业务账号继续操作</text>
      </view>

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
          <text class="field-label">账号</text>
          <input
              v-model="form.account"
              class="input"
              placeholder="请输入手机号/账号"
              placeholder-class="input-placeholder"
          />
        </view>

        <view v-if="loginType === 'password'" class="input-item">
          <text class="field-label">密码</text>
          <input
              v-model="form.password"
              class="input"
              type="password"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
          />
        </view>

        <view v-if="loginType === 'code'" class="input-item row-between">
          <view class="code-input">
            <text class="field-label">验证码</text>
            <input
                v-model="form.code"
                class="input"
                type="number"
                placeholder="请输入验证码"
                placeholder-class="input-placeholder"
            />
          </view>
          <text :class="{ disabled: countdown > 0 }" class="code-text" @click="handleGetCode">
            {{ codeText }}
          </text>
        </view>
      </view>

      <button class="submit-btn" hover-class="btn-hover" @click="handleLogin">
        登录
      </button>

      <view class="footer-actions">
        <text class="action-text" @click="goToRegister">注册新账号</text>
        <text class="divider">|</text>
        <text class="action-text" @click="goToForget">忘记密码</text>
      </view>
    </view>

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
  background: #f4f7f6;
  padding: 72rpx 44rpx 56rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  box-sizing: border-box;
}

.header-section {
  margin-bottom: 52rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20rpx;

  .logo-box {
    width: 72rpx;
    height: 72rpx;
    border-radius: 18rpx;
    background: #2f7d57;
    display: flex;
    align-items: center;
    justify-content: center;

    text {
      color: #fff;
      font-size: 28rpx;
      font-weight: 700;
      letter-spacing: 1rpx;
    }
  }

  .brand-copy {
    display: flex;
    flex-direction: column;
  }

  .app-name {
    font-size: 34rpx;
    font-weight: 700;
    color: #1f2d2a;
    letter-spacing: 0;
  }

  .app-slogan {
    margin-top: 6rpx;
    font-size: 24rpx;
    color: #6b7a75;
    letter-spacing: 0;
  }
}

.form-card {
  width: 100%;
  background: #fff;
  border: 1rpx solid #e2e8e5;
  border-radius: 18rpx;
  padding: 40rpx 32rpx 34rpx;
  box-sizing: border-box;
  box-shadow: 0 20rpx 48rpx rgba(24, 39, 33, 0.06);
}

.form-title {
  margin-bottom: 34rpx;

  .title {
    display: block;
    color: #1f2d2a;
    font-size: 36rpx;
    font-weight: 700;
  }

  .desc {
    display: block;
    margin-top: 12rpx;
    color: #7a8580;
    font-size: 24rpx;
  }
}

.login-tabs {
  display: flex;
  margin-bottom: 32rpx;
  border-bottom: 1rpx solid #e6ece8;

  .tab-item {
    margin-right: 44rpx;
    font-size: 28rpx;
    color: #8a9691;
    font-weight: 500;
    position: relative;
    padding-bottom: 18rpx;
    transition: all 0.3s;

    &.active {
      color: #1f2d2a;
      font-weight: 700;
      font-size: 28rpx;
    }

    .active-line {
      position: absolute;
      bottom: -1rpx;
      left: 0;
      width: 100%;
      height: 4rpx;
      background: #2f7d57;
      border-radius: 4rpx 4rpx 0 0;
    }
  }
}

.input-group {
  margin-bottom: 36rpx;

  .input-item {
    background: #f8faf9;
    border-radius: 10rpx;
    padding: 18rpx 22rpx;
    margin-bottom: 22rpx;
    border: 1rpx solid #dce5e0;
    transition: all 0.3s;

    &.row-between {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &:focus-within {
      background: #fff;
      border-color: #2f7d57;
      box-shadow: 0 0 0 4rpx rgba(47, 125, 87, 0.08);
    }

    .field-label {
      display: block;
      margin-bottom: 10rpx;
      color: #66736e;
      font-size: 22rpx;
      font-weight: 500;
    }

    .input {
      font-size: 28rpx;
      color: #1f2d2a;
      width: 100%;
      height: 42rpx;
      line-height: 42rpx;
    }

    .input-placeholder {
      color: #a0aaa5;
    }

    .code-input {
      flex: 1;
      min-width: 0;
    }

    .code-text {
      font-size: 26rpx;
      color: #2f7d57;
      font-weight: 600;
      padding-left: 24rpx;
      white-space: nowrap;

      &.disabled {
        color: #a0aaa5;
      }
    }
  }
}

.submit-btn {
  background: #2f7d57;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 10rpx;
  letter-spacing: 0;
  box-shadow: none;
  margin-bottom: 26rpx;

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
    font-size: 24rpx;
    color: #6b7a75;
    padding: 10rpx;
  }

  .divider {
    margin: 0 18rpx;
    color: #c3ccc7;
    font-size: 24rpx;
  }
}

.bottom-decoration {
  margin-top: auto;
  width: 100%;
  text-align: center;
  padding-top: 48rpx;
  box-sizing: border-box;

  .copyright {
    font-size: 20rpx;
    color: #9aa4a0;
    letter-spacing: 2rpx;
    text-transform: uppercase;
  }
}
</style>
