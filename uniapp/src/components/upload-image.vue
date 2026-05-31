<template>
  <view class="upload-container" @click="handleUpload">
    <image v-if="modelValue" :src="imageUrl" class="preview-image" mode="aspectFill"/>
    <view v-else class="placeholder">
      <text class="plus">+</text>
      <text class="text">上传头像</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {uploadFile} from '@/api/common'

const props = defineProps<{
  modelValue?: string
  businessId?: number
  businessType?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

// 处理图片地址
const imageUrl = computed(() => {
  if (!props.modelValue) return ''
  if (props.modelValue.startsWith('http')) return props.modelValue
  const baseUrl = import.meta.env.VITE_IMG_BASE_URL || ''
  return `${baseUrl}${props.modelValue}`
})

const handleUpload = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      try {
        uni.showLoading({title: '上传中...'})
        const url = await uploadFile(tempFilePath, props.businessId)
        emit('update:modelValue', url)
        uni.hideLoading()
      } catch (error: any) {
        uni.hideLoading()
        uni.showToast({
          title: error.message || '上传失败',
          icon: 'none'
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.upload-container {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  overflow: hidden;
  background-color: #f5f5f5;
  position: relative;
  border: 2rpx solid #eee;

  .preview-image {
    width: 100%;
    height: 100%;
  }

  .placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #999;

    .plus {
      font-size: 60rpx;
      line-height: 1;
      margin-bottom: 10rpx;
    }

    .text {
      font-size: 24rpx;
    }
  }
}
</style>
