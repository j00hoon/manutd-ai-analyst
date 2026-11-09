import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Sidebar } from './Sidebar'

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-canvas text-slate-100">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="min-h-screen lg:pl-64">
        <Header onOpenMenu={() => setSidebarOpen(true)} />
        <main className="mx-auto w-full max-w-[1600px] px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
