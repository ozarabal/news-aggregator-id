import { apiClient } from './client'
import type { ApiResponse, CacheStats } from '../types'

export function getCacheStats(): Promise<ApiResponse<CacheStats>> {
  return apiClient.get('/cache/stats').then(r => r.data)
}

export function evictArticleCache(): Promise<ApiResponse<void>> {
  return apiClient.delete('/cache/articles').then(r => r.data)
}

export function evictSourceCache(): Promise<ApiResponse<void>> {
  return apiClient.delete('/cache/sources').then(r => r.data)
}

export function evictAllCache(): Promise<ApiResponse<void>> {
  return apiClient.delete('/cache').then(r => r.data)
}
