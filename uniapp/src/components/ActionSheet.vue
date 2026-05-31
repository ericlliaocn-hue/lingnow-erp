<template>
  <view v-if="visible" class="action-sheet-mask" @click="close">
    <view class="action-sheet-content" @click.stop>
      <view class="action-group">
        <view v-if="title" class="action-sheet-title">{{ title }}</view>
        <view
            v-for="(item, index) in list"
            :key="index"
            class="action-sheet-item"
            @click="select(index)"
        >
          {{ item }}
        </view>
      </view>
      <view class="cancel-group">
        <view class="action-sheet-item cancel-btn" @click="close">取消</view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
const props = defineProps<{
  visible: boolean
  list: string[]
  title?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'select', index: number): void
}>()

const close = () => {
  emit('update:visible', false)
}

const select = (index: number) => {
  emit('select', index)
  close()
}
</script>

<style lang="scss" scoped>
.action-sheet-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  z-index: 999;
  animation: fadeIn 0.2s ease-out;
}

.action-sheet-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 20rpx calc(env(safe-area-inset-bottom) + 20rpx);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-group, .cancel-group {
  background-color: var(--card-bg, #ffffff);
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.cancel-group {
  margin-bottom: 0;
}

.action-sheet-title {
  padding: 30rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--sub-text, #999);
  border-bottom: 1rpx solid var(--border-color, #eee);
  background-color: var(--card-bg, #ffffff);
}

.action-sheet-item {
  padding: 34rpx;
  text-align: center;
  font-size: 34rpx;
  color: var(--text-color, #333);
  background-color: var(--card-bg, #ffffff);
  border-bottom: 1rpx solid var(--border-color, #eee);

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background-color: var(--bg-color, #f5f5f5);
  }

  &.cancel-btn {
    font-weight: 600;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}
</style>
