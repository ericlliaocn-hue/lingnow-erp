import request from '@/utils/request'

export interface FileQuery {
  pageNum: number
  pageSize: number
  fileName?: string
  storageType?: string
  startTime?: string
  endTime?: string
}

export interface FileVO {
  id: number
  fileName: string
  fileUrl: string
  fileSize: number
  fileSuffix: string
  storageType: string
  createTime: string
}

export interface FileConfigVO {
  id: number
  platform: string
  configJson: string
  isActive: number
  remark: string
  updateTime: string
}

export interface FileConfigUpdate {
  id?: number
  platform: string
  configJson: string
  isActive: number
  remark?: string
}

export function getFileList(params: FileQuery) {
  return request({
    url: '/admin/file/page',
    method: 'get',
    params
  })
}

export function deleteFile(id: number) {
  return request({
    url: `/admin/file/${id}`,
    method: 'delete'
  })
}

export function getFileConfigList() {
  return request({
    url: '/admin/file/config/list',
    method: 'get'
  })
}

export function saveFileConfig(data: FileConfigUpdate) {
  return request({
    url: '/admin/file/config',
    method: 'post',
    data
  })
}

export function uploadFile(data: any) {
  return request({
    url: '/admin/file/upload',
    method: 'post',
    data
  })
}

export function uploadChunk(data: any) {
  return request({
    url: '/admin/file/upload/chunk',
    method: 'post',
    data
  })
}

export function mergeChunks(params: any) {
  return request({
    url: '/admin/file/upload/merge',
    method: 'post',
    params
  })
}
