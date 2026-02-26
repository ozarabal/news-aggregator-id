import { useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useArticles } from '../hooks/useArticles'
import HeroArticle from '../components/articles/HeroArticle'
import ArticleCard from '../components/articles/ArticleCard'
import { HeroSkeleton, CardSkeleton, SidebarCardSkeleton } from '../components/articles/ArticleSkeleton'
import Pagination from '../components/ui/Pagination'
import Sidebar from '../components/layout/Sidebar'
import EmptyState from '../components/ui/EmptyState'

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const category = searchParams.get('category') || undefined
  const pageParam = parseInt(searchParams.get('page') || '0', 10)
  const [page, setPage] = useState(pageParam)

  const { data, isLoading } = useArticles({ page, size: 20, category })

  const articles = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0

  function handlePageChange(newPage: number) {
    setPage(newPage)
    const params = new URLSearchParams(searchParams)
    if (newPage === 0) {
      params.delete('page')
    } else {
      params.set('page', String(newPage))
    }
    setSearchParams(params)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Reset page when category changes
  const hero = articles[0]
  const sidebarArticles = articles.slice(1, 6)
  const gridArticles = articles.slice(6)

  return (
    <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-8">

      {/* Category indicator */}
      {category && (
        <div className="flex items-center gap-3 mb-6">
          <div className="section-rule flex-1" />
          <div className="flex items-center gap-2">
            <span className="editorial-label text-paper">Kategori:</span>
            <span className="editorial-label capitalize">{category}</span>
            <button
              onClick={() => {
                navigate('/')
                setPage(0)
              }}
              className="editorial-label text-muted hover:text-accent transition-colors ml-1"
            >
              ✕ Hapus
            </button>
          </div>
          <div className="section-rule flex-1" />
        </div>
      )}

      {/* Main editorial grid: Hero + Sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-0 mb-10">
        {/* Hero — 2/3 width */}
        <div className="lg:col-span-2 lg:border-r lg:border-ink3 lg:pr-8">
          {isLoading ? (
            <HeroSkeleton />
          ) : hero ? (
            <HeroArticle article={hero} />
          ) : (
            <EmptyState title="Belum ada berita" description="Tambahkan sumber RSS di halaman Admin untuk mulai mengumpulkan berita." />
          )}
        </div>

        {/* Sidebar — 1/3 width */}
        <div className="lg:pl-8 mt-8 lg:mt-0">
          {/* Categories nav */}
          <div className="mb-6">
            <Sidebar />
          </div>

          {/* Sidebar articles */}
          <div className="section-rule pt-3 mb-2">
            <h3 className="editorial-label mt-2">Berita Lainnya</h3>
          </div>
          {isLoading ? (
            <div>
              {Array.from({ length: 5 }).map((_, i) => <SidebarCardSkeleton key={i} />)}
            </div>
          ) : (
            <div>
              {sidebarArticles.map(a => (
                <ArticleCard key={a.id} article={a} size="sm" />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Section divider with editorial label */}
      {(gridArticles.length > 0 || isLoading) && (
        <div className="flex items-center gap-4 mb-8">
          <div className="section-rule flex-1" />
          <span className="font-display text-lg font-bold text-paper italic">Berita Terbaru</span>
          <div className="section-rule flex-1" />
        </div>
      )}

      {/* Main article grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 12 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : gridArticles.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {gridArticles.map((article, idx) => (
            <div
              key={article.id}
              className={
                // Every 9th item: intentional full-width grid break
                (idx + 1) % 9 === 0 ? 'sm:col-span-2 lg:col-span-1' : ''
              }
            >
              <ArticleCard article={article} size="md" />
            </div>
          ))}
        </div>
      ) : null}

      {/* Pagination */}
      <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
    </div>
  )
}
