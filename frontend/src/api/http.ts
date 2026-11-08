import axios, { type AxiosRequestConfig } from 'axios';
import type { ApiFailureBody, ApiFieldError } from './types';

/**
 * 前端统一请求封装：成功解包 data，失败按后端错误语义（EMP-04）携带 code 与字段错误。
 */
export class ApiRequestError extends Error {
  readonly status?: number;
  readonly code?: string;
  readonly fieldErrors: ApiFieldError[];

  constructor(message: string, options: { status?: number; code?: string; fieldErrors?: ApiFieldError[] } = {}) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = options.status;
    this.code = options.code;
    this.fieldErrors = options.fieldErrors ?? [];
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { Accept: 'application/json' },
});

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const body = error.response?.data as ApiFailureBody | undefined;
      const detail = body?.error;
      return Promise.reject(
        new ApiRequestError(detail?.message || error.message || '网络请求失败', {
          status: error.response?.status,
          code: detail?.code,
          fieldErrors: Array.isArray(detail?.fieldErrors) ? detail.fieldErrors : [],
        }),
      );
    }
    return Promise.reject(new ApiRequestError('网络异常，请稍后重试'));
  },
);

export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<{ success: true; data: T }>(config);
  return response.data.data;
}
