import axios, { AxiosInstance, AxiosResponse } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ApiResponse } from '../types/api';

// API Base Configuration
const API_BASE_URL = 'http://localhost:8080/api/v1';
const AI_API_BASE_URL = 'http://localhost:8000/api/v1';

// Create axios instances
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

const aiApiClient: AxiosInstance = axios.create({
  baseURL: AI_API_BASE_URL,
  timeout: 30000, // AI requests might take longer
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  async (config) => {
    try {
      const token = await AsyncStorage.getItem('access_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.error('Error getting token from storage:', error);
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// AI API Request interceptor to add auth token and user ID
aiApiClient.interceptors.request.use(
  async (config) => {
    try {
      const token = await AsyncStorage.getItem('access_token');
      const userInfo = await AsyncStorage.getItem('user');
      
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      
      if (userInfo) {
        const user = JSON.parse(userInfo);
        config.headers['X-User-ID'] = user.id.toString();
      }
    } catch (error) {
      console.error('Error setting AI headers:', error);
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // Handle 401 unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = await AsyncStorage.getItem('refresh_token');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });
          
          const { accessToken } = response.data;
          await AsyncStorage.setItem('access_token', accessToken);
          
          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, redirect to login
        await AsyncStorage.multiRemove(['access_token', 'refresh_token']);
        console.error('Token refresh failed:', refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

// Pagination interface
interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

class ApiClient {
  private client: AxiosInstance;
  private aiClient: AxiosInstance;

  constructor() {
    this.client = apiClient;
    this.aiClient = aiApiClient;
  }

  async setToken(token: string | null) {
    try {
      if (token) {
        await AsyncStorage.setItem('access_token', token);
      } else {
        await AsyncStorage.removeItem('access_token');
      }
    } catch (error) {
      console.error('Error setting token:', error);
    }
  }

  // GET 요청
  async get<T>(endpoint: string, params?: any): Promise<ApiResponse<T>> {
    try {
      const response = await this.client.get<T>(endpoint, { params });
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '요청 처리 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }

  // POST 요청
  async post<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    try {
      const response = await this.client.post<T>(endpoint, data);
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '요청 처리 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }

  // PUT 요청
  async put<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    try {
      const response = await this.client.put<T>(endpoint, data);
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '요청 처리 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }

  // PATCH 요청
  async patch<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    try {
      const response = await this.client.patch<T>(endpoint, data);
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '요청 처리 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }

  // DELETE 요청
  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    try {
      const response = await this.client.delete<T>(endpoint);
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '요청 처리 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }

  // AI 서비스 POST 요청
  async aiPost<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    try {
      const response = await this.aiClient.post<T>(endpoint, data);
      return {
        success: true,
        data: response.data,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || error.response?.data?.detail || 'AI 서비스 요청 중 오류가 발생했습니다.',
        errors: error.response?.data?.errors,
      };
    }
  }
}

const client = new ApiClient();

export { client as apiClient, PaginatedResponse };
export type { PaginatedResponse };