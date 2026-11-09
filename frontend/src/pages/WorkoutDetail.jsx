import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { useCharacterEvent } from '../themes/context/characterEventContext'
import { CHARACTER_EVENTS } from '../themes/context/characterEvents'
import { SetRecordList } from '../components/ExerciseCard'
import { ConfirmModal, ErrorMessage, Icon, Link, Loading, Modal, PageHeader } from '../components/ui'
import { completedVolume, countSets, groupLabel } from '../lib/setGroups'
import { bodyPartLabel, formatDate, formatNumber, todayString } from '../lib/utils'
import { draftFromSession } from '../lib/workoutSession'

export default function WorkoutDetail({ id, navigate, auth }) {
  const { triggerCharacter } = useCharacterEvent()
  const [session, setSession] = useState(null)
  const [error, setError] = useState(null)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [replaceOpen, setReplaceOpen] = useState(false)
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
  const draftKey = `workout_draft:${auth.user.id}`
  const loadWorkout = () => {
    localStorage.setItem(draftKey, JSON.stringify(draftFromSession(session, todayString())))
    triggerCharacter(CHARACTER_EVENTS.RECORD_LOAD)
    navigate('/workout')
  }
  const deleteSession = async () => {
    await api(`/api/workout-sessions/${id}`, { method: 'DELETE' })
    navigate('/dashboard', false, { selectedDate: session.workoutDate })
  }
  const saveRoutine = async (event) => {
    event.preventDefault(); setRoutineSaving(true); setRoutineError(null)
    try {
      await api(`/api/routines/from-session/${id}`, { method: 'POST', body: { name: routineName } })
      setRoutineOpen(false); navigate('/routines')
    } catch (requestError) { setRoutineError(requestError) } finally { setRoutineSaving(false) }
  }
  return <div className="page workout-detail-page"><PageHeader back="/dashboard" navigate={navigate} meta={formatDate(session.workoutDate)} title={session.routineName || '자유 운동'}/><div className="detail-actions">{session.routineId == null && <button className="secondary-button compact" onClick={() => setRoutineOpen(true)}>루틴으로 저장</button>}<Link to={`/workouts/${id}/edit`} navigate={navigate} className="secondary-button compact">수정</Link><button className="text-button danger" onClick={() => setDeleteOpen(true)}>삭제</button></div><div className="detail-summary"><div><span>운동 종목</span><b>{session.exercises.length}<small>개</small></b></div><div><span>완료 세트</span><b>{countSets(session.exercises, true)}<small>세트</small></b></div><div><span>총 볼륨</span><b>{formatNumber(completedVolume(session.exercises), 2)}<small>kg</small></b></div></div><section className="detail-exercises">{session.exercises.map((exercise, index) => <article className="detail-exercise" key={index}><header><span>{String(index + 1).padStart(2, '0')}</span><div><h2>{exercise.name}</h2><p>{[bodyPartLabel(exercise.bodyPart), groupLabel(session.exercises, exercise)].filter(Boolean).join(' · ')}</p></div></header><SetRecordList sets={exercise.sets}/></article>)}</section><button className="primary-button full load-workout-button" onClick={() => localStorage.getItem(draftKey) ? setReplaceOpen(true) : loadWorkout()}><Icon name="history" size={19}/> 이 운동 불러오기</button>{routineOpen && <Modal title="루틴으로 저장" onClose={() => !routineSaving && setRoutineOpen(false)}><form className="modal-form" onSubmit={saveRoutine}><ErrorMessage error={routineError}/><label className="field-label" htmlFor="saved-routine-name">루틴 이름</label><input id="saved-routine-name" className="large-input" value={routineName} onChange={(event) => setRoutineName(event.target.value)} maxLength="100" required/><button className="primary-button full" disabled={routineSaving}>{routineSaving ? '저장 중…' : '루틴 저장'}</button></form></Modal>}{replaceOpen && <ConfirmModal title="이 운동 불러오기" message="작성 중인 운동을 이 기록으로 교체할까요?" confirmLabel="교체" onConfirm={loadWorkout} onClose={() => setReplaceOpen(false)}/>}{deleteOpen && <ConfirmModal title="운동 기록 삭제" message="이 운동 기록을 삭제하시겠습니까? 삭제한 기록은 복구할 수 없습니다." onConfirm={deleteSession} onClose={() => setDeleteOpen(false)}/>}</div>
}
