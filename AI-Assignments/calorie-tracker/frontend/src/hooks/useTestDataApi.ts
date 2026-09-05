import { useCalorieApi } from './useCalorieApi';

export function useTestDataApi(token: string) {
  const { post } = useCalorieApi(token);

  const generateTestData = async () => {
    const url = import.meta.env.VITE_API_URL + '/calories/test-data';
    return post(url, {});
  };

  return { generateTestData };
}
