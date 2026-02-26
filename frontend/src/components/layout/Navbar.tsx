import { useState, useRef, useEffect } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Search, X, Menu, LogOut, Bookmark } from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

export default function Navbar() {
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchValue, setSearchValue] = useState('')
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()

  const { token, user, clearAuth } = useAuthStore()

  useEffect(() => {
    if (searchOpen && searchInputRef.current) {
      searchInputRef.current.focus()
    }
  }, [searchOpen])

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (searchValue.trim()) {
      navigate(`/cari?q=${encodeURIComponent(searchValue.trim())}`)
      setSearchOpen(false)
      setSearchValue('')
    }
  }

  function handleLogout() {
    clearAuth()
    setMobileMenuOpen(false)
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-50 bg-ink border-b border-ink3">
      {/* Top strip — editorial header */}
      <div className="border-b border-ink3 py-1 px-4 md:px-8 flex items-center justify-between">
        <span className="editorial-label">Agregator Berita Indonesia</span>
        <span className="editorial-label opacity-50 hidden sm:block">
          {new Date().toLocaleDateString('id-ID', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </span>
      </div>

      {/* Main nav */}
      <div className="px-4 md:px-8 py-0">
        <div className="flex items-center justify-between h-14">
          {/* Logo */}
          <Link
            to="/"
            className="font-display text-2xl font-black text-paper tracking-tight flex items-center gap-1 hover:opacity-80 transition-opacity"
          >
            <span className="text-accent">■</span>
            <span>BERITA.ID</span>
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-6">
            <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              Beranda
            </NavLink>
            <NavLink to="/cari" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              Cari
            </NavLink>
            {token && (
              <NavLink to="/simpan" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Simpan
              </NavLink>
            )}
            {user?.role === 'ADMIN' && (
              <NavLink to="/admin" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Admin
              </NavLink>
            )}
            {user?.role === 'ADMIN' && (
              <NavLink to="/monitoring" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                Monitoring
              </NavLink>
            )}
          </nav>

          {/* Right side: Search + Auth + Mobile menu */}
          <div className="flex items-center gap-3">
            {/* Search toggle */}
            {searchOpen ? (
              <form onSubmit={handleSearchSubmit} className="flex items-center gap-2">
                <input
                  ref={searchInputRef}
                  value={searchValue}
                  onChange={e => setSearchValue(e.target.value)}
                  placeholder="Cari berita…"
                  className="bg-ink2 border border-ink3 text-paper text-sm px-3 py-1.5 w-48 md:w-64 focus:outline-none focus:border-accent placeholder-muted"
                />
                <button
                  type="button"
                  onClick={() => setSearchOpen(false)}
                  className="text-muted hover:text-paper transition-colors"
                >
                  <X size={16} />
                </button>
              </form>
            ) : (
              <button
                onClick={() => setSearchOpen(true)}
                className="text-muted hover:text-paper transition-colors p-1"
                aria-label="Cari"
              >
                <Search size={18} />
              </button>
            )}

            {/* Auth — desktop only */}
            {!searchOpen && (
              <div className="hidden md:flex items-center gap-3">
                {token && user ? (
                  <>
                    <span className="editorial-label opacity-60 max-w-[120px] truncate" title={user.fullName}>
                      {user.fullName}
                    </span>
                    <button
                      onClick={handleLogout}
                      className="flex items-center gap-1.5 text-muted hover:text-paper transition-colors text-xs font-body font-semibold uppercase tracking-wider"
                      aria-label="Keluar"
                    >
                      <LogOut size={14} />
                      Keluar
                    </button>
                  </>
                ) : (
                  <Link
                    to="/login"
                    className="border border-ink3 text-muted hover:text-paper hover:border-paper px-3 py-1.5 text-xs font-body font-semibold uppercase tracking-wider transition-colors"
                  >
                    Masuk
                  </Link>
                )}
              </div>
            )}

            {/* Mobile menu toggle */}
            <button
              onClick={() => setMobileMenuOpen(v => !v)}
              className="md:hidden text-muted hover:text-paper transition-colors p-1"
              aria-label="Menu"
            >
              {mobileMenuOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileMenuOpen && (
        <nav className="md:hidden border-t border-ink3 px-4 py-3 flex flex-col gap-3 bg-ink">
          <NavLink
            to="/"
            end
            onClick={() => setMobileMenuOpen(false)}
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            Beranda
          </NavLink>
          <NavLink
            to="/cari"
            onClick={() => setMobileMenuOpen(false)}
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            Cari
          </NavLink>
          {token && (
            <NavLink
              to="/simpan"
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <span className="flex items-center gap-1.5">
                <Bookmark size={13} />
                Simpan
              </span>
            </NavLink>
          )}
          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              Admin
            </NavLink>
          )}
          {user?.role === 'ADMIN' && (
            <NavLink
              to="/monitoring"
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              Monitoring
            </NavLink>
          )}

          {/* Auth links — mobile */}
          <div className="thin-rule" />
          {token && user ? (
            <>
              <span className="editorial-label opacity-60">{user.fullName}</span>
              <button
                onClick={handleLogout}
                className="nav-link text-left flex items-center gap-1.5"
              >
                <LogOut size={13} />
                Keluar
              </button>
            </>
          ) : (
            <Link
              to="/login"
              onClick={() => setMobileMenuOpen(false)}
              className="nav-link"
            >
              Masuk
            </Link>
          )}
        </nav>
      )}
    </header>
  )
}
