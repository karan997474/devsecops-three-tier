# `stable-alpine` is refreshed by the upstream NGINX maintainers; Jenkins uses
# `docker build --pull` so every release is scanned against the current base.
FROM nginxinc/nginx-unprivileged:stable-alpine
USER root
# Upgrade the package separately so the image contains the Alpine security fix
# even if the upstream NGINX image was published before the package rebuild.
RUN apk upgrade --no-cache c-ares
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY app.js /usr/share/nginx/html/app.js
USER 101
EXPOSE 8080
