import type { EmployeeStatus, Gender } from '../api/types';

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: '男',
  FEMALE: '女',
  UNKNOWN: '未知',
};

export const STATUS_LABELS: Record<EmployeeStatus, string> = {
  ACTIVE: '在职',
  INACTIVE: '离职',
};

export function genderLabel(gender: Gender | null | undefined): string {
  return gender ? GENDER_LABELS[gender] ?? '未知' : '未知';
}

export function statusLabel(status: EmployeeStatus | null | undefined): string {
  return status ? STATUS_LABELS[status] ?? status : '—';
}

export function displayOrDash(value: string | null | undefined): string {
  return value && value.trim() ? value : '—';
}

/** 接口返回 ISO 日期，列表只展示 yyyy-MM-dd。 */
export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  return value.length >= 10 ? value.slice(0, 10) : value;
}
