#!/bin/bash
set -euo pipefail

/back/main &
go_pid=$!

nginx -g 'daemon off;' &
nginx_pid=$!

shutdown() {
  kill "$go_pid" "$nginx_pid" 2>/dev/null || true
  wait "$go_pid" 2>/dev/null || true
  wait "$nginx_pid" 2>/dev/null || true
}

trap 'shutdown; exit 0' TERM INT

wait -n "$go_pid" "$nginx_pid"
status=$?
shutdown
exit "$status"
