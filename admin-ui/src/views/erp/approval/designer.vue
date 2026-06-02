<template>
  <div class="app-container workflow-designer-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <strong>流程设计器</strong>
          <el-button type="primary" @click="openWarmFlow">打开 Warm-Flow 接口入口</el-button>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="接口根路径">/admin-api/warm-flow</el-descriptions-item>
        <el-descriptions-item label="UI配置路径">/admin-api/warm-flow-ui/token-name</el-descriptions-item>
        <el-descriptions-item label="当前状态">Warm-Flow 后端插件已接入，流程定义与设计器接口使用真实插件能力。</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
function openWarmFlow() {
  window.open('/admin-api/warm-flow-ui/token-name', '_blank')
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
</style>
