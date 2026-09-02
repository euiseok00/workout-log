import { useEffect, useRef, useState } from 'react'

import { supabase } from './lib/supabaseClient.js'
import AuthPage from './pages/AuthPage.jsx'
import ExerciseManagementPage from './pages/ExerciseManagementPage.jsx'
import RoutineManagementPage from './pages/RoutineManagementPage.jsx'
import WorkoutCreatePage from './pages/WorkoutCreatePage.jsx'
import WorkoutRecordPage from './pages/WorkoutRecordPage.jsx'
import './App.css'

function App() {
  const [page, setPage] = useState('today')
  const [session, setSession] = useState(null)
  const [isAuthLoading, setIsAuthLoading] = useState(true)
  const [isSigningOut, setIsSigningOut] = useState(false)
  const lastButtonClickRef = useRef({ button: null, time: 0 })

  useEffect(() => {
    let ignore = false

    supabase.auth
      .getSession()
      .then(({ data }) => {
        if (!ignore) {
          setSession(data.session)
          setIsAuthLoading(false)
        }
      })
      .catch(() => {
        if (!ignore) {
          setSession(null)
          setIsAuthLoading(false)
        }
      })

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession)
      setIsAuthLoading(false)
      setIsSigningOut(false)
    })

    return () => {
      ignore = true
      subscription.unsubscribe()
    }
  }, [])

  function blockRepeatedClick(event) {
    const button = event.target.closest('button')
    if (!button) return

    const now = Date.now()
    const lastClick = lastButtonClickRef.current
    if (lastClick.button === button && now - lastClick.time < 600) {
      event.preventDefault()
      event.stopPropagation()
      return
    }

    lastButtonClickRef.current = { button, time: now }
  }

  async function signOut() {
    if (isSigningOut) return

    setIsSigningOut(true)
    const { error } = await supabase.auth.signOut()
    if (error) {
      setIsSigningOut(false)
      window.alert('로그아웃하지 못했습니다.')
    }
  }

  if (isAuthLoading) {
    return (
      <main className="auth-loading-page">
        <p className="eyebrow">WORKOUT LOG</p>
        <h1>세션을 확인하는 중입니다.</h1>
      </main>
    )
  }

  if (!session) {
    return <AuthPage />
  }

  const headerAction = (
    <button type="button" className="header-sign-out-button" disabled={isSigningOut} onClick={signOut}>
      {isSigningOut ? '처리 중' : '로그아웃'}
    </button>
  )

  let pageComponent = <WorkoutCreatePage headerAction={headerAction} onNavigate={setPage} />
  if (page === 'record') pageComponent = <WorkoutRecordPage headerAction={headerAction} onNavigate={setPage} />
  if (page === 'routine') pageComponent = <RoutineManagementPage headerAction={headerAction} onNavigate={setPage} />
  if (page === 'exercise') pageComponent = <ExerciseManagementPage headerAction={headerAction} onNavigate={setPage} />

  return <div onClickCapture={blockRepeatedClick}>{pageComponent}</div>
}

export default App
