import { createApp } from 'vue'
import './permission' // 引入路由守卫
import { permission } from './directives/permission'
import './style.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VueApexCharts from 'vue3-apexcharts'
import 'apexcharts/dist/apexcharts.css'
import Pagination from '@/components/Pagination'

const enforceProductionHttps = () => {
    if (import.meta.env.PROD && window.location.protocol === 'http:' && window.location.hostname.endsWith('.oioio.chat')) {
        window.location.replace(`https://${window.location.host}${window.location.pathname}${window.location.search}${window.location.hash}`)
    }
}
enforceProductionHttps()

const app = createApp(App)

// 注册 Pinia
app.use(createPinia())

// 注册 Router
app.use(router)

// 注册权限指令
app.directive('permission', permission)

// 注册 Element Plus
app.use(ElementPlus)

// 注册全局组件
app.use(Pagination)

// 注册 VueApexCharts - 全局组件
app.component('apexchart', VueApexCharts)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.mount('#app')
