import { useState } from "react";

export function useImageAnalysis() {
  const [analyzing, setAnalyzing] = useState(false);

  const analyzeImageCalorie = async (
    file: File
  ): Promise<{ description: string; estimatedCalories: number }> => {
    setAnalyzing(true);
    try {
      const formData = new FormData();
      formData.append("image", file);
      const response = await fetch(
        `${import.meta.env.VITE_IMAGE_ANALYZER_SERVICE_URL}/food/analyze`,
        {
          method: "POST",
          body: formData,
        }
      );
      if (!response.ok) {
        let errorMsg = "Failed to analyze image";

        const err = await response.json();
        if (err && err.error) {
          errorMsg = err.error;
        } else if (err && err.message) {
          errorMsg = err.message;
        }

        throw new Error(errorMsg);
      }
      return await response.json();
    } finally {
      setAnalyzing(false);
    }
  };

  return { analyzing, analyzeImageCalorie };
}
