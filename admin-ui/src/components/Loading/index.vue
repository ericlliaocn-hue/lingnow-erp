<template>
  <div class="loading-wrapper" v-if="visible">
    <div class="loading-backdrop"></div>
    <div class="loading-container">
      <div class="loading-logo-box">
        <div class="logo-border" :style="{ borderColor: `rgba(${hexToRgb(primaryColor)}, 0.24)` }"></div>
        <div class="logo-content" :style="{ background: `linear-gradient(135deg, rgba(${hexToRgb(primaryColor)}, 0.12), rgba(${hexToRgb(primaryColor)}, 0.06))` }">
          <Logo width="56px" height="56px" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useSettingsStore } from '@/store/modules/settings'
import Logo from '@/components/Logo/index.vue'

interface Props {
  closeOnClick?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  closeOnClick: false
})

const visible = ref(true)
const settingsStore = useSettingsStore()

// 获取当前主题色
const primaryColor = computed(() => {
  return settingsStore.primaryColorPresets[settingsStore.settings.primaryColor] || '#00A76F'
})

// 将 hex 颜色转换为 rgb
const hexToRgb = (hex: string) => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result
    ? `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
    : '0, 167, 111'
}

const close = () => {
  visible.value = false
}

defineExpose({
  close
})
</script>

<style scoped>
.loading-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #161c24;
}

.loading-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-logo-box {
  position: relative;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 脉冲边框动画 */
.logo-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 2px solid rgba(0, 167, 111, 0.24); /* 默认颜色，会被内联样式覆盖 */
  border-radius: 24px;
  animation: pulse-border 2s ease-in-out infinite;
}

@keyframes pulse-border {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

/* Logo 内容 */
.logo-content {
  position: relative;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(0, 167, 111, 0.12), rgba(91, 228, 155, 0.12)); /* 默认颜色，会被内联样式覆盖 */
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: logo-pulse 2s ease-in-out infinite;
}

@keyframes logo-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(0.96);
  }
}

/* 暗色模式 */
:global(.dark) .loading-backdrop {
  background-color: #141a21;
}
</style>
