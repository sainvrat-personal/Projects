import React from "react";
import { useTheme } from "../theme/useTheme";

export const ThemeToggle: React.FC = () => {
  const { theme, toggleTheme } = useTheme();
  return (
    <button
      onClick={toggleTheme}
      style={{
        padding: "0.5rem 1rem",
        borderRadius: 6,
        border: `1px solid var(--color-border)`,
        background: "var(--color-surface)",
        color: "var(--color-primary)",
        cursor: "pointer",
        fontWeight: 600,
        margin: 8,
      }}
      aria-label="Toggle theme"
    >
      {theme === "dark" ? "🌙 Dark" : "☀️ Light"}
    </button>
  );
};
