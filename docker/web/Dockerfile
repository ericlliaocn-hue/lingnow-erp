FROM node:20-alpine AS admin-build
WORKDIR /workspace/admin-ui
COPY admin-ui/package*.json ./
RUN npm ci
COPY admin-ui ./
RUN npm run build

FROM node:20-alpine AS h5-build
WORKDIR /workspace/uniapp
COPY uniapp/package*.json ./
RUN npm ci
COPY uniapp ./
RUN npm run build:h5

FROM nginx:1.27-alpine
COPY docker/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=admin-build /workspace/admin-ui/dist /usr/share/nginx/html/admin
COPY --from=h5-build /workspace/uniapp/dist/build/h5 /usr/share/nginx/html/h5
EXPOSE 80
