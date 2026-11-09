import { RecentMatches } from '../components/matches/RecentMatches'
import { useMatches } from '../hooks/useMatches'

export function MatchesPage() {
  const query = useMatches(5)

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-8 rounded-2xl border border-line bg-panel px-5 py-5 sm:px-6">
        <p className="text-sm leading-6 text-slate-400">
          Showing up to five latest finished matches from the backend. Scores and statuses are displayed exactly as provided.
        </p>
      </div>
      <RecentMatches
        matches={query.data}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
      />
    </div>
  )
}
