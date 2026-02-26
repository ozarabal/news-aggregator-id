import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Layout from './components/layout/Layout'
import AdminGuard from './components/auth/AdminGuard'
import HomePage from './pages/HomePage'
import ArticleDetailPage from './pages/ArticleDetailPage'
import SearchPage from './pages/SearchPage'
import AdminPage from './pages/AdminPage'
import MonitoringPage from './pages/MonitoringPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import BookmarkPage from './pages/BookmarkPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="artikel/:id" element={<ArticleDetailPage />} />
            <Route path="cari" element={<SearchPage />} />
            <Route element={<AdminGuard />}>
              <Route path="admin" element={<AdminPage />} />
              <Route path="monitoring" element={<MonitoringPage />} />
            </Route>
            <Route path="login" element={<LoginPage />} />
            <Route path="daftar" element={<RegisterPage />} />
            <Route path="simpan" element={<BookmarkPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
