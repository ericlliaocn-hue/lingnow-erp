import request from '@/utils/request'

export interface AddressParseResult {
  contactName?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  street?: string
  village?: string
  regionPath?: string[]
  regionPathNames?: string[]
  contactCandidates?: string[]
  detailAddress?: string
  normalizedAddress?: string
  confidence?: number
  warnings?: string[]
}

export interface AddressRegionOption {
  code: string
  name: string
  level?: number
  leaf?: boolean
  path?: string[]
  pathNames?: string[]
}

export function parseAddress(rawText: string) {
  return request<AddressParseResult>({
    url: '/erp/common/address/parse',
    method: 'post',
    data: { rawText }
  })
}

export function listAddressRegions(parentCode?: string) {
  return request<AddressRegionOption[]>({
    url: '/erp/common/address/regions',
    method: 'get',
    params: { parentCode }
  })
}

export function searchAddressRegions(keyword: string, limit = 20) {
  return request<AddressRegionOption[]>({
    url: '/erp/common/address/search',
    method: 'get',
    params: { keyword, limit }
  })
}
