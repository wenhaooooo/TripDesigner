import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'
import type { TokenResponse, UserVO } from '@/types/api'
// Pinia 认证状态管理。
// 管理 Token（access + refresh）、用户信息和登录状态。
// Token 同时存储在 Pinia（内存）和 localStorage（持久化）中。


const TOKEN_KEY = 'trip_designer_token'
const REFRESH_TOKEN_KEY = 'trip_designer_refresh'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY))
  const user = ref<UserVO | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  async function fetchMe() {
    if (!token.value) return
    try {
      const res = await authApi.me()
      user.value = res.data
    } catch {
      logout()
    }
  }

  async function login(email: string, password: string) {
    loading.value = true
    try {
      const res = await authApi.login({ email, password })
      const data: TokenResponse = res.data
      token.value = data.accessToken
      refreshToken.value = data.refreshToken
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
      await fetchMe()
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(email: string, password: string) {
    loading.value = true
    try {
      const res = await authApi.register({ email, password })
      const data: TokenResponse = res.data
      token.value = data.accessToken
      refreshToken.value = data.refreshToken
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
      await fetchMe()
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  return { token, refreshToken, user, loading, isAuthenticated, login, register, logout, fetchMe }
})
