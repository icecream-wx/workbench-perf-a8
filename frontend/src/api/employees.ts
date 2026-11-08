import { apiRequest } from './http';
import type { Employee, EmployeeInput } from './types';

export function listEmployees(): Promise<Employee[]> {
  return apiRequest<Employee[]>({ method: 'GET', url: '/employees' });
}

export function getEmployee(id: number): Promise<Employee> {
  return apiRequest<Employee>({ method: 'GET', url: `/employees/${id}` });
}

export function createEmployee(input: EmployeeInput): Promise<Employee> {
  return apiRequest<Employee>({ method: 'POST', url: '/employees', data: input });
}

export function updateEmployee(id: number, input: EmployeeInput): Promise<Employee> {
  return apiRequest<Employee>({ method: 'PUT', url: `/employees/${id}`, data: input });
}
