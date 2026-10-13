import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, register as registerApi, type LoginPayload, type RegisterPayload } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/api/request'
import type { LoginResponse } from '@/api/types'

interface StoredUser {
  userId: number
  name: string
  loginName: string
  type: number
}

function readStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as StoredUser
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<StoredUser | null>(readStoredUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.name || user.value?.loginName || '')
  const isAdmin = computed(() => user.value?.type === 1)

  function persist(payload: LoginResponse) {
    token.value = payload.token
    user.value = {
      userId: payload.userId,
      name: payload.name,
      loginName: payload.loginName,
      type: payload.type
    }
    localStorage.setItem(TOKEN_KEY, payload.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  async function login(payload: LoginPayload) {
    persist(await loginApi(payload))
  }

  async function register(payload: RegisterPayload) {
    persist(await registerApi(payload))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, displayName, isAdmin, login, register, logout }
})
