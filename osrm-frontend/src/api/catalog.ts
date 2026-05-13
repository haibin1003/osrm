import request from './request'

export interface SystemCatalogItem {
  id: number
  systemCode: string
  systemName: string
  systemAlias?: string
  unit?: string
  category?: string
  domainL1?: string
  domainL2?: string
  domainL3?: string
  status: string
  description?: string
  hasApplications: boolean
  vendor?: string
  level?: string
  responsibleDept?: string
  responsiblePerson?: string
  responsiblePhone?: string
  onlineDate?: string
  buildMode?: string
  tags?: string
  enabled: boolean
  applications?: ApplicationCatalogItem[]
}

export interface ApplicationCatalogItem {
  id: number
  applicationCode: string
  systemCode: string
  applicationName: string
  status: string
  description?: string
  vendor?: string
  responsiblePerson?: string
  responsiblePhone?: string
  enabled: boolean
}

export function getSystemCatalogList(params: { keyword?: string; status?: string; enabled?: boolean; page?: number; size?: number }) {
  return request.get('/v1/system-catalog', { params })
}

export function getSystemCatalogDetail(id: number) {
  return request.get(`/v1/system-catalog/${id}`)
}

export function getSystemApplications(id: number) {
  return request.get(`/v1/system-catalog/${id}/applications`)
}

export function setSystemCatalogEnabled(id: number, enabled: boolean) {
  return request.put(`/v1/system-catalog/${id}/status`, null, { params: { enabled } })
}
