import { useEffect, useState } from 'react'
import { api } from './api'
import { useCharacterEvent } from './themes/characterEventContext'
import { CHARACTER_EVENTS } from './themes/characterEvents'
import { ErrorMessage, Icon, Link, Loading, Modal, PageHeader } from './ui'
import { bodyPartLabel, formatDate, formatNumber, todayString } from './utils'

export default function WorkoutDetail({ id, navigate, auth }) {
  const { triggerCharacter } = useCharacterEvent()
  const [session, setSession] = useState(null)
  const [error, setError] = useState(null)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleteError, setDeleteError] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [routineOpen, setRoutineOpen] = useState(false)
  const [routineName, setRoutineName] = useState('')
  const [routineSaving, setRoutineSaving] = useState(false)
  const [routineError, setRoutineError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/workout-sessions/${id}`).then((value) => active && setSession(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [id])
  if (!session) return <div className="page"><PageHeader back="/dashboard" navigate={navigate} title="운동 상세"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  const completedSets = session.exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed)
  const volume = completedSets.reduce((sum, set) => sum + Number(set.weight) * Number(set.reps), 0)
  const loadWorkout = async () => {
    const key = `workout_draft:${auth.user.id}`
    if (localStorage.getItem(key) && !window.confirm('작성 중인 운동을 이 기록으로 교체할까요?')) return
    let routineId = session.routineId
    let routineName = session.routineName
    if (routineId != null) {
      try { await api(`/api/routines/${routineId}`) }
      catch (requestError) {
        if (requestError.status !== 404) { window.alert(requestError.message); return }
        routineId = null; routineName = null
      }
    }
    localStorage.setItem(key, JSON.stringify({ workoutDate: todayString(), routineId, routineName, exercises: session.exercises.map((exercise, exerciseIndex) => ({ ...exercise, exerciseOrder: exerciseIndex + 1, sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1, completed: false })) })), updatedAt: new Date().toISOString() }))
    triggerCharacter(CHARACTER_EVENTS.RECORD_LOAD)
    navigate('/workout')
  }
  const deleteSession = async () => {
    setDeleteError(null)
    setDeleting(true)
    try {
      await api(`/api/workout-sessions/${id}`, { method: 'DELETE' })
      navigate('/dashboard', false, { selectedDate: session.workoutDate })
    } catch (requestError) {
      setDeleteError(requestError)
      setDeleting(false)
    }
  }
  const saveRoutine = async (event) => {
    event.preventDefault(); setRoutineSaving(true); setRoutineError(null)
    try {
      await api(`/api/routines/from-session/${id}`, { method: 'POST', body: { name: routineName } })
      setRoutineOpen(false); navigate('/routines')
    } catch (requestError) { setRoutineError(requestError) } finally { setRoutineSaving(false) }
  }
  return <div className="page workout-detail-page"><PageHeader back="/dashboard" navigate={navigate} eyebrow={formatDate(session.workoutDate)} title={session.routineName || '자유 운동'}/><div className="detail-actions">{session.routineId == null && <button className="secondary-button compact" onClick={() => setRoutineOpen(true)}>루틴으로 저장</button>}<Link to={`/workouts/${id}/edit`} navigate={navigate} className="secondary-button compact">수정</Link><button className="text-button danger" onClick={() => setDeleteOpen(true)}>삭제</button></div><div className="detail-summary"><div><span>운동 종목</span><b>{session.exercises.length}<small>개</small></b></div><div><span>완료 세트</span><b>{completedSets.length}<small>세트</small></b></div><div><span>총 볼륨</span><b>{formatNumber(volume, 2)}<small>kg</small></b></div></div><section className="detail-exercises">{session.exercises.map((exercise, index) => <article className="detail-exercise" key={index}><header><span>{String(index + 1).padStart(2, '0')}</span><div><h2>{exercise.name}</h2><p>{bodyPartLabel(exercise.bodyPart)}</p></div></header><div>{exercise.sets.map((set) => <p className={set.completed ? 'completed' : ''} key={set.setNumber}><span>{set.setNumber}세트</span><b>{Number(set.weight) === 0 ? '맨몸' : `${set.weight}kg`} × {set.reps}회</b><i>{set.completed ? <Icon name="check" size={16}/> : '미완료'}</i></p>)}</div></article>)}</section><button className="primary-button full load-workout-button" onClick={loadWorkout}><Icon name="history" size={19}/> 이 운동 불러오기</button>{routineOpen && <Modal title="루틴으로 저장" onClose={() => !routineSaving && setRoutineOpen(false)}><form className="modal-form" onSubmit={saveRoutine}><ErrorMessage error={routineError}/><label className="field-label" htmlFor="saved-routine-name">루틴 이름</label><input id="saved-routine-name" className="large-input" value={routineName} onChange={(event) => setRoutineName(event.target.value)} maxLength="100" required/><button className="primary-button full" disabled={routineSaving}>{routineSaving ? '저장 중...' : '루틴 저장'}</button></form></Modal>}{deleteOpen && <Modal title="운동 기록 삭제" onClose={() => !deleting && setDeleteOpen(false)}><p>이 운동 기록을 삭제하시겠습니까? 삭제한 기록은 복구할 수 없습니다.</p><ErrorMessage error={deleteError}/><div className="detail-delete-actions"><button className="secondary-button" onClick={() => setDeleteOpen(false)} disabled={deleting}>취소</button><button className="primary-button delete-confirm-button" onClick={deleteSession} disabled={deleting}>{deleting ? '삭제 중...' : '삭제'}</button></div></Modal>}</div>
}
