package com.example.workbenchperfa7.controller;

import com.example.workbenchperfa7.domain.EmployeeStatus;
import com.example.workbenchperfa7.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 新增/编辑共用请求体：新增与编辑对同一字段使用同一份校验规则（EMP-04）。
 */
public record EmployeeRequest(
        @NotBlank(message = "工号不能为空")
        @Pattern(regexp = "^$|^[A-Za-z0-9_-]{2,20}$", message = "工号需为2-20位字母、数字、下划线或短横线")
        String employeeNo,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 30, message = "姓名长度不能超过30个字符")
        String name,

        @NotBlank(message = "部门不能为空")
        @Size(max = 50, message = "部门长度不能超过50个字符")
        String department,

        @NotBlank(message = "职位不能为空")
        @Size(max = 50, message = "职位长度不能超过50个字符")
        String position,

        @NotNull(message = "入职日期不能为空")
        LocalDate hireDate,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的手机号")
        String phone,

        @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "请输入正确的邮箱地址")
        @Size(max = 100, message = "邮箱长度不能超过100个字符")
        String email,

        Gender gender,

        @NotNull(message = "员工状态不能为空")
        EmployeeStatus status
) {
}
