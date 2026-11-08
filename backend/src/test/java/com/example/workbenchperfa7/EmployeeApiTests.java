package com.example.workbenchperfa7;

import com.example.workbenchperfa7.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EMP-01/02/03 的后端行为与 EMP-04 的四类错误语义。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Map<String, Object> validBody(String employeeNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("employeeNo", employeeNo);
        body.put("name", "张三");
        body.put("department", "研发部");
        body.put("position", "后端工程师");
        body.put("hireDate", "2024-01-31");
        body.put("phone", "13800138000");
        body.put("email", "zhangsan@example.com");
        body.put("gender", "MALE");
        body.put("status", "ACTIVE");
        return body;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private long createEmployee(String employeeNo) throws Exception {
        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validBody(employeeNo))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void listIsEmptyBeforeAnyCreate() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void createReturnsCreatedEmployee() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validBody("E1001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.department").value("研发部"))
                .andExpect(jsonPath("$.data.position").value("后端工程师"))
                .andExpect(jsonPath("$.data.hireDate").value("2024-01-31"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.email").value("zhangsan@example.com"))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));
    }

    @Test
    void createNormalizesOptionalFields() throws Exception {
        Map<String, Object> body = validBody("E1002");
        body.put("phone", "");
        body.put("email", "");
        body.remove("gender");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phone").value(nullValue()))
                .andExpect(jsonPath("$.data.email").value(nullValue()))
                .andExpect(jsonPath("$.data.gender").value("UNKNOWN"));
    }

    @Test
    void createRejectsBlankEmployeeNoWithFieldError() throws Exception {
        Map<String, Object> body = validBody("E1003");
        body.put("employeeNo", "  ");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", hasItem("employeeNo")))
                .andExpect(jsonPath("$.error.fieldErrors[*].message", hasItem("工号不能为空")));
        assertThat(employeeRepository.count()).isZero();
    }

    @Test
    void createRejectsInvalidPhoneAndEmail() throws Exception {
        Map<String, Object> body = validBody("E1004");
        body.put("phone", "12345");
        body.put("email", "not-an-email");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", containsInAnyOrder("phone", "email")));
    }

    @Test
    void createRejectsMalformedHireDate() throws Exception {
        Map<String, Object> body = validBody("E1005");
        body.put("hireDate", "2024-13-01");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", hasItem("hireDate")));
    }

    @Test
    void createRejectsUnknownStatus() throws Exception {
        Map<String, Object> body = validBody("E1006");
        body.put("status", "ON_LEAVE");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", hasItem("status")));
    }

    @Test
    void duplicateEmployeeNoReturnsConflictWithoutSecondRecord() throws Exception {
        createEmployee("E1007");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validBody("e1007"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMPLOYEE_NO"))
                .andExpect(jsonPath("$.error.message").value("工号已存在，请更换后重试"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", contains("employeeNo")));
        assertThat(employeeRepository.count()).isEqualTo(1);
    }

    @Test
    void listReturnsAllEmployeesInCreationOrder() throws Exception {
        createEmployee("E2001");
        createEmployee("E2002");

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].employeeNo", contains("E2001", "E2002")));
    }

    @Test
    void detailReturnsEmployeeById() throws Exception {
        long id = createEmployee("E3001");

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("E3001"))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    @Test
    void detailReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void updateChangesSingleEmployee() throws Exception {
        long targetId = createEmployee("E4001");
        createEmployee("E4002");

        Map<String, Object> body = validBody("E4001");
        body.put("name", "李四");
        body.put("department", "平台部");
        body.put("status", "INACTIVE");

        mockMvc.perform(put("/api/employees/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(targetId))
                .andExpect(jsonPath("$.data.name").value("李四"))
                .andExpect(jsonPath("$.data.department").value("平台部"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get("/api/employees"))
                .andExpect(jsonPath("$.data[*].name", contains("李四", "张三")));
    }

    @Test
    void updateMissingEmployeeReturnsNotFoundAndCreatesNothing() throws Exception {
        mockMvc.perform(put("/api/employees/{id}", 424242)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validBody("E5001"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_NOT_FOUND"));
        assertThat(employeeRepository.count()).isZero();
    }

    @Test
    void updateRejectsEmployeeNoChange() throws Exception {
        long id = createEmployee("E6001");
        Map<String, Object> body = validBody("E6002");

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", contains("employeeNo")));

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(jsonPath("$.data.employeeNo").value("E6001"));
    }

    @Test
    void updateWithUnchangedValuesSucceedsWithoutSideEffects() throws Exception {
        long id = createEmployee("E7001");

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validBody("E7001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张三"));

        mockMvc.perform(get("/api/employees"))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
