import { useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useArticles } from '../hooks/useArticles'
import ArticleCard from '../components/articles/ArticleCard'
import { CardSkeleton } from '../components/articles/ArticleSkeleton'
import SearchBar from '../components/ui/SearchBar'
import Pagination from '../components/ui/Pagination'
import EmptyState from '../components/ui/EmptyState'

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const query = searchParams.get('q') || ''
  const [page, setPage] = useState(0)

  const { data, isLoading } = useArticles({
    page,
    size: 20,
    search: query || undefined,
  })

  const articles = data?.data?.content ?? []
  const totalElements = data?.data?.totalElements ?? 0
  const totalPages = data?.data?.totalPages ?? 0

  function handleSearch(keyword: string) {
    navigate(`/cari?q=${encodeURIComponent(keyword)}`)
    setPage(0)
  }

  function handlePageChange(newPage: number) {
    setPage(newPage)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="max-w-screen-xl mx-auto px-4 md:px-8 py-10">
      {/* Search header */}
      <div className="max-w-2xl mb-10">
        <h1 className="font-display text-display-md font-black text-paper mb-4">
          Cari Berita
        </h1>
        <SearchBar defaultValue={query} onSearch={handleSearch} />
      </div>

      {/* Results count */}
      {query && !isLoading && (
        <div className="flex items-center gap-4 mb-6">
          <div className="section-rule flex-1" />
          <p className="text-muted text-xs font-body shrink-0">
            {totalElements > 0
              ? `Ditemukan ${totalElements.toLocaleString('id-ID')} artikel untuk "${query}"`
              : `Tidak ada hasil untuk "${query}"`
            }
          </p>
          <div className="section-rule flex-1" />
        </div>
      )}

      {/* Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 9 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : articles.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {articles.map(a => (
            <ArticleCard key={a.id} article={a} size="md" />
          ))}
        </div>
      ) : query ? (
        <EmptyState
          title={`Tidak ada hasil`}
          description={`Tidak ditemukan artikel yang cocok dengan "${query}". Coba kata kunci lain.`}
        />
      ) : (
        <EmptyState
          title="Masukkan kata kunci"
          description="Ketik di kotak pencarian di atas untuk mencari berita."
        />
      )}

      {/* Pagination */}
      <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
    </div>
  )
}
