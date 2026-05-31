import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface NoticeVO {
  noticeId: string
  noticeTitle: string
  noticeType: number
  noticeContent: string
  status: number
  createTime: string
  createBy: string
}

export interface NoticeQuery {
  current: number
  size: number
  noticeTitle?: string
  noticeType?: number
  createBy?: string
  status?: number
}

export interface NoticeForm {
  noticeId?: string
  noticeTitle: string
  noticeType: number
  noticeContent: string
  status: number
}

// 查询公告列表
export function listNotice(params: NoticeQuery) {
  return request<PageResult<NoticeVO>>({
    url: '/system/notice/list',
    method: 'get',
    params
  })
}

// 查询公告详细
export function getNotice(noticeId: string) {
  return request<NoticeVO>({
    url: `/system/notice/${noticeId}`,
    method: 'get'
  })
}

// 新增公告
export function addNotice(data: NoticeForm) {
  return request({
    url: '/system/notice',
    method: 'post',
    data
  })
}

// 修改公告
export function updateNotice(data: NoticeForm) {
  return request({
    url: '/system/notice',
    method: 'put',
    data
  })
}

// 删除公告
export function delNotice(noticeId: string) {
  return request({
    url: `/system/notice/${noticeId}`,
    method: 'delete'
  })
}
