#!/usr/bin/env bash
# OrchidPlugins build script
#
# Layout:
#   code/    = plugin source (pom.xml, src/, README.md, ...)
#   output/  = built channel jars
#
# Usage:
#   ./build.sh          build both channels
#   ./build.sh neuro    build the testing channel only
#   ./build.sh stable   build the stable channel only
#
# Requires JDK 21 and Maven.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CODE_DIR="$SCRIPT_DIR/code"
OUT_DIR="$SCRIPT_DIR/output"

usage() {
    cat <<'EOF'
Usage: ./build.sh [neuro|stable|both]

  neuro   build the testing channel  -> output/OrchidPlugins-<ver>-neuro.jar
  stable  build the promoted channel -> output/OrchidPlugins-<ver>-stable.jar
  both    build both (default)
EOF
}

channel="${1:-both}"

cd "$CODE_DIR"

build() {
    local phase="$1"
    shift
    echo "==> Building $phase channel..."
    mvn -q -B clean package "$@"

    local jar
    jar="$(find "$CODE_DIR/target" -maxdepth 1 -name 'OrchidPlugins-*.jar' ! -name 'original-*' | head -1 || true)"
    if [ -z "$jar" ] || [ ! -f "$jar" ]; then
        echo "!! No shaded jar found in $CODE_DIR/target for the $phase channel." >&2
        return 1
    fi
    cp "$jar" "$OUT_DIR/"
    echo "    -> $OUT_DIR/$(basename "$jar")"
}

mkdir -p "$OUT_DIR"

case "$channel" in
    neuro) build neuro ;;
    stable) build stable -Pstable ;;
    both) build neuro && build stable -Pstable ;;
    *) usage; exit 1 ;;
esac

echo "Done. Jars are in: $OUT_DIR"