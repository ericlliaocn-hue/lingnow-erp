import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface ConfigVO {
  configId: string
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark?: string
  createTime: string
}

export interface ConfigQuery {
  current: number
  size: number
  configName?: string
  configKey?: string
  configType?: string
}

export interface ConfigForm {
  configId?: string
  configName: string
  configKey: string
  configValue: string
  configType: string
  remark?: string
}

// 查询参数列表
export function listConfig(params: ConfigQuery) {
  return request<PageResult<ConfigVO>>({
    url: '/system/config/list',
    method: 'get',
    params
  })
}

// 查询参数详细
export function getConfig(configId: string) {
  return request<ConfigVO>({
    url: `/system/config/${configId}`,
    method: 'get'
  })
}

// 根据参数键名查询参数值
export function getConfigKey(configKey: string) {
  return request<string>({
    url: `/system/config/configKey/${configKey}`,
    method: 'get'
  })
}

// 新增参数配置
export function addConfig(data: ConfigForm) {
  return request({
    url: '/system/config',
    method: 'post',
    data
  })
}

// 修改参数配置
export function updateConfig(data: ConfigForm) {
  return request({
    url: '/system/config',
    method: 'put',
    data
  })
}

// 删除参数配置
export function delConfig(configId: string) {
  return request({
    url: `/system/config/${configId}`,
    method: 'delete'
  })
}

// 刷新参数缓存
export function refreshCache() {
  return request({
    url: '/system/config/refreshCache',
    method: 'delete'
  })
}
