<template>
  <view :class="themeClass" class="tab-bar">
    <view
        v-for="(item, index) in list"
        :key="index"
        :class="{ 'active': props.current === index }"
        class="tab-item"
        @click="switchTab(item, index)"
    >
      <view :class="{ 'bounce-anim': animatingIndex === index }" class="icon-box">
        <image :src="props.current === index ? item.selectedIcon : item.icon" class="icon-img" mode="aspectFit"></image>
      </view>
      <text class="text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {icons} from '@/utils/icons'
import {useTheme} from '@/utils/theme'

const {themeClass} = useTheme()

const props = defineProps({
  current: {
    type: Number,
    default: 0
  }
})

// 动画状态
const animatingIndex = ref<number | null>(null)

const list = [
  {
    pagePath: "pages/business/home/index",
    text: "首页",
    icon: icons.home,
    selectedIcon: icons.homeActive
  },
  {
    pagePath: "pages/business/category/index",
    text: "分类",
    icon: icons.category,
    selectedIcon: icons.categoryActive
  },
  {
    pagePath: "pages/business/cart/index",
    text: "购物车",
    icon: icons.cart,
    selectedIcon: icons.cartActive
  },
  {
    pagePath: "pages/business/order/index",
    text: "订单",
    icon: icons.order,
    selectedIcon: icons.orderActive
  },
  {
    pagePath: "pages/business/mine/index",
    text: "我的",
    icon: icons.mine,
    selectedIcon: icons.mineActive
  }
]

const switchTab = async (item: any, index: number) => {
  // Prevent navigation if already on the tab
  if (props.current === index) return

  // 触发动画
  animatingIndex.value = index

  // 延迟切换，让动画播放可见
  setTimeout(() => {
    uni.switchTab({
      url: '/' + item.pagePath
    })
    setTimeout(() => {
      animatingIndex.value = null
    }, 100)
  }, 200)
}
</script>

<style lang="scss">
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: var(--bg-color);
  display: flex;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 20rpx rgba(var(--primary-color-rgb), 0.15);
  z-index: 999;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;

  .icon-box {
    font-size: 44rpx;
    margin-bottom: 4rpx;
    transition: all 0.3s;
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    position: relative;
  }

  .icon-img {
    width: 50rpx;
    height: 50rpx;
    display: block;
  }

  .text {
    font-size: 22rpx;
    color: var(--sub-text);
    transition: color 0.3s;
    font-weight: 500;
  }

  &.active {
    .icon-box {
      /* transform: scale(1.2); Removed persistent scale */

    }

    .text {
      color: var(--primary-color);
      font-weight: bold;
    }
  }
}

@keyframes bounce {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.bounce-anim {
  animation: bounce 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

/* Theme Adaptations */
.tab-bar.theme-dark {
  background: var(--bg-color) !important;
  border-top: 1rpx solid var(--border-color);
  box-shadow: none;

  .tab-item {
    .text {
      color: var(--sub-text);
    }

    &.active .text {
      color: var(--primary-color);
    }
  }
}
</style>
