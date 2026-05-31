import request from '@/utils/request'

// 获取Dashboard数据
export function getDashboardData() {
  return request({
    url: '/monitor/admin/dashboard',
    method: 'get'
  })
}

// 获取首页用户维度数据看板
export function getUserDashboardData() {
  return request({
    url: '/dashboard/user',
    method: 'get'
  })
}
