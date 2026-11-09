import { LayoutDashboard, Shield, Trophy, Users, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'

interface SidebarProps {
  open: boolean
  onClose: () => void
}

const navigation = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard, end: true },
  { label: 'Matches', to: '/matches', icon: Trophy, end: false },
  { label: 'Players', to: '/players', icon: Users, end: false },
]

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      {open && (
        <button
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
          type="button"
          aria-label="Close navigation"
          onClick={onClose}
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-line bg-panel transition-transform duration-200 lg:visible lg:translate-x-0 ${open ? 'visible translate-x-0' : 'invisible -translate-x-full'}`}
        aria-label="Primary navigation"
      >
        <div className="flex h-20 items-center gap-3 border-b border-line px-5">
          <div className="grid size-10 place-items-center rounded-xl bg-united-red text-white shadow-lg shadow-red-950/40">
            <Shield size={21} aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-bold tracking-wide text-white">UNITED ANALYST</p>
            <p className="text-xs text-slate-500">Football intelligence</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="ml-auto rounded-lg p-2 text-slate-400 hover:bg-white/5 hover:text-white lg:hidden"
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 px-3 py-6">
          <p className="mb-3 px-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-600">
            Analysis
          </p>
          <ul className="space-y-1">
            {navigation.map(({ label, to, icon: Icon, end }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={end}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-united-red/12 text-white'
                        : 'text-slate-400 hover:bg-white/5 hover:text-slate-100'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <span
                        className={`grid size-8 place-items-center rounded-lg ${isActive ? 'bg-united-red text-white' : 'bg-white/4'}`}
                      >
                        <Icon size={17} aria-hidden="true" />
                      </span>
                      {label}
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="border-t border-line p-5">
          <div className="rounded-xl border border-line bg-canvas/60 p-3">
            <div className="mb-1.5 flex items-center gap-2 text-xs font-semibold text-slate-300">
              <span className="size-2 rounded-full bg-emerald-400" aria-hidden="true" />
              Phase 1
            </div>
            <p className="text-xs leading-5 text-slate-500">Match data dashboard</p>
          </div>
        </div>
      </aside>
    </>
  )
}
