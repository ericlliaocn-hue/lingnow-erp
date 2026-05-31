<template>
  <div class="login-wrapper">
    <Loading v-if="loading" />

    <div class="login-header">
      <h4 class="title">登录 LingNow ERP</h4>
      <div class="subtitle">请输入账号密码进入管理后台</div>
    </div>

    <el-form :model="form" class="login-form" @keyup.enter="handleLogin">
      <el-form-item>
        <div class="form-label">账号</div>
        <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            autocomplete="username"
        />
      </el-form-item>

      <el-form-item>
        <div class="form-label">
           <span>密码</span>
        </div>
        <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            autocomplete="current-password"
        />
      </el-form-item>

      <el-button
        type="primary"
        size="large"
        class="login-btn"
        @click="handleLogin"
      >
        登录
      </el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import Loading from '@/components/Loading/index.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
     // 添加最小延迟确保能看到 loading 动画
     await Promise.all([
       userStore.login(form),
       new Promise(resolve => setTimeout(resolve, 800)) // 最少显示 800ms
     ])

     ElMessage.success('登录成功')

     // 获取 redirect 参数，如果有则跳转到原页面，否则跳转到首页
     const redirect = route.query.redirect as string
     router.push(redirect || '/')
  } catch (err) {
    // request.ts 已处理错误提示
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
.login-wrapper {
  width: 100%;
  max-width: 480px;
  padding: 0 16px;
}

.login-header {
  margin-bottom: 40px;
  text-align: left;
}

.title {
  margin: 0 0 16px;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.5;
  color: #212b36;
}

.subtitle {
  font-size: 14px;
  color: #637381;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Form */
.login-form {
  margin-bottom: 24px;
}

.form-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #212b36;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-btn {
  width: 100%;
  font-weight: 700;
  font-size: 15px;
  height: 48px;
  border-radius: 8px;
  margin-top: 8px;
  background-color: #212b36;
  border-color: #212b36;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.login-btn:hover {
  background-color: #454f5b;
  border-color: #454f5b;
}

.login-btn:active {
  background-color: #161c24;
  border-color: #161c24;
}

/* 自定义 Loading 动画 */
.login-btn.is-loading {
  pointer-events: none;
}

.login-btn :deep(.el-icon.is-loading) {
  margin-right: 8px;
}

.login-btn :deep(.el-loading-spinner) {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -10px;
}

.login-btn :deep(.el-loading-spinner .circular) {
  width: 20px;
  height: 20px;
  animation: loading-rotate 2s linear infinite;
}

.login-btn :deep(.el-loading-spinner .path) {
  stroke: #fff;
  stroke-width: 3;
  stroke-linecap: round;
  animation: loading-dash 1.5s ease-in-out infinite;
}

/* 按钮 loading 图标样式 */
.login-btn :deep(.el-icon-loading) {
  font-size: 16px;
  animation: loading-rotate 2s linear infinite;
}

.login-btn :deep(.el-icon-loading svg) {
  width: 16px;
  height: 16px;
}

.login-btn :deep(.el-icon-loading path) {
  stroke: currentColor;
  stroke-width: 3;
  stroke-linecap: round;
}

@keyframes loading-rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes loading-dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}

/* Input Styling */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 0 0 1px rgba(145, 158, 171, 0.2);
  padding: 0 14px;
  transition: all 0.2s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(145, 158, 171, 0.32);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(33, 43, 54, 0.24);
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #212b36;
}

:deep(.el-input__inner::placeholder) {
  color: #919eab;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

/* Dark Mode Support */
:global(.dark) .title {
  color: #fff;
}

:global(.dark) .subtitle {
  color: #919eab;
}

:global(.dark) .form-label {
  color: #fff;
}

:global(.dark) .login-btn {
  background-color: #fff;
  border-color: #fff;
  color: #212b36;
}

:global(.dark) .login-btn:hover {
  background-color: #c4cdd5;
  border-color: #c4cdd5;
}

:global(.dark) .login-btn:active {
  background-color: #919eab;
  border-color: #919eab;
}

/* 暗色模式下的 loading 颜色 */
:global(.dark) .login-btn :deep(.el-loading-spinner .path) {
  stroke: #212b36;
}

:global(.dark) :deep(.el-input__wrapper) {
  background-color: rgba(145, 158, 171, 0.08);
  box-shadow: none;
  border: 1px solid rgba(145, 158, 171, 0.2);
}

:global(.dark) :deep(.el-input__wrapper:hover) {
  border-color: rgba(145, 158, 171, 0.32);
}

:global(.dark) :deep(.el-input__wrapper.is-focus) {
  border-color: #fff;
  box-shadow: 0 0 0 1px #fff;
}

:global(.dark) :deep(.el-input__inner) {
  color: #fff;
}

:global(.dark) :deep(.el-input__inner::placeholder) {
  color: #637381;
}

/* 全局对话框暗色模式适配 */
:global(.dark) :deep(.el-dialog) {
  background-color: #1c252e !important;
  border: 1px solid rgba(145, 158, 171, 0.12);
}

:global(.dark) :deep(.el-dialog__header) {
  background-color: #1c252e;
  border-bottom: 1px solid rgba(145, 158, 171, 0.12);
}

:global(.dark) :deep(.el-dialog__title) {
  color: #fff;
}

:global(.dark) :deep(.el-dialog__body) {
  background-color: #1c252e;
  color: #fff;
}

:global(.dark) :deep(.el-dialog__footer) {
  background-color: #1c252e;
  border-top: 1px solid rgba(145, 158, 171, 0.12);
}

:global(.dark) :deep(.el-dialog__close) {
  color: #919eab;
}

:global(.dark) :deep(.el-dialog__close:hover) {
  color: #fff;
}
</style>
