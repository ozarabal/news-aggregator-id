import { useQuery } from '@tanstack/react-query'
import { getArticles, getArticleById, type ArticleParams } from '../api/articles'

export function useArticles(params: ArticleParams = {}) {
  return useQuery({
    queryKey: ['articles', params],
    queryFn: () => getArticles(params),
    staleTime: 1000 * 60 * 5,
  })
}

export function useArticleDetail(id: number) {
  return useQuery({
    queryKey: ['articles', 'detail', id],
    queryFn: () => getArticleById(id),
    staleTime: 1000 * 60 * 5,
    enabled: !!id,
  })
}
