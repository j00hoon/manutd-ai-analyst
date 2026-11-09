import { Menu } from 'lucide-react'
import { useLocation } from 'react-router-dom'

interface HeaderProps {
  onOpenMenu: () => void
}

const pageCopy: Record<string, { title: string; description: string }> = {
  '/': {
    title: 'Performance overview',
    description: 'Manchester United match analysis',
  },
  '/matches': {
    title: 'Match centre',
    description: 'Latest completed fixtures',
  },
  '/players': {
    title: 'Players',
    description: 'Squad analysis workspace',
  },
}

export function Header({ onOpenMenu }: HeaderProps) {
  const { pathname } = useLocation()
  const copy = pageCopy[pathname] ?? pageCopy['/']

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-canvas/85 backdrop-blur-xl">
      <div className="flex h-20 items-center gap-4 px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={onOpenMenu}
          className="rounded-xl border border-line bg-panel p-2.5 text-slate-300 hover:text-white lg:hidden"
          aria-label="Open navigation"
        >
          <Menu size={20} />
        </button>
        <div>
          <h1 className="text-lg font-semibold tracking-tight text-white sm:text-xl">{copy.title}</h1>
          <p className="mt-0.5 hidden text-sm text-slate-500 sm:block">{copy.description}</p>
        </div>
        <div className="ml-auto flex items-center gap-2 rounded-full border border-line bg-panel px-3 py-2 text-xs font-medium text-slate-400">
          <span className="size-2 rounded-full bg-united-red" aria-hidden="true" />
          Backend source
        </div>
      </div>
    </header>
  )
}
