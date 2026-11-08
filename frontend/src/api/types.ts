export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN';
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export interface Employee {
  id: number;
  employeeNo: string;
  name: string;
  department: string;
  position: string;
  hireDate: string;
  phone: string | null;
  email: string | null;
  gender: Gender;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeInput {
  employeeNo: string;
  name: string;
  department: string;
  position: string;
  hireDate: string;
  phone: string | null;
  email: string | null;
  gender: Gender;
  status: EmployeeStatus;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiFailureBody {
  success: false;
  error: {
    code: string;
    message: string;
    fieldErrors?: ApiFieldError[];
  };
}
