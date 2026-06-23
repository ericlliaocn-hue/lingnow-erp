import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export type MasterType =
  | 'product-category'
  | 'unit'
  | 'product-brand'
  | 'product-attribute'
  | 'customer'
  | 'supplier'
  | 'warehouse'
  | 'account'
  | 'agent-level'

export interface ErpMasterVO {
  id: string
  code: string
  name: string
  parentId?: string
  contact?: string
  phone?: string
  address?: string
  levelId?: string
  accountType?: string
  openingBalance?: number
  discountRate?: number
  attributeIds?: string
  sortOrder: number
  status: number
  remark?: string
  createTime?: string
  children?: ErpMasterVO[]
}

export interface ErpMasterQuery {
  current: number
  size: number
  code?: string
  name?: string
  status?: number
  contact?: string
  phone?: string
}

export interface ErpMasterForm {
  id?: string
  code: string
  name: string
  parentId?: string
  contact?: string
  phone?: string
  address?: string
  levelId?: string
  accountType?: string
  openingBalance?: number
  discountRate?: number
  attributeIds?: string
  sortOrder: number
  status: number
  remark?: string
}

export function listMaster(type: MasterType, params: ErpMasterQuery) {
  return request<PageResult<ErpMasterVO>>({
    url: `/erp/master/${type}/list`,
    method: 'get',
    params
  })
}

export function getMaster(type: MasterType, id: string) {
  return request<ErpMasterVO>({
    url: `/erp/master/${type}/${id}`,
    method: 'get'
  })
}

export function addMaster(type: MasterType, data: ErpMasterForm) {
  return request({
    url: `/erp/master/${type}`,
    method: 'post',
    data
  })
}

export function updateMaster(type: MasterType, data: ErpMasterForm) {
  return request({
    url: `/erp/master/${type}`,
    method: 'put',
    data
  })
}

export function deleteMaster(type: MasterType, ids: string | string[]) {
  return request({
    url: `/erp/master/${type}/${Array.isArray(ids) ? ids.join(',') : ids}`,
    method: 'delete'
  })
}
