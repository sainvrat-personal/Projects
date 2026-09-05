import "./theme/theme.css";
import "./App.css";
import { useState } from "react";
import {
  Navigate,
  Route,
  BrowserRouter as Router,
  Routes,
} from "react-router-dom";
import ProtectedPage from "./components/ProtectedPage";
import LandingPage from "./pages/LandingPage";
import StatsPage from "./pages/StatsPage";
import { ThemeProvider } from "./theme/ThemeContext";
import { Layout } from "./ui";
import Header from "./ui/Header";

function App() {
  const [token, setToken] = useState<string | null>(() => {
    return sessionStorage.getItem("accessToken");
  });

  const handleLogin = (newToken: string) => {
    setToken(newToken);
    sessionStorage.setItem("accessToken", newToken);
  };

  const handleLogout = () => {
    setToken(null);
    sessionStorage.removeItem("accessToken");
  };

  return (
    <ThemeProvider>
      <Router>
        <Header token={token} onLogout={handleLogout} />
        <Layout>
          <Routes>
            <Route
              path="/"
              element={
                !token ? (
                  <LandingPage onLogin={handleLogin} />
                ) : (
                  <Navigate to="/stats" replace />
                )
              }
            />
            <Route
              path="/stats"
              element={
                <ProtectedPage>
                  <StatsPage />
                </ProtectedPage>
              }
            />
            {/* Future routes can be added here */}
          </Routes>
        </Layout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
