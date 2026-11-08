package com.example.workbenchperfa7.controller;

import com.example.workbenchperfa7.exception.ApiResponse;
import com.example.workbenchperfa7.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** 列表：最小可用列表，按创建顺序返回全部记录（不做分页/搜索/排序）。 */
    @GetMapping
    public ApiResponse<List<EmployeeResponse>> list() {
        List<EmployeeResponse> items = employeeService.list().stream()
                .map(EmployeeResponse::from)
                .toList();
        return ApiResponse.okList(items);
    }

    /** 编辑回填所需的单条读取。 */
    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> detail(@PathVariable long id) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = EmployeeResponse.from(employeeService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable long id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.update(id, request)));
    }
}
