# AGENTS.md · workbench-perf-a7

应用：员工信息管理（列表 / 新增 / 编辑）。后端 `backend/`，前端 `frontend/`，启停与验证脚本在根目录。

## 工程事实

| 项目 | 栈与版本 | 构建 | 测试 | 运行 |
| --- | --- | --- | --- | --- |
| 后端 | Spring Boot 3.5.16、Spring Cloud 2025.0.3、Java 17、H2 + Flyway + JPA | `cd backend && mvn -q -DskipTests package` | `cd backend && mvn test` | `node scripts/app.mjs start local` |
| 前端 | Vue 3.5.45、Vite 7.3.7、TypeScript 5.9.3、Element Plus 2.14.5 | `cd frontend && npm run build` | `cd frontend && npm run typecheck` | `cd frontend && npm run dev` |

- 后端端口 `8080`，前端开发端口 `5173`（Vite 代理 `/api`、`/actuator` 到 8080）。
- 后端在 `local`/`verify` 下同时托管 `frontend/dist`，访问 `http://127.0.0.1:8080/#/employees`。
- `local`：H2 文件库（`./data`）；`verify`：H2 内存库（每次启动为空，用于可复现验证）。
- 依赖使用工作区已有的 `frontend/node_modules`（版本见 `package.json`），**禁止 `npm install`**，也不要读取 `node_modules` 源码猜 API。

## 命令策略（性能相关）

- 迭代期：`cd backend && mvn -q -DskipTests compile`、定向 `mvn -Dtest=EmployeeApiTests test`、`cd frontend && npm run typecheck`。
- 提交前一次性：`mvn test`（全量）、`npm run build`、`node scripts/verify.mjs`（UI smoke + API smoke，只启停一次）。完整验证不要反复跑。
- 页面/接口专项检查放在 `tools/`，复用 `tools/cdp.mjs`，不要新建临时 Playwright 工程。

## 目录与约定

- `backend/src/main/java/com/example/workbenchperfa7/controller|service|repository|domain|exception`：后端分层。
- `backend/src/main/resources/db/migration`：Flyway 迁移；表结构以迁移为准，JPA 仅 `validate`。
- `frontend/src/api`：所有 HTTP 调用集中封装，视图不直接发请求。
- `frontend/src/views` / `utils` / `router` / `styles`：页面、展示映射、路由（hash 模式）、样式。
- 员工字段、校验规则、接口与错误语义见 `docs/employee-spec.md`，是 EMP-01~04 的同源依据。
- 不修改 `pom.xml` 的 Spring Boot/Cloud 版本与 `vite.config.ts` 代理约定；业务需要时先说明。

## Git 与交付

- 开发开始与提交前各同步一次远端主干（`git fetch` + `git merge`），使用 merge，不强推。
- 交付证据写入 `.opencode/delivery/<批次ID>.md`，随代码一起提交并推送。
- 批次需求以 `.opencode/batch/` 内规格为准；发现需求缺项记录到交付证据的"未解决项"。
