import { X, AlertTriangle } from 'lucide-react'

interface Props {
  open: boolean
  sourceName?: string
  onConfirm: () => void
  onCancel: () => void
  loading?: boolean
}

export default function DeleteConfirmModal({ open, sourceName, onConfirm, onCancel, loading }: Props) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70" onClick={onCancel} />
      <div className="relative bg-ink2 border border-ink3 w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between p-5 border-b border-ink3">
          <div className="flex items-center gap-2">
            <AlertTriangle size={18} className="text-accent" />
            <h2 className="font-display text-display-sm font-bold text-paper">Hapus Sumber</h2>
          </div>
          <button onClick={onCancel} className="text-muted hover:text-paper transition-colors">
            <X size={18} />
          </button>
        </div>
        <div className="p-5">
          <p className="text-sm text-muted font-body leading-relaxed mb-5">
            Yakin ingin menghapus{' '}
            <span className="text-paper font-semibold">{sourceName ?? 'sumber ini'}</span>?
            {' '}Semua artikel dari sumber ini juga akan terhapus.
          </p>
          <div className="flex gap-3">
            <button
              onClick={onConfirm}
              disabled={loading}
              className="flex-1 bg-accent hover:bg-accent-dark text-paper py-2.5 text-sm font-body font-semibold uppercase tracking-wider transition-colors disabled:opacity-50"
            >
              {loading ? 'Menghapus…' : 'Ya, Hapus'}
            </button>
            <button
              onClick={onCancel}
              disabled={loading}
              className="px-5 border border-ink3 text-muted hover:text-paper hover:border-paper text-sm font-body font-semibold uppercase tracking-wider transition-colors"
            >
              Batal
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
