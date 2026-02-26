import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { useCreateSource, useUpdateSource } from '../../hooks/useSources'
import { getSourceById } from '../../api/sources'
import type { SourceRequest } from '../../types'
import toast from 'react-hot-toast'

interface Props {
  open: boolean
  onClose: () => void
  sourceId: number | null
}

const EMPTY_FORM: SourceRequest = { name: '', url: '', websiteUrl: '', category: '', isActive: true }

export default function SourceFormModal({ open, onClose, sourceId }: Props) {
  const [form, setForm] = useState<SourceRequest>(EMPTY_FORM)
  const [loading, setLoading] = useState(false)
  const createMutation = useCreateSource()
  const updateMutation = useUpdateSource()

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      return
    }
    if (sourceId) {
      setLoading(true)
      getSourceById(sourceId)
        .then(res => {
          const s = res.data
          setForm({ name: s.name, url: s.url, websiteUrl: s.websiteUrl ?? '', category: s.category, isActive: s.isActive })
        })
        .catch(() => toast.error('Gagal memuat data sumber'))
        .finally(() => setLoading(false))
    }
  }, [open, sourceId])

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const { name, value, type, checked } = e.target
    setForm(f => ({ ...f, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const data: SourceRequest = {
      name: form.name,
      url: form.url,
      websiteUrl: form.websiteUrl || undefined,
      category: form.category.toLowerCase(),
      isActive: form.isActive,
    }
    try {
      if (sourceId) {
        await updateMutation.mutateAsync({ id: sourceId, data })
        toast.success('Sumber berhasil diperbarui')
      } else {
        await createMutation.mutateAsync(data)
        toast.success('Sumber berhasil ditambahkan')
      }
      onClose()
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Terjadi kesalahan')
    }
  }

  if (!open) return null

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/70" onClick={onClose} />

      {/* Modal */}
      <div className="relative bg-ink2 border border-ink3 w-full max-w-md shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-ink3">
          <h2 className="font-display text-display-sm font-bold text-paper">
            {sourceId ? 'Edit Sumber RSS' : 'Tambah Sumber RSS'}
          </h2>
          <button onClick={onClose} className="text-muted hover:text-paper transition-colors">
            <X size={20} />
          </button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-muted">Memuat…</div>
        ) : (
          <form onSubmit={handleSubmit} className="p-5 space-y-4">
            <div>
              <label className="editorial-label block mb-1.5">Nama Sumber *</label>
              <input
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                placeholder="CNN Indonesia - Teknologi"
                className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
              />
            </div>

            <div>
              <label className="editorial-label block mb-1.5">URL RSS Feed *</label>
              <input
                name="url"
                value={form.url}
                onChange={handleChange}
                required
                placeholder="https://example.com/rss"
                className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
              />
            </div>

            <div>
              <label className="editorial-label block mb-1.5">URL Website (opsional)</label>
              <input
                name="websiteUrl"
                value={form.websiteUrl}
                onChange={handleChange}
                placeholder="https://example.com"
                className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
              />
            </div>

            <div>
              <label className="editorial-label block mb-1.5">Kategori *</label>
              <input
                name="category"
                value={form.category}
                onChange={handleChange}
                required
                placeholder="teknologi"
                className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
              />
              <p className="text-muted text-[0.65rem] mt-1">Huruf kecil, tanpa spasi (contoh: teknologi, bisnis)</p>
            </div>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="isActive"
                checked={form.isActive}
                onChange={handleChange}
                className="w-4 h-4 accent-accent"
              />
              <span className="editorial-label">Aktif</span>
            </label>

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex-1 bg-accent hover:bg-accent-dark text-paper py-2.5 text-sm font-body font-semibold uppercase tracking-wider transition-colors disabled:opacity-50"
              >
                {isSubmitting ? 'Menyimpan…' : sourceId ? 'Perbarui' : 'Tambah'}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="px-5 border border-ink3 text-muted hover:text-paper hover:border-paper text-sm font-body font-semibold uppercase tracking-wider transition-colors"
              >
                Batal
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
