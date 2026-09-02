import { useState } from 'react'

import { supabase } from '../lib/supabaseClient.js'

function AuthPage() {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const isSignUp = mode === 'signup'

  function switchMode(nextMode) {
    if (isSubmitting || nextMode === mode) return

    setMode(nextMode)
    setMessage('')
    setErrorMessage('')
  }

  async function submitAuth(event) {
    event.preventDefault()
    if (isSubmitting) return

    const trimmedEmail = email.trim()
    if (!trimmedEmail || !password) {
      setErrorMessage('이메일과 비밀번호를 입력해주세요.')
      return
    }

    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    const { data, error } = isSignUp
      ? await supabase.auth.signUp({ email: trimmedEmail, password })
      : await supabase.auth.signInWithPassword({ email: trimmedEmail, password })

    if (error) {
      setErrorMessage(error.message)
      setIsSubmitting(false)
      return
    }

    if (isSignUp && !data.session) {
      setMessage('가입 확인 후 로그인해주세요.')
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-label="인증">
        <p className="eyebrow">WORKOUT LOG</p>
        <h1>{isSignUp ? '회원가입' : '로그인'}</h1>

        <div className="auth-mode-tabs" role="tablist" aria-label="인증 방식">
          <button type="button" className={!isSignUp ? 'active' : ''} onClick={() => switchMode('login')}>
            로그인
          </button>
          <button type="button" className={isSignUp ? 'active' : ''} onClick={() => switchMode('signup')}>
            회원가입
          </button>
        </div>

        <form className="auth-form" onSubmit={submitAuth}>
          {message && <p className="success-message">{message}</p>}
          {errorMessage && <p className="error-message">{errorMessage}</p>}

          <label>
            이메일
          <input
              type="email"
              value={email}
              autoComplete="email"
              placeholder="name@example.com"
              required
              disabled={isSubmitting}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>

          <label>
            비밀번호
            <input
              type="password"
              value={password}
              minLength={isSignUp ? 8 : undefined}
              autoComplete={isSignUp ? 'new-password' : 'current-password'}
              placeholder={isSignUp ? '8자 이상' : '비밀번호'}
              required
              disabled={isSubmitting}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>

          <button type="submit" className="primary-action" disabled={isSubmitting}>
            {isSubmitting ? '처리 중...' : isSignUp ? '회원가입' : '로그인'}
          </button>
        </form>
      </section>
    </main>
  )
}

export default AuthPage
