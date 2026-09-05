# Calorie Tracker


## Run the app using Docker

```sh
docker compose up
```

Visit [http://localhost:9001/](http://localhost:9001/)

---

## Run the app without Docker

### Prerequisites
- Node.js v20+ and npm installed
https://nodejs.org/en/download

---

### 1. Backend Setup

```sh
cd backend
npm i
npm run start:dev
```
Backend runs on [http://localhost:9000/](http://localhost:9000/)

---

### 2. Frontend Setup

```sh
cd frontend
npm i
# Set environment variables if needed (see Dockerfile for VITE_API_URL and VITE_GOOGLE_CLIENT_ID)
npm run dev
```
Frontend runs on [http://localhost:4173/](http://localhost:4173/) (default Vite preview port)

---

### 3. Mock Service Setup

```sh
cd mock-service
npm i
node index.js
```
Mock service runs on [http://localhost:9002/](http://localhost:9002/)

---

### 4. Access the App

- Visit [http://localhost:4173/](http://localhost:4173/) for the frontend.
- Ensure backend ([http://localhost:9000/](http://localhost:9000/)) and mock-service ([http://localhost:9002/](http://localhost:9002/)) are running.
