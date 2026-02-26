import { apiClient } from './client'
import type { ApiResponse, AuthResponse } from '../types'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  categories?: string[]
  digestEnabled?: boolean
  digestFrequency?: string
}

export function login(data: LoginRequest): Promise<ApiResponse<AuthResponse>> {
  return apiClient.post('/auth/login', data).then(r => r.data)
}

export function register(data: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
  return apiClient.post('/auth/register', data).then(r => r.data)
}
