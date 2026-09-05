# SwiftEats Customer UI

Thin React SPA for browsing restaurants, placing orders, and tracking delivery.

## Dev

```bash
npm install
cp .env.example .env
npm run dev
```

Open http://localhost:3000 — Vite proxies `/api` to `http://localhost:8080`.

**Sign in** at `/login` or **register** at `/register`. Demo account: `demo.customer@example.com` / `Demo@123`.
Session (customer id + API token) is stored in browser `localStorage`.

## Docker

Included in root `docker-compose.yml`:

```bash
docker compose up --build
```

Customer UI: http://localhost:3000
