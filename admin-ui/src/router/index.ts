import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePermissionStore } from '@/store/modules/permission'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    component: () => import('@/layout/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('../views/login/index.vue'),
        meta: { title: '登录' }
      }
    ]
  },
  {
    path: '/register',
    component: () => import('@/layout/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'Register',
        component: () => import('../views/register/index.vue'),
        meta: { title: '注册' }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    name: 'Root',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis', affix: true }
      },
      {
        path: '/erp/production/add',
        name: 'ProductionBillMaintain',
        component: () => import('@/views/erp/bill/form.vue'),
        meta: { title: '生产单', hidden: true }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
