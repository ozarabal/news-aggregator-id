import { apiClient } from './client'
import type { ApiResponse, CrawlResult, CrawlStats } from '../types'

export function crawlAll(): Promise<ApiResponse<string>> {
  return apiClient.post('/crawler/crawl-all').then(r => r.data)
}

export function crawlOne(sourceId: number): Promise<ApiResponse<CrawlResult>> {
  return apiClient.post(`/crawler/crawl/${sourceId}`).then(r => r.data)
}

export function getCrawlStats(): Promise<ApiResponse<CrawlStats>> {
  return apiClient.get('/crawler/stats').then(r => r.data)
}
