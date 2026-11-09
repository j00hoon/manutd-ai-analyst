import { AlertTriangle, RefreshCw } from 'lucide-react'

interface ErrorStateProps {
  message?: string
  onRetry: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="rounded-2xl border border-red-900/60 bg-red-950/20 px-6 py-10 text-center" role="alert">
      <AlertTriangle className="mx-auto mb-3 text-red-400" size={28} aria-hidden="true" />
      <h3 className="font-semibold text-white">Match data is unavailable</h3>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
        {message ?? 'The backend could not be reached. Confirm it is running and try again.'}
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-5 inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-slate-200"
      >
        <RefreshCw size={16} aria-hidden="true" />
        Try again
      </button>
    </div>
  )
}
