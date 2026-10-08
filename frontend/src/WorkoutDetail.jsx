import { useEffect, useState } from 'react'
import { api } from './api'
import { ErrorMessage, Icon, Loading, PageHeader } from './ui'
import { bodyPartLabel, formatDate, formatNumber, todayString } from './utils'

export default function WorkoutDetail({ id, navigate, auth }) {
  const [session, setSession] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/workout-sessions/${id}`).then((value) => active && setSession(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [id])
  if (!session) return <div className="page"><PageHeader back="/dashboard" navigate={navigate} title="운동 상세"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  const completedSets = session.exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed)
  const volume = completedSets.reduce((sum, set) => sum + Number(set.weight) * Number(set.reps), 0)
  const loadWorkout = () => {
    const key = `workout_draft:${auth.user.id}`
    if (localStorage.getItem(key) && !window.confirm('작성 중인 운동을 이 기록으로 교체할까요?')) return
    localStorage.setItem(key, JSON.stringify({ workoutDate: todayString(), routineId: session.routineId, routineName: session.routineName, exercises: session.exercises.map((exercise, exerciseIndex) => ({ ...exercise, exerciseOrder: exerciseIndex + 1, sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1, completed: false })) })), updatedAt: new Date().toISOString() }))
    navigate('/workout')
  }
  return <div className="page workout-detail-page"><PageHeader back="/dashboard" navigate={navigate} eyebrow={formatDate(session.workoutDate)} title={session.routineName || '자유 운동'}/><div className="detail-summary"><div><span>운동 종목</span><b>{session.exercises.length}<small>개</small></b></div><div><span>완료 세트</span><b>{completedSets.length}<small>세트</small></b></div><div><span>총 볼륨</span><b>{formatNumber(volume, 2)}<small>kg</small></b></div></div><section className="detail-exercises">{session.exercises.map((exercise, index) => <article className="detail-exercise" key={exercise.exerciseId}><header><span>{String(index + 1).padStart(2, '0')}</span><div><h2>{exercise.name}</h2><p>{bodyPartLabel(exercise.bodyPart)}</p></div></header><div>{exercise.sets.map((set) => <p className={set.completed ? 'completed' : ''} key={set.setNumber}><span>{set.setNumber}세트</span><b>{Number(set.weight) === 0 ? '맨몸' : `${set.weight}kg`} × {set.reps}회</b><i>{set.completed ? <Icon name="check" size={16}/> : '미완료'}</i></p>)}</div></article>)}</section><button className="primary-button full load-workout-button" onClick={loadWorkout}><Icon name="history" size={19}/> 이 운동 불러오기</button></div>
}
