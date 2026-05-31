import request from '@/utils/request'

export interface MenuItem {
  menuId: number
  parentId: number
  menuName: string
  menuType: number
  icon?: string
  path?: string
  component?: string
  permission?: string
  sortOrder: number
  visible: number
  status: number
  remark?: string
  children?: MenuItem[]
}

/**
 * 获取菜单树
 */
export function getMenuTree(params?: any) {
  return request({
    url: '/admin/menu/tree',
    method: 'get',
    params
  }) as unknown as Promise<MenuItem[]>
}

/**
 * 获取菜单树
 */
export function getAllMenuTree(params?: any) {
  return request({
    url: '/admin/menu/tree/all',
    method: 'get',
    params
  }) as unknown as Promise<MenuItem[]>
}

/**
 * 获取所有菜单列表
 */
export function getMenuList() {
  return request({
    url: '/admin/menu/list',
    method: 'get'
  })
}

/**
 * 根据ID获取菜单
 */
export function getMenuById(menuId: number) {
  return request({
    url: `/admin/menu/${menuId}`,
    method: 'get'
  })
}

/**
 * 新增菜单
 */
export function createMenu(data: MenuItem) {
  return request({
    url: '/admin/menu',
    method: 'post',
    data
  })
}

/**
 * 更新菜单
 */
export function updateMenu(data: MenuItem) {
  return request({
    url: '/admin/menu',
    method: 'put',
    data
  })
}

/**
 * 删除菜单
 */
export function deleteMenu(menuId: number) {
  return request({
    url: `/admin/menu/${menuId}`,
    method: 'delete'
  })
}
