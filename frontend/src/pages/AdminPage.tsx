import { useState } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAllSources } from '../hooks/useSources'
import { useDeleteSource, useToggleSource } from '../hooks/useSources'
import { useCrawlAll, useCrawlOne } from '../hooks/useCrawler'
import { useAppStore } from '../store/useAppStore'
import SourceTable from '../components/sources/SourceTable'
import SourceFormModal from '../components/sources/SourceFormModal'
import DeleteConfirmModal from '../components/sources/DeleteConfirmModal'

export default function AdminPage() {
  const { data, isLoading } = useAllSources()
  const sources = data?.data ?? []

  const {
    sourceModalOpen, editingSourceId, openSourceModal, closeSourceModal,
    deleteConfirmId, openDeleteConfirm, closeDeleteConfirm,
  } = useAppStore()

  const deleteMutation = useDeleteSource()
  const toggleMutation = useToggleSource()
  const crawlOneMutation = useCrawlOne()
  const crawlAllMutation = useCrawlAll()

  const [crawlingId, setCrawlingId] = useState<number | null>(null)
  const [togglingId, setTogglingId] = useState<number | null>(null)

  const deletingSource = sources.find(s => s.id === deleteConfirmId)

  async function handleDelete() {
    if (!deleteConfirmId) return
    try {
      await deleteMutation.mutateAsync(deleteConfirmId)
      toast.success('Sumber berhasil dihapus')
      closeDeleteConfirm()
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Gagal menghapus')
    }
  }

  async function handleToggle(id: number) {
    setTogglingId(id)
    try {
      const res = await toggleMutation.mutateAsync(id)
      toast.success(res.data.isActive ? 'Sumber diaktifkan' : 'Sumber dinonaktifkan')
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Gagal mengubah status')
    } finally {
      setTogglingId(null)
    }
  }

  async function handleCrawlOne(id: number) {
    setCrawlingId(id)
    try {
      const res = await crawlOneMutation.mutateAsync(id)
      const result = res.data
      if (result.status === 'SUCCESS') {
        toast.success(`✓ ${result.articlesSaved} artikel baru dari ${result.sourceName} (${result.durationMs}ms)`)
      } else {
        toast.error(`Crawl gagal: ${result.errorMessage || 'Unknown error'}`)
      }
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Crawl gagal')
    } finally {
      setCrawlingId(null)
    }
  }

  async function handleCrawlAll() {
    try {
      const res = await crawlAllMutation.mutateAsync()
      toast.success(res.message || 'Crawl semua sumber dimulai')
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Gagal memulai crawl')
    }
  }

  return (
    <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-10">
      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="font-display text-display-md font-black text-paper">Kelola Sumber RSS</h1>
          <p className="text-muted text-sm font-body mt-1">
            {sources.length} sumber · {sources.filter(s => s.isActive).length} aktif
          </p>
        </div>

        <div className="flex items-center gap-3 flex-shrink-0 mt-1">
          <button
            onClick={handleCrawlAll}
            disabled={crawlAllMutation.isPending}
            className="
              flex items-center gap-2 border border-ink3 text-muted hover:text-paper hover:border-paper
              px-4 py-2.5 text-xs font-body font-semibold uppercase tracking-wider transition-colors
              disabled:opacity-40
            "
          >
            <RefreshCw size={14} className={crawlAllMutation.isPending ? 'animate-spin' : ''} />
            Crawl Semua
          </button>

          <button
            onClick={() => openSourceModal()}
            className="
              flex items-center gap-2 bg-accent hover:bg-accent-dark text-paper
              px-4 py-2.5 text-xs font-body font-semibold uppercase tracking-wider transition-colors
            "
          >
            <Plus size={14} />
            Tambah Sumber
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="border border-ink3">
        {/* Section rule header */}
        <div className="section-rule" />
        {isLoading ? (
          <div className="py-12 text-center text-muted text-sm font-body animate-pulse">
            Memuat sumber…
          </div>
        ) : (
          <SourceTable
            sources={sources}
            onEdit={openSourceModal}
            onDelete={openDeleteConfirm}
            onToggle={handleToggle}
            onCrawl={handleCrawlOne}
            crawlingId={crawlingId}
            togglingId={togglingId}
          />
        )}
      </div>

      {/* Modals */}
      <SourceFormModal
        open={sourceModalOpen}
        onClose={closeSourceModal}
        sourceId={editingSourceId}
      />

      <DeleteConfirmModal
        open={deleteConfirmId !== null}
        sourceName={deletingSource?.name}
        onConfirm={handleDelete}
        onCancel={closeDeleteConfirm}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}
