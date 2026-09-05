
import React, { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { ThemeContext } from "./ThemeContextDef";

export type Theme = "light" | "dark";

const lightColors = {
  primary: "#4CAF50", // fresh green
  secondary: "#FF9800", // warm orange
  accent: "#2196F3", // bright blue
  background: "#FAFAFA", // very light gray
  surface: "#fff", // card/modal backgrounds
  text: "#212121", // dark gray
  legend: "#4CAF50",
  contrast: "#fff", // for buttons on primary
  border: "#e0e0e0", // light gray
  success: "#43A047", // vibrant green
  warning: "#FFC107", // amber
};

const darkColors = {
  primary: "#66BB6A", // softer green
  secondary: "#FFB74D", // muted orange
  accent: "#64B5F6", // light blue
  background: "#121212", // dark gray
  surface: "#1E1E1E", // elevated dark gray
  text: "#E0E0E0", // light gray
  legend: "#66BB6A",
  contrast: "#121212", // for buttons on primary
  border: "#333", // dark border
  success: "#4CAF50", // bright green
  warning: "#FFCC02", // soft amber
};

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>(() => {
    if (typeof window !== "undefined") {
      return (localStorage.getItem("theme") as Theme) || "light";
    }
    return "light";
  });

  useEffect(() => {
    document.body.dataset.theme = theme;
    localStorage.setItem("theme", theme);
  }, [theme]);

  const toggleTheme = () => setTheme((t) => (t === "light" ? "dark" : "light"));

  const colors = theme === "dark" ? darkColors : lightColors;

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, colors }}>
      {children}
    </ThemeContext.Provider>
  );
};


