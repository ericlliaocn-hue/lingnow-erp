import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getMenuTree, type MenuItem } from '@/api/sys/menu'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'

// 匹配 views 里面所有的 .vue 文件
const modules = import.meta.glob('/src/views/**/*.vue')

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const menus = ref<MenuItem[]>([])
  const isRoutesLoaded = ref(false)

  // 将后端菜单数据转换为路由配置
  const generateRoutes = async () => {
    try {
      let menuData = await getMenuTree()
      // 容错处理：确保 menuData 是数组
      if (!Array.isArray(menuData)) {
        // 如果后端返回的是包含 records 的对象（分页结构），尝试提取
        if ((menuData as any).records && Array.isArray((menuData as any).records)) {
             menuData = (menuData as any).records
        } else {
             console.warn('getMenuTree return non-array data:', menuData)
             menuData = []
        }
      }

      menus.value = menuData // 保存原始菜单数据

      // 生成路由
      const asyncRoutes = filterAsyncRoutes(menuData)

      // 构造最终路由结构
      // 1. 创建一个默认的 Layout 根路由，用于包裹那些不是 Layout 的顶层页面
      const mainLayoutRoute: RouteRecordRaw = {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: []
      }

      const finalRoutes: RouteRecordRaw[] = []

      asyncRoutes.forEach(route => {
        // 如果路由本身就是 Layout (通常是目录)，直接作为根路由
        if (route.component === Layout) {
           // 确保根路由路径以 / 开头
           if (route.path && !route.path.startsWith('/') && !route.path.startsWith('http')) {
             route.path = '/' + route.path
           }
           finalRoutes.push(route)
        } else {
           // 如果是普通页面，放入 mainLayoutRoute 的 children
           
           // 修复 404 问题：如果 route.path 已经是绝对路径（以 / 开头），需要去掉 / 才能作为子路由正确匹配
           // 例如 /tool/build -> tool/build，这样在 Layout 下访问 /tool/build 才能命中
           // 但是，如果后端返回的就是相对路径 tool/build，则不需要处理
           // 注意：Vue Router 4 支持嵌套路由使用绝对路径（以 / 开头），此时会作为根路径处理
           
           // 克隆一份路由对象，避免修改原引用影响其他逻辑
           const childRoute = { ...route }
           // if (childRoute.path && childRoute.path.startsWith('/')) {
           //   childRoute.path = childRoute.path.slice(1)
           // }
           
           // 确保 path 不为空
           if (childRoute.path) {
             mainLayoutRoute.children?.push(childRoute)
           }
        }
      })

      // 如果 mainLayoutRoute 有子路由，加入它
      if (mainLayoutRoute.children && mainLayoutRoute.children.length > 0) {
        finalRoutes.push(mainLayoutRoute)
      }

      routes.value = finalRoutes
      isRoutesLoaded.value = true
      return finalRoutes
    } catch (error) {
      console.error('生成动态路由失败:', error)
      throw error
    }
  }

  // 递归过滤异步路由表
  const filterAsyncRoutes = (menus: MenuItem[]): RouteRecordRaw[] => {
    const res: RouteRecordRaw[] = []

    menus.forEach(menu => {
      // 忽略按钮类型的菜单 (menuType === 2)
      if (menu.menuType === 2) {
        return
      }

      // 构造路由对象
      let routePath = menu.path || ''
      // 如果是根路由且不以 / 开头，自动补全 /
      // 只有第一层递归（无法直接判断是否第一层，但可以判断 parentId 或其他）
      // 这里简化处理：如果 path 不包含 / 且不是 http 链接，可能是根路由
      // 但更好的方式是：在 filterAsyncRoutes 外部处理根节点，或者传递 level 参数

      const route: RouteRecordRaw = {
        path: routePath,
        name: generateRouteName(routePath, menu.menuId),
        meta: {
          title: menu.menuName,
          icon: menu.icon,
          requiresAuth: true,
          menuId: menu.menuId,
          sortOrder: menu.sortOrder
        },
        component: undefined,
        children: []
      }

      // 处理组件
      if (menu.component) {
        if (menu.component === 'Layout') {
          route.component = Layout
        } else if (menu.component === 'ParentView') {
          route.component = ParentView
        } else {
          // 动态导入组件
          // 假设后端返回的是 'sys/menu/index' 这样的相对路径
          // 同时也兼容 '/views/sys/menu/index.vue' 这种格式
          let rawPath = menu.component
          // 移除开头的 /
          if (rawPath.startsWith('/')) rawPath = rawPath.slice(1)
          // 移除 src/views/ 或 views/ 前缀，标准化为相对路径
          if (rawPath.startsWith('src/views/')) rawPath = rawPath.replace('src/views/', '')
          else if (rawPath.startsWith('views/')) rawPath = rawPath.replace('views/', '')
          // 移除 .vue 后缀
          if (rawPath.endsWith('.vue')) rawPath = rawPath.replace('.vue', '')

          // 尝试多种路径匹配，增强健壮性
          const possiblePaths = [
            `/src/views/${rawPath}.vue`,
            `src/views/${rawPath}.vue`,
            `/views/${rawPath}.vue`,
            `views/${rawPath}.vue`
          ]

          let found = false
          for (const p of possiblePaths) {
            if (modules[p]) {
              route.component = modules[p]
              found = true
              break
            }
          }
          
          if (!found) {
             console.warn(`未找到组件路径: ${menu.component}, 尝试路径: ${possiblePaths.join(', ')}`)
          }
        }
      } else {
        // 如果没有指定组件，但有子菜单，通常作为目录
        // 这里给一个默认的 RouterView 容器，或者如果它是根级目录，给 Layout
        if (menu.children && menu.children.length > 0) {
            // 如果是根节点(这里简单判断，如果它没有 parentId 或者 context 决定)
            // 暂时统一给 Layout，如果是嵌套的子目录，Layout 也能正常渲染 router-view
            // 但更好的做法是区分：根级目录用 Layout，嵌套目录用 EmptyLayout
            route.component = Layout
        }
      }

      // 处理子菜单
      if (menu.children && menu.children.length > 0) {
        route.children = filterAsyncRoutes(menu.children)
        // 如果是 Layout 组件且没有指定 redirect，自动重定向到第一个子路由
        if (route.component === Layout && !route.redirect && route.children.length > 0) {
            const firstChild = route.children[0]
            if (firstChild) {
                // 处理路径拼接
                const childPath = firstChild.path || ''
                if (childPath.startsWith('/')) {
                    route.redirect = childPath
                } else {
                    // 简单拼接，注意处理 /
                    const parentPath = route.path || ''
                    const separator = parentPath.endsWith('/') ? '' : '/'
                    route.redirect = parentPath + separator + childPath
                }
            }
        }
      }

      res.push(route)
    })

    return res
  }

  // 生成路由名称
  const generateRouteName = (path: string, id: number): string => {
    let name = ''
    if (path) {
      // 简单的转驼峰，移除 / 和 -
      const camel = path.replace(/^[/-]/, '').replace(/[/-](\w)/g, (_, c) => c.toUpperCase())
      name = camel.charAt(0).toUpperCase() + camel.slice(1)
    }
    // 始终追加 id 以确保唯一性，解决父子路由同名问题
    return name ? `${name}Id${id}` : `Route${id}`
  }

  const resetRoutes = () => {
    routes.value = []
    menus.value = []
    isRoutesLoaded.value = false
  }

  return {
    routes,
    menus,
    isRoutesLoaded,
    generateRoutes,
    resetRoutes
  }
})
