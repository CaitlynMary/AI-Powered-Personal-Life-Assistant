/** Mirrors the Spring Boot auth and user DTOs. Field names must stay in camelCase. */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

export interface UpdateUserRequest {
  fullName: string;
  phoneNumber: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginResponse extends TokenResponse {
  message: string;
}

export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  active: boolean;
  createdAt: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  validationErrors?: Record<string, string> | null;
}
