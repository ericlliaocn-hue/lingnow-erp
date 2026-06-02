import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export type ConfigType = 'bill-no-rule' | 'field-setting' | 'print-template'

export interface BillNoRule {
  id?: string
  billType: string
  billName: string
  prefix: string
  datePattern?: string
  serialLength: number
  nextSerial: number
  resetCycle?: string
  enabled?: number
  remark?: string
}

export interface FieldSetting {
  id?: string
  moduleCode: string
  fieldKey: string
  fieldLabel: string
  visible?: number
  required?: number
  sortOrder?: number
  width?: number
  remark?: string
}

export interface PrintTemplate {
  id?: string
  templateCode: string
  templateName: string
  billType: string
  paperType?: string
  contentJson?: string
  isDefault?: number
  status?: number
  remark?: string
}

export interface ConfigQuery {
  current: number
  size: number
  [key: string]: any
}

export function listConfig<T>(type: ConfigType, params: ConfigQuery) {
  return request<PageResult<T>>({ url: `/erp/config/${type}/list`, method: 'get', params })
}

export function getConfig<T>(type: ConfigType, id: string) {
  return request<T>({ url: `/erp/config/${type}/${id}`, method: 'get' })
}

export function addConfig(type: ConfigType, data: any) {
  return request({ url: `/erp/config/${type}`, method: 'post', data })
}

export function updateConfig(type: ConfigType, data: any) {
  return request({ url: `/erp/config/${type}`, method: 'put', data })
}

export function deleteConfig(type: ConfigType, ids: string | string[]) {
  return request({ url: `/erp/config/${type}/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}
