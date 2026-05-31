<template>
  <view :class="themeClass" class="container">
    <view class="form-item">
      <text class="label">原手机号</text>
      <input :value="desensitizedPhone" class="input" disabled type="text"/>
    </view>
    <view class="form-item">
      <text class="label">验证码</text>
      <input v-model="form.oldCode" class="input" placeholder="请输入验证码" type="number"/>
      <text class="code-btn" @click="handleSendOldCode">{{ oldTimer > 0 ? `${oldTimer}s` : '获取验证码' }}</text>
    </view>

    <view v-if="step === 2">
      <view class="form-item">
        <text class="label">新手机号</text>
        <input v-model="form.newPhone" class="input" placeholder="请输入新手机号" type="number"/>
      </view>
      <view class="form-item">
        <text class="label">验证码</text>
        <input v-model="form.newCode" class="input" placeholder="请输入验证码" type="number"/>
        <text class="code-btn" @click="handleSendNewCode">{{ newTimer > 0 ? `${newTimer}s` : '获取验证码' }}</text>
      </view>
    </view>

    <button class="submit-btn" @click="handleSubmit">{{ step === 1 ? '下一步' : '确认修改' }}</button>
    <GlobalLoginPopup/>
  </view>
</template>

<script lang="ts" setup>
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {computed, reactive, ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {changePhone, sendCode, validateCode} from '@/api/user'
import {useTheme} from '@/utils/theme'

const {themeClass, updateNavigationBar} = useTheme()

onShow(() => {
  updateNavigationBar()
})

const step = ref(1)
const userInfo = uni.getStorageSync('userInfo') || {}
const phone = userInfo.phone || ''

const form = reactive({
  oldCode: '',
  newPhone: '',
  newCode: ''
})

const oldTimer = ref(0)
const newTimer = ref(0)

const desensitizedPhone = computed(() => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
})

const handleSendOldCode = async () => {
  if (oldTimer.value > 0) return
  try {
    await sendCode(phone)
    uni.showToast({title: '验证码已发送', icon: 'none'})
    oldTimer.value = 60
    const timer = setInterval(() => {
      oldTimer.value--
      if (oldTimer.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    // ignore
  }
}

const handleSendNewCode = async () => {
  if (newTimer.value > 0) return
  if (!form.newPhone) return uni.showToast({title: '请输入新手机号', icon: 'none'})
  if (!/^1[3-9]\d{9}$/.test(form.newPhone)) return uni.showToast({title: '手机号格式错误', icon: 'none'})

  try {
    await sendCode(form.newPhone)
    uni.showToast({title: '验证码已发送', icon: 'none'})
    newTimer.value = 60
    const timer = setInterval(() => {
      newTimer.value--
      if (newTimer.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    // ignore
  }
}

const handleSubmit = async () => {
  if (step.value === 1) {
    if (!form.oldCode) return uni.showToast({title: '请输入验证码', icon: 'none'})
    try {
      await validateCode({phone, code: form.oldCode})
      step.value = 2
    } catch (e) {
      // ignore
    }
  } else {
    if (!form.newPhone) return uni.showToast({title: '请输入新手机号', icon: 'none'})
    if (!form.newCode) return uni.showToast({title: '请输入验证码', icon: 'none'})

    try {
      await changePhone(form)
      uni.showToast({title: '修改成功'})
      setTimeout(() => {
        // 清除登录状态重新登录
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.reLaunch({url: '/pages/login/index'})
      }, 1500)
    } catch (e) {
      // ignore
    }
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

  .code-btn {
    color: var(--primary-color);
    font-size: 28rpx;
    padding-left: 20rpx;
    border-left: 1rpx solid var(--border-color);
  }
}

.submit-btn {
  margin-top: 60rpx;
  background-color: var(--primary-color);
  color: #fff;
  border-radius: 50rpx;
}
</style>
