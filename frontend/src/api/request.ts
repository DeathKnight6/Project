import axios, { type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from './types'

export const TOKEN_KEY = 'stock_token'
export const USER_KEY = 'stock_user'

const request = axios.create({
  // 开发环境由 Vite proxy 转发到 http://localhost:8080
  baseURL: '/api',
  timeout: 15000
})

/** 请求拦截：自动带上 JWT */
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function redirectToLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (!window.location.pathname.startsWith('/login')) {
    // 用整页跳转而不是 router.push，避免与 router -> store -> api 形成循环依赖
    window.location.replace('/login')
  }
}

/**
 * 响应拦截：拆掉 ApiResponse 外壳，业务失败统一提示。
 * 这里故意返回 body.data 而不是 AxiosResponse，axios 的类型签名不接受这种"改写返回值"的用法，
 * 所以显式标注返回 any；调用方通过下面 http 封装的泛型拿回正确类型。
 */
request.interceptors.response.use(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (response: AxiosResponse<ApiResult<unknown>>): any => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return response.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.error('无权访问该资源')
    } else {
      const message = error.response?.data?.message || error.message || '网络异常'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

/**
 * 拦截器已经把 ApiResponse 解包成 data，但 axios 的类型签名不知道这件事。
 * 这里用一层薄封装把返回类型纠正回来。
 */
export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    request.get(url, { params }) as unknown as Promise<T>,
  post: <T>(url: string, data?: unknown) => request.post(url, data) as unknown as Promise<T>
}

export default request
