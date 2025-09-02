import { apiClient } from './api';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { User } from '../types';
import { ApiResponse } from '../types/api';

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  email: string;
  password: string;
  username: string;
  nickname: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

class AuthService {
  // 로그인
  async login(credentials: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
    
    if (response.success && response.data) {
      await apiClient.setToken(response.data.accessToken);
      await AsyncStorage.setItem('access_token', response.data.accessToken);
      await AsyncStorage.setItem('refresh_token', response.data.refreshToken);
      await AsyncStorage.setItem('user', JSON.stringify(response.data.user));
    }
    
    return response;
  }

  // 회원가입
  async register(userData: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
    return apiClient.post('/auth/register', userData);
  }

  // 로그아웃
  async logout(): Promise<void> {
    await apiClient.setToken(null);
    await AsyncStorage.multiRemove(['access_token', 'refresh_token', 'user']);
  }

  // 토큰 갱신
  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    const response = await apiClient.post<AuthResponse>('/auth/refresh', {
      refreshToken,
    });
    
    if (response.success && response.data) {
      await apiClient.setToken(response.data.accessToken);
      await AsyncStorage.setItem('access_token', response.data.accessToken);
      if (response.data.refreshToken) {
        await AsyncStorage.setItem('refresh_token', response.data.refreshToken);
      }
    }
    
    return response;
  }

  // 현재 사용자 정보 조회
  async getCurrentUser(): Promise<ApiResponse<User>> {
    return apiClient.get('/auth/me');
  }

  // 로컬 저장소에서 사용자 정보 조회
  async getStoredUser(): Promise<User | null> {
    try {
      const userJson = await AsyncStorage.getItem('user');
      return userJson ? JSON.parse(userJson) : null;
    } catch (error) {
      console.error('Error getting stored user:', error);
      return null;
    }
  }

  // 저장된 토큰 확인
  async getStoredToken(): Promise<string | null> {
    try {
      return await AsyncStorage.getItem('access_token');
    } catch (error) {
      console.error('Error getting stored token:', error);
      return null;
    }
  }

  // 자동 로그인 체크
  async checkAutoLogin(): Promise<boolean> {
    try {
      const token = await AsyncStorage.getItem('access_token');
      if (token) {
        await apiClient.setToken(token);
        const response = await this.getCurrentUser();
        return response.success;
      }
      return false;
    } catch (error) {
      console.error('Auto login check failed:', error);
      return false;
    }
  }

  // 프로필 업데이트
  async updateProfile(data: {
    nickname?: string;
    bio?: string;
    profileImageUrl?: string;
  }): Promise<ApiResponse<User>> {
    const response = await apiClient.patch<User>('/auth/profile', data);
    
    if (response.success && response.data) {
      await AsyncStorage.setItem('user', JSON.stringify(response.data));
    }
    
    return response;
  }

  // 비밀번호 변경
  async changePassword(data: {
    currentPassword: string;
    newPassword: string;
  }): Promise<ApiResponse<void>> {
    return apiClient.patch('/auth/password', data);
  }

  // 비밀번호 재설정 요청
  async requestPasswordReset(email: string): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/password-reset/request', { email });
  }

  // 비밀번호 재설정
  async resetPassword(token: string, newPassword: string): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/password-reset/confirm', {
      token,
      newPassword,
    });
  }

  // 이메일 인증 요청
  async requestEmailVerification(): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/email-verification/request');
  }

  // 이메일 인증 확인
  async verifyEmail(token: string): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/email-verification/confirm', { token });
  }

  // 계정 삭제
  async deleteAccount(password: string): Promise<ApiResponse<void>> {
    const response = await apiClient.delete('/auth/account', { password });
    
    if (response.success) {
      await this.logout();
    }
    
    return response;
  }
}

export const authService = new AuthService();

export type { LoginRequest, RegisterRequest, AuthResponse };