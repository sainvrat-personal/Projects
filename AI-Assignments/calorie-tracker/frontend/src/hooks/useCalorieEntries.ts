import { useCalorieCrud } from './useCalorieCrud';

export function useCalorieEntries(token: string, timeframe: 'week' | 'twoweek' | 'fourweek') {
  return useCalorieCrud(token, timeframe);
}
