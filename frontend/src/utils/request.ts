import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const TOKEN_KEY = 'trip_designer_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 600000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const errorMessages: Record<number, string> = {
  400: '请求参数错误，请检查输入内容',
  401: '登录已过期，请重新登录',
  403: '没有权限访问此资源',
  404: '请求的资源不存在',
  408: '请求超时，请稍后重试',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误，请稍后重试',
  502: '服务暂时不可用，请稍后重试',
  503: '服务暂时不可用，请稍后重试',
  504: '网关超时，请稍后重试',
}

const bizErrorMessages: Record<string, string> = {
  AUTH_TOKEN_INVALID: '登录已过期，请重新登录',
  AUTH_USER_NOT_FOUND: '用户不存在，请重新登录',
  AUTH_PASSWORD_ERROR: '密码错误，请重试',
  AUTH_USER_DISABLED: '用户已被禁用，请联系管理员',
  CONV_NOT_FOUND: '对话不存在或已被删除',
  CONV_NOT_OWNER: '无权访问此对话',
  TRIP_NOT_FOUND: '行程不存在或已被删除',
  TRIP_NOT_OWNER: '无权访问此行程',
  TRIP_NOT_EDITABLE: '行程已发布，无法编辑',
  PRICE_MONITOR_NOT_FOUND: '价格监测不存在或已被删除',
  PRICE_MONITOR_NOT_OWNER: '无权访问此价格监测',
  COMMUNITY_POST_NOT_FOUND: '帖子不存在或已被删除',
  COMMUNITY_COMMENT_NOT_FOUND: '评论不存在或已被删除',
  TEAM_NOT_FOUND: '队伍不存在或已解散',
  TEAM_NOT_OWNER: '无权管理此队伍',
  TEAM_ALREADY_MEMBER: '您已经是该队伍成员',
  TEAM_NOT_MEMBER: '您不是该队伍成员',
  CHECKIN_NOT_FOUND: '签到记录不存在',
  CHECKIN_ALREADY_EXISTS: '今天已经签到过了',
  AI_GENERATION_FAILED: 'AI生成失败，请稍后重试',
  INVALID_REQUEST: '请求参数错误，请检查输入内容',
}

function getErrorMessage(error: any): string {
  const response = error.response
  if (!response) {
    if (error.message?.includes('timeout')) {
      return '请求超时，请稍后重试'
    }
    if (error.message?.includes('Network Error')) {
      return '网络连接失败，请检查网络设置'
    }
    return error.message || '未知错误'
  }

  const status = response.status
  const data = response.data

  if (data?.code && bizErrorMessages[data.code]) {
    return bizErrorMessages[data.code]
  }

  if (data?.message) {
    return data.message
  }

  if (errorMessages[status]) {
    return errorMessages[status]
  }

  return `请求失败 (${status})`
}

request.interceptors.response.use(
  (response) => {
    const resData = response.data
    if (resData && typeof resData.code === 'number' && resData.code !== 0) {
      const error: any = new Error(resData.message || '请求失败')
      error.response = response
      error.status = response.status
      error.code = resData.code
      return Promise.reject(error)
    }
    return {
      ...response,
      data: resData?.data ?? resData,
    }
  },
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('trip_designer_refresh')
      ElMessageBox.confirm('登录已过期，是否重新登录？', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        window.location.href = '/login'
      }).catch(() => {
        ElMessage.info('请在需要时手动登录')
      })
    } else {
      const msg = getErrorMessage(error)
      ElMessage.error(msg)
    }

    return Promise.reject(error)
  }
)

export default request