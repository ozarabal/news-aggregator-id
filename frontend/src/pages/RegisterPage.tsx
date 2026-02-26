import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useRegister } from '../hooks/useAuth'
import { useAuthStore } from '../store/useAuthStore'
import { useCategories } from '../hooks/useSources'
import type { UserProfile } from '../types'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { token, setAuth } = useAuthStore()
  const registerMutation = useRegister()
  const { data: categoriesData } = useCategories()
  const availableCategories = categoriesData?.data ?? []

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [digestEnabled, setDigestEnabled] = useState(false)
  const [digestFrequency, setDigestFrequency] = useState<'DAILY' | 'WEEKLY'>('DAILY')

  // Already logged in → redirect to home
  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

  function toggleCategory(cat: string) {
    setSelectedCategories(prev =>
      prev.includes(cat) ? prev.filter(c => c !== cat) : [...prev, cat]
    )
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    try {
      const res = await registerMutation.mutateAsync({
        email,
        password,
        fullName,
        categories: selectedCategories,
        digestEnabled,
        digestFrequency: digestEnabled ? digestFrequency : undefined,
      })
      const data = res.data
      const user: UserProfile = {
        id: data.userId,
        email: data.email,
        fullName: data.fullName,
        digestEnabled: data.digestEnabled,
        digestFrequency: data.digestFrequency,
        categories: data.categories,
        role: (data.role as 'ADMIN' | 'USER') ?? 'USER',
      }
      setAuth(data.token, user)
      toast.success('Akun berhasil dibuat!')
      navigate('/')
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Registrasi gagal')
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="section-rule mb-6" />
          <h1 className="font-display text-display-md font-black text-paper">Buat Akun</h1>
          <p className="text-muted text-sm font-body mt-2">
            Simpan artikel & terima ringkasan berita pilihan Anda
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Nama Lengkap */}
          <div>
            <label className="editorial-label block mb-1.5">Nama Lengkap</label>
            <input
              type="text"
              value={fullName}
              onChange={e => setFullName(e.target.value)}
              required
              placeholder="Nama Anda"
              className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
            />
          </div>

          {/* Email */}
          <div>
            <label className="editorial-label block mb-1.5">Email</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              placeholder="nama@email.com"
              className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
            />
          </div>

          {/* Password */}
          <div>
            <label className="editorial-label block mb-1.5">Password</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              minLength={8}
              placeholder="Minimal 8 karakter"
              className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
            />
          </div>

          {/* Category Preferences */}
          <div>
            <label className="editorial-label block mb-2">
              Kategori Berita Favorit
              <span className="text-muted normal-case font-normal text-[0.65rem] ml-2">(opsional)</span>
            </label>
            {availableCategories.length === 0 ? (
              <p className="text-muted text-xs font-body">Memuat kategori…</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {availableCategories.map(cat => {
                  const selected = selectedCategories.includes(cat)
                  return (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => toggleCategory(cat)}
                      className={`
                        px-3 py-1 text-xs font-body font-semibold uppercase tracking-wider border transition-colors
                        ${selected
                          ? 'bg-accent border-accent text-paper'
                          : 'bg-transparent border-ink3 text-muted hover:border-paper hover:text-paper'
                        }
                      `}
                    >
                      {cat}
                    </button>
                  )
                })}
              </div>
            )}
          </div>

          {/* Digest Toggle */}
          <div className="border border-ink3 p-4 space-y-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={digestEnabled}
                onChange={e => setDigestEnabled(e.target.checked)}
                className="w-4 h-4 accent-accent"
              />
              <div>
                <span className="editorial-label">Terima ringkasan berita lewat email</span>
                <p className="text-muted text-[0.65rem] font-body mt-0.5">
                  Artikel terpopuler dari kategori pilihan Anda
                </p>
              </div>
            </label>

            {digestEnabled && (
              <div className="pl-7 space-y-2">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="digestFrequency"
                    value="DAILY"
                    checked={digestFrequency === 'DAILY'}
                    onChange={() => setDigestFrequency('DAILY')}
                    className="accent-accent"
                  />
                  <span className="text-sm font-body text-paper">Setiap hari, jam 7 pagi</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="digestFrequency"
                    value="WEEKLY"
                    checked={digestFrequency === 'WEEKLY'}
                    onChange={() => setDigestFrequency('WEEKLY')}
                    className="accent-accent"
                  />
                  <span className="text-sm font-body text-paper">Setiap Senin pagi</span>
                </label>
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={registerMutation.isPending}
            className="w-full bg-accent hover:bg-accent-dark text-paper py-2.5 text-sm font-body font-semibold uppercase tracking-wider transition-colors disabled:opacity-50"
          >
            {registerMutation.isPending ? 'Membuat akun…' : 'Buat Akun'}
          </button>
        </form>

        <div className="thin-rule my-6" />

        <p className="text-center text-sm text-muted font-body">
          Sudah punya akun?{' '}
          <Link to="/login" className="text-paper hover:text-accent transition-colors underline underline-offset-2">
            Masuk
          </Link>
        </p>
      </div>
    </div>
  )
}
