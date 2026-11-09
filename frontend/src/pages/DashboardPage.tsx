import { Activity, BarChart3, Minus, TrendingDown, TrendingUp } from 'lucide-react'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { RecentMatches } from '../components/matches/RecentMatches'
import { useMatches } from '../hooks/useMatches'
import { getMatchOutcome } from '../types/match'

const resultConfig = [
  { key: 'W', name: 'Wins', color: '#34d399', icon: TrendingUp },
  { key: 'D', name: 'Draws', color: '#fbbf24', icon: Minus },
  { key: 'L', name: 'Losses', color: '#fb7185', icon: TrendingDown },
] as const

export function DashboardPage() {
  const query = useMatches(5)
  const matches = query.data ?? []
  const resultData = resultConfig.map((item) => ({
    ...item,
    value: matches.filter((match) => getMatchOutcome(match) === item.key).length,
  }))
  const completedCount = resultData.reduce((total, item) => total + item.value, 0)

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-3xl border border-line bg-panel px-6 py-8 sm:px-8 lg:px-10">
        <div className="absolute -right-16 -top-28 size-80 rounded-full bg-united-red/12 blur-3xl" aria-hidden="true" />
        <div className="relative max-w-2xl">
          <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-red-900/60 bg-red-950/30 px-3 py-1.5 text-xs font-semibold text-red-300">
            <Activity size={14} aria-hidden="true" />
            Phase 1 match feed
          </div>
          <h2 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">Football data, made clear.</h2>
          <p className="mt-4 max-w-xl text-sm leading-7 text-slate-400 sm:text-base">
            A focused view of Manchester United's latest completed fixtures. Results below come directly from the configured backend provider.
          </p>
        </div>
      </section>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-line bg-panel p-5">
          <div className="mb-5 grid size-10 place-items-center rounded-xl bg-sky-500/10 text-sky-300"><BarChart3 size={19} /></div>
          <p className="text-3xl font-bold text-white">{query.isPending ? '—' : completedCount}</p>
          <p className="mt-1 text-sm text-slate-500">Matches in view</p>
        </div>
        {resultData.map(({ key, name, value, icon: Icon, color }) => (
          <div key={key} className="rounded-2xl border border-line bg-panel p-5">
            <div className="mb-5 grid size-10 place-items-center rounded-xl bg-white/5" style={{ color }}><Icon size={19} /></div>
            <p className="text-3xl font-bold text-white">{query.isPending ? '—' : value}</p>
            <p className="mt-1 text-sm text-slate-500">{name}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-8 xl:grid-cols-[minmax(0,1.6fr)_minmax(280px,0.7fr)]">
        <RecentMatches
          matches={query.data}
          isLoading={query.isPending}
          error={query.error}
          onRetry={() => void query.refetch()}
          showAllLink
        />

        <section className="h-fit rounded-2xl border border-line bg-panel p-5" aria-labelledby="result-split-heading">
          <p className="mb-1 text-xs font-semibold uppercase tracking-[0.18em] text-united-red">Overview</p>
          <h2 id="result-split-heading" className="text-xl font-semibold text-white">Result split</h2>
          <p className="mt-2 text-sm text-slate-500">Calculated only from the matches currently shown.</p>
          <div className="mt-5 h-56" aria-label="Win, draw, and loss distribution chart">
            {completedCount > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={resultData} dataKey="value" nameKey="name" innerRadius={52} outerRadius={78} stroke="none">
                    {resultData.map((item) => <Cell key={item.key} fill={item.color} />)}
                  </Pie>
                  <Tooltip
                    contentStyle={{ background: '#151a23', border: '1px solid #252c38', borderRadius: 12, color: '#fff' }}
                    itemStyle={{ color: '#fff' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="grid h-full place-items-center text-sm text-slate-600">Awaiting match data</div>
            )}
          </div>
          <div className="grid grid-cols-3 gap-2">
            {resultData.map((item) => (
              <div key={item.key} className="rounded-xl bg-canvas/70 p-3 text-center">
                <span className="mx-auto mb-2 block size-2 rounded-full" style={{ backgroundColor: item.color }} />
                <p className="text-sm font-semibold text-white">{item.value}</p>
                <p className="text-[11px] text-slate-500">{item.name}</p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
