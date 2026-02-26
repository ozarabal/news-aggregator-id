import { ChevronLeft, ChevronRight } from 'lucide-react'

interface Props {
  page: number         // 0-indexed from backend
  totalPages: number
  onPageChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null

  const currentDisplay = page + 1
  const pages = getPageNumbers(currentDisplay, totalPages)

  return (
    <div className="flex items-center justify-center gap-1 py-8">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="
          flex items-center gap-1 px-3 py-2 text-xs font-body font-semibold uppercase tracking-wider
          border border-ink3 text-muted hover:border-accent hover:text-accent
          disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:border-ink3 disabled:hover:text-muted
          transition-colors
        "
      >
        <ChevronLeft size={14} />
        Prev
      </button>

      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`dot-${i}`} className="px-2 py-2 text-muted text-xs">…</span>
        ) : (
          <button
            key={p}
            onClick={() => onPageChange((p as number) - 1)}
            className={`
              w-9 h-9 text-xs font-body font-semibold border transition-colors
              ${currentDisplay === p
                ? 'bg-accent border-accent text-paper'
                : 'border-ink3 text-muted hover:border-accent hover:text-accent'
              }
            `}
          >
            {p}
          </button>
        )
      )}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="
          flex items-center gap-1 px-3 py-2 text-xs font-body font-semibold uppercase tracking-wider
          border border-ink3 text-muted hover:border-accent hover:text-accent
          disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:border-ink3 disabled:hover:text-muted
          transition-colors
        "
      >
        Next
        <ChevronRight size={14} />
      </button>
    </div>
  )
}

function getPageNumbers(current: number, total: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages: (number | string)[] = [1]
  if (current > 3) pages.push('...')
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
}
