import { Power, Edit2, Trash2, RefreshCw } from 'lucide-react'
import type { Source } from '../../types'
import { formatDateShort } from '../../utils/format'

interface Props {
  sources: Source[]
  onEdit: (id: number) => void
  onDelete: (id: number) => void
  onToggle: (id: number) => void
  onCrawl: (id: number) => void
  crawlingId: number | null
  togglingId: number | null
}

function CrawlStatusBadge({ status }: { status: Source['crawlStatus'] }) {
  if (!status) return <span className="text-muted text-xs">—</span>
  const styles: Record<string, string> = {
    SUCCESS: 'text-emerald-400 border-emerald-800 bg-emerald-900/30',
    ERROR: 'text-red-400 border-red-800 bg-red-900/30',
    PENDING: 'text-muted border-ink3 bg-ink3',
  }
  return (
    <span className={`text-[0.6rem] font-semibold uppercase tracking-wider px-1.5 py-0.5 border ${styles[status] ?? styles.PENDING}`}>
      {status}
    </span>
  )
}

export default function SourceTable({ sources, onEdit, onDelete, onToggle, onCrawl, crawlingId, togglingId }: Props) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b-2 border-accent">
            <th className="text-left py-3 px-4 editorial-label text-muted">Nama</th>
            <th className="text-left py-3 px-4 editorial-label text-muted hidden md:table-cell">Kategori</th>
            <th className="text-left py-3 px-4 editorial-label text-muted hidden lg:table-cell">Status Crawl</th>
            <th className="text-left py-3 px-4 editorial-label text-muted hidden lg:table-cell">Terakhir Crawl</th>
            <th className="text-left py-3 px-4 editorial-label text-muted">Aktif</th>
            <th className="text-right py-3 px-4 editorial-label text-muted">Aksi</th>
          </tr>
        </thead>
        <tbody>
          {sources.map(source => (
            <tr key={source.id} className="border-b border-ink3 hover:bg-ink2/50 transition-colors">
              <td className="py-3 px-4">
                <div>
                  <p className="font-body text-paper text-sm font-medium">{source.name}</p>
                  <p className="text-muted text-[0.65rem] mt-0.5 hidden sm:block truncate max-w-xs">
                    {source.url}
                  </p>
                </div>
              </td>
              <td className="py-3 px-4 hidden md:table-cell">
                <span className="capitalize text-muted text-xs font-body">{source.category}</span>
              </td>
              <td className="py-3 px-4 hidden lg:table-cell">
                <CrawlStatusBadge status={source.crawlStatus} />
              </td>
              <td className="py-3 px-4 hidden lg:table-cell">
                <span className="text-muted text-xs font-body">
                  {source.lastCrawledAt ? formatDateShort(source.lastCrawledAt) : '—'}
                </span>
              </td>
              <td className="py-3 px-4">
                <button
                  onClick={() => onToggle(source.id)}
                  disabled={togglingId === source.id}
                  className={`transition-colors ${
                    source.isActive ? 'text-emerald-400 hover:text-emerald-300' : 'text-muted hover:text-paper'
                  } disabled:opacity-40`}
                  title={source.isActive ? 'Nonaktifkan' : 'Aktifkan'}
                >
                  <Power size={15} />
                </button>
              </td>
              <td className="py-3 px-4">
                <div className="flex items-center justify-end gap-2">
                  <button
                    onClick={() => onCrawl(source.id)}
                    disabled={crawlingId === source.id}
                    className="text-muted hover:text-paper transition-colors disabled:opacity-40"
                    title="Crawl Manual"
                  >
                    <RefreshCw size={14} className={crawlingId === source.id ? 'animate-spin' : ''} />
                  </button>
                  <button
                    onClick={() => onEdit(source.id)}
                    className="text-muted hover:text-paper transition-colors"
                    title="Edit"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button
                    onClick={() => onDelete(source.id)}
                    className="text-muted hover:text-accent transition-colors"
                    title="Hapus"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {sources.length === 0 && (
        <div className="text-center py-12 text-muted text-sm font-body">Belum ada sumber RSS.</div>
      )}
    </div>
  )
}
