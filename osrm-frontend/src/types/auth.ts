export interface UserInfo {
  id: number
  username: string
  realName?: string
  email?: string
  roles: string[]
  permissions: string[]
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}
