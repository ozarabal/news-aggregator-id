import { apiClient } from './client'
import type { ApiResponse, Page, ArticleSummary, ArticleDetail } from '../types'

export interface ArticleParams {
  page?: number
  size?: number
  category?: string
  search?: string
  sourceId?: number
}

export function getArticles(params: ArticleParams = {}): Promise<ApiResponse<Page<ArticleSummary>>> {
  const cleanParams: Record<string, string | number> = {}
  if (params.page !== undefined) cleanParams.page = params.page
  if (params.size !== undefined) cleanParams.size = params.size
  if (params.category) cleanParams.category = params.category
  if (params.search) cleanParams.search = params.search
  if (params.sourceId !== undefined) cleanParams.sourceId = params.sourceId
  return apiClient.get('/articles', { params: cleanParams }).then(r => r.data)
}

export function getArticleById(id: number): Promise<ApiResponse<ArticleDetail>> {
  return apiClient.get(`/articles/${id}`).then(r => r.data)
}
