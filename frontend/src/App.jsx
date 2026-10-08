import { useCallback, useEffect, useState } from 'react'
import './App.css'
import { readAuth } from './api'
import AuthPage from './AuthPage'
import Dashboard from './Dashboard'
import { ExerciseHistory, ExerciseList } from './Exercises'
import Layout from './Layout'
import { RoutineForm, RoutineList } from './Routines'
import Workout from './Workout'
import WorkoutDetail from './WorkoutDetail'

export default function App() {
  const [path, setPath] = useState(window.location.pathname)
  const [auth, setAuth] = useState(readAuth)

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    const onExpired = () => { setAuth(null); window.history.replaceState({}, '', '/login'); setPath('/login') }
    window.addEventListener('popstate', onPopState)
    window.addEventListener('auth-expired', onExpired)
    return () => { window.removeEventListener('popstate', onPopState); window.removeEventListener('auth-expired', onExpired) }
  }, [])

  const navigate = useCallback((to, replace = false) => {
    window.history[replace ? 'replaceState' : 'pushState']({}, '', to)
    setPath(to)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  const publicRoute = path === '/login' || path === '/signup'
  if (!auth) return <AuthPage mode={path === '/signup' ? 'signup' : 'login'} navigate={navigate} onAuth={setAuth}/>

  const route = publicRoute || path === '/' ? '/dashboard' : path
  let page
  const workoutMatch = route.match(/^\/workouts\/(\d+)$/)
  const routineMatch = route.match(/^\/routines\/(\d+)$/)
  const exerciseMatch = route.match(/^\/exercises\/(\d+)\/history$/)

  if (route === '/dashboard') page = <Dashboard navigate={navigate} auth={auth}/>
  else if (route === '/workout') page = <Workout navigate={navigate} auth={auth}/>
  else if (workoutMatch) page = <WorkoutDetail id={workoutMatch[1]} navigate={navigate} auth={auth}/>
  else if (route === '/routines') page = <RoutineList navigate={navigate}/>
  else if (route === '/routines/new') page = <RoutineForm navigate={navigate}/>
  else if (routineMatch) page = <RoutineForm id={routineMatch[1]} navigate={navigate}/>
  else if (route === '/exercises') page = <ExerciseList navigate={navigate}/>
  else if (exerciseMatch) page = <ExerciseHistory id={exerciseMatch[1]} navigate={navigate}/>
  else page = <Dashboard navigate={navigate} auth={auth}/>

  return <Layout path={route} navigate={navigate} auth={auth} onLogout={() => setAuth(null)}>{page}</Layout>
}
