import { supabase } from './supabaseClient.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

console.log('API_BASE_URL:', API_BASE_URL)

export async function apiFetch(url, options = {}) {
  const { data } = await supabase.auth.getSession()
  const accessToken = data.session?.access_token

  if (!accessToken) {
    throw new Error('로그인이 필요합니다.')
  }

  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)

  return fetch(`${API_BASE_URL}${url}`, { ...options, headers })
}
