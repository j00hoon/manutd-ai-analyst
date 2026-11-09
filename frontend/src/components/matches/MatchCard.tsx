import { CalendarDays } from 'lucide-react'
import type { MatchOutcome, MatchResponse } from '../../types/match'
import { getMatchOutcome, isSyntheticMatch } from '../../types/match'

interface MatchCardProps {
  match: MatchResponse
}

const outcomeStyles: Record<MatchOutcome, string> = {
  W: 'border-emerald-700/60 bg-emerald-500/10 text-emerald-300',
  D: 'border-amber-700/60 bg-amber-500/10 text-amber-300',
  L: 'border-red-800/70 bg-red-500/10 text-red-300',
}

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function MatchCard({ match }: MatchCardProps) {
  const outcome = getMatchOutcome(match)
  const synthetic = isSyntheticMatch(match)
  const score =
    match.homeGoals === null || match.awayGoals === null
      ? '—'
      : `${match.homeGoals} – ${match.awayGoals}`

  return (
    <article className="group relative overflow-hidden rounded-2xl border border-line bg-panel p-4 transition-colors hover:border-slate-600/70 sm:p-5">
      <div className="absolute inset-y-0 left-0 w-1 bg-united-red opacity-0 transition-opacity group-hover:opacity-100" />
      <div className="mb-5 flex flex-wrap items-center gap-x-3 gap-y-2 text-xs text-slate-500">
        <span className="font-semibold uppercase tracking-wider text-slate-400">{match.competition}</span>
        <span className="hidden size-1 rounded-full bg-slate-700 sm:block" aria-hidden="true" />
        <span className="inline-flex items-center gap-1.5">
          <CalendarDays size={13} aria-hidden="true" />
          <time dateTime={match.kickoffTime}>{dateFormatter.format(new Date(match.kickoffTime))}</time>
        </span>
        {synthetic && (
          <span className="ml-auto rounded-full border border-amber-700/60 bg-amber-500/10 px-2.5 py-1 font-bold tracking-wider text-amber-300">
            DEMO DATA
          </span>
        )}
      </div>

      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 sm:gap-6">
        <p className="text-right text-sm font-semibold leading-5 text-slate-100 sm:text-base">{match.homeTeam}</p>
        <div className="min-w-20 rounded-xl border border-line bg-canvas px-3 py-2 text-center text-xl font-bold tracking-tight text-white sm:min-w-24 sm:text-2xl">
          {score}
        </div>
        <p className="text-left text-sm font-semibold leading-5 text-slate-100 sm:text-base">{match.awayTeam}</p>
      </div>

      <div className="mt-5 flex items-center justify-center gap-2">
        <span className="rounded-full border border-line bg-panel-soft px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
          {match.status}
        </span>
        {outcome && (
          <span className={`rounded-full border px-2.5 py-1 text-[11px] font-bold ${outcomeStyles[outcome]}`}>
            {outcome} · United
          </span>
        )}
      </div>
    </article>
  )
}
