import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getBookmarks, addBookmark, removeBookmark } from '../api/bookmarks'
import { useAuthStore } from '../store/useAuthStore'

export function useBookmarks() {
  const token = useAuthStore(s => s.token)
  return useQuery({
    queryKey: ['bookmarks'],
    queryFn: getBookmarks,
    enabled: !!token,
    staleTime: 1000 * 60 * 5,
  })
}

/** Returns a Set of bookmarked article IDs for O(1) lookup. */
export function useBookmarkIds(): Set<number> {
  const { data } = useBookmarks()
  const articles = data?.data ?? []
  return new Set(articles.map(a => a.id))
}

export function useAddBookmark() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (articleId: number) => addBookmark(articleId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['bookmarks'] }),
  })
}

export function useRemoveBookmark() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (articleId: number) => removeBookmark(articleId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['bookmarks'] }),
  })
}
