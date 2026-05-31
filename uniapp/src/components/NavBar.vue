<template>
  <view class="nav-bar-wrapper">
    <!-- Placeholder to prevent content overlap -->
    <view :style="{ height: totalHeight + 'px' }" class="nav-placeholder"></view>

    <!-- Fixed Navigation Bar -->
    <view :class="['nav-bar', themeClass]"
          :style="navStyle"
          class="nav-bar">
      <view class="left-section" @click="handleBack">
        <image v-if="showBack" :src="icons.arrowBack" class="back-icon" mode="aspectFit"/>
        <slot name="left"></slot>
      </view>

      <view class="center-section">
        <text :style="{ color: titleColor }" class="title">{{ title }}</text>
      </view>

      <view class="right-section">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {icons} from '@/utils/icons'
import {useTheme} from '@/utils/theme'

const {themeClass, currentTheme} = useTheme()

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  },
  backgroundColor: {
    type: String,
    default: '#ffffff'
  },
  titleColor: {
    type: String,
    default: '#000000'
  },
  customBack: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['back'])

const sysInfo = uni.getSystemInfoSync()
const statusBarHeight = sysInfo.statusBarHeight || 20
const navContentHeight = 44
const totalHeight = computed(() => statusBarHeight + navContentHeight)

// Computed style for nav bar
const navStyle = computed(() => {
  const style: any = {
    height: `${totalHeight.value}px`,
    paddingTop: `${statusBarHeight}px`
  }

  // Only apply prop background if it's NOT a theme that manages it
  // Or if we want to allow props to override, we need to be careful.
  // Strategy: If theme is 'light', use prop (default white).
  // If theme is 'dark' or 'frosted', let CSS handle it (set transparent or ignore).

  if (currentTheme.value === 'light') {
    style.backgroundColor = props.backgroundColor
    style.color = props.titleColor
  } else {
    // For dark/frosted, let CSS classes handle bg and color
    style.backgroundColor = 'transparent' // Allow CSS to set it
  }

  return style
})

const handleBack = () => {
  if (props.customBack) {
    emit('back')
  } else {
    // Check if can go back
    const pages = getCurrentPages()
    if (pages.length > 1) {
      uni.navigateBack()
    } else {
      // Fallback to home if no history
      uni.switchTab({
        url: '/pages/business/home/index'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.nav-bar-wrapper {
  width: 100%;
}

.nav-placeholder {
  width: 100%;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 12px;
  padding-right: 12px;
  box-sizing: border-box;
  z-index: 999;

  .left-section {
    width: 60px;
    height: 100%;
    display: flex;
    align-items: center;

    .back-icon {
      width: 24px;
      height: 24px;
    }
  }

  .center-section {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;

    .title {
      font-size: 17px;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .right-section {
    width: 60px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-end;

    .play-icon-container {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      overflow: hidden;
      display: flex;
      justify-content: center;
      align-items: center;
      border: 1px solid #eee;
      background-color: #fff;
    }

    .play-icon {
      width: 100%;
      height: 100%;
      border-radius: 50%;

      &.playing {
        animation: rotate 10s linear infinite;
      }
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.nav-bar {
  &.theme-dark {
    background-color: #1e1e1e !important;

    .center-section .title {
      color: #fff !important;
    }

    .left-section .back-icon {
      filter: invert(1);
    }

    .right-section .play-icon-container {
      background-color: #2c2c2c;
      border-color: #333;
    }
  }
}
</style>
