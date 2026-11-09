import { ArrowRight, FlaskConical } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { MatchResponse } from '../../types/match'
import { isSyntheticMatch } from '../../types/match'
import { ErrorState } from '../feedback/ErrorState'
import { LoadingState } from '../feedback/LoadingState'
import { MatchCard } from './MatchCard'

interface RecentMatchesProps {
  matches?: MatchResponse[]
  isLoading: boolean
  error: Error | null
  onRetry: () => void
  showAllLink?: boolean
}

export function RecentMatches({
  matches,
  isLoading,
  error,
  onRetry,
  showAllLink = false,
}: RecentMatchesProps) {
  const containsSyntheticData = matches?.some(isSyntheticMatch) ?? false

  return (
    <section aria-labelledby="recent-matches-heading">
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-xs font-semibold uppercase tracking-[0.18em] text-united-red">Fixtures</p>
          <h2 id="recent-matches-heading" className="text-xl font-semibold tracking-tight text-white">
            Recent matches
          </h2>
        </div>
        {showAllLink && (
          <Link to="/matches" className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-400 hover:text-white">
            View match centre
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        )}
      </div>

      {containsSyntheticData && (
        <div className="mb-4 flex items-start gap-3 rounded-xl border border-amber-700/50 bg-amber-500/8 px-4 py-3 text-sm text-amber-200" role="status">
          <FlaskConical className="mt-0.5 shrink-0" size={17} aria-hidden="true" />
          <p><strong>DEMO DATA:</strong> These are synthetic examples supplied by the backend, not real match results.</p>
        </div>
      )}

      {isLoading && <LoadingState />}
      {error && !isLoading && <ErrorState message={error.message} onRetry={onRetry} />}
      {!isLoading && !error && matches?.length === 0 && (
        <div className="rounded-2xl border border-dashed border-line bg-panel/50 px-6 py-12 text-center text-sm text-slate-500">
          No finished matches are available.
        </div>
      )}
      {!isLoading && !error && matches && matches.length > 0 && (
        <div className="space-y-3">
          {matches.map((match) => <MatchCard key={match.fixtureId} match={match} />)}
        </div>
      )}
    </section>
  )
}
