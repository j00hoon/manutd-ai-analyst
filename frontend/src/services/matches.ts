import { api } from './api'
import type { MatchResponse } from '../types/match'

export const getManchesterUnitedMatches = async (limit = 5) => {
  const response = await api.get<MatchResponse[]>(
    '/teams/manchester-united/matches',
    { params: { limit } },
  )
  return response.data
}
