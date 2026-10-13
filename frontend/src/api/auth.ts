import { http } from './request'
import type { LoginResponse, UserProfile } from './types'

export interface LoginPayload {
  loginName: string
  passwd: string
}

export interface RegisterPayload {
  name: string
  loginName: string
  passwd: string
}

export function login(payload: LoginPayload) {
  return http.post<LoginResponse>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return http.post<LoginResponse>('/auth/register', payload)
}

export function fetchProfile() {
  return http.get<UserProfile>('/auth/me')
}
