import axios from 'axios'
import type { AxiosInstance } from 'axios'

const h5Request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

h5Request.interceptors.request.use((config) => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')
  if (token) {
    config.headers['X-H5-Token'] = token
  }
  return config
})

h5Request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code !== 200) {
      if (code === 403) {
        window.location.replace('/h5/expired')
      }
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    if (status === 403) {
      window.location.replace('/h5/expired')
    }
    const msg = error.response?.data?.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export interface SystemCatalogItem {
  id: number
  systemCode: string
  systemName: string
  systemAlias?: string
  responsiblePerson?: string
  responsiblePhone?: string
  responsibleDept?: string
  hasApplications: boolean
}

export interface ApplicationCatalogItem {
  id: number
  applicationCode: string
  applicationName: string
  responsiblePerson?: string
  responsiblePhone?: string
}

export interface H5SoftwareEntry {
  packageId?: number
  packageName: string
  versionNo?: string
  softwareType?: string
  deployEnvironment?: string
  serverCount?: number
  usageScenario?: string
  remarks?: string
}

export interface H5SubmitData {
  responsiblePerson: string
  phone: string
  department?: string
  email?: string
  systemCatalogId: number
  applicationCatalogId?: number | null
  softwareEntries: H5SoftwareEntry[]
  captchaKey: string
  captchaCode: string
}

export function getCaptcha() {
  return h5Request.get('/v1/h5/captcha')
}

export function searchSystems(keyword: string, page = 1, size = 20) {
  return h5Request.get('/v1/h5/systems', {
    params: { keyword, page, size }
  })
}

export function getApplications(systemCatalogId: number) {
  return h5Request.get(`/v1/h5/systems/${systemCatalogId}/applications`)
}

export function getSuggestion(systemCatalogId: number) {
  return h5Request.get(`/v1/h5/systems/${systemCatalogId}/suggestion`)
}

export function submitInventory(data: H5SubmitData) {
  return h5Request.post('/v1/h5/inventory/submit', data)
}
