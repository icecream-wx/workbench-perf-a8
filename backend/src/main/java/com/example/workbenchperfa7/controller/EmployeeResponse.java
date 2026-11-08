package com.example.workbenchperfa7.controller;

import com.example.workbenchperfa7.domain.Employee;
import com.example.workbenchperfa7.domain.EmployeeStatus;
import com.example.workbenchperfa7.domain.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工响应体：字段名与前端 Employee 类型一致。
 */
public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        String department,
        String position,
        LocalDate hireDate,
        String phone,
        String email,
        Gender gender,
        EmployeeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getHireDate(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getGender(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
