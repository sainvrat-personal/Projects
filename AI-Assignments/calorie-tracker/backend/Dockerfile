FROM node:20-slim AS development

# Required to compile sqlite3 native bindings
RUN apt-get update && apt-get install -y \
  g++ \
  libsqlite3-dev \
  make \
  python3 \
  && rm -rf /var/lib/apt/lists/*
  
# Set working directory
WORKDIR /app

# Copy package files and install dependencies
COPY ./package*.json ./
RUN npm ci

# ----------- Build Stage -----------
FROM node:20-slim AS build

# Set working directory
WORKDIR /app

# Copy all source files and node_modules from development stage
COPY ./ ./
COPY --from=development /app/node_modules ./node_modules

# Build the application (e.g., TypeScript to JS)
RUN npm run build

# Set environment to production and install only production dependencies
ENV NODE_ENV production
RUN npm ci --omit=dev && npm cache clean --force

# ----------- Production Stage -----------
FROM node:20-slim AS production

# Set environment variables for the app
ENV PORT=9000
ENV DB_NAME=calorie_tracker.db
ENV JWT_SECRET=some_jwt_secret
ENV JWT_EXPIRATION=1d
ENV GOOGLE_CLIENT_ID=777870652478-sl3btg5k3qqvs4ovs5gm74chdbk4jqvr.apps.googleusercontent.com

# Copy only necessary files for running the app
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/.env ./.env

# Expose the backend port
EXPOSE 9000

# Start the backend server
CMD [ "node", "dist/main.js" ]
