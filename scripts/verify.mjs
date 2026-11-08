#!/usr/bin/env node
/**
 * 一次性完整验证：确认构建产物 -> 用 verify profile（内存库）启动 -> UI smoke -> API smoke -> 停止。
 * 提交前只运行一次；迭代期请直接跑 mvn 定向测试或 npm run typecheck。
 */
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const APP = path.join(ROOT, 'scripts', 'app.mjs');
const PORT = process.env.SERVER_PORT || '8080';
const BACKEND = `http://127.0.0.1:${PORT}`;

function run(label, args) {
  console.log(`\n=== ${label} ===`);
  const result = spawnSync(process.execPath, args, {
    cwd: ROOT,
    stdio: 'inherit',
    windowsHide: true,
    env: { ...process.env, APP_BACKEND_URL: BACKEND, APP_FRONTEND_URL: BACKEND },
  });
  return result.status ?? 1;
}

const jar = path.join(ROOT, 'backend', 'target', 'app.jar');
const index = path.join(ROOT, 'frontend', 'dist', 'index.html');
if (!fs.existsSync(jar) || !fs.existsSync(index)) {
  console.error('verify: build artifacts missing. Run build.cmd (frontend build + backend package) first.');
  process.exit(1);
}

let failed = false;
run('stop leftover instance', [APP, 'stop']);
const started = run('start (verify profile, in-memory db)', [APP, 'start', 'verify']);
if (started !== 0) {
  console.error('verify: start failed');
  failed = true;
} else {
  if (run('ui smoke', [path.join(ROOT, 'tools', 'ui-smoke.mjs')]) !== 0) failed = true;
  if (run('api smoke', [path.join(ROOT, 'tools', 'api-smoke.mjs')]) !== 0) failed = true;
}
if (run('stop', [APP, 'stop']) !== 0) failed = true;

if (failed) {
  console.error('\nverify: FAILED');
  process.exitCode = 1;
} else {
  console.log('\nverify: PASS');
}
