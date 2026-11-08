package com.example.workbenchperfa7.repository;

import com.example.workbenchperfa7.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** 列表按创建顺序（id 升序）返回；本批次不做分页/排序参数。 */
    List<Employee> findAllByOrderByIdAsc();

    boolean existsByEmployeeNoIgnoreCase(String employeeNo);
}
