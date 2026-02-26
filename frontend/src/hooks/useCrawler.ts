import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getCrawlStats, crawlAll, crawlOne } from '../api/crawler'

export function useCrawlStats() {
  return useQuery({
    queryKey: ['crawler', 'stats'],
    queryFn: getCrawlStats,
    staleTime: 1000 * 30,
  })
}

export function useCrawlAll() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: crawlAll,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['crawler', 'stats'] })
      qc.invalidateQueries({ queryKey: ['sources'] })
    },
  })
}

export function useCrawlOne() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (sourceId: number) => crawlOne(sourceId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['crawler', 'stats'] })
      qc.invalidateQueries({ queryKey: ['sources'] })
      qc.invalidateQueries({ queryKey: ['articles'] })
    },
  })
}
