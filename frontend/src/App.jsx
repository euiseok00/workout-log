import { useState } from 'react'

import ExerciseManagementPage from './pages/ExerciseManagementPage.jsx'
import RoutineManagementPage from './pages/RoutineManagementPage.jsx'
import WorkoutCreatePage from './pages/WorkoutCreatePage.jsx'
import WorkoutRecordPage from './pages/WorkoutRecordPage.jsx'
import './App.css'

function App() {
  const [page, setPage] = useState('today')

  if (page === 'record') return <WorkoutRecordPage onNavigate={setPage} />
  if (page === 'routine') return <RoutineManagementPage onNavigate={setPage} />
  if (page === 'exercise') return <ExerciseManagementPage onNavigate={setPage} />

  return <WorkoutCreatePage onNavigate={setPage} />
}

export default App
