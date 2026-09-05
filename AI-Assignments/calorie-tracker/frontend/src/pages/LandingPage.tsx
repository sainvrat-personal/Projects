import { GoogleLogin } from "@react-oauth/google";
import React from "react";
import "./LandingPage.css";
import { Layout } from "../ui";

const LandingPage: React.FC<{ onLogin: (token: string) => void }> = ({
  onLogin,
}) => (
  <Layout>
    <div className="landing-root">
      <div className="landing-card">
        <h1 className="landing-title">Calorie Tracker</h1>
        <p className="landing-desc">
          Track your daily calorie intake with ease.
          <br />
          Sign in to get started!
        </p>
        <GoogleLogin
          onSuccess={(credentialResponse) => {
            fetch(`${import.meta.env.VITE_API_URL}/auth/token-signin`, {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
              },
              body: JSON.stringify({ token: credentialResponse.credential }),
            })
              .then((res) => res.json())
              .then((user) => {
                if (user && user.accessToken) {
                  onLogin(user.accessToken);
                } else {
                  alert("Login failed");
                }
              })
              .catch(() => alert("Login failed"));
          }}
          onError={() => {
            alert("Login Failed");
          }}
          theme="filled_blue"
          shape="pill"
          size="large"
          width="300"
        />
      </div>
      <footer className="landing-footer">
        &copy; {new Date().getFullYear()} Calorie Tracker
      </footer>
    </div>
  </Layout>
);

export default LandingPage;
