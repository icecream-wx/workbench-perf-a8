#!/usr/bin/env node
/**
 * 接口层自测：EMP-01/02/03 的读写行为 + EMP-04 的四类错误语义。
 * 依赖真实启动的后端（verify/local profile）。
 */
const BACKEND = process.env.APP_BACKEND_URL || 'http://127.0.0.1:8080';

let failed = false;
function check(label, ok, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!ok) failed = true;
}

async function call(path, options = {}) {
  const response = await fetch(BACKEND + path, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {}),
    },
  });
  const text = await response.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* non-JSON */ }
  return { status: response.status, json, text };
}

const fieldNames = (body) => (body?.error?.fieldErrors ?? []).map((item) => item.field);
const errorShapeOk = (body) =>
  body?.success === false && typeof body?.error?.code === 'string' && typeof body?.error?.message === 'string';

function employeeBody(employeeNo, overrides = {}) {
  return {
    employeeNo,
    name: '接口烟测员工',
    department: '研发部',
    position: '后端工程师',
    hireDate: '2024-01-31',
    phone: '13800138000',
    email: 'smoke@example.com',
    gender: 'MALE',
    status: 'ACTIVE',
    ...overrides,
  };
}

async function listEmployees() {
  const listed = await call('/api/employees');
  return { ...listed, items: Array.isArray(listed.json?.data) ? listed.json.data : [] };
}

const stamp = Date.now();
const employeeNo = `API${stamp}`;

try {
  const health = await call('/actuator/health');
  check('health UP', health.status === 200 && health.json?.status === 'UP', health.text.slice(0, 120));

  // Spring Cloud Context 提供的刷新端点，用于确认 Spring Cloud 能力已接入。
  const refresh = await call('/actuator/refresh', { method: 'POST' });
  check('spring-cloud refresh endpoint', refresh.status === 200 && Array.isArray(refresh.json), refresh.text.slice(0, 120));

  const before = await listEmployees();
  check('list returns plain array', before.status === 200 && before.json?.success === true && Array.isArray(before.json?.data));
  const initialCount = before.items.length;

  const created = await call('/api/employees', { method: 'POST', body: JSON.stringify(employeeBody(employeeNo)) });
  const createdEmployee = created.json?.data;
  check('create 201 with echoed fields',
    created.status === 201
      && createdEmployee?.employeeNo === employeeNo
      && createdEmployee?.name === '接口烟测员工'
      && createdEmployee?.department === '研发部'
      && createdEmployee?.position === '后端工程师'
      && createdEmployee?.hireDate === '2024-01-31'
      && createdEmployee?.status === 'ACTIVE'
      && Number.isInteger(createdEmployee?.id),
    created.text.slice(0, 200));

  const listed = await listEmployees();
  check('list contains created employee',
    listed.status === 200 && listed.items.some((item) => item.employeeNo === employeeNo),
    listed.text.slice(0, 160));
  check('list count increased by 1', listed.items.length === initialCount + 1);

  const detail = await call(`/api/employees/${createdEmployee?.id}`);
  check('detail returns same employee',
    detail.status === 200 && detail.json?.data?.employeeNo === employeeNo,
    detail.text.slice(0, 160));

  const updatedBody = employeeBody(employeeNo, {
    name: '接口烟测员工改',
    department: '平台部',
    position: '高级工程师',
    status: 'INACTIVE',
  });
  const updated = await call(`/api/employees/${createdEmployee?.id}`, {
    method: 'PUT',
    body: JSON.stringify(updatedBody),
  });
  check('update applies edited fields',
    updated.status === 200
      && updated.json?.data?.name === '接口烟测员工改'
      && updated.json?.data?.department === '平台部'
      && updated.json?.data?.position === '高级工程师'
      && updated.json?.data?.status === 'INACTIVE'
      && updated.json?.data?.employeeNo === employeeNo,
    updated.text.slice(0, 200));

  const duplicate = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify(employeeBody(employeeNo.toLowerCase())),
  });
  check('duplicate employeeNo -> 409 DUPLICATE_EMPLOYEE_NO',
    duplicate.status === 409
      && errorShapeOk(duplicate.json)
      && duplicate.json?.error?.code === 'DUPLICATE_EMPLOYEE_NO'
      && fieldNames(duplicate.json).includes('employeeNo'),
    duplicate.text.slice(0, 160));
  const afterDuplicate = await listEmployees();
  check('duplicate attempt created no record', afterDuplicate.items.length === initialCount + 1);

  const blankNo = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify(employeeBody('  ')),
  });
  check('blank employeeNo -> 400 VALIDATION_ERROR',
    blankNo.status === 400
      && errorShapeOk(blankNo.json)
      && blankNo.json?.error?.code === 'VALIDATION_ERROR'
      && fieldNames(blankNo.json).includes('employeeNo'),
    blankNo.text.slice(0, 160));

  const badContact = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify(employeeBody(`APIB${stamp}`, { phone: '12345', email: 'bad-email' })),
  });
  check('invalid phone/email -> 400 with field names',
    badContact.status === 400
      && fieldNames(badContact.json).includes('phone')
      && fieldNames(badContact.json).includes('email'),
    badContact.text.slice(0, 160));

  const badDate = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify(employeeBody(`APID${stamp}`, { hireDate: '2024-13-40' })),
  });
  check('invalid hireDate -> 400 field hireDate',
    badDate.status === 400 && fieldNames(badDate.json).includes('hireDate'),
    badDate.text.slice(0, 160));

  const badStatus = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify(employeeBody(`APIS${stamp}`, { status: 'ON_LEAVE' })),
  });
  check('invalid status -> 400 field status',
    badStatus.status === 400 && fieldNames(badStatus.json).includes('status'),
    badStatus.text.slice(0, 160));

  const missing = await call('/api/employees/987654321', {
    method: 'PUT',
    body: JSON.stringify(employeeBody(`APIM${stamp}`)),
  });
  check('update missing employee -> 404 EMPLOYEE_NOT_FOUND',
    missing.status === 404 && missing.json?.error?.code === 'EMPLOYEE_NOT_FOUND',
    missing.text.slice(0, 160));
  const afterMissing = await listEmployees();
  check('missing update created nothing', afterMissing.items.length === initialCount + 1);

  const changeNo = await call(`/api/employees/${createdEmployee?.id}`, {
    method: 'PUT',
    body: JSON.stringify(employeeBody(`APICHG${stamp}`)),
  });
  check('changing employeeNo -> 400 field employeeNo',
    changeNo.status === 400 && fieldNames(changeNo.json).includes('employeeNo'),
    changeNo.text.slice(0, 160));
  const unchanged = await call(`/api/employees/${createdEmployee?.id}`);
  check('employeeNo stays unchanged', unchanged.json?.data?.employeeNo === employeeNo);

  const deleteAttempt = await call(`/api/employees/${createdEmployee?.id}`, { method: 'DELETE' });
  check('delete is not supported', deleteAttempt.status === 405, `status=${deleteAttempt.status}`);

  const index = await fetch(BACKEND + '/');
  const html = await index.text();
  check('frontend index served by backend',
    index.status === 200 && html.includes('id="app"'),
    `status=${index.status}`);
  const assetPath = html.match(/src="(\/assets\/[^"]+\.js)"/)?.[1];
  const asset = assetPath ? await fetch(BACKEND + assetPath) : null;
  check('frontend bundle asset reachable', Boolean(assetPath && asset && asset.status === 200), assetPath || 'no asset found');
} catch (error) {
  console.error('API smoke failed:', error.message);
  failed = true;
}

if (failed) process.exitCode = 1;
else console.log('API smoke: PASS');
