@echo off
setlocal
cd /d "%~dp0"
pushd frontend
call npm run build
if errorlevel 1 (
  popd
  exit /b 1
)
popd
pushd backend
call mvn -q -DskipTests package
if errorlevel 1 (
  popd
  exit /b 1
)
popd
echo build: OK
