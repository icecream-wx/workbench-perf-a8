@echo off
cd /d "%~dp0"
if "%~1"=="" (
  node scripts\app.mjs start local
) else (
  node scripts\app.mjs start %1
)
