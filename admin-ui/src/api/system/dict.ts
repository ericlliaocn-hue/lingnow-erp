import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface DictTypeVO {
  dictId: string
  dictName: string
  dictType: string
  status: number
  createTime: string
  remark?: string
}

export interface DictDataVO {
  dictCode: string
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault: string
  status: number
  createTime: string
  remark?: string
}

export interface DictTypeQuery {
  current: number
  size: number
  dictName?: string
  dictType?: string
  status?: number
}

export interface DictDataQuery {
  current: number
  size: number
  dictType?: string
  dictLabel?: string
  status?: number
}

export interface DictTypeForm {
  dictId?: string
  dictName: string
  dictType: string
  status: number
  remark?: string
}

export interface DictDataForm {
  dictCode?: string
  dictSort: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault: string
  status: number
  remark?: string
}

// === 字典类型 ===

export function listDictType(params: DictTypeQuery) {
  return request<PageResult<DictTypeVO>>({
    url: '/system/dict/type/list',
    method: 'get',
    params
  })
}

export function getDictType(dictId: string) {
  return request<DictTypeVO>({
    url: `/system/dict/type/${dictId}`,
    method: 'get'
  })
}

export function addDictType(data: DictTypeForm) {
  return request({
    url: '/system/dict/type',
    method: 'post',
    data
  })
}

export function updateDictType(data: DictTypeForm) {
  return request({
    url: '/system/dict/type',
    method: 'put',
    data
  })
}

export function delDictType(dictId: string) {
  return request({
    url: `/system/dict/type/${dictId}`,
    method: 'delete'
  })
}

export function refreshDictCache() {
  return request({
    url: '/system/dict/type/refreshCache',
    method: 'delete'
  })
}

// 获取字典选择框列表
export function optionselect() {
  return request({
    url: '/system/dict/type/optionselect',
    method: 'get'
  })
}

// === 字典数据 ===

export function listDictData(params: DictDataQuery) {
  return request<PageResult<DictDataVO>>({
    url: '/system/dict/data/list',
    method: 'get',
    params
  })
}

export function getDictData(dictCode: string) {
  return request<DictDataVO>({
    url: `/system/dict/data/${dictCode}`,
    method: 'get'
  })
}

export function getDicts(dictType: string) {
  return request({
    url: `/system/dict/data/type/${dictType}`,
    method: 'get'
  })
}

export function addDictData(data: DictDataForm) {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data
  })
}

export function updateDictData(data: DictDataForm) {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data
  })
}

export function delDictData(dictCode: string) {
  return request({
    url: `/system/dict/data/${dictCode}`,
    method: 'delete'
  })
}
