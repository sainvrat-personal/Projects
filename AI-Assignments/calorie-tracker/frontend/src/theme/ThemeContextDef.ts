import type { Theme } from "./ThemeContext";
import { createContext } from "react";

export interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
  colors: {
    primary: string;
    secondary: string;
    accent: string;
    background: string;
    surface: string;
    text: string;
    legend: string;
    contrast: string;
    border: string;
    success: string;
    warning: string;
    axis?: string;
  };
}

export const ThemeContext = createContext<ThemeContextType | undefined>(undefined);
