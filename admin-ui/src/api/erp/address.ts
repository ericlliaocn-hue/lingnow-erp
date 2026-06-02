import request from '@/utils/request'

export interface AddressParseResult {
  contactName?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  detailAddress?: string
  normalizedAddress?: string
  confidence?: number
  warnings?: string[]
}

export function parseAddress(rawText: string) {
  return request<AddressParseResult>({
    url: '/erp/common/address/parse',
    method: 'post',
    data: { rawText }
  })
}
