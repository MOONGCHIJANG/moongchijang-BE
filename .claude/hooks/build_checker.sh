#!/usr/bin/env bash
set -euo pipefail

payload="$(cat || true)"

if ! printf '%s' "$payload" | grep -Eq '"file_path":"[^"]+\.(kt|kts)"|"path":"[^"]+\.(kt|kts)"'; then
  exit 0
fi

if [ ! -x "./gradlew" ]; then
  echo "[claude-hook] gradlew not found or not executable; skipping Kotlin compile check"
  exit 0
fi

echo "[claude-hook] Kotlin file changed; running ./gradlew compileKotlin"
./gradlew compileKotlin
