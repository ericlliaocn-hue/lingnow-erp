<template>
  <view :class="themeClass" class="container">
    <view class="form-item">
      <text class="label">手机号</text>
      <input :value="desensitizedPhone" class="input" disabled type="text"/>
    </view>
    <view class="form-item">
      <text class="label">旧密码</text>
      <input v-model="form.oldPassword" class="input" placeholder="请输入旧密码" type="password"/>
    </view>
    <view class="form-item">
      <text class="label">新密码</text>
      <input v-model="form.newPassword" class="input" placeholder="请输入新密码" type="password"/>
    </view>
    <view class="form-item">
      <text class="label">确认密码</text>
      <input v-model="confirmPassword" class="input" placeholder="请再次输入新密码" type="password"/>
    </view>

    <button class="submit-btn" @click="handleSubmit">确认修改</button>
    <GlobalLoginPopup/>
  </view>
</template>

<script lang="ts" setup>
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {computed, reactive, ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {changePassword} from '@/api/user'
import {useTheme} from '@/utils/theme'
import {requireLogin} from '@/utils/auth'

const {themeClass, updateNavigationBar} = useTheme()

onShow(() => {
  updateNavigationBar()
  requireLogin('/pages/settings/password')
})

const userInfo = uni.getStorageSync('userInfo') || {}
const phone = userInfo.phone || ''

const form = reactive({
  oldPassword: '',
  newPassword: ''
})
const confirmPassword = ref('')

const desensitizedPhone = computed(() => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
})

const handleSubmit = async () => {
  if (!requireLogin('/pages/settings/password')) return
  if (!form.oldPassword) return uni.showToast({title: '请输入旧密码', icon: 'none'})
  if (!form.newPassword) return uni.showToast({title: '请输入新密码', icon: 'none'})
  if (form.newPassword !== confirmPassword.value) return uni.showToast({title: '两次密码输入不一致', icon: 'none'})

  try {
    await changePassword(form)
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')

    uni.showToast({
      title: '修改成功，请重新登录',
      icon: 'none',
      duration: 1500,
      mask: true
    })

    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/login/index',
        fail: (err) => {
          console.error('reLaunch failed', err)
          // 备用跳转方案
          uni.redirectTo({url: '/pages/login/index'})
        }
      })
    }, 1500)
  } catch (e: any) {
    uni.showToast({title: e.message || '修改失败', icon: 'none'})
  }
}
</script>

<style lang="scss">
.container {
  padding: 0 30rpx 30rpx;
  background-color: var(--bg-color);
  min-height: 100vh;
}

.form-item {
  background-color: var(--card-bg);
  padding: 30rpx;
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid var(--border-color);

  .label {
    width: 160rpx;
    font-size: 30rpx;
    color: var(--text-color);
  }

  .input {
    flex: 1;
    font-size: 30rpx;
    color: var(--text-color);
  }
}

.submit-btn {
  margin-top: 60rpx;
  background-color: var(--primary-color);
  color: #fff;
  border-radius: 50rpx;
}
</style>
