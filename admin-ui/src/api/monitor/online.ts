import request from '@/utils/request'

// 在线用户列表
export function getOnlineUserList(params: any) {
  return request({
    url: '/monitor/online/list',
    method: 'get',
    params
  })
}

// 强退用户
export function forceLogout(tokenId: string) {
  return request({
    url: '/monitor/online/' + tokenId,
    method: 'delete'
  })
}
