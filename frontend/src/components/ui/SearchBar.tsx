import { useState } from 'react'
import { Search } from 'lucide-react'

interface Props {
  defaultValue?: string
  onSearch: (keyword: string) => void
  placeholder?: string
}

export default function SearchBar({ defaultValue = '', onSearch, placeholder = 'Cari berita…' }: Props) {
  const [value, setValue] = useState(defaultValue)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (value.trim()) onSearch(value.trim())
  }

  return (
    <form onSubmit={handleSubmit} className="flex items-stretch w-full">
      <input
        type="text"
        value={value}
        onChange={e => setValue(e.target.value)}
        placeholder={placeholder}
        className="
          flex-1 bg-ink2 border border-ink3 border-r-0 text-paper text-sm px-4 py-3
          focus:outline-none focus:border-accent placeholder-muted font-body
        "
      />
      <button
        type="submit"
        className="
          bg-accent hover:bg-accent-dark text-paper px-4 py-3
          flex items-center justify-center transition-colors
          border border-accent
        "
        aria-label="Cari"
      >
        <Search size={16} />
      </button>
    </form>
  )
}
