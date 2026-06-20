#!/usr/bin/env bash
#
# Interactive secret entry for the VM .env - run via:
#   ssh -t wss2@vm-resafetytool.cin.ufpe.br 'bash -s' ... (piped base64)
#
# Prompts for GOOGLE_API_KEY and AUTH_USERS and writes them into
# /opt/docker/resafetytool/deploy/vm/.env. Reads from /dev/tty so it works even
# when the script body arrives on stdin. Values are typed directly on the VM and
# never leave the terminal.
set -euo pipefail

cd /opt/docker/resafetytool/deploy/vm
ENVF=.env

echo "=== Enter application secrets (input hidden) ==="
printf 'GOOGLE_API_KEY (Gemini): '
read -rs GKEY < /dev/tty
echo
printf 'AUTH_USERS  as  user1:pass1,user2:pass2 : '
read -rs AUSERS < /dev/tty
echo

if [ -z "$GKEY" ] || [ -z "$AUSERS" ]; then
  echo "ERROR: one of the values was empty - nothing changed." >&2
  exit 1
fi

tmp="$(mktemp)"
grep -v -E '^(GOOGLE_API_KEY|AUTH_USERS)=' "$ENVF" > "$tmp"
printf 'GOOGLE_API_KEY=%s\n' "$GKEY"  >> "$tmp"
printf 'AUTH_USERS=%s\n'    "$AUSERS" >> "$tmp"
chmod 600 "$tmp"
mv "$tmp" "$ENVF"
chmod 600 "$ENVF"

users="$(printf '%s' "$AUSERS" | tr ',' '\n' | cut -d: -f1 | paste -sd, -)"
echo "Saved to .env."
echo "  GOOGLE_API_KEY: ${#GKEY} chars"
echo "  Login users:    ${users}"
echo "SECRETS_DONE_OK"
