import { apiClient } from './client'
import type { ApiResponse, Source, SourceRequest } from '../types'

export function getAllSources(): Promise<ApiResponse<Source[]>> {
  return apiClient.get('/sources').then(r => r.data)
}

export function getAllCategories(): Promise<ApiResponse<string[]>> {
  return apiClient.get('/sources/categories').then(r => r.data)
}

export function getSourceById(id: number): Promise<ApiResponse<Source>> {
  return apiClient.get(`/sources/${id}`).then(r => r.data)
}

export function createSource(data: SourceRequest): Promise<ApiResponse<Source>> {
  return apiClient.post('/sources', data).then(r => r.data)
}

export function updateSource(id: number, data: SourceRequest): Promise<ApiResponse<Source>> {
  return apiClient.put(`/sources/${id}`, data).then(r => r.data)
}

export function toggleSource(id: number): Promise<ApiResponse<Source>> {
  return apiClient.patch(`/sources/${id}/toggle`).then(r => r.data)
}

export function deleteSource(id: number): Promise<ApiResponse<void>> {
  return apiClient.delete(`/sources/${id}`).then(r => r.data)
}
