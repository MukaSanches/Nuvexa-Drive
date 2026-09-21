#!/bin/sh
set -eu

PORT="${PORT:-10000}"

# Render terminates TLS before forwarding to the container.
export OVERWRITEPROTOCOL="${OVERWRITEPROTOCOL:-https}"
if [ -n "${RENDER_EXTERNAL_HOSTNAME:-}" ]; then
  export NEXTCLOUD_TRUSTED_DOMAINS="${NEXTCLOUD_TRUSTED_DOMAINS:-${RENDER_EXTERNAL_HOSTNAME}}"
  export OVERWRITEHOST="${OVERWRITEHOST:-${RENDER_EXTERNAL_HOSTNAME}}"
fi

# Bind Apache to Render's assigned port.
sed -ri "s/^Listen [0-9]+$/Listen ${PORT}/" /etc/apache2/ports.conf
for file in /etc/apache2/sites-available/*.conf; do
  [ -f "$file" ] || continue
  sed -ri "s/<VirtualHost \*:[0-9]+>/<VirtualHost *:${PORT}>/" "$file"
done

exec /entrypoint.sh "$@"
