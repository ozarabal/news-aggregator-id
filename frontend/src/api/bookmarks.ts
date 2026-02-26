import { apiClient } from './client'
import type { ApiResponse, ArticleSummary } from '../types'

export function getBookmarks(): Promise<ApiResponse<ArticleSummary[]>> {
  return apiClient.get('/bookmarks').then(r => r.data)
}

export function addBookmark(articleId: number): Promise<ApiResponse<void>> {
  return apiClient.post(`/bookmarks/${articleId}`).then(r => r.data)
}

export function removeBookmark(articleId: number): Promise<ApiResponse<void>> {
  return apiClient.delete(`/bookmarks/${articleId}`).then(r => r.data)
}
