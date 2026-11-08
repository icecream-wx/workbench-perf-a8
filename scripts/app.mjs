#!/usr/bin/env node
/**
 * 启停后端（同时托管 frontend/dist 构建产物）。
 * 用法：node scripts/app.mjs start [local|verify] | stop | status
 */
import { spawn, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const JAR = path.join(ROOT, 'backend', 'target', 'app.jar');
const RUN_DIR = path.join(ROOT, 'run');
const LOG_DIR = path.join(ROOT, 'logs');
const PID_FILE = path.join(RUN_DIR, 'backend.pid');
const PORT = process.env.SERVER_PORT || '8080';
const HEALTH_URL = `http://127.0.0.1:${PORT}/actuator/health`;

function isAlive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

async function isHealthy() {
  try {
    const response = await fetch(HEALTH_URL);
    return response.status === 200;
  } catch {
    return false;
  }
}

async function waitForHealth(timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await isHealthy()) return true;
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  return false;
}

function readPid() {
  if (!fs.existsSync(PID_FILE)) return null;
  const pid = Number(fs.readFileSync(PID_FILE, 'utf8').trim());
  return Number.isInteger(pid) && pid > 0 ? pid : null;
}

async function start(profile) {
  const existing = readPid();
  if (existing && isAlive(existing) && (await isHealthy())) {
    console.log(`already running (pid ${existing})`);
    return 0;
  }
  if (existing) fs.rmSync(PID_FILE, { force: true });
  if (!fs.existsSync(JAR)) {
    console.error('backend jar not found: run build.cmd / build.sh first');
    return 1;
  }
  if (!fs.existsSync(path.join(ROOT, 'frontend', 'dist', 'index.html'))) {
    console.warn('frontend/dist/index.html not found: API works, page verification will fail');
  }
  fs.mkdirSync(RUN_DIR, { recursive: true });
  fs.mkdirSync(LOG_DIR, { recursive: true });
  const logFd = fs.openSync(path.join(LOG_DIR, 'backend.log'), 'a');
  const child = spawn('java', ['-jar', JAR, `--spring.profiles.active=${profile}`], {
    cwd: ROOT,
    detached: true,
    stdio: ['ignore', logFd, logFd],
    windowsHide: true,
  });
  child.unref();
  fs.writeFileSync(PID_FILE, String(child.pid));
  console.log(`starting backend (profile=${profile}, pid=${child.pid}) on port ${PORT} ...`);
  if (!(await waitForHealth(90000))) {
    console.error('backend did not become healthy; see logs/backend.log');
    return 1;
  }
  console.log(`backend ready: http://127.0.0.1:${PORT}`);
  return 0;
}

async function stop() {
  const pid = readPid();
  if (!pid) {
    console.log('not running');
    return 0;
  }
  if (isAlive(pid)) {
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true });
    } else {
      process.kill(pid, 'SIGTERM');
    }
    const deadline = Date.now() + 15000;
    while (Date.now() < deadline && (await isHealthy())) {
      await new Promise((resolve) => setTimeout(resolve, 300));
    }
  }
  fs.rmSync(PID_FILE, { force: true });
  console.log('stopped');
  return 0;
}

async function status() {
  const pid = readPid();
  const healthy = await isHealthy();
  if (pid && isAlive(pid)) {
    console.log(`running (pid ${pid}) healthy=${healthy}`);
    return healthy ? 0 : 1;
  }
  console.log(healthy ? `port ${PORT} responds but no pid file` : 'not running');
  return healthy ? 0 : 1;
}

const [command = 'status', arg = 'local'] = process.argv.slice(2);
let code = 1;
if (command === 'start') {
  code = await start(arg);
} else if (command === 'stop') {
  code = await stop();
} else if (command === 'restart') {
  await stop();
  code = await start(arg);
} else if (command === 'status') {
  code = await status();
} else {
  console.error(`unknown command: ${command}`);
}
process.exitCode = code;
