import { useMutation } from '@tanstack/react-query'
import { login, register, type LoginRequest, type RegisterRequest } from '../api/auth'

export function useLogin() {
  return useMutation({
    mutationFn: (data: LoginRequest) => login(data),
  })
}

export function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => register(data),
  })
}
