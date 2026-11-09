export interface MatchResponse {
  fixtureId: number
  kickoffTime: string
  competition: string
  homeTeam: string
  awayTeam: string
  homeGoals: number | null
  awayGoals: number | null
  status: string
  result: string
}

export type MatchOutcome = 'W' | 'D' | 'L'

const isManchesterUnited = (teamName: string) =>
  teamName.toLowerCase().includes('manchester united')

export const getMatchOutcome = (match: MatchResponse): MatchOutcome | null => {
  if (match.homeGoals === null || match.awayGoals === null) return null
  if (match.homeGoals === match.awayGoals) return 'D'

  const unitedIsHome = isManchesterUnited(match.homeTeam)
  const unitedWon = unitedIsHome
    ? match.homeGoals > match.awayGoals
    : match.awayGoals > match.homeGoals

  return unitedWon ? 'W' : 'L'
}

export const isSyntheticMatch = (match: MatchResponse) =>
  match.status.toUpperCase() === 'SYNTHETIC' ||
  match.result.toUpperCase().includes('SYNTHETIC')
