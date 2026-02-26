import { Outlet } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import Navbar from './Navbar'

export default function Layout() {
  return (
    <div className="min-h-screen bg-ink text-paper">
      <Navbar />
      <main>
        <Outlet />
      </main>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#1a1a1a',
            color: '#f5f0e8',
            border: '1px solid #2a2a2a',
            borderRadius: '0',
            fontFamily: 'Inter, sans-serif',
            fontSize: '0.875rem',
          },
          success: {
            iconTheme: { primary: '#10b981', secondary: '#1a1a1a' },
          },
          error: {
            iconTheme: { primary: '#c41e3a', secondary: '#1a1a1a' },
          },
        }}
      />
    </div>
  )
}
