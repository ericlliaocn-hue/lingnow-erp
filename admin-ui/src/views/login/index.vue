<template>
  <div class="login-wrapper">
    <Loading v-if="loading" />

    <div class="login-header">
      <h4 class="title">Sign in to LingNow Admin</h4>
      <div class="subtitle">
        New user?
        <el-link type="primary" :underline="false" @click="$router.push('/register')" class="register-link">
          Create an account
        </el-link>
      </div>
    </div>

    <!-- Social Login Buttons -->
    <div class="social-login">
      <button class="social-btn" @click="handleSocialLogin('google')">
        <svg width="24" height="24" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
      </button>

      <button class="social-btn" @click="handleSocialLogin('github')">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.17 6.839 9.49.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.603-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.463-1.11-1.463-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.831.092-.646.35-1.086.636-1.336-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.578 9.578 0 0112 6.836c.85.004 1.705.114 2.504.336 1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.137 20.167 22 16.418 22 12c0-5.523-4.477-10-10-10z"/>
        </svg>
      </button>

      <button class="social-btn" @click="handleSocialLogin('twitter')">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="#1DA1F2">
          <path d="M23.643 4.937c-.835.37-1.732.62-2.675.733.962-.576 1.7-1.49 2.048-2.578-.9.534-1.897.922-2.958 1.13-.85-.904-2.06-1.47-3.4-1.47-2.572 0-4.658 2.086-4.658 4.66 0 .364.042.718.12 1.06-3.873-.195-7.304-2.05-9.602-4.868-.4.69-.63 1.49-.63 2.342 0 1.616.823 3.043 2.072 3.878-.764-.025-1.482-.234-2.11-.583v.06c0 2.257 1.605 4.14 3.737 4.568-.392.106-.803.162-1.227.162-.3 0-.593-.028-.877-.082.593 1.85 2.313 3.198 4.352 3.234-1.595 1.25-3.604 1.995-5.786 1.995-.376 0-.747-.022-1.112-.065 2.062 1.323 4.51 2.093 7.14 2.093 8.57 0 13.255-7.098 13.255-13.254 0-.2-.005-.402-.014-.602.91-.658 1.7-1.477 2.323-2.41z"/>
        </svg>
      </button>
    </div>

    <!-- Divider -->
    <div class="divider">
      <span class="divider-text">OR</span>
    </div>

    <!-- Login Form -->
    <el-form :model="form" class="login-form" @keyup.enter="handleLogin">
      <el-form-item>
        <div class="form-label">Email address</div>
        <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            autocomplete="username"
        />
      </el-form-item>

      <el-form-item>
        <div class="form-label">
           <span>Password</span>
           <el-link class="forgot-link" :underline="false">Forgot password?</el-link>
        </div>
        <el-input
            v-model="form.password"
            type="password"
            placeholder="6+ characters"
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
        Sign in
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

const handleSocialLogin = (provider: string) => {
  ElMessage.info(`${provider} 登录暂未接入`)
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

.register-link {
  font-weight: 600;
  font-size: 14px;
}

/* Social Login */
.social-login {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.social-btn {
  flex: 1;
  height: 48px;
  border-radius: 8px;
  border: 1px solid rgba(145, 158, 171, 0.2);
  background-color: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #637381;
}

.social-btn:hover {
  background-color: rgba(145, 158, 171, 0.08);
  border-color: rgba(145, 158, 171, 0.32);
}

.social-btn:active {
  transform: scale(0.98);
}

.social-btn svg {
  width: 24px;
  height: 24px;
}

/* Divider */
.divider {
  position: relative;
  text-align: center;
  margin: 32px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background-color: rgba(145, 158, 171, 0.2);
}

.divider-text {
  position: relative;
  display: inline-block;
  padding: 0 16px;
  background-color: #fff;
  color: #919eab;
  font-size: 12px;
  font-weight: 700;
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

.forgot-link {
  font-size: 13px;
  color: #637381;
  cursor: pointer;
  font-weight: 600;
}

.forgot-link:hover {
  text-decoration: underline;
  color: #212b36;
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

:global(.dark) .divider::before {
  background-color: rgba(145, 158, 171, 0.2);
}

:global(.dark) .divider-text {
  background-color: #141a21;
  color: #637381;
}

:global(.dark) .social-btn {
  border-color: rgba(145, 158, 171, 0.16);
  color: #919eab;
  background-color: rgba(145, 158, 171, 0.08);
}

:global(.dark) .social-btn:hover {
  background-color: rgba(145, 158, 171, 0.16);
  border-color: rgba(145, 158, 171, 0.24);
}

:global(.dark) .form-label {
  color: #fff;
}

:global(.dark) .forgot-link {
  color: #919eab;
}

:global(.dark) .forgot-link:hover {
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
