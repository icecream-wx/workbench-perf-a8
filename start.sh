#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
node scripts/app.mjs start "${1:-local}"
