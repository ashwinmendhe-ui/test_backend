#!/bin/sh
set -eu
umask 077
R=/home/dhiveserver/robopilot-local-20260911
CA="$R/server-identity-ca"
D="$R/aws-tools/server-identity"
exec 9>"$CA/renew.lock"
flock -n 9 || exit 0
openssl x509 -checkend 31622400 -noout -in "$CA/ca.pem" >/dev/null || { echo "CA renewal required" >&2; exit 1; }
if [ "${1:-}" = --force ] || ! openssl x509 -checkend 2592000 -noout -in "$D/server.pem" >/dev/null; then
 openssl x509 -req -in "$D/server.csr" -CA "$CA/ca.pem" -CAkey "$CA/ca.key" -CAserial "$CA/ca.srl" -days 365 -sha256 -extfile "$CA/leaf.ext" -out "$D/server.next.pem"
 openssl verify -CAfile "$CA/ca.pem" "$D/server.next.pem"
 cp "$D/server.pem" "$D/server.previous.pem"
 mv "$D/server.next.pem" "$D/server.pem"
fi
date -u +%FT%TZ > "$D/last-cert-check.txt"
