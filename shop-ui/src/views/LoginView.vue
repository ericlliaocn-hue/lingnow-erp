<template>
  <main class="login-page" :style="loginBackgroundStyle">
    <section class="auth-shell">
      <div class="form-side">
        <div class="form-head">
          <p class="eyebrow">荣时衣架</p>
          <h1>业务员登录</h1>
        </div>

        <form class="auth-form" @submit.prevent="submitLogin">
          <label class="field">
            <span>ERP账号</span>
            <input
              v-model.trim="loginForm.username"
              class="input"
              autocomplete="username"
              placeholder="请输入ERP账号"
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

        <p class="switch-link">使用现有ERP业务员账号登录</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const heroImage = `${import.meta.env.BASE_URL}images/rs-hanger-hero.png`
const loginBackgroundStyle = {
  backgroundImage: `url(${heroImage})`
}
const loginForm = reactive({ username: '', password: '' })

function redirectAfterAuth() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
  router.replace(redirect)
}

/** 把后端原始报错包装成友好文案，避免 ERP 内部话术外露 */
function friendlyError(action: string, err: unknown) {
  const raw = err instanceof Error ? err.message : ''
  if (/不存在|未注册|找不到/.test(raw)) return 'ERP账号不存在'
  if (/密码|credential/i.test(raw)) return '账号或密码不正确'
  return raw || `${action}失败，请稍后重试`
}

async function submitLogin() {
  error.value = ''
  if (!loginForm.username) {
    error.value = '请输入ERP账号'
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
