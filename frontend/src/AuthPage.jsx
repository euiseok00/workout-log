import { useState } from 'react'
import { api, saveAuth } from './api'
import { ErrorMessage, Icon, Link } from './ui'

export default function AuthPage({ mode, navigate, onAuth }) {
  const signup = mode === 'signup'
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [availability, setAvailability] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const checkAvailability = async () => {
    setError(null)
    if (!/^[a-z0-9_]{4,20}$/.test(loginId)) {
      setAvailability({ available: false, message: '4~20자의 영문 소문자, 숫자, 밑줄을 입력하세요.' })
      return
    }
    try {
      const result = await api(`/api/auth/login-id/availability?loginId=${encodeURIComponent(loginId)}`)
      setAvailability({ ...result, message: result.available ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.' })
    } catch (requestError) {
      setError(requestError)
    }
  }

  const submit = async (event) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const auth = await api(`/api/auth/${signup ? 'signup' : 'login'}`, { method: 'POST', body: { loginId, password } })
      saveAuth(auth)
      onAuth(auth)
      navigate('/dashboard', true)
    } catch (requestError) {
      setError(requestError)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-visual">
        <div className="brand brand-light"><span className="brand-mark"><Icon name="workout" size={24}/></span><span>Workout <b>Log</b></span></div>
        <div className="auth-visual-copy"><span className="auth-kicker">BUILD YOUR ROUTINE</span><h1>오늘의 한 세트가<br/>내일의 기록이 됩니다.</h1><p>루틴부터 세트까지, 운동에만 집중할 수 있도록 간결하게 기록하세요.</p></div>
        <div className="auth-art" aria-hidden="true"><div className="art-ring"><b>12</b><span>이번 달 운동</span></div><span className="art-pill art-pill-one">84,320 kg</span><span className="art-pill art-pill-two">126 sets</span></div>
      </section>
      <section className="auth-form-wrap">
        <form className="auth-form" onSubmit={submit}>
          <div className="mobile-brand"><span className="brand-mark"><Icon name="workout" size={22}/></span><b>Workout Log</b></div>
          <p className="eyebrow">{signup ? 'START YOUR JOURNEY' : 'WELCOME BACK'}</p>
          <h2>{signup ? '회원가입' : '다시 만나서 반가워요'}</h2>
          <p className="form-description">{signup ? '운동 기록을 시작할 계정을 만들어보세요.' : '내 운동 기록을 이어서 관리하세요.'}</p>
          <ErrorMessage error={error}/>
          <label className="field-label">아이디</label>
          <div className="field-with-action"><input value={loginId} onChange={(e) => { setLoginId(e.target.value); setAvailability(null) }} placeholder="아이디를 입력하세요" autoComplete="username" required pattern="[a-z0-9_]{4,20}"/>{signup && <button type="button" className="field-button" onClick={checkAvailability}>중복 확인</button>}</div>
          {signup && availability && <p className={`field-help ${availability.available ? 'success' : 'danger'}`}>{availability.message}</p>}
          <label className="field-label">비밀번호</label>
          <div className="password-field"><input type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="8자 이상 입력하세요" autoComplete={signup ? 'new-password' : 'current-password'} required minLength="8" maxLength="72"/><button type="button" onClick={() => setShowPassword((value) => !value)}>{showPassword ? '숨기기' : '보기'}</button></div>
          <button className="primary-button auth-submit" disabled={loading}>{loading ? '처리 중...' : signup ? '회원가입' : '로그인'}</button>
          <p className="auth-switch">{signup ? '이미 계정이 있나요?' : '계정이 없나요?'} <Link to={signup ? '/login' : '/signup'} navigate={navigate}>{signup ? '로그인' : '회원가입'}</Link></p>
        </form>
      </section>
    </main>
  )
}
