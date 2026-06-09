import axios, { AxiosError, type AxiosResponse } from 'axios';
import { clearStoredSession, readStoredSession } from '@/stores/authStorage';
import type { ApiEnvelope } from '@/types/api';

export class ApiError extends Error {
  code: number;
  requestId?: string | null;

  constructor(message: string, code = -1, requestId?: string | null) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.requestId = requestId;
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 200_000,
  headers: {
    'Content-Type': 'application/json'
  }
});

function isAuthFailureCode(code: number) {
  return code === 401 || code === 403 || code === 4010 || code === 4030;
}

http.interceptors.request.use((config) => {
  const session = readStoredSession();

  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }

  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data as Partial<ApiEnvelope<unknown>>;

    if (typeof body?.code === 'number' && body.code !== 0) {
      if (isAuthFailureCode(body.code)) {
        clearStoredSession();
      }
      throw new ApiError(body.message || 'Request failed.', body.code, body.requestId);
    }

    return response;
  },
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const responseBody = error.response?.data;
    const statusCode = error.response?.status;

    if (statusCode === 401 || statusCode === 403) {
      clearStoredSession();
    }

    if (responseBody && typeof responseBody.code === 'number') {
      if (isAuthFailureCode(responseBody.code)) {
        clearStoredSession();
      }
      return Promise.reject(new ApiError(responseBody.message, responseBody.code, responseBody.requestId));
    }

    return Promise.reject(new ApiError(error.message || 'Network connection failed.'));
  }
);

export async function unwrap<T>(request: Promise<AxiosResponse<ApiEnvelope<T>>>): Promise<T> {
  const response = await request;
  return response.data.data;
}
