import axios, { AxiosRequestConfig } from 'axios';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

type UnauthorizedHandler = () => void | Promise<void>;

let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler;
}

// For the deployed frontend, comment out the local API_BASE_URL below and
// uncomment this endpoint to connect to the deployed Render backend:
// export const API_BASE_URL = 'https://restfulhousekeeping-v1.onrender.com';

export const API_BASE_URL = Platform.select({
  default: 'http://192.168.68.104:50000',
  android: 'http://10.0.2.2:50000',
  ios: 'http://127.0.0.1:50000',
});

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use(
  async (config) => {
    const token = await SecureStore.getItemAsync('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log('🛜 API request:', {
      method: config.method,
      url: `${config.baseURL}${config.url}`,
      hasToken: !!token,
    });
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('❌ API error:', {
      status: error.response?.status,
      data: error.response?.data,
      url: error.config?.url,
    });

    if (error.response?.status === 401) {
      void Promise.resolve()
        .then(() => unauthorizedHandler?.())
        .catch((logoutError) => {
          console.error('Failed clearing unauthorized session', logoutError);
        });
    }

    return Promise.reject(error);
  },
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<T>(config);
  console.log('✅ API response:', response.data);
  return response.data;
}

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Something went wrong. Please try again.',
) {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : fallback;
  }

  const data = error.response?.data;
  if (data && typeof data === 'object') {
    const responseMessage =
      ('detail' in data && data.detail) || ('message' in data && data.message);
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage;
    }
  }

  return fallback;
}
