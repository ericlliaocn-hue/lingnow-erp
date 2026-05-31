import router from './router'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const whiteList = ['/login', '/404', '/register'] // 白名单

router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - LingNow ERP`
  } else {
    document.title = 'LingNow ERP'
  }

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const hasToken = userStore.token

  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      // 判断是否已加载路由
      if (permissionStore.isRoutesLoaded) {
        next()
      } else {
        try {
          // 生成路由
          const accessRoutes = await permissionStore.generateRoutes()
          
          // 动态添加路由
          accessRoutes.forEach(route => {
            // 如果是根路由的扩展（那些没有指定 Layout 的页面），添加到 Root 下，避免覆盖静态定义的 Dashboard
            if (route.path === '/' && route.children) {
               route.children.forEach(child => {
                   router.addRoute('Root', child)
               })
            } else {
                router.addRoute(route)
             }
           })

           // 添加 404 路由（必须在最后添加）
           router.addRoute({
             path: '/:pathMatch(.*)*',
             redirect: '/404',
             meta: { hidden: true }
           })
           
           // 确保 addRoute 完整生效，设置 replace: true，这样导航就不会留下历史记录
          next({ ...to, replace: true })
        } catch (error) {
          console.error('路由加载失败:', error)
          // 出错需重置 token 并跳转登录页
          await userStore.logout()
          next(`/login?redirect=${to.path}`)
        }
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1 || to.meta.isPublic) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})
