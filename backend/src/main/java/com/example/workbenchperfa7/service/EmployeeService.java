package com.example.workbenchperfa7.service;

import com.example.workbenchperfa7.controller.EmployeeRequest;
import com.example.workbenchperfa7.domain.Employee;
import com.example.workbenchperfa7.domain.EmployeeStatus;
import com.example.workbenchperfa7.domain.Gender;
import com.example.workbenchperfa7.exception.BizException;
import com.example.workbenchperfa7.exception.FieldErrorItem;
import com.example.workbenchperfa7.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmployeeService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<Employee> list() {
        return employeeRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Employee get(long id) {
        return employeeRepository.findById(id).orElseThrow(EmployeeService::notFound);
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        String employeeNo = request.employeeNo().trim();
        if (employeeRepository.existsByEmployeeNoIgnoreCase(employeeNo)) {
            throw duplicateEmployeeNo();
        }
        Employee employee = new Employee();
        employee.setEmployeeNo(employeeNo);
        applyEditableFields(employee, request);
        LocalDateTime now = now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            // 并发写入时唯一约束是最终防线，仍归类为唯一性冲突。
            throw duplicateEmployeeNo();
        }
    }

    @Transactional
    public Employee update(long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id).orElseThrow(EmployeeService::notFound);
        if (!employee.getEmployeeNo().equalsIgnoreCase(request.employeeNo().trim())) {
            throw new BizException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "工号不可修改",
                    List.of(new FieldErrorItem("employeeNo", "工号不可修改")));
        }
        applyEditableFields(employee, request);
        employee.setUpdatedAt(monotonicNow(employee.getUpdatedAt()));
        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmployeeNo();
        }
    }

    private void applyEditableFields(Employee employee, EmployeeRequest request) {
        employee.setName(request.name().trim());
        employee.setDepartment(request.department().trim());
        employee.setJobTitle(request.position().trim());
        employee.setHireDate(request.hireDate());
        employee.setPhone(emptyToNull(request.phone()));
        employee.setEmail(emptyToNull(request.email()));
        employee.setGender(request.gender() == null ? Gender.UNKNOWN : request.gender());
        employee.setStatus(request.status() == null ? EmployeeStatus.ACTIVE : request.status());
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 保证 updatedAt 严格递增，避免同一秒内多次保存产生相同时间戳。 */
    private static LocalDateTime monotonicNow(LocalDateTime previous) {
        LocalDateTime now = now();
        if (previous != null && !now.isAfter(previous)) {
            return previous.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        }
        return now;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    private static BizException notFound() {
        return new BizException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "员工记录不存在");
    }

    private static BizException duplicateEmployeeNo() {
        return new BizException(HttpStatus.CONFLICT, "DUPLICATE_EMPLOYEE_NO", "工号已存在，请更换后重试",
                List.of(new FieldErrorItem("employeeNo", "工号已存在，请更换后重试")));
    }
}
