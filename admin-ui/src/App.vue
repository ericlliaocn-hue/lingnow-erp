<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import Loading from '@/components/Loading/index.vue'
import { useSettingsStore } from '@/store/modules/settings'

const settingsStore = useSettingsStore()
const showLoading = ref(true)

const locale = computed(() => {
  return settingsStore.settings.language === 'zh-CN' ? zhCn : en
})

onMounted(() => {
  // 页面加载完成后隐藏 loading
  setTimeout(() => {
    showLoading.value = false
  }, 300)
})
</script>

<template>
  <el-config-provider :locale="locale">
    <Loading v-if="showLoading" />
    <router-view v-show="!showLoading" />
  </el-config-provider>
</template>

<style>
/* Reset default styles if needed */
body {
    margin: 0;
    padding: 0;
}
</style>
