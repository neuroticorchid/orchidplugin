#!/usr/bin/env bash
#
# OrchidPlugins license-key generator (SELLER / author only).
#
# Signing keypair (Ed25519):
#   ./keygen.sh gen-keypair [outdir]         # creates keys/orchid-private.pem + keys/orchid-public.key
#   ./keygen.sh install-public [keydir]      # installs the public key into src/main/resources/licensing/public.key
#
# Issuing a key to a buyer:
#   ./keygen.sh make <license-id> [expiry]   # prints the key string; expiry = "none" or a date like 2027-12-31
#   ./keygen.sh make-file <list.txt> [keydir]  # lines: <license-id> [expiry]
#
# NEVER commit or share keys/orchid-private.pem. The plugin only embeds the public key.
# The key string format is:  base64url(payload) "." base64url(ed25519-signature)
# where payload = k1:<license-id>:<expiry-epoch>  (0 expiry-epoch = never expires).
#
set -euo pipefail

SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SELF_DIR/.." && pwd)"
PUBLIC_RESOURCE="$REPO_DIR/code/src/main/resources/licensing/public.key"
MSG_TAG="k1"

die()  { echo "error: $*" >&2; exit 1; }
usage() {
  sed -n '2,12p' "$0" >&2
  exit 1
}

b64url() { base64 -w0 | tr '+/' '-_' | tr -d '='; }

require_openssl() {
  command -v openssl >/dev/null 2>&1 || die "openssl is required (OpenSSL 3.x with Ed25519 support)"
  local v
  v="$(openssl version 2>/dev/null | awk '{print $2}' | cut -d. -f1)"
  [ "${v:-0}" -ge 3 ] || die "OpenSSL 3.x+ required (found $(openssl version))"
}

cmd_gen_keypair() {
  require_openssl
  local out="${2:-$SELF_DIR/keys}"
  mkdir -p "$out"
  [ -f "$out/orchid-private.pem" ] && die "$out/orchid-private.pem already exists - refusing to overwrite"
  openssl genpkey -algorithm Ed25519 -out "$out/orchid-private.pem"
  openssl pkey -in "$out/orchid-private.pem" -pubout -outform DER | b64url > "$out/orchid-public.key"
  chmod 600 "$out/orchid-private.pem"
  echo "Created: $out/orchid-private.pem  (KEEP THIS SECRET)"
  echo "         $out/orchid-public.key"
  echo "Install the public key into the plugin with:"
  echo "  $0 install-public $out"
}

cmd_install_public() {
  local key="${2:-$SELF_DIR/keys}/orchid-public.key" out dir
  out="$PUBLIC_RESOURCE"
  [ -f "$key" ] || die "public key not found: $key"
  dir="$(dirname "$out")"; mkdir -p "$dir"
  tr -d '\n ' < "$key" > "$out"
  echo "Installed public key -> $out"
}

expire_epoch() {
  local e="$1"
  [ "x$e" = "xnone" ] || [ -z "$e" ] && echo 0 && return
  local t
  t="$(date -d "$e" +%s 2>/dev/null)" || die "cannot parse expiry date: $e"
  echo "$t"
}

make_key() {
  require_openssl
  local id="$1" expiry="${2:-none}" priv="${3:-$SELF_DIR/keys/orchid-private.pem}"
  [ -f "$priv" ] || die "private key not found: $priv"
  local epoch payload p64 sig s64 tmp
  epoch="$(expire_epoch "$expiry")"
  payload="$MSG_TAG:$id:$epoch"
  p64="$(printf '%s' "$payload" | b64url)"
  tmp="$(mktemp)"
  if ! printf '%s' "$payload" > "$tmp" \
     || ! openssl pkeyutl -sign -inkey "$priv" -rawin -in "$tmp" 2>/dev/null | b64url > "$tmp.b64"; then
    rm -f "$tmp" "$tmp.b64"
    die "signing failed (rawin sign requires OpenSSL 3.x)"
  fi
  s64="$(cat "$tmp.b64")"
  rm -f "$tmp" "$tmp.b64"
  echo "$p64.$s64"
}

cmd_make() {
  [ $# -ge 2 ] || usage
  make_key "$2" "${3:-none}" "${4:-$SELF_DIR/keys/orchid-private.pem}"
}

cmd_make_file() {
  local list="${2:-}" priv="${3:-$SELF_DIR/keys/orchid-private.pem}"
  [ -n "$list" ] && [ -f "$list" ] || die "usage: $0 make-file <list.txt>"
  [ -f "$priv" ] || die "private key not found: $priv"
  local line
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|\#*) continue ;;
    esac
    read -r id expiry _ <<< "$line"
    echo -n "$id  ->  "
    make_key "$id" "${expiry:-none}" "$priv"
  done < "$list"
}

cmd="${1:-}"
case "$cmd" in
  gen-keypair)     cmd_gen_keypair "$@" ;;
  install-public)  cmd_install_public "$@" ;;
  make)            cmd_make "$@" ;;
  make-file)       cmd_make_file "$@" ;;
  *)               usage ;;
esac