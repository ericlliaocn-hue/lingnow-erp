<template>
  <div class="auth-layout">
    <!-- Header -->
    <header class="auth-header">
      <div class="header-logo">
        <Logo width="40px" height="40px" />
      </div>
      <div class="header-actions">
        <a href="#" class="help-link">LingNow ERP</a>
        <div class="settings-btn" @click="settingsStore.setDrawerOpen(true)">
          <el-icon class="settings-icon is-loading"><Setting /></el-icon>
        </div>
      </div>
    </header>

    <SettingsDrawer />

    <!-- Left Section -->
    <div class="auth-side">
      <div class="auth-side-content">
        <h3 class="auth-title">欢迎使用 LingNow ERP</h3>
        <p class="auth-subtitle">通用后台基座已接入权限、菜单、日志、文件与 ERP 基础资料能力，后续业务模块可按行业独立扩展。</p>
        <img src="/assets/illustrations/illustration-dashboard.webp" alt="Dashboard" class="auth-illustration" />
      </div>
    </div>

    <!-- Right Section -->
    <div class="auth-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import Logo from '@/components/Logo/index.vue'
import SettingsDrawer from '@/components/Settings/SettingsDrawer.vue'
import { useSettingsStore } from '@/store/modules/settings'

const settingsStore = useSettingsStore()
</script>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.auth-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px;
  z-index: 9;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.help-link {
  font-size: 14px;
  font-weight: 600;
  color: #212b36;
  text-decoration: none;
  transition: opacity 0.2s;
}

.help-link:hover {
  opacity: 0.72;
}

.settings-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.settings-btn:hover {
  background-color: rgba(145, 158, 171, 0.08);
}

.settings-icon {
  font-size: 20px;
  color: #637381;
  animation: rotation 2s linear infinite;
}

.auth-side {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 50%;
  max-width: 600px;
  background: linear-gradient(135deg, rgba(145, 158, 171, 0.12), rgba(145, 158, 171, 0.04)),
              #f9fafb;
  background-size: cover;
  background-position: center;
  border-right: 1px dashed rgba(145, 158, 171, 0.24);
  padding: 120px 64px;
  position: relative;
}

.auth-side::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 30% 50%, rgba(145, 158, 171, 0.04), transparent 60%),
              radial-gradient(circle at 70% 80%, rgba(145, 158, 171, 0.04), transparent 60%);
  pointer-events: none;
}



@media (min-width: 900px) {
  .auth-side {
    display: flex;
  }
}

.auth-side-content {
  text-align: center;
  position: relative;
  z-index: 1;
}

.auth-title {
  margin: 0 0 16px;
  font-weight: 700;
  font-size: 40px;
  line-height: 1.5;
  color: #212b36;
}

.auth-subtitle {
  margin: 0 0 64px;
  color: #637381;
  font-size: 16px;
  line-height: 1.57;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.auth-illustration {
  width: 100%;
  max-width: 480px;
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0px 12px 24px rgba(145, 158, 171, 0.24));
}

.auth-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 16px;
  background-color: #fff;
}

@keyframes rotation {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Dark Mode */
:global(.dark) .auth-side {
  background: linear-gradient(135deg, rgba(145, 158, 171, 0.08), rgba(145, 158, 171, 0.02)),
              #1c252e;
  border-right-color: rgba(145, 158, 171, 0.08);
}

:global(.dark) .auth-title {
  color: #fff;
}

:global(.dark) .auth-subtitle {
  color: #919eab;
}

:global(.dark) .auth-content {
  background-color: #141a21;
}

:global(.dark) .help-link {
  color: #fff;
}
</style>
