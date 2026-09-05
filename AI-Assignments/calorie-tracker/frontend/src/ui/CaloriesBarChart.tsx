
import type { ChartData, ChartOptions } from "chart.js";
import { Chart, registerables } from "chart.js";
import React from "react";
import { Bar } from "react-chartjs-2";
import { useTheme } from "../theme/useTheme";

Chart.register(...registerables);

interface ICaloriesBarChartProps {
  data: ChartData<"bar", number[], string>;
  height?: number;
}

const CaloriesBarChart: React.FC<ICaloriesBarChartProps> = ({
  data,
  height = 350,
}) => {
  const { colors } = useTheme();
  const options: ChartOptions<"bar"> = {
    plugins: {
      legend: {
        labels: { color: colors.legend, font: { size: 16, weight: "bold" } },
      },
    },
    maintainAspectRatio: false,
    responsive: true,
    scales: {
      x: {
        ticks: { color: colors.text, font: { size: 14 } },
        grid: { color: colors.border },
        // @ts-expect-error: maxBarThickness is not in Chart.js types but is supported
        maxBarThickness: 40,
      },
      y: {
        ticks: { color: colors.text, font: { size: 14 } },
        grid: { color: colors.border },
      },
    },
  };
  return <Bar data={data} options={options} height={height} />;
};

export default CaloriesBarChart;
