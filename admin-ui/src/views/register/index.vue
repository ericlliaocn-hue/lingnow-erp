<template>
  <div class="login-wrapper">
    <div class="login-header">
      <h4 class="title">Get started absolutely free</h4>
      <div class="subtitle">
        Already have an account? 
        <el-link type="primary" :underline="false" @click="$router.push('/login')">Sign in</el-link>
      </div>
    </div>

    <el-form :model="form" class="login-form">
      <div class="name-row">
          <el-form-item class="half-width">
            <div class="form-label">First name</div>
            <el-input 
                v-model="form.firstName" 
                placeholder="First name" 
                size="large"
            />
          </el-form-item>
          <el-form-item class="half-width">
            <div class="form-label">Last name</div>
            <el-input 
                v-model="form.lastName" 
                placeholder="Last name" 
                size="large"
            />
          </el-form-item>
      </div>

      <el-form-item>
        <div class="form-label">Email address</div>
        <el-input 
            v-model="form.username" 
            placeholder="Email address" 
            size="large"
        />
      </el-form-item>
      
      <el-form-item>
        <div class="form-label">Password</div>
        <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="6+ characters" 
            size="large" 
            show-password
        />
      </el-form-item>

      <el-button
        :loading="loading"
        type="default"
        size="large"
        class="login-btn"
        @click="handleRegister"
      >
        Create account
      </el-button>

      <div class="terms-text">
          By signing up, I agree to <a href="#">Terms of Service</a> and <a href="#">Privacy Policy</a>.
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  firstName: '',
  lastName: '',
  username: '',
  password: ''
})

const handleRegister = async () => {
  if (!form.username || !form.password) {
     ElMessage.warning('Please enter required fields')
     return
  }
  loading.value = true
  setTimeout(() => {
      loading.value = false
      ElMessage.success('Registration successful')
      router.push('/login')
  }, 1000)
}
</script>

<style scoped>
.login-wrapper {
  width: 100%;
  max-width: 420px;
}
.login-header {
  margin-bottom: 24px;
}
.title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.name-row {
    display: flex;
    gap: 16px;
}
.half-width {
    flex: 1;
}

.form-label {
    margin-bottom: 6px;
    font-size: 14px;
    color: var(--el-text-color-primary);
    font-weight: 600;
    width: 100%;
}

.login-btn {
    width: 100%;
    font-weight: 700;
    font-size: 15px;
    height: 48px;
    border-radius: 8px;
    margin-top: 16px;
}

/* Custom styling for "inherit" look in Dark Mode (White button) to match Minimals */
:global(.dark) .login-btn {
    background-color: #ffffff;
    color: #212b36;
    border-color: #ffffff;
}
:global(.dark) .login-btn:hover {
    background-color: #c4cdd5;
    border-color: #c4cdd5;
}

:deep(.el-input__wrapper) {
    border-radius: 8px;
    background-color: transparent; 
}
:global(.dark) :deep(.el-input__wrapper) {
   box-shadow: 0 0 0 1px #919eab4d inset;
}

.terms-text {
    margin-top: 24px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-align: center;
}
.terms-text a {
    text-decoration: underline;
    color: var(--el-text-color-primary);
}
</style>
