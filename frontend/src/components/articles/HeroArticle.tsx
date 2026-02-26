import { Link } from 'react-router-dom'
import CategoryBadge from '../ui/CategoryBadge'
import { formatDate } from '../../utils/format'
import type { ArticleSummary } from '../../types'
import { Eye } from 'lucide-react'

interface Props {
  article: ArticleSummary
}

export default function HeroArticle({ article }: Props) {
  return (
    <Link
      to={`/artikel/${article.id}`}
      className="group block relative overflow-hidden"
    >
      {/* Thumbnail */}
      <div className="relative aspect-[16/9] overflow-hidden bg-ink2">
        {article.thumbnailUrl ? (
          <img
            src={article.thumbnailUrl}
            alt={article.title}
            className="w-full h-full object-cover group-hover:brightness-90 transition-all duration-500 group-hover:scale-[1.02]"
            onError={e => {
              (e.target as HTMLImageElement).style.display = 'none'
            }}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-ink2">
            <span className="font-display text-5xl font-black text-ink3 uppercase">
              {article.category.charAt(0)}
            </span>
          </div>
        )}

        {/* Category badge overlay */}
        <div className="absolute top-4 left-4">
          <CategoryBadge category={article.category} size="md" />
        </div>

        {/* Gradient overlay at bottom for text legibility */}
        <div className="absolute inset-0 bg-gradient-to-t from-ink/90 via-ink/30 to-transparent" />

        {/* Text overlaid on image */}
        <div className="absolute bottom-0 left-0 right-0 p-5 md:p-7">
          {/* Source + date */}
          <div className="flex items-center gap-3 mb-3">
            <span className="editorial-label text-paper/60">{article.sourceName}</span>
            <span className="text-paper/40 text-[0.6rem]">·</span>
            <span className="editorial-label text-paper/60">{formatDate(article.publishedAt)}</span>
          </div>

          {/* Headline */}
          <h2 className="font-display text-display-lg md:text-display-xl font-black text-paper leading-tight
            group-hover:text-accent transition-colors duration-200 line-clamp-3 md:line-clamp-2">
            {article.title}
          </h2>

          {/* Description */}
          {article.description && (
            <p className="mt-3 text-paper/70 font-body text-sm leading-relaxed line-clamp-2 hidden md:block">
              {article.description}
            </p>
          )}

          {/* View count */}
          <div className="mt-3 flex items-center gap-1 text-paper/40">
            <Eye size={12} />
            <span className="text-[0.65rem] font-body">{article.viewCount} tampilan</span>
          </div>
        </div>
      </div>
    </Link>
  )
}
