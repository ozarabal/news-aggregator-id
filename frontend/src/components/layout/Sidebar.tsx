import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getAllCategories } from '../../api/sources'

export default function Sidebar() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const activeCategory = searchParams.get('category')

  const { data } = useQuery({
    queryKey: ['sources', 'categories'],
    queryFn: getAllCategories,
    staleTime: 1000 * 60 * 5,
  })

  const categories = data?.data ?? []

  function handleCategory(cat: string | null) {
    if (cat) {
      navigate(`/?category=${encodeURIComponent(cat)}`)
    } else {
      navigate('/')
    }
  }

  return (
    <aside className="w-full">
      {/* Section header */}
      <div className="section-rule pt-3 mb-4">
        <h3 className="editorial-label mt-2">Kategori</h3>
      </div>

      {/* Category list */}
      <ul className="space-y-0">
        <li>
          <button
            onClick={() => handleCategory(null)}
            className={`
              w-full text-left px-0 py-2.5 text-sm font-body border-b border-ink3 transition-colors
              flex items-center gap-2
              ${!activeCategory
                ? 'text-paper font-semibold'
                : 'text-muted hover:text-paper'
              }
            `}
          >
            {!activeCategory && (
              <span className="w-1 h-4 bg-accent flex-shrink-0" />
            )}
            Semua Berita
          </button>
        </li>
        {categories.map((cat) => (
          <li key={cat}>
            <button
              onClick={() => handleCategory(cat)}
              className={`
                w-full text-left px-0 py-2.5 text-sm font-body border-b border-ink3 transition-colors
                flex items-center gap-2 capitalize
                ${activeCategory === cat
                  ? 'text-paper font-semibold'
                  : 'text-muted hover:text-paper'
                }
              `}
            >
              {activeCategory === cat && (
                <span className="w-1 h-4 bg-accent flex-shrink-0" />
              )}
              {cat}
            </button>
          </li>
        ))}
      </ul>

      {/* Thin divider */}
      <div className="mt-6 pt-4 border-t border-ink3">
        <p className="text-xs text-muted font-body leading-relaxed">
          Berita dikumpulkan otomatis dari berbagai sumber RSS terpercaya Indonesia.
        </p>
      </div>
    </aside>
  )
}
