import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'

const DashboardPage = lazy(() =>
  import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })),
)
const MatchesPage = lazy(() =>
  import('./pages/MatchesPage').then((module) => ({ default: module.MatchesPage })),
)
const PlayersPage = lazy(() =>
  import('./pages/PlayersPage').then((module) => ({ default: module.PlayersPage })),
)

const withPageLoading = (page: ReactNode) => (
  <Suspense fallback={<div className="h-64 animate-pulse rounded-2xl border border-line bg-panel" aria-label="Loading page" />}>
    {page}
  </Suspense>
)

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={withPageLoading(<DashboardPage />)} />
        <Route path="matches" element={withPageLoading(<MatchesPage />)} />
        <Route path="players" element={withPageLoading(<PlayersPage />)} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
