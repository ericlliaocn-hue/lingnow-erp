<template>
  <view :class="themeClass" class="container">
    <NavBar title="设置"/>
    <view class="list-menu">
      <view class="list-item" @click="handleThemeChange">
        <text>主题风格</text>
        <view class="right-content">
          <text class="value">{{ themeName }}</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="list-item" @click="goToPage('/pages/settings/profile-edit')">
        <text>个人信息</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="goToPage('/pages/settings/account-binding')">
        <text>账号绑定</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="goToPage('/pages/settings/security')">
        <text>安全中心</text>
        <text class="arrow">></text>
      </view>
    </view>

    <button v-if="loggedIn" class="logout-btn" @click="handleLogout">退出登录</button>

    <ActionSheet
        v-model:visible="showThemeSheet"
        :list="themeList"
        title="选择主题风格"
        @select="onThemeSelect"
    />
    <GlobalLoginPopup/>
  </view>
</template>

<script setup lang="ts">
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {computed, ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {request} from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import ActionSheet from '@/components/ActionSheet.vue'
import {type Theme, useTheme} from '@/utils/theme'
import {isLoggedIn, requireLogin} from '@/utils/auth'

const {currentTheme, setTheme, themeClass} = useTheme()

const themeName = computed(() => {
  switch (currentTheme.value) {
    case 'dark':
      return '暗黑模式'
    default:
      return '简约白'
  }
})

const showThemeSheet = ref(false)
const loggedIn = ref(isLoggedIn())
const themeList = ['简约白', '暗黑模式']
const themeValues: Theme[] = ['light', 'dark']

onShow(() => {
  loggedIn.value = isLoggedIn()
  requireLogin('/pages/settings/index')
})

const handleThemeChange = () => {
  showThemeSheet.value = true
}

const onThemeSelect = (index: number) => {
  setTheme(themeValues[index])
}

const goToPage = (url: string) => {
  if (!requireLogin(url)) return
  uni.navigateTo({ url })
}

const handleLogout = async () => {
  try {
    await request({ url: '/app/auth/logout', method: 'POST' })
  } catch (e) {
    // ignore
  } finally {
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/login/index' })
  }
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
    align-items: center;
    color: var(--text-color);

    .right-content {
      display: flex;
      align-items: center;

      .value {
        color: var(--sub-text);
        font-size: 28rpx;
        margin-right: 10rpx;
      }
    }

    .arrow {
      color: var(--sub-text);
    }
  }
}
.logout-btn {
  margin: 60rpx 40rpx;
  background-color: var(--card-bg);
  color: #ff4d4f;
  border: 1rpx solid var(--border-color);
}
</style>
