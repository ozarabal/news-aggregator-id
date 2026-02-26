import { useParams, Link } from 'react-router-dom'
import { ChevronLeft, Eye, Clock, User, ExternalLink, Bookmark, BookmarkCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { useArticleDetail } from '../hooks/useArticles'
import { useBookmarkIds, useAddBookmark, useRemoveBookmark } from '../hooks/useBookmarks'
import { useAuthStore } from '../store/useAuthStore'
import CategoryBadge from '../components/ui/CategoryBadge'
import { formatDate } from '../utils/format'

export default function ArticleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isLoading, error } = useArticleDetail(Number(id))
  const article = data?.data

  const token = useAuthStore(s => s.token)
  const bookmarkIds = useBookmarkIds()
  const addBookmark = useAddBookmark()
  const removeBookmark = useRemoveBookmark()

  const isBookmarked = article ? bookmarkIds.has(article.id) : false

  async function handleBookmarkToggle() {
    if (!article) return
    try {
      if (isBookmarked) {
        await removeBookmark.mutateAsync(article.id)
        toast.success('Dihapus dari simpanan')
      } else {
        await addBookmark.mutateAsync(article.id)
        toast.success('Artikel disimpan')
      }
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Gagal mengubah bookmark')
    }
  }

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto px-4 md:px-8 py-10 animate-pulse">
        <div className="h-3 w-24 bg-ink3 mb-6" />
        <div className="h-6 w-32 bg-ink3 mb-3" />
        <div className="h-10 bg-ink3 w-full mb-2" />
        <div className="h-10 bg-ink3 w-3/4 mb-8" />
        <div className="aspect-video bg-ink2 mb-8" />
        <div className="space-y-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-3 bg-ink3 w-full" style={{ width: `${70 + (i % 3) * 10}%` }} />
          ))}
        </div>
      </div>
    )
  }

  if (error || !article) {
    return (
      <div className="max-w-3xl mx-auto px-4 md:px-8 py-20 text-center">
        <h2 className="font-display text-display-md text-paper mb-3">Artikel tidak ditemukan</h2>
        <p className="text-muted mb-6 font-body text-sm">Artikel mungkin telah dihapus atau URL tidak valid.</p>
        <Link to="/" className="text-accent hover:underline text-sm font-body">← Kembali ke beranda</Link>
      </div>
    )
  }

  const isScraped = article.isScraped ?? article.scraped
  const isBookmarkPending = addBookmark.isPending || removeBookmark.isPending

  return (
    <div className="max-w-3xl mx-auto px-4 md:px-8 py-10">
      {/* Back link */}
      <Link
        to="/"
        className="inline-flex items-center gap-1.5 text-muted hover:text-accent transition-colors text-xs font-body font-semibold uppercase tracking-wider mb-8 group"
      >
        <ChevronLeft size={14} className="group-hover:-translate-x-0.5 transition-transform" />
        Kembali
      </Link>

      {/* Metadata row */}
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <CategoryBadge category={article.category} size="md" />
        <span className="editorial-label text-muted">{article.sourceName}</span>
        {article.sourceWebsiteUrl && (
          <a
            href={article.sourceWebsiteUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="editorial-label text-muted hover:text-accent transition-colors"
          >
            ↗
          </a>
        )}
      </div>

      {/* Headline */}
      <h1 className="font-display text-display-lg md:text-display-xl font-black text-paper leading-tight mb-5">
        {article.title}
      </h1>

      {/* Byline + Bookmark button */}
      <div className="flex items-start justify-between gap-4 mb-8">
        <div className="flex items-center gap-4 text-muted flex-wrap">
          {article.author && (
            <div className="flex items-center gap-1.5">
              <User size={13} />
              <span className="text-xs font-body">{article.author}</span>
            </div>
          )}
          <div className="flex items-center gap-1.5">
            <Clock size={13} />
            <span className="text-xs font-body">{formatDate(article.publishedAt)}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Eye size={13} />
            <span className="text-xs font-body">{article.viewCount} tampilan</span>
          </div>
        </div>

        {/* Bookmark button — only when authenticated */}
        {token && (
          <button
            onClick={handleBookmarkToggle}
            disabled={isBookmarkPending}
            className={`
              flex items-center gap-1.5 text-xs font-body font-semibold uppercase tracking-wider
              border px-3 py-1.5 transition-colors flex-shrink-0 disabled:opacity-50
              ${isBookmarked
                ? 'border-accent text-accent hover:bg-accent hover:text-paper'
                : 'border-ink3 text-muted hover:border-paper hover:text-paper'
              }
            `}
          >
            {isBookmarked
              ? <><BookmarkCheck size={13} /> Tersimpan</>
              : <><Bookmark size={13} /> Simpan</>
            }
          </button>
        )}
      </div>

      {/* Hero thumbnail */}
      {article.thumbnailUrl && (
        <div className="mb-8 overflow-hidden">
          <img
            src={article.thumbnailUrl}
            alt={article.title}
            className="w-full max-h-96 object-cover"
            onError={e => {
              (e.currentTarget.parentElement as HTMLElement).style.display = 'none'
            }}
          />
        </div>
      )}

      {/* Red divider */}
      <div className="section-rule mb-8" />

      {/* Content */}
      {isScraped && article.content ? (
        <div className="font-body text-paper/90 text-base leading-relaxed space-y-4">
          {article.content.split('\n').filter(Boolean).map((para, i) => (
            <p key={i}>{para}</p>
          ))}
        </div>
      ) : (
        <div>
          {article.description && (
            <p className="font-body text-paper/80 text-base leading-relaxed mb-8 text-lg italic">
              {article.description}
            </p>
          )}
          <div className="border-l-2 border-accent pl-5 py-1 mb-8">
            <p className="text-muted text-sm font-body">
              Konten lengkap tersedia di sumber asli.
            </p>
          </div>
          <a
            href={article.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 bg-accent hover:bg-accent-dark text-paper px-6 py-3 text-sm font-body font-semibold uppercase tracking-wider transition-colors"
          >
            <ExternalLink size={15} />
            Baca di Sumber Asli
          </a>
        </div>
      )}

      {/* Bottom back link */}
      <div className="thin-rule mt-12 pt-6">
        <Link
          to="/"
          className="text-muted hover:text-accent transition-colors text-xs font-body font-semibold uppercase tracking-wider"
        >
          ← Kembali ke beranda
        </Link>
      </div>
    </div>
  )
}
