import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAllSources,
  getAllCategories,
  createSource,
  updateSource,
  toggleSource,
  deleteSource,
} from '../api/sources'
import type { SourceRequest } from '../types'

export function useAllSources() {
  return useQuery({
    queryKey: ['sources'],
    queryFn: getAllSources,
    staleTime: 1000 * 60 * 5,
  })
}

export function useCategories() {
  return useQuery({
    queryKey: ['sources', 'categories'],
    queryFn: getAllCategories,
    staleTime: 1000 * 60 * 5,
  })
}

export function useCreateSource() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: SourceRequest) => createSource(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sources'] }),
  })
}

export function useUpdateSource() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: SourceRequest }) => updateSource(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sources'] }),
  })
}

export function useToggleSource() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => toggleSource(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sources'] }),
  })
}

export function useDeleteSource() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteSource(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sources'] }),
  })
}
