import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Trash2, Send } from 'lucide-react'
import toast from 'react-hot-toast'
import { getCacheStats, evictArticleCache, evictSourceCache, evictAllCache } from '../api/cache'
import { getDigestStats, triggerDigestAll } from '../api/digest'
import { getCrawlStats } from '../api/crawler'

function StatCard({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="border border-ink3 p-5 hover:border-accent/50 transition-colors">
      <p className="editorial-label text-muted mb-2">{label}</p>
      <p className="font-display text-display-lg font-black text-paper">{value}</p>
      {sub && <p className="text-muted text-xs font-body mt-1">{sub}</p>}
    </div>
  )
}

export default function MonitoringPage() {
  const qc = useQueryClient()
  const REFETCH = 30_000

  const { data: crawlData } = useQuery({
    queryKey: ['crawler', 'stats'],
    queryFn: getCrawlStats,
    refetchInterval: REFETCH,
  })

  const { data: cacheData, isLoading: cacheLoading } = useQuery({
    queryKey: ['cache', 'stats'],
    queryFn: getCacheStats,
    refetchInterval: REFETCH,
  })

  const { data: digestData } = useQuery({
    queryKey: ['digest', 'stats'],
    queryFn: getDigestStats,
    refetchInterval: REFETCH,
  })

  const evictArticle = useMutation({
    mutationFn: evictArticleCache,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cache', 'stats'] })
      toast.success('Cache artikel dihapus')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const evictSource = useMutation({
    mutationFn: evictSourceCache,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cache', 'stats'] })
      toast.success('Cache sumber dihapus')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const evictAll = useMutation({
    mutationFn: evictAllCache,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cache', 'stats'] })
      toast.success('Semua cache dihapus')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const triggerDigest = useMutation({
    mutationFn: triggerDigestAll,
    onSuccess: (res) => toast.success(res.message || 'Digest dimulai'),
    onError: (e: Error) => toast.error(e.message),
  })

  const crawlStats = crawlData?.data
  const cacheStats = cacheData?.data
  const digestStats = digestData?.data

  const cacheEntries = cacheStats ? Object.entries(cacheStats) : []

  function getCacheEvictFn(key: string) {
    if (key.toLowerCase().includes('article') || key.toLowerCase().includes('search')) {
      return () => evictArticle.mutate()
    }
    if (key.toLowerCase().includes('source') || key.toLowerCase().includes('categor')) {
      return () => evictSource.mutate()
    }
    return () => evictAll.mutate()
  }

  return (
    <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-10">
      {/* Header */}
      <div className="mb-10">
        <h1 className="font-display text-display-md font-black text-paper mb-1">Monitoring</h1>
        <p className="text-muted text-sm font-body">Auto-refresh setiap 30 detik</p>
      </div>

      {/* Stats row */}
      <section className="mb-10">
        <div className="section-rule mb-4">
          <h2 className="editorial-label mt-2">Statistik</h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <StatCard
            label="Artikel Disimpan Hari Ini"
            value={crawlStats?.articlesSavedToday ?? '—'}
          />
          <StatCard
            label="Total Source Aktif"
            value={crawlStats?.totalActiveSources ?? '—'}
          />
          <StatCard
            label="Digest Terkirim Hari Ini"
            value={digestStats?.sentToday ?? '—'}
            sub={digestStats ? `${digestStats.failedToday} gagal · ${digestStats.totalUsers} user` : undefined}
          />
        </div>
      </section>

      {/* Cache section */}
      <section className="mb-10">
        <div className="flex items-center justify-between mb-4 section-rule pt-3">
          <h2 className="editorial-label mt-2">Cache Redis</h2>
          <button
            onClick={() => evictAll.mutate()}
            disabled={evictAll.isPending}
            className="
              flex items-center gap-2 border border-accent text-accent hover:bg-accent hover:text-paper
              px-3 py-1.5 text-xs font-body font-semibold uppercase tracking-wider transition-colors
              disabled:opacity-40 -mt-0.5
            "
          >
            <Trash2 size={12} />
            Flush Semua Cache
          </button>
        </div>

        <div className="border border-ink3">
          <div className="section-rule" />
          {cacheLoading ? (
            <div className="py-8 text-center text-muted text-sm animate-pulse">Memuat…</div>
          ) : cacheEntries.length === 0 ? (
            <div className="py-8 text-center text-muted text-sm font-body">Cache kosong atau tidak tersedia.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ink3">
                  <th className="text-left py-3 px-4 editorial-label text-muted">Cache Name</th>
                  <th className="text-right py-3 px-4 editorial-label text-muted">Keys</th>
                  <th className="text-right py-3 px-4 editorial-label text-muted">Aksi</th>
                </tr>
              </thead>
              <tbody>
                {cacheEntries.map(([name, info]) => (
                  <tr key={name} className="border-b border-ink3 hover:bg-ink2/50 transition-colors">
                    <td className="py-3 px-4 font-mono text-paper/80 text-xs">{name}</td>
                    <td className="py-3 px-4 text-right">
                      <span className="text-paper font-semibold">{info.keyCount}</span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={getCacheEvictFn(name)}
                        className="text-muted hover:text-accent transition-colors"
                        title="Evict cache"
                      >
                        <Trash2 size={13} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      {/* Digest section */}
      <section>
        <div className="section-rule mb-4">
          <h2 className="editorial-label mt-2">Email Digest</h2>
        </div>
        <div className="border border-ink3 p-5 flex items-center justify-between">
          <div>
            <p className="text-paper font-body text-sm font-medium">Kirim Digest ke Semua Pengguna</p>
            <p className="text-muted text-xs font-body mt-0.5">
              Mengirim ringkasan berita ke seluruh subscriber yang aktif.
            </p>
          </div>
          <button
            onClick={() => triggerDigest.mutate()}
            disabled={triggerDigest.isPending}
            className="
              flex items-center gap-2 bg-accent hover:bg-accent-dark text-paper
              px-4 py-2.5 text-xs font-body font-semibold uppercase tracking-wider
              transition-colors disabled:opacity-40 flex-shrink-0 ml-4
            "
          >
            <Send size={13} />
            {triggerDigest.isPending ? 'Mengirim…' : 'Trigger Digest'}
          </button>
        </div>
      </section>

      {/* Manual refresh */}
      <div className="mt-8 flex justify-end">
        <button
          onClick={() => {
            qc.invalidateQueries({ queryKey: ['crawler', 'stats'] })
            qc.invalidateQueries({ queryKey: ['cache', 'stats'] })
            qc.invalidateQueries({ queryKey: ['digest', 'stats'] })
            toast.success('Data diperbarui')
          }}
          className="flex items-center gap-2 text-muted hover:text-paper transition-colors text-xs font-body font-semibold uppercase tracking-wider"
        >
          <RefreshCw size={13} />
          Refresh Manual
        </button>
      </div>
    </div>
  )
}
