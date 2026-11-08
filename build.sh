#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

pushd frontend >/dev/null
npm run build
popd >/dev/null

pushd backend >/dev/null
mvn -q -DskipTests package
popd >/dev/null

echo "build: OK"
