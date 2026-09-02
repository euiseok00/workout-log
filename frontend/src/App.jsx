import { useRef, useState } from 'react'

import ExerciseManagementPage from './pages/ExerciseManagementPage.jsx'
import RoutineManagementPage from './pages/RoutineManagementPage.jsx'
import WorkoutCreatePage from './pages/WorkoutCreatePage.jsx'
import WorkoutRecordPage from './pages/WorkoutRecordPage.jsx'
import './App.css'

function App() {
  const [page, setPage] = useState('today')
  const lastButtonClickRef = useRef({ button: null, time: 0 })

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

  let pageComponent = <WorkoutCreatePage onNavigate={setPage} />
  if (page === 'record') pageComponent = <WorkoutRecordPage onNavigate={setPage} />
  if (page === 'routine') pageComponent = <RoutineManagementPage onNavigate={setPage} />
  if (page === 'exercise') pageComponent = <ExerciseManagementPage onNavigate={setPage} />

  return <div onClickCapture={blockRepeatedClick}>{pageComponent}</div>
}

export default App
