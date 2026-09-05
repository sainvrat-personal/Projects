# Use Node.js image to build the frontend
FROM node:20-slim AS febuilder

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN echo "Installing frontend dependencies..." \
    && npm ci

# Copy the rest of the frontend code
COPY . .

# Set Vite environment variables directly
ENV VITE_API_URL=http://localhost:9000/api
ENV VITE_GOOGLE_CLIENT_ID=777870652478-sl3btg5k3qqvs4ovs5gm74chdbk4jqvr.apps.googleusercontent.com

# Build the frontend
RUN echo "Building frontend..." \
    && npm run build

# Use Nginx to serve the built frontend
FROM nginx:alpine
COPY --from=febuilder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
