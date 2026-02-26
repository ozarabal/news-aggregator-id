import axios from 'axios'
import { useAuthStore } from '../store/useAuthStore'

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// Attach JWT token to every request
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth()
      // Redirect to login unless already on an auth page
      const { pathname } = window.location
      if (!pathname.startsWith('/login') && !pathname.startsWith('/daftar')) {
        window.location.href = '/login'
      }
    }
    const message = error.response?.data?.message || 'Terjadi kesalahan'
    return Promise.reject(new Error(message))
  }
)
