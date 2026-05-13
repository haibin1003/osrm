import request from './request'
import type { PageResult } from '@/types/storage'

export interface H5AccessTokenItem {
  id: number
  token: string
  expireAt: string
  enabled: boolean
  createdByName: string
  remark: string
  accessCount: number
  lastAccessAt: string | null
  createdAt: string
}

export interface GenerateTokenParams {
  validDays?: number
  remark?: string
}

export function getH5Tokens(page = 1, size = 20): Promise<PageResult<H5AccessTokenItem>> {
  return request.get('/v1/admin/h5-tokens', { params: { page, size } })
}

export function generateH5Token(params: GenerateTokenParams): Promise<H5AccessTokenItem> {
  return request.post('/v1/admin/h5-tokens', params)
}

export function revokeH5Token(id: number): Promise<H5AccessTokenItem> {
  return request.post(`/v1/admin/h5-tokens/${id}/revoke`)
}

export function extendH5Token(id: number, extraDays: number): Promise<H5AccessTokenItem> {
  return request.post(`/v1/admin/h5-tokens/${id}/extend`, { extraDays })
}
