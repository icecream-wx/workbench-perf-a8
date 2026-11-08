<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { createEmployee, getEmployee, updateEmployee } from '../api/employees';
import { ApiRequestError } from '../api/http';
import type { EmployeeInput, EmployeeStatus, Gender } from '../api/types';

interface FormModel {
  employeeNo: string;
  name: string;
  department: string;
  position: string;
  hireDate: string;
  phone: string;
  email: string;
  gender: Gender;
  status: EmployeeStatus;
}

const route = useRoute();
const router = useRouter();

const isEdit = computed(() => route.name === 'employee-edit');
const employeeId = computed<number | null>(() => {
  const raw = route.params.id;
  const value = Number(Array.isArray(raw) ? raw[0] : raw);
  return Number.isInteger(value) && value > 0 ? value : null;
});

const formRef = ref<FormInstance>();
const form = reactive<FormModel>({
  employeeNo: '',
  name: '',
  department: '',
  position: '',
  hireDate: '',
  phone: '',
  email: '',
  gender: 'UNKNOWN',
  status: 'ACTIVE',
});

const loading = ref(false);
const loadError = ref('');
const submitting = ref(false);
const submitError = ref<{ title: string; message: string } | null>(null);

// 与后端 EmployeeRequest 保持同一套规则（EMP-04）。
const rules: FormRules<FormModel> = {
  employeeNo: [
    { required: true, whitespace: true, message: '工号不能为空', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{2,20}$/, message: '工号需为2-20位字母、数字、下划线或短横线', trigger: 'blur' },
  ],
  name: [
    { required: true, whitespace: true, message: '姓名不能为空', trigger: 'blur' },
    { max: 30, message: '姓名长度不能超过30个字符', trigger: 'blur' },
  ],
  department: [
    { required: true, whitespace: true, message: '部门不能为空', trigger: 'blur' },
    { max: 50, message: '部门长度不能超过50个字符', trigger: 'blur' },
  ],
  position: [
    { required: true, whitespace: true, message: '职位不能为空', trigger: 'blur' },
    { max: 50, message: '职位长度不能超过50个字符', trigger: 'blur' },
  ],
  hireDate: [{ required: true, message: '入职日期不能为空', trigger: 'change' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [
    { pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '请输入正确的邮箱地址', trigger: 'blur' },
    { max: 100, message: '邮箱长度不能超过100个字符', trigger: 'blur' },
  ],
  status: [{ required: true, message: '员工状态不能为空', trigger: 'change' }],
};

async function loadEmployee(): Promise<void> {
  if (!isEdit.value) {
    return;
  }
  if (employeeId.value === null) {
    loadError.value = '员工标识无效，请返回列表后重新进入编辑';
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    const employee = await getEmployee(employeeId.value);
    form.employeeNo = employee.employeeNo;
    form.name = employee.name;
    form.department = employee.department;
    form.position = employee.position;
    form.hireDate = employee.hireDate;
    form.phone = employee.phone ?? '';
    form.email = employee.email ?? '';
    form.gender = employee.gender;
    form.status = employee.status;
  } catch (error) {
    loadError.value = describeError(error).message;
  } finally {
    loading.value = false;
  }
}

onMounted(loadEmployee);

function describeError(error: unknown): { title: string; message: string } {
  if (error instanceof ApiRequestError) {
    if (error.code === 'DUPLICATE_EMPLOYEE_NO') {
      return { title: '工号已存在', message: '该工号已被其他员工使用，请更换工号后重试。' };
    }
    if (error.code === 'EMPLOYEE_NOT_FOUND') {
      return { title: '员工记录不存在', message: '该员工可能已被删除，请返回列表刷新后重试。' };
    }
    if (error.code === 'VALIDATION_ERROR') {
      const fieldMessage = error.fieldErrors[0]?.message;
      return { title: '提交内容校验未通过', message: fieldMessage || error.message };
    }
    if (error.status === 500) {
      return { title: '保存失败', message: '服务器处理异常，请稍后重试。' };
    }
    return { title: '保存失败', message: error.message };
  }
  return { title: '保存失败', message: '网络异常，请稍后重试。' };
}

function toInput(): EmployeeInput {
  return {
    employeeNo: form.employeeNo.trim(),
    name: form.name.trim(),
    department: form.department.trim(),
    position: form.position.trim(),
    hireDate: form.hireDate,
    phone: form.phone.trim() ? form.phone.trim() : null,
    email: form.email.trim() ? form.email.trim() : null,
    gender: form.gender,
    status: form.status,
  };
}

async function submit(): Promise<void> {
  if (submitting.value || loading.value || loadError.value) {
    return;
  }
  submitting.value = true;
  submitError.value = null;
  try {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) {
      return;
    }
    if (isEdit.value && employeeId.value !== null) {
      await updateEmployee(employeeId.value, toInput());
      ElMessage.success('员工信息已更新');
    } else {
      await createEmployee(toInput());
      ElMessage.success('新增员工成功');
    }
    void router.push({ name: 'employee-list' });
  } catch (error) {
    submitError.value = describeError(error);
  } finally {
    submitting.value = false;
  }
}

function goBack(): void {
  void router.push({ name: 'employee-list' });
}
</script>

<template>
  <section class="page page--form">
    <header class="page-header">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑员工' : '新增员工' }}</h1>
        <p class="page-subtitle">
          {{ isEdit ? '修改员工信息后保存，工号不可修改' : '填写员工基本信息，带 * 为必填项' }}
        </p>
      </div>
      <el-button data-testid="back-to-list" @click="goBack">返回列表</el-button>
    </header>

    <el-skeleton v-if="loading" class="state-panel" :rows="8" animated data-testid="form-loading" />

    <div v-else-if="loadError" class="state-panel" data-testid="form-load-error">
      <el-alert type="error" :closable="false" show-icon title="员工信息加载失败" :description="loadError" />
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <div v-else class="form-card">
      <el-alert
        v-if="submitError"
        class="form-alert"
        data-testid="form-error"
        type="error"
        :closable="false"
        show-icon
        :title="submitError.title"
        :description="submitError.message"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
        @submit.prevent="submit"
      >
        <el-form-item label="工号" prop="employeeNo">
          <el-input
            v-model="form.employeeNo"
            data-testid="field-employeeNo"
            :disabled="isEdit"
            maxlength="20"
            placeholder="如 E1001"
          />
          <p v-if="isEdit" class="field-hint">工号创建后不可修改</p>
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" data-testid="field-name" maxlength="30" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" data-testid="field-department" maxlength="50" placeholder="如 研发部" />
        </el-form-item>

        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" data-testid="field-position" maxlength="50" placeholder="如 后端工程师" />
        </el-form-item>

        <el-form-item label="入职日期" prop="hireDate">
          <div class="field-control" data-testid="field-hireDate">
            <el-date-picker
              v-model="form.hireDate"
              type="date"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              placeholder="请选择入职日期"
              style="width: 100%"
            />
          </div>
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" data-testid="field-phone" maxlength="11" placeholder="11 位手机号，选填" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" data-testid="field-email" maxlength="100" placeholder="选填" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" data-testid="field-gender" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" data-testid="field-status" style="width: 100%">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="离职" value="INACTIVE" />
          </el-select>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button
            type="primary"
            native-type="submit"
            data-testid="submit-employee"
            :loading="submitting"
            :disabled="submitting"
          >
            {{ submitting ? '保存中…' : '保存' }}
          </el-button>
          <el-button data-testid="cancel-employee" :disabled="submitting" @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>
