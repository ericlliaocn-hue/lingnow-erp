<script setup lang="ts">
import {onHide, onLaunch, onShow} from "@dcloudio/uni-app";
import {useTheme} from "@/utils/theme";
import {watch} from "vue";
import {COLORS} from "@/utils/icons";

const {currentTheme} = useTheme();

const updateSystemTheme = (theme: string) => {
  const isDark = theme === 'dark';
  const bgColor = isDark ? '#121212' : '#ffffff';
  const frontColor = isDark ? '#ffffff' : '#000000';
  const navBg = isDark ? '#1e1e1e' : '#ffffff';

  if (typeof window !== 'undefined') {
    return;
  }

  // Some APIs are not available on H5 runtime; guard to avoid runtime errors.
  try {
    if (typeof uni.setBackgroundColor === 'function') {
      uni.setBackgroundColor({
        backgroundColor: bgColor,
        backgroundColorTop: bgColor,
        backgroundColorBottom: bgColor
      });
    }
    if (typeof uni.setNavigationBarColor === 'function') {
      uni.setNavigationBarColor({
        frontColor: frontColor,
        backgroundColor: navBg,
        animation: {
          duration: 300,
          timingFunc: 'easeIn'
        }
      });
    }
    if (typeof uni.setTabBarStyle === 'function') {
      uni.setTabBarStyle({
        backgroundColor: navBg,
        borderStyle: isDark ? 'white' : 'black',
        color: isDark ? '#999999' : '#909399',
        selectedColor: COLORS.primary
      });
    }
  } catch (e) {
    console.warn('Theme API not supported on this platform', e);
  }
};

onLaunch(() => {
  updateSystemTheme(currentTheme.value);
});

watch(currentTheme, (newTheme) => {
  updateSystemTheme(newTheme);
});

onShow(() => {
});
onHide(() => {
});
</script>
<style lang="scss">
/* Global Theme Styles */
page {
  --bg-color: #ffffff;
  --card-bg: #ffffff;
  --text-color: #333333;
  --sub-text: #999999;
  --border-color: #f5f5f5;
  --nav-bg: #ffffff;
  --primary-color: #FF6F61;
  --primary-color-rgb: 255, 111, 97;
  --secondary-color: #FFCC80;
  --card-bg-highlight: #FFF0EC;
  --mask-gradient-start: rgba(248, 248, 248, 0.1);
  --mask-gradient-end: rgba(248, 248, 248, 0.8);
  --search-bg: #f5f5f5;
  --skeleton-bg: #eeeeee;
  --text-color-inverse: #ffffff;
  --notice-bg: #FFF3F0;
  --badge-bg: #FF9800;
  --badge-text: #ffffff;
  --warning-color: #FF9800;
  --error-color: #FF5252;
  --success-color: #4CAF50;
  --success-color-rgb: 76, 175, 80;
  --success-light: #A0EACD;
  --info-color: #81D4FA;
  --accent-purple: #7C4DFF;
  --icon-bg-teal: #E8F8F5;
  --icon-bg-green: #E9F7EF;
  --icon-bg-purple: #F4ECF7;
  --icon-bg-amber: #FFF8E1;
}

/* Dark Theme */
.theme-dark {
  --bg-color: #121212;
  --card-bg: #1e1e1e;
  --text-color: #e0e0e0;
  --sub-text: #a0a0a0;
  --border-color: #333333;
  --nav-bg: #1e1e1e;
  --warning-color: #FFD54F;
  --warning-color-rgb: 255, 213, 79;
  --warning-bg: #3E2723;
  --error-color: #FF5252;
  --success-color: #69F0AE;
  --success-color-rgb: 105, 240, 174;
  --success-light: #004D40;
  --info-color: #0277BD;
  --search-bg: #2C2C2C;
  --skeleton-bg: #333333;
  --notice-bg: #3E2723;
  --input-bg: #2C2C2C;
  --bg-color-page: #121212;
  --mask-gradient-start: rgba(18, 18, 18, 0.1);
  --mask-gradient-end: rgba(18, 18, 18, 0.8);
  --accent-purple: #7C4DFF;
  --icon-bg-teal: rgba(232, 248, 245, 0.1);
  --icon-bg-green: rgba(233, 247, 239, 0.1);
  --icon-bg-purple: rgba(244, 236, 247, 0.1);
  --icon-bg-amber: rgba(255, 248, 225, 0.1);
}

/* Apply Theme Variables to Common Elements */
.theme-dark {
  background-color: var(--bg-color) !important;
  color: var(--text-color);

  .container, .mine-container, .page-container {
    background-color: var(--bg-color) !important;
    color: var(--text-color);
  }

  /* Handle both .nav-header (custom in page) and .nav-bar (component) */
  .nav-header, .navbar, .nav-bar {
    background-color: var(--nav-bg) !important;
    color: var(--text-color);

    .title, .nav-title-text {
      color: var(--text-color) !important;
    }
  }

  .card-item, .user-card, .menu-card, .dashboard-card, .section, .list-menu, .list-item, .grid-item {
    background-color: var(--card-bg) !important;
    color: var(--text-color);
    border-color: var(--border-color) !important;
  }

  .list-item {
    border-bottom-color: var(--border-color) !important;
  }

  .title, .item-title, .nickname, .label, .tab-text, .filter-label {
    color: var(--text-color) !important;
  }

  .desc, .sub-label, .item-desc, .notice-text, .more {
    color: var(--sub-text) !important;
  }

  /* Input/Search */
  .search-bar {
    background-color: var(--card-bg) !important;

    .placeholder {
      color: var(--sub-text) !important;
    }

    input {
      color: var(--text-color);
    }
  }

  /* TabBar */
  .tab-bar {
    background-color: var(--nav-bg) !important;
    border-top-color: var(--border-color) !important;

    .tab-item {
      .text {
        color: var(--sub-text);
      }

      &.active .text {
        color: var(--primary-color);
      }
    }
  }

  /* Overrides for specific page elements with hardcoded light backgrounds */
  .icon-box, .icon-circle {
    background: rgba(255, 255, 255, 0.1) !important;
  }

  .logout-btn {
    background-color: var(--card-bg) !important;
    border-color: var(--border-color) !important;
    color: #ff4d4f;
  }
}
</style>
