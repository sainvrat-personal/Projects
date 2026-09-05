import { useState, useCallback } from 'react';
import { useCalorieApi } from './useCalorieApi';

export interface CalorieEntry {
  id: number;
  description: string;
  calories: number;
  createdAt: string;
}

export function useCalorieCrud(token: string, timeframe: 'week' | 'twoweek' | 'fourweek') {
  const { get, post, put, del } = useCalorieApi(token);
  const [entries, setEntries] = useState<CalorieEntry[]>([]);
  const [dailyCalories, setDailyCalories] = useState<{ date: string; totalCalories: number }[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchEntries = useCallback(async () => {
    let url = import.meta.env.VITE_API_URL + '/calories';
    let byDayUrl = import.meta.env.VITE_API_URL + '/calories/by-day';
    const now = new Date();
    const params: Record<string, string> = { limit: '100' };
    if (timeframe === 'week') {
      const weekAgo = new Date(now);
      weekAgo.setDate(now.getDate() - 7);
      params.startDate = weekAgo.toISOString();
    } else if (timeframe === 'twoweek') {
      const twoWeekAgo = new Date(now);
      twoWeekAgo.setDate(now.getDate() - 14);
      params.startDate = twoWeekAgo.toISOString();
    } else if (timeframe === 'fourweek') {
      const fourWeekAgo = new Date(now);
      fourWeekAgo.setDate(now.getDate() - 28);
      params.startDate = fourWeekAgo.toISOString();
    }
    const query = new URLSearchParams(params).toString();
    if (query) url += '?' + query;
    if (query) byDayUrl += '?' + query;
    setLoading(true);
    try {
      const data = await get(url);
      setEntries(data);
      const byDayData = await get(byDayUrl);
      setDailyCalories(byDayData);
    } finally {
      setLoading(false);
    }
  }, [timeframe, get]);

  const addEntry = useCallback(async (entry: Omit<CalorieEntry, 'id' | 'createdAt'>) => {
    const url = import.meta.env.VITE_API_URL + '/calories';
    return post(url, entry);
  }, [post]);

  const editEntry = useCallback(async (id: number, entry: Omit<CalorieEntry, 'id' | 'createdAt'>) => {
    const url = `${import.meta.env.VITE_API_URL}/calories/${id}`;
    return put(url, entry);
  }, [put]);

  const deleteEntry = useCallback(async (id: number) => {
    const url = `${import.meta.env.VITE_API_URL}/calories/${id}`;
    return del(url);
  }, [del]);

  return {
    entries,
    setEntries,
    dailyCalories,
    setDailyCalories,
    loading,
    fetchEntries,
    addEntry,
    editEntry,
    deleteEntry,
  };
}
