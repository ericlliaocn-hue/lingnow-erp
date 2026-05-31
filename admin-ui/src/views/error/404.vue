<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在或已被移除。</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: var(--el-bg-color);
}

.error-content {
  text-align: center;
}

h1 {
  font-size: 72px;
  color: var(--el-color-primary);
  margin: 0;
}

h2 {
  margin: 20px 0;
  color: var(--el-text-color-primary);
}

p {
  color: var(--el-text-color-secondary);
  margin-bottom: 30px;
}
</style>
