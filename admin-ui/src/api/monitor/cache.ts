import request from '@/utils/request'

// 获取缓存监控信息
export function getCacheInfo() {
  return request({
    url: '/monitor/cache',
    method: 'get'
  })
}
