<template>
  <view :class="themeClass" class="container">
    <view class="form-item avatar-item">
      <text class="label">头像</text>
      <view class="right">
        <upload-image v-model="form.avatar"/>
      </view>
    </view>
    <view class="form-item">
      <text class="label">昵称</text>
      <input class="input" type="text" v-model="form.nickname" style="text-align: right;" />
    </view>
    <view class="form-item">
      <text class="label">手机号</text>
      <text class="value">{{ form.phone }}</text>
    </view>

    <button class="save-btn" @click="handleSave">保存修改</button>
    <GlobalLoginPopup/>
  </view>
</template>

<script setup lang="ts">
import GlobalLoginPopup from '@/components/GlobalLoginPopup.vue'
import {onMounted, reactive} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {getProfile, updateProfile} from '@/api/user'
import UploadImage from '@/components/upload-image.vue'
import {useTheme} from '@/utils/theme'

const {themeClass, updateNavigationBar} = useTheme()

const form = reactive({
  userId: undefined as number | undefined,
  avatar: '',
  nickname: '',
  phone: ''
})

onShow(() => {
  updateNavigationBar()
})

onMounted(async () => {
  const cachedUser = uni.getStorageSync('userInfo')
  if (cachedUser) {
    Object.assign(form, cachedUser)
  }

  try {
    const res = await getProfile()
    if (res.code === 200) {
      Object.assign(form, res.data)
      uni.setStorageSync('userInfo', res.data)
    }
  } catch (e) {
    // ignore
  }
})

const handleSave = async () => {
  try {
    const res = await updateProfile({
      avatar: form.avatar,
      nickname: form.nickname
    })
    if (res.code === 200) {
      uni.showToast({ title: '保存成功' })
      // 更新本地缓存
      const cachedUser = uni.getStorageSync('userInfo')
      uni.setStorageSync('userInfo', {...cachedUser, ...form})

      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    }
  } catch (e) {
    console.error(e)
  }
}
</script>

<style lang="scss">
.container {
  background-color: var(--bg-color);
  min-height: 100vh;
}
.form-item {
  background-color: var(--card-bg);
  padding: 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid var(--border-color);

  .label {
    font-size: 30rpx;
    color: var(--text-color);
  }

  .right {
    display: flex;
    align-items: center;

    .avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      margin-right: 20rpx;
    }

    .arrow {
      color: var(--sub-text);
      font-size: 30rpx;
    }
  }

  .input {
    flex: 1;
    font-size: 30rpx;
    color: var(--text-color);
    margin-left: 20rpx;
  }

  .value {
    font-size: 30rpx;
    color: var(--sub-text);
  }
}

.avatar-item {
  padding: 20rpx 30rpx;
}

.save-btn {
  margin: 60rpx 30rpx;
  background-color: #ff4d4f;
  color: #fff;
  border-radius: 44rpx;
  font-size: 32rpx;

  &:active {
    opacity: 0.9;
  }
}
</style>
