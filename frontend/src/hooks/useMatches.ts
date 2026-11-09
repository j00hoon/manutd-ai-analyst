import { useQuery } from '@tanstack/react-query'
import { getManchesterUnitedMatches } from '../services/matches'

export const useMatches = (limit = 5) =>
  useQuery({
    queryKey: ['manchester-united', 'matches', limit],
    queryFn: () => getManchesterUnitedMatches(limit),
  })
