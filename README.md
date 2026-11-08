# workbench-perf-a7 · 员工信息管理

员工信息管理应用（列表 / 新增 / 编辑），前端 Vue 3 + Vite + TypeScript + Element Plus，后端 Spring Boot + Spring Cloud，本地使用 H2 文件库。

## 工程事实

| 项目 | 目录 | 栈与版本 | 构建 | 测试 | 运行 |
| --- | --- | --- | --- | --- | --- |
| 后端 | `backend/` | Spring Boot 3.5.16、Spring Cloud 2025.0.3、Java 17 | `mvn -q -DskipTests package` | `mvn test` | `java -jar target/app.jar` |
| 前端 | `frontend/` | Vue 3.5.45、Vite 7.3.7、TypeScript 5.9.3、Element Plus 2.14.5 | `npm run build` | `npm run typecheck` | `npm run dev` |

## 命令

- 构建：`build.cmd`（Windows）／`build.sh`
- 启动：`start.cmd`（默认 `local` 外置 H2 文件库，后端同时托管前端构建产物）／`start.sh`
- 停止：`stop.cmd`／`stop.sh`
- 一键验证（仅提交前跑一次）：`node scripts/verify.mjs`
- 后端迭代：`cd backend && mvn -q -DskipTests compile`
- 后端定向测试：`cd backend && mvn -Dtest=EmployeeApiTests test`
- 前端迭代：`cd frontend && npm run typecheck`
- 默认访问：前端 `http://127.0.0.1:8080/#/employees`，健康检查 `http://127.0.0.1:8080/actuator/health`

`npm install` 不允许执行；`node_modules` 使用工作区已安装的依赖（版本见 `package.json`）。

## 目录约定

- `backend/src/main/java/com/example/workbenchperfa7/controller|service|repository|domain|exception`：后端分层
- `backend/src/main/resources/db/migration`：Flyway 迁移
- `frontend/src/api`：所有业务接口统一封装在此，不在视图内直接发请求
- `frontend/src/views`／`components`／`composables`：页面、组件、组合式逻辑
- `scripts/`：启停与验证脚本；`tools/`：接口与页面专项检查（CDP，无额外依赖）

## 业务约定

- 员工字段、校验规则、接口与错误语义见 `docs/employee-spec.md`（EMP-04 的同源落地文档）。
- 列表接口按需求"最小可用列表"实现：不分页、不搜索、不排序（默认按创建顺序 `id` 升序），不提供删除/批量写操作。
