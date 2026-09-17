#!/bin/sh
set -eu
R=/home/dhiveserver/robopilot-local-20260911
exec 9>"$R/https-ip/renew.lock"
flock -n 9 || exit 0
docker run --rm -v "$R/https-ip/letsencrypt:/etc/letsencrypt" -v "$R/external-web/html:/webroot" certbot/certbot@sha256:c23159d30afdd9c97960578aa4654f5901de6cae394958f894074dedd55e599d renew --non-interactive --quiet
docker exec robopilot-dev-web nginx -t
docker exec robopilot-dev-web nginx -s reload
date -u +%FT%TZ > "$R/https-ip/last-renew-check.txt"
