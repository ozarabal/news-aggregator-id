import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useLogin } from '../hooks/useAuth'
import { useAuthStore } from '../store/useAuthStore'
import type { UserProfile } from '../types'

export default function LoginPage() {
  const navigate = useNavigate()
  const { token, setAuth } = useAuthStore()
  const loginMutation = useLogin()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // Already logged in → redirect to home
  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    try {
      const res = await loginMutation.mutateAsync({ email, password })
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
      toast.success(`Selamat datang, ${data.fullName}!`)
      navigate('/')
    } catch (err: unknown) {
      toast.error((err as Error).message || 'Login gagal')
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="section-rule mb-6" />
          <h1 className="font-display text-display-md font-black text-paper">Masuk</h1>
          <p className="text-muted text-sm font-body mt-2">
            Akses berita tersimpan & preferensi Anda
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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

          <div>
            <label className="editorial-label block mb-1.5">Password</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              placeholder="Minimal 8 karakter"
              className="w-full bg-ink border border-ink3 text-paper text-sm px-3 py-2.5 focus:outline-none focus:border-accent placeholder-muted"
            />
          </div>

          <button
            type="submit"
            disabled={loginMutation.isPending}
            className="w-full bg-accent hover:bg-accent-dark text-paper py-2.5 text-sm font-body font-semibold uppercase tracking-wider transition-colors disabled:opacity-50 mt-2"
          >
            {loginMutation.isPending ? 'Masuk…' : 'Masuk'}
          </button>
        </form>

        <div className="thin-rule my-6" />

        <p className="text-center text-sm text-muted font-body">
          Belum punya akun?{' '}
          <Link to="/daftar" className="text-paper hover:text-accent transition-colors underline underline-offset-2">
            Daftar sekarang
          </Link>
        </p>
      </div>
    </div>
  )
}
