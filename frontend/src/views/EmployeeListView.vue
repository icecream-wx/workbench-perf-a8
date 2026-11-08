<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { listEmployees } from '../api/employees';
import { ApiRequestError } from '../api/http';
import type { Employee } from '../api/types';
import { displayOrDash, formatDate, genderLabel, statusLabel } from '../utils/display';

const router = useRouter();
const employees = ref<Employee[]>([]);
const loading = ref(false);
const loaded = ref(false);
const errorMessage = ref('');

async function load(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    employees.value = await listEmployees();
    loaded.value = true;
  } catch (error) {
    loaded.value = false;
    errorMessage.value = error instanceof ApiRequestError ? error.message : '加载员工列表失败，请重试';
  } finally {
    loading.value = false;
  }
}

onMounted(load);

function openCreate(): void {
  void router.push({ name: 'employee-create' });
}

function openEdit(employee: Employee): void {
  void router.push({ name: 'employee-edit', params: { id: String(employee.id) } });
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">员工列表</h1>
        <p class="page-subtitle">
          <template v-if="loaded">共 {{ employees.length }} 名员工</template>
          <template v-else>浏览员工信息，支持新增与编辑</template>
        </p>
      </div>
      <el-button type="primary" size="large" data-testid="create-employee" @click="openCreate">
        新增员工
      </el-button>
    </header>

    <div v-if="errorMessage" class="state-panel" data-testid="employees-error">
      <el-alert
        type="error"
        :closable="false"
        show-icon
        title="员工列表加载失败"
        :description="errorMessage"
      />
      <el-button data-testid="retry-load" @click="load">重试</el-button>
    </div>

    <el-skeleton v-else-if="loading && !loaded" class="state-panel" :rows="6" animated data-testid="employees-loading" />

    <div v-else-if="loaded && employees.length === 0" class="state-panel state-panel--empty" data-testid="employees-empty">
      <el-empty description="暂无员工数据">
        <el-button type="primary" @click="openCreate">新增第一位员工</el-button>
      </el-empty>
    </div>

    <el-table
      v-else-if="loaded"
      v-loading="loading"
      class="employee-table"
      data-testid="employee-table"
      :data="employees"
      row-key="id"
      stripe
    >
      <el-table-column prop="employeeNo" label="工号" min-width="110" fixed />
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="department" label="部门" min-width="120" />
      <el-table-column prop="position" label="职位" min-width="130" />
      <el-table-column prop="hireDate" label="入职日期" min-width="120">
        <template #default="{ row }">{{ formatDate(row.hireDate) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130">
        <template #default="{ row }">{{ displayOrDash(row.phone) }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="{ row }">{{ displayOrDash(row.email) }}</template>
      </el-table-column>
      <el-table-column prop="gender" label="性别" min-width="80">
        <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="light">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="96" align="right" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" data-testid="edit-employee" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
