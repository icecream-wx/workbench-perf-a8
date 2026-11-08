-- 员工表：字段与校验规则见 docs/employee-spec.md（EMP-04）。
create table employee (
    id          bigint auto_increment primary key,
    employee_no varchar(20)  not null,
    name        varchar(30)  not null,
    department  varchar(50)  not null,
    job_title   varchar(50)  not null,
    hire_date   date         not null,
    phone       varchar(20),
    email       varchar(100),
    gender      varchar(10)  not null,
    status      varchar(10)  not null,
    created_at  timestamp    not null,
    updated_at  timestamp    not null,
    constraint uk_employee_employee_no unique (employee_no)
);
