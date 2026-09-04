# --- Stage 1: Build ---
# Full Node here — discarded after the build, so its size doesn't matter.
FROM node:20-alpine AS build
WORKDIR /app

# Dependencies as a SEPARATE layer, before source code: a source-only
# change won't force a full npm install on every rebuild (same caching
# reasoning as the backend's Dockerfile).
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Stage 2: Runtime ---
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production

# Non-root user — same reasoning as the backend image: a compromised
# process shouldn't have root inside the container.
RUN addgroup -S nearnow && adduser -S nearnow -G nearnow

# Only what `next start` actually needs at runtime — no source, no
# dev dependencies, no build cache.
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
RUN chown -R nearnow:nearnow /app
USER nearnow

EXPOSE 3000
CMD ["npm", "start"]
