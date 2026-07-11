<template>
  <main class="login-page" :style="loginBackgroundStyle">
    <section class="auth-shell">
      <div class="form-side">
        <div class="form-head">
          <p class="eyebrow">荣时衣架</p>
          <h1>{{ mode === 'login' ? '登录账号' : '注册账号' }}</h1>
        </div>

        <div class="mode-tabs" aria-label="账号操作">
          <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
          <label class="field">
            <span>手机号</span>
            <input
              v-model.trim="loginForm.username"
              class="input"
              autocomplete="username"
              inputmode="tel"
              placeholder="请输入手机号"
            />
          </label>
          <label class="field">
            <span>密码</span>
            <input
              v-model="loginForm.password"
              class="input"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
          </label>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        </form>

        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <label class="field">
            <span>昵称</span>
            <input v-model.trim="registerForm.name" class="input" autocomplete="nickname" placeholder="请输入昵称" />
          </label>
          <label class="field">
            <span>手机号</span>
            <input
              v-model.trim="registerForm.phone"
              class="input"
              autocomplete="tel"
              inputmode="tel"
              placeholder="请输入手机号"
            />
          </label>
          <label class="field">
            <span>设置密码</span>
            <input
              v-model="registerForm.password"
              class="input"
              type="password"
              autocomplete="new-password"
              placeholder="请输入 6-32 位密码"
            />
          </label>
          <label class="field">
            <span>确认密码</span>
            <input
              v-model="registerForm.confirmPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              placeholder="请再次输入密码"
            />
          </label>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '注册中...' : '注册并进入' }}</button>
        </form>

        <button class="switch-link" type="button" @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '还没有账号，立即注册' : '已有账号，返回登录' }}
        </button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

type AuthMode = 'login' | 'register'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const mode = ref<AuthMode>(route.query.mode === 'register' ? 'register' : 'login')
const heroImage = `${import.meta.env.BASE_URL}images/rs-hanger-hero.png`
const loginBackgroundStyle = {
  backgroundImage: `url(${heroImage})`
}
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ name: '', phone: '', password: '', confirmPassword: '' })

function switchMode(value: AuthMode) {
  mode.value = value
  error.value = ''
}

function redirectAfterAuth() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
  router.replace(redirect)
}

function validatePhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

/** 把后端原始报错包装成友好文案，避免 ERP 内部话术外露 */
function friendlyError(action: string, err: unknown) {
  const raw = err instanceof Error ? err.message : ''
  if (/不存在|未注册|找不到/.test(raw)) return '账号不存在，请先注册'
  if (/密码|credential/i.test(raw)) return '手机号或密码不正确'
  if (/已注册|已存在|duplicate/i.test(raw)) return '该手机号已注册，请直接登录'
  return raw || `${action}失败，请稍后重试`
}

async function submitLogin() {
  error.value = ''
  if (!validatePhone(loginForm.username)) {
    error.value = '请输入正确的手机号'
    return
  }
  if (!loginForm.password) {
    error.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
    redirectAfterAuth()
  } catch (err) {
    error.value = friendlyError('登录', err)
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  error.value = ''
  if (!registerForm.name) {
    error.value = '请输入昵称'
    return
  }
  if (!validatePhone(registerForm.phone)) {
    error.value = '请输入正确的手机号'
    return
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 32) {
    error.value = '密码长度需为 6-32 位'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await auth.register({ ...registerForm })
    redirectAfterAuth()
  } catch (err) {
    error.value = friendlyError('注册', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100svh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 14px calc(18px + env(safe-area-inset-bottom));
  background-color: #241b16;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  overflow-y: auto;
}

.auth-shell {
  width: min(420px, 100%);
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: var(--radius);
  background: rgba(255, 253, 248, 0.38);
  box-shadow: 0 18px 36px rgba(25, 18, 14, 0.20);
  backdrop-filter: blur(8px) saturate(1.04);
}

.form-side {
  padding: 24px 18px 20px;
}

.form-head {
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--brand-brown-soft);
  font-size: 13px;
  font-weight: 900;
}

.form-head h1 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
  line-height: 1.15;
}

.mode-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-bottom: 18px;
  padding: 4px;
  border: 1px solid rgba(224, 204, 186, 0.54);
  border-radius: var(--radius);
  background: rgba(255, 246, 232, 0.26);
}

.mode-tabs button {
  min-height: 38px;
  border-radius: var(--radius-sm);
  color: var(--text-sub);
  background: transparent;
  font-weight: 900;
  font-size: 14px;
}

.mode-tabs button.active {
  color: var(--text-main);
  background: rgba(255, 253, 248, 0.76);
  box-shadow: 0 4px 12px rgba(88, 68, 45, 0.1);
}

.auth-form {
  display: grid;
  gap: 4px;
}

.field span {
  color: var(--brand-brown-soft);
  font-size: 13px;
  font-weight: 800;
}

.input {
  min-height: 44px;
  border-color: var(--border-line);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.76);
  font-size: 15px;
}

.input:focus {
  border-color: var(--brand-teal);
  box-shadow: 0 0 0 3px rgba(29, 109, 95, 0.14);
}

.form-error {
  margin: 0 0 8px;
  color: #b3322a;
  font-size: 13px;
}

.primary-button {
  min-height: 46px;
  margin-top: 6px;
  border-radius: var(--radius-sm);
  background: var(--brand-teal);
  box-shadow: 0 8px 18px rgba(29, 109, 95, 0.22);
  font-size: 15px;
}

.primary-button:disabled {
  cursor: default;
  opacity: 0.72;
}

.switch-link {
  width: 100%;
  min-height: 38px;
  margin-top: 10px;
  color: var(--brand-brown);
  background: transparent;
  font-weight: 900;
  font-size: 13px;
}

@media (min-width: 860px) {
  .login-page {
    align-items: center;
    justify-content: center;
    padding: 48px clamp(48px, 8vw, 120px);
  }

  .auth-shell {
    margin: 0;
  }

  .form-side {
    padding: 34px;
  }

  .form-head h1 {
    font-size: 34px;
  }
}

@media (max-width: 420px) {
  .login-page {
    padding: 18px 14px calc(18px + env(safe-area-inset-bottom));
  }

  .auth-shell {
    width: 100%;
  }
}

@media (max-height: 720px) {
  .login-page {
    align-items: center;
  }
}
</style>
