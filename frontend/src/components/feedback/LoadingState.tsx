export function LoadingState() {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Loading matches">
      {[1, 2, 3].map((item) => (
        <div
          key={item}
          className="h-32 animate-pulse rounded-2xl border border-line bg-panel-soft/70"
        />
      ))}
      <span className="sr-only">Loading match data</span>
    </div>
  )
}
