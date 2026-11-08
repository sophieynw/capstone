// api/auth.ts
import { request } from './apiClient';
import { CreateUserPayload, User } from '@/types/entityTypes';
import { normalizeUsername } from '@/utils/username';

type AuthResponse = {
  token: string;
  user: User;
};

type UsernameAvailabilityResponse = {
  available: boolean;
};

export function checkUsernameAvailability(username: string) {
  return request<UsernameAvailabilityResponse>({
    method: 'GET',
    url: '/api/v1/auth/username-availability',
    params: { username: normalizeUsername(username) },
  });
}

export function register(payload: CreateUserPayload) {
  return request<AuthResponse>({
    method: 'POST',
    url: '/api/v1/auth/register',
    data: {
      ...payload,
      username: normalizeUsername(payload.username),
    },
  });
}

export function authenticate(username: string, password: string) {
  return request<AuthResponse>({
    method: 'POST',
    url: '/api/v1/auth/authenticate',
    data: {
      username: normalizeUsername(username),
      password,
    },
  });
}
