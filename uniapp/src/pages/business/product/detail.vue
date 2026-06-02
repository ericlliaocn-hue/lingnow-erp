<template>
  <view class="page">
    <view class="banner"></view>
    <view v-if="loadError" class="content">
      <text class="error">{{ loadError }}</text>
    </view>
    <view v-else class="content">
      <text class="title">{{ product?.name || '商品详情' }}</text>
      <text class="line">编号：{{ product?.code || '-' }}</text>
      <text class="line">规格：{{ product?.spec || '-' }}</text>
      <text class="line">单位：{{ product?.unitName || '-' }}</text>
      <text class="line">库存：{{ product?.stockQty ?? '-' }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import {getMobileProductDetail} from '@/api/business'

const product = ref<any>(null)
const productId = ref('')
const loadError = ref('')

onLoad((options: any) => {
  productId.value = String(options?.id || '')
})

const loadData = async () => {
  if (!productId.value) return
  loadError.value = ''
  try {
    const res = await getMobileProductDetail(productId.value)
    product.value = res?.data || null
  } catch (e) {
    loadError.value = '商品详情加载失败，请登录后重试'
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #fff; }
.banner { height: 280rpx; background: #e8f2ec; }
.content { padding: 28rpx 24rpx; }
.title { display: block; font-size: 34rpx; font-weight: 700; color: #1f2d2a; margin-bottom: 18rpx; }
.line { display: block; color: #66736e; font-size: 26rpx; margin-top: 12rpx; }
.error { display: block; color: #c2410c; font-size: 26rpx; }
</style>
