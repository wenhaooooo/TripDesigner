import request from '@/utils/request'
import type { LoginRequest, RegisterRequest, TokenResponse, UserVO } from '@/types/api'
// 认证相关 API 调用封装。
// 包含登录、注册、获取用户信息和 Token 刷新。


export const authApi = {
  register: (data: RegisterRequest) => request.post<TokenResponse>('/auth/register', data),
  login: (data: LoginRequest) => request.post<TokenResponse>('/auth/login', data),
  refresh: () => request.post<TokenResponse>('/auth/refresh'),
  logout: () => request.post('/auth/logout'),
  me: () => request.get<UserVO>('/user/me'),
  updateMe: (data: Partial<UserVO>) => request.put<UserVO>('/user/me', data),
}
