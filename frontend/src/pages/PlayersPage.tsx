import { Construction, Users } from 'lucide-react'

export function PlayersPage() {
  return (
    <div className="grid min-h-[60vh] place-items-center">
      <section className="w-full max-w-xl rounded-3xl border border-line bg-panel px-6 py-14 text-center sm:px-10">
        <div className="mx-auto mb-6 grid size-16 place-items-center rounded-2xl bg-united-red/10 text-united-red">
          <Users size={30} aria-hidden="true" />
        </div>
        <div className="mx-auto mb-4 inline-flex items-center gap-2 rounded-full border border-line bg-canvas px-3 py-1.5 text-xs font-semibold text-slate-400">
          <Construction size={14} aria-hidden="true" />
          Planned workspace
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-white">Player analysis is coming later</h2>
        <p className="mt-4 text-sm leading-7 text-slate-400">
          No player profiles or statistics are connected in Phase 1. This page is intentionally a placeholder until a verified data source is available.
        </p>
      </section>
    </div>
  )
}
