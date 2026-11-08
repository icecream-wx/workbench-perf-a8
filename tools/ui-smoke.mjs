#!/usr/bin/env node
/**
 * 页面层自测（CDP，无需额外依赖）：EMP-01/02/03 的关键交互与状态。
 * 需要后端同时托管前端构建产物，并以 verify profile（空库）启动。
 */
import fs from 'node:fs';
import path from 'node:path';
import { Browser } from './cdp.mjs';

const URL = process.env.APP_FRONTEND_URL || 'http://127.0.0.1:8080';
const BACKEND = process.env.APP_BACKEND_URL || 'http://127.0.0.1:8080';
const candidates = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
].filter(Boolean);
const chrome = candidates.find((candidate) => fs.existsSync(candidate));

if (!chrome) {
  if (process.env.SKIP_UI_SMOKE === '1') {
    console.log('UI smoke: SKIP (no Chrome/Edge, SKIP_UI_SMOKE=1)');
    process.exit(0);
  }
  console.error('UI smoke: no Chrome/Edge found; set CHROME_PATH or SKIP_UI_SMOKE=1');
  process.exit(1);
}

let failed = false;
function check(label, ok, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!ok) failed = true;
}

const stamp = Date.now();
const employeeNo = `UISEED${stamp}`;

async function listEmployees() {
  const response = await fetch(`${BACKEND}/api/employees`, { headers: { Accept: 'application/json' } });
  const body = await response.json();
  return Array.isArray(body?.data) ? body.data : [];
}

function rowText(text) {
  return `([...document.querySelectorAll('.el-table__row')].map((row) => row.innerText).find((value) => value.includes(${JSON.stringify(text)})) || '')`;
}

const browser = await new Browser({ chromePath: chrome, width: 1440, height: 940 }).launch();
try {
  await browser.goto(`${URL}/#/employees`, { waitMs: 1200 });
  await browser.waitFor(`document.querySelector('[data-testid="employees-empty"]')`, 20000);
  check('EMP-01 empty state visible on empty database', true);

  // 新增入口 -> 空表单
  await browser.click('[data-testid="create-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="submit-employee"]')`, 10000);
  check('EMP-02 create entry opens empty form',
    (await browser.evaluate(`location.hash`)).includes('/employees/new')
      && (await browser.evaluate(`document.querySelector('[data-testid="field-employeeNo"]')?.value ?? 'x'`)) === '');

  // 空提交：前端拦截，不发写请求
  await browser.click('[data-testid="submit-employee"]');
  await browser.waitFor(`document.querySelectorAll('.el-form-item__error').length > 1`, 10000);
  const validationText = await browser.text('.el-form-item__error');
  check('EMP-02 empty submit blocked by client validation', /不能为空/.test(validationText), validationText);
  check('EMP-02 invalid submit sent no request', (await listEmployees()).length === 0);

  // 填写合法数据并保存
  await browser.fill('[data-testid="field-employeeNo"]', employeeNo);
  await browser.fill('[data-testid="field-name"]', '界面测试员工');
  await browser.fill('[data-testid="field-department"]', '研发部');
  await browser.fill('[data-testid="field-position"]', '前端工程师');
  await browser.fillAndBlur('[data-testid="field-hireDate"] input', '2024-03-15');
  await browser.fill('[data-testid="field-phone"]', '13900139000');
  await browser.fill('[data-testid="field-email"]', 'ui-smoke@example.com');
  await browser.click('[data-testid="submit-employee"]');
  await browser.waitFor(`document.querySelector('.el-message--success')`, 15000);
  await browser.waitFor(`location.hash.includes('/employees') && !location.hash.includes('/new')`, 15000);
  await browser.waitFor(`document.querySelector('[data-testid="employee-table"]')`, 15000);
  await browser.waitFor(`document.querySelectorAll('.el-table__row').length === 1`, 15000);

  const createdRows = await listEmployees();
  check('EMP-02 created record persisted',
    createdRows.length === 1
      && createdRows[0].employeeNo === employeeNo
      && createdRows[0].name === '界面测试员工'
      && createdRows[0].hireDate === '2024-03-15'
      && createdRows[0].phone === '13900139000',
    JSON.stringify(createdRows[0] || null));

  const firstRow = await browser.evaluate(rowText(employeeNo));
  check('EMP-02 list shows new record fields',
    firstRow.includes('界面测试员工') && firstRow.includes('研发部') && firstRow.includes('2024-03-15'));

  // 唯一性冲突：表单保持打开并给出明确提示
  await browser.click('[data-testid="create-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="submit-employee"]')`, 10000);
  await browser.fill('[data-testid="field-employeeNo"]', employeeNo);
  await browser.fill('[data-testid="field-name"]', '重复工号员工');
  await browser.fill('[data-testid="field-department"]', '研发部');
  await browser.fill('[data-testid="field-position"]', '测试');
  await browser.fillAndBlur('[data-testid="field-hireDate"] input', '2024-04-01');
  await browser.click('[data-testid="submit-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="form-error"]')`, 15000);
  const conflictText = await browser.text('[data-testid="form-error"]');
  check('EMP-02 uniqueness conflict shows distinct message',
    conflictText.includes('工号已存在') && (await browser.evaluate(`location.hash`)).includes('/employees/new'),
    conflictText.replace(/\s+/g, ' ').slice(0, 120));
  check('EMP-02 conflict attempt created no record', (await listEmployees()).length === 1);

  // 取消新增不写入
  await browser.click('[data-testid="cancel-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="employee-table"]')`, 10000);
  check('EMP-02 cancel returns to list without write', (await listEmployees()).length === 1);

  // 编辑回填 + 保存
  await browser.click('[data-testid="edit-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="submit-employee"]')`, 10000);
  await browser.waitFor(`document.querySelector('[data-testid="field-name"]')?.value === '界面测试员工'`, 15000);
  const prefilled = await browser.evaluate(`({
    no: document.querySelector('[data-testid="field-employeeNo"]')?.value ?? '',
    noDisabled: document.querySelector('[data-testid="field-employeeNo"]')?.disabled ?? false,
    name: document.querySelector('[data-testid="field-name"]')?.value ?? '',
    department: document.querySelector('[data-testid="field-department"]')?.value ?? '',
    hireDate: document.querySelector('[data-testid="field-hireDate"] input')?.value ?? '',
  })`);
  check('EMP-03 edit form prefilled with existing data',
    prefilled.no === employeeNo
      && prefilled.noDisabled === true
      && prefilled.name === '界面测试员工'
      && prefilled.department === '研发部'
      && prefilled.hireDate === '2024-03-15',
    JSON.stringify(prefilled));

  await browser.fill('[data-testid="field-name"]', '界面测试员工改');
  await browser.fill('[data-testid="field-department"]', '平台部');
  await browser.click('[data-testid="submit-employee"]');
  await browser.waitFor(`document.querySelector('.el-message--success')`, 15000);
  await browser.waitFor(`document.querySelector('[data-testid="employee-table"]')`, 15000);
  await browser.waitFor(`document.querySelectorAll('.el-table__row').length === 1 && document.querySelector('.el-table__row').innerText.includes('界面测试员工改')`, 15000);
  const updatedRows = await listEmployees();
  check('EMP-03 update persisted and list refreshed',
    updatedRows[0].name === '界面测试员工改'
      && updatedRows[0].department === '平台部'
      && updatedRows[0].employeeNo === employeeNo,
    JSON.stringify(updatedRows[0]));

  // 编辑后取消：不写入
  await browser.click('[data-testid="edit-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="field-name"]')?.value === '界面测试员工改'`, 15000);
  await browser.fill('[data-testid="field-name"]', '取消不应保存');
  await browser.click('[data-testid="cancel-employee"]');
  await browser.waitFor(`document.querySelector('[data-testid="employee-table"]')`, 10000);
  await browser.waitFor(`document.querySelector('.el-table__row')?.innerText.includes('界面测试员工改')`, 10000);
  check('EMP-03 cancel keeps original values', (await listEmployees())[0].name === '界面测试员工改');

  // 列表不提供未确认的写操作
  const buttonTexts = await browser.evaluate(`[...document.querySelectorAll('button')].map((button) => button.innerText.trim()).filter(Boolean)`);
  check('EMP-01 no delete/batch operations exposed',
    !buttonTexts.some((text) => /删除|批量|导入|导出/.test(text)),
    buttonTexts.join('|'));

  // 错误态 + 重试（只阻断列表接口，页面资源仍可加载）
  await browser.setBlockedUrls(['*/api/employees*']);
  await browser.reload({ waitMs: 800 });
  await browser.waitFor(`document.querySelector('[data-testid="employees-error"]')`, 20000);
  check('EMP-01 read failure shows error state', true);
  await browser.setBlockedUrls([]);
  await browser.click('[data-testid="retry-load"]');
  await browser.waitFor(`document.querySelector('[data-testid="employee-table"]')`, 15000);
  await browser.waitFor(`document.querySelectorAll('.el-table__row').length === 1`, 15000);
  check('EMP-01 retry after failure renders list', true);

  if (process.env.UI_SMOKE_SCREENSHOT) {
    const file = path.resolve('verify', 'ui-smoke.png');
    await browser.screenshot(file);
    console.log('screenshot:', file);
  }

  const errors = browser.consoleLogs.filter((entry) => entry.type === 'error' && !/favicon/i.test(entry.text));
  if (browser.exceptions.length || errors.length) {
    console.error('browser errors:', browser.exceptions.join(' | '), errors.map((entry) => entry.text).join(' | '));
    failed = true;
  }
} catch (error) {
  console.error('UI smoke failed:', error.message);
  try {
    console.error('diagnostics:', JSON.stringify(await browser.evaluate(`({
      hash: location.hash,
      errors: [...document.querySelectorAll('.el-form-item__error')].map((el) => el.innerText),
      alert: document.querySelector('[data-testid="form-error"]')?.innerText ?? '',
      messages: [...document.querySelectorAll('.el-message')].map((el) => el.className + '|' + el.innerText),
      values: {
        no: document.querySelector('[data-testid="field-employeeNo"]')?.value ?? '',
        name: document.querySelector('[data-testid="field-name"]')?.value ?? '',
        date: document.querySelector('[data-testid="field-hireDate"] input')?.value ?? '',
        phone: document.querySelector('[data-testid="field-phone"]')?.value ?? '',
        email: document.querySelector('[data-testid="field-email"]')?.value ?? '',
      },
    })`)));
  } catch { /* page may be gone */ }
  failed = true;
} finally {
  await browser.close();
}

if (failed) process.exitCode = 1;
else console.log('UI smoke: PASS');
