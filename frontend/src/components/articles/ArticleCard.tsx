import { Link } from 'react-router-dom'
import CategoryBadge from '../ui/CategoryBadge'
import { formatDate } from '../../utils/format'
import type { ArticleSummary } from '../../types'
import { Eye } from 'lucide-react'

interface Props {
  article: ArticleSummary
  size?: 'sm' | 'md'
}

export default function ArticleCard({ article, size = 'md' }: Props) {
  if (size === 'sm') {
    return (
      <Link
        to={`/artikel/${article.id}`}
        className="group flex gap-3 py-3 border-b border-ink3 last:border-0 hover:border-accent transition-colors"
      >
        {/* Thumbnail */}
        <div className="flex-shrink-0 w-20 h-14 bg-ink2 overflow-hidden">
          {article.thumbnailUrl ? (
            <img
              src={article.thumbnailUrl}
              alt={article.title}
              className="w-full h-full object-cover group-hover:brightness-90 transition-all"
              onError={e => {
                (e.target as HTMLImageElement).style.display = 'none'
              }}
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-ink2">
              <span className="font-display text-xl font-black text-ink3 uppercase">
                {article.category.charAt(0)}
              </span>
            </div>
          )}
        </div>

        {/* Text */}
        <div className="flex-1 min-w-0">
          <h3 className="font-display text-sm font-bold text-paper group-hover:text-accent transition-colors line-clamp-2 leading-snug mb-1.5">
            {article.title}
          </h3>
          <div className="flex items-center gap-2">
            <span className="editorial-label text-muted">{article.sourceName}</span>
            <span className="text-muted text-[0.6rem]">·</span>
            <span className="editorial-label text-muted">{formatDate(article.publishedAt)}</span>
          </div>
        </div>
      </Link>
    )
  }

  // MD (grid) variant
  return (
    <Link
      to={`/artikel/${article.id}`}
      className="group block border border-ink3 hover:border-accent/50 transition-colors overflow-hidden"
    >
      {/* Thumbnail */}
      <div className="aspect-video bg-ink2 overflow-hidden">
        {article.thumbnailUrl ? (
          <img
            src={article.thumbnailUrl}
            alt={article.title}
            className="w-full h-full object-cover group-hover:brightness-90 group-hover:scale-[1.03] transition-all duration-400"
            onError={e => {
              (e.target as HTMLImageElement).style.display = 'none'
            }}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-ink2">
            <span className="font-display text-3xl font-black text-ink3 uppercase">
              {article.category.charAt(0)}
            </span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="p-4">
        <div className="flex items-center justify-between mb-2">
          <CategoryBadge category={article.category} />
          <div className="flex items-center gap-1 text-muted">
            <Eye size={10} />
            <span className="text-[0.6rem] font-body">{article.viewCount}</span>
          </div>
        </div>

        <h3 className="font-display text-display-sm font-bold text-paper group-hover:text-accent transition-colors line-clamp-2 leading-snug mb-2">
          {article.title}
        </h3>

        {article.description && (
          <p className="text-muted text-xs font-body leading-relaxed line-clamp-2 mb-3">
            {article.description}
          </p>
        )}

        <div className="flex items-center gap-2 pt-2 border-t border-ink3">
          <span className="editorial-label text-muted">{article.sourceName}</span>
          <span className="text-muted text-[0.6rem]">·</span>
          <span className="editorial-label text-muted">{formatDate(article.publishedAt)}</span>
        </div>
      </div>
    </Link>
  )
}
