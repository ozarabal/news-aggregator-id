import { apiClient } from './client'
import type { ApiResponse, DigestStats } from '../types'

export function triggerDigestAll(): Promise<ApiResponse<string>> {
  return apiClient.post('/digest/trigger-all').then(r => r.data)
}

export function triggerDigestOne(userId: number): Promise<ApiResponse<string>> {
  return apiClient.post(`/digest/trigger/${userId}`).then(r => r.data)
}

export function getDigestStats(): Promise<ApiResponse<DigestStats>> {
  return apiClient.get('/digest/stats').then(r => r.data)
}
