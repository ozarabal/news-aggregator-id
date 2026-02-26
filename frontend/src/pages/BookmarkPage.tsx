import { Navigate } from 'react-router-dom'
import { X } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuthStore } from '../store/useAuthStore'
import { useBookmarks, useRemoveBookmark } from '../hooks/useBookmarks'
import ArticleCard from '../components/articles/ArticleCard'
import { CardSkeleton } from '../components/articles/ArticleSkeleton'
import EmptyState from '../components/ui/EmptyState'

export default function BookmarkPage() {
  const token = useAuthStore(s => s.token)
  const { data, isLoading } = useBookmarks()
  const removeBookmark = useRemoveBookmark()

  // Guard: must be logged in
  if (!token) return <Navigate to="/login" replace />

  const articles = data?.data ?? []

  async function handleRemove(articleId: number) {
    try {
      await removeBookmark.mutateAsync(articleId)
      toast.success('Dihapus dari simpanan')
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Gagal menghapus bookmark')
    }
  }

  return (
    <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-10">
      {/* Header */}
      <div className="mb-8">
        <div className="section-rule mb-4" />
        <h1 className="font-display text-display-md font-black text-paper">Artikel Tersimpan</h1>
        {!isLoading && (
          <p className="text-muted text-sm font-body mt-1">
            {articles.length} artikel
          </p>
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-px bg-ink3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-ink">
              <CardSkeleton />
            </div>
          ))}
        </div>
      ) : articles.length === 0 ? (
        <EmptyState
          title="Belum ada artikel tersimpan"
          description="Buka artikel dan tekan tombol 'Simpan' untuk menyimpannya di sini"
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-px bg-ink3">
          {articles.map(article => (
            <div key={article.id} className="relative bg-ink group">
              <ArticleCard article={article} size="md" />
              {/* Remove button */}
              <button
                onClick={() => handleRemove(article.id)}
                disabled={removeBookmark.isPending}
                className="
                  absolute top-2 right-2 z-10
                  w-7 h-7 flex items-center justify-center
                  bg-ink2/80 border border-ink3 text-muted
                  hover:bg-accent hover:border-accent hover:text-paper
                  transition-colors opacity-0 group-hover:opacity-100
                  disabled:opacity-30
                "
                title="Hapus dari simpanan"
              >
                <X size={13} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
