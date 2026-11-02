import { useEffect, useMemo, useState } from 'react'
import { api, query } from '../lib/api'
import ExercisePicker from '../components/ExercisePicker'
import WorkoutCalendar from '../components/WorkoutCalendar'
import { ExerciseCard, SetRecordList, SetTable } from '../components/ExerciseCard'
import { BindExercisesButton } from '../components/ExerciseGroups'
import { useCharacterEvent } from '../themes/context/characterEventContext'
import { CHARACTER_EVENTS } from '../themes/context/characterEvents'
import { ConfirmModal, EmptyState, ErrorMessage, Icon, Loading, Modal, PageHeader } from '../components/ui'
import { formatDate, formatNumber, isoDate, todayString } from '../lib/utils'
import { draftFromSession, workoutRequest } from '../lib/workoutSession'
import { bindExercises, completedVolume, countSets, nextSet, normalizeGroups, targetSetCount, unbindGroup } from '../lib/setGroups'

function readDraft(key) {
  try { return JSON.parse(localStorage.getItem(key)) } catch { return null }
}

function normalizeDraft(draft) {
  return { ...draft, exercises: normalizeGroups(draft.exercises).map((exercise, exerciseIndex) => ({ ...exercise, exerciseOrder: exerciseIndex + 1, sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1 })) })), updatedAt: new Date().toISOString() }
}

function PreviousRecords({ exercise, onLoad, onClose }) {
  const [records, setRecords] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/exercises/${exercise.exerciseId}/records`).then((value) => active && setRecords(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [exercise.exerciseId])
  return <Modal title={`${exercise.name} 이전 기록`} onClose={onClose}>
    <ErrorMessage error={error}/>
    {!records ? <Loading/> : <div className="record-list">{records.length ? records.map((record) => <article className="record-card" key={record.sessionId}><header><b>{formatDate(record.workoutDate)}</b><span>{countSets([record], true)}세트</span></header><SetRecordList sets={record.sets}/><button className="secondary-button full" onClick={() => onLoad(record.sets)}>이 기록 불러오기</button></article>) : <EmptyState icon="history" title="아직 이전 기록이 없어요" description="첫 기록을 완료하면 이곳에서 다시 불러올 수 있어요."/>}</div>}
  </Modal>
}

function RecordCalendar({ workoutDate, onLoad, onBack }) {
  const [cursor, setCursor] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1))
  const [selected, setSelected] = useState(todayString)
  const [sessions, setSessions] = useState([])
  const [error, setError] = useState(null)
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const from = isoDate(year, month, 1)
  const to = isoDate(year, month, new Date(year, month + 1, 0).getDate())
  useEffect(() => {
    let active = true
    api(`/api/workout-sessions${query({ from, to })}`).then((value) => { if (active) { setSessions(value); setError(null) } }).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [from, to])
  const byDate = useMemo(() => sessions.reduce((map, session) => {
    map[session.workoutDate] = [...(map[session.workoutDate] || []), session]
    return map
  }, {}), [sessions])
  const selectedSessions = byDate[selected] || []
  return <div className="page"><PageHeader back="/workout" navigate={onBack} eyebrow={formatDate(workoutDate)} title="불러올 기록을 선택하세요"/>
    <ErrorMessage error={error}/>
    <div className="dashboard-grid">
      <WorkoutCalendar cursor={cursor} selected={selected} byDate={byDate} onMonthChange={(next) => { setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} onSelect={setSelected}/>
      <section className="day-sessions">
        <header className="section-header"><div><p className="eyebrow">DAILY LOG</p><h2>{formatDate(selected)}</h2></div><span className="count-badge">{selectedSessions.length}개</span></header>
        <div className="session-list">{selectedSessions.length ? selectedSessions.map((session) => <button type="button" className="session-card" key={session.id} onClick={() => onLoad(session)}><span className="session-icon"><Icon name="workout"/></span><div><h3>{session.routineName || '자유 운동'}</h3><p>운동 {session.exercises.length}개 · 완료 {countSets(session.exercises, true)}세트</p></div><Icon name="chevron"/></button>) : <EmptyState icon="calendar" title="이날 기록된 운동이 없어요" description="운동 기록이 있는 다른 날짜를 선택해 주세요."/>}</div>
      </section>
    </div>
  </div>
}

export default function Workout({ navigate, auth, sessionId }) {
  const { triggerCharacter } = useCharacterEvent()
  const draftKey = `workout_draft:${auth.user.id}`
  const initialDraft = useMemo(() => sessionId ? null : readDraft(draftKey), [draftKey, sessionId])
  const [draft, setDraft] = useState(initialDraft)
  const [view, setView] = useState(sessionId ? 'editor' : initialDraft ? 'resume' : 'hub')
  const [routines, setRoutines] = useState(null)
  const [exercises, setExercises] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [historyExercise, setHistoryExercise] = useState(null)
  const [dragIndex, setDragIndex] = useState(null)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveFailed, setSaveFailed] = useState(false)
  const [confirming, setConfirming] = useState(null)

  useEffect(() => {
    let active = true
    Promise.all([api('/api/routines'), api('/api/exercises'), sessionId ? api(`/api/workout-sessions/${sessionId}`) : null]).then(([routineData, exerciseData, sessionData]) => {
      if (active) { setRoutines(routineData); setExercises(exerciseData); if (sessionData) setDraft(sessionData) }
    }).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [sessionId])

  useEffect(() => {
    if (draft && !sessionId) localStorage.setItem(draftKey, JSON.stringify(draft))
  }, [draft, draftKey, sessionId])

  const newWorkoutDate = window.history.state?.workoutDate || todayString()
  const updateDraft = (updater) => setDraft((current) => normalizeDraft(typeof updater === 'function' ? updater(current) : updater))
  const startFree = () => {
    const next = { workoutDate: newWorkoutDate, routineId: null, routineName: null, exercises: [], updatedAt: new Date().toISOString() }
    setDraft(next); setView('editor'); triggerCharacter(CHARACTER_EVENTS.FREE_WORKOUT_START)
  }
  const startRoutine = (routine) => {
    const next = { workoutDate: newWorkoutDate, routineId: routine.id, routineName: routine.name, exercises: routine.exercises.map((exercise, index) => ({ exerciseId: exercise.exerciseId, name: exercise.name, bodyPart: exercise.bodyPart, exerciseOrder: index + 1, groupNumber: exercise.groupNumber, sets: exercise.sets?.length ? exercise.sets.map((set) => ({ ...set, completed: false })) : Array.from({ length: exercise.targetSets || 1 }, (_, setIndex) => ({ setNumber: setIndex + 1, weight: 0, reps: exercise.targetReps || 0, completed: false, setType: 'WORKING' })) })), updatedAt: new Date().toISOString() }
    setDraft(next); setView('editor'); triggerCharacter(CHARACTER_EVENTS.ROUTINE_SELECT)
  }
  const startFromSession = (session) => {
    setDraft(draftFromSession(session, newWorkoutDate)); setView('editor'); triggerCharacter(CHARACTER_EVENTS.RECORD_LOAD)
  }
  const discard = () => setConfirming({ title: '새로 시작', message: '작성 중인 운동을 삭제하고 새로 시작할까요?', onConfirm: () => { localStorage.removeItem(draftKey); setDraft(null); setView('hub') } })
  const addExercise = (exercise) => {
    updateDraft((current) => ({ ...current, exercises: [...current.exercises, { exerciseId: exercise.id, name: exercise.name, bodyPart: exercise.bodyPart, exerciseOrder: current.exercises.length + 1, groupNumber: null, sets: [{ setNumber: 1, weight: 0, reps: 0, completed: false, setType: 'WORKING' }] }] })); setPickerOpen(false); triggerCharacter(CHARACTER_EVENTS.EXERCISE_ADD)
  }
  const removeExercise = (index) => setConfirming({ title: '운동 삭제', message: '이 운동과 입력한 세트를 삭제할까요?', onConfirm: () => updateDraft((current) => ({ ...current, exercises: current.exercises.filter((_, itemIndex) => itemIndex !== index) })) })
  const moveExercise = (from, to) => {
    if (to < 0 || to >= draft.exercises.length || from === to) return
    updateDraft((current) => { const items = [...current.exercises]; const [item] = items.splice(from, 1); items.splice(to, 0, item); return { ...current, exercises: items } })
  }
  const changeSet = (exerciseIndex, setIndex, field, value) => updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, currentExerciseIndex) => currentExerciseIndex === exerciseIndex ? { ...exercise, sets: exercise.sets.map((set, currentSetIndex) => currentSetIndex === setIndex ? { ...set, [field]: value } : set) } : exercise) }))
  const changeSetValue = (exerciseIndex, setIndex, field, value) => { changeSet(exerciseIndex, setIndex, field, value); if (field === 'completed' && value) triggerCharacter(CHARACTER_EVENTS.SET_COMPLETE) }
  const addSet = (exerciseIndex) => { updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, index) => index === exerciseIndex ? { ...exercise, sets: [...exercise.sets, { ...nextSet(current.exercises, exercise), setNumber: exercise.sets.length + 1, completed: false }] } : exercise) })); triggerCharacter(CHARACTER_EVENTS.SET_ADD) }
  const removeSet = (exerciseIndex, setIndex) => updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, index) => index === exerciseIndex ? { ...exercise, sets: exercise.sets.filter((_, currentSetIndex) => currentSetIndex !== setIndex) } : exercise) }))
  const loadPrevious = (sets) => {
    const index = draft.exercises.findIndex((exercise) => exercise.exerciseId === historyExercise.exerciseId)
    const apply = () => { updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, exerciseIndex) => exerciseIndex === index ? { ...exercise, sets: sets.map((set) => ({ ...set, completed: false })) } : exercise) })); setHistoryExercise(null); triggerCharacter(CHARACTER_EVENTS.RECORD_LOAD) }
    if (draft.exercises[index].sets.length) setConfirming({ title: '이전 기록 불러오기', message: '현재 입력한 세트를 이전 기록으로 교체할까요?', confirmLabel: '교체', onConfirm: apply })
    else apply()
  }
  const save = async () => {
    setError(null)
    setSaveFailed(false)
    if (!draft.exercises.length) { setError(new Error('운동 종목을 하나 이상 추가해 주세요.')); triggerCharacter(CHARACTER_EVENTS.WORKOUT_SAVE_ERROR); return }
    setSaving(true)
    try {
      await api(sessionId ? `/api/workout-sessions/${sessionId}` : '/api/workout-sessions', { method: sessionId ? 'PUT' : 'POST', body: workoutRequest(draft) })
      if (!sessionId) localStorage.removeItem(draftKey)
      triggerCharacter(CHARACTER_EVENTS.WORKOUT_SAVE_SUCCESS)
      if (!sessionId) await new Promise((resolve) => window.setTimeout(resolve, 1200))
      const { workoutDate } = draft
      setDraft(null); navigate(sessionId ? `/workouts/${sessionId}` : '/dashboard', false, sessionId ? {} : { selectedDate: workoutDate })
    } catch (requestError) { setError(requestError); setSaveFailed(true); triggerCharacter(CHARACTER_EVENTS.WORKOUT_SAVE_ERROR) } finally { setSaving(false) }
  }

  if (!routines || !exercises || !draft && sessionId) return <div className="page"><PageHeader back={sessionId ? `/workouts/${sessionId}` : null} navigate={navigate} title={sessionId ? '기록 수정' : '운동 시작'}/><ErrorMessage error={error}/>{!error && <Loading/>}</div>

  if (view === 'resume' && draft) {
    return <div className="page narrow-page"><PageHeader eyebrow="DRAFT FOUND" title="작성 중인 운동이 있어요"/><section className="resume-card"><div className="resume-graphic"><span>{countSets(draft.exercises, true)}</span><small>/{countSets(draft.exercises)} 세트</small></div><div><p>{formatDate(draft.workoutDate)}</p><h2>{draft.routineName || '자유 운동'}</h2><span>운동 {draft.exercises.length}개 · {new Date(draft.updatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 저장</span></div></section><button className="primary-button full" onClick={() => setView('editor')}>이어하기</button><button className="text-button danger" onClick={discard}>새로 시작</button>{confirming && <ConfirmModal {...confirming} onConfirm={() => { confirming.onConfirm(); setConfirming(null) }} onClose={() => setConfirming(null)}/>}</div>
  }

  if (view === 'hub') return <div className="page narrow-page"><PageHeader eyebrow={formatDate(newWorkoutDate)} title="어떻게 시작할까요?"/><p className="page-lead">오늘의 컨디션에 맞는 방법을 선택하세요.</p><div className="start-options"><button className="start-option primary-option" onClick={() => { setView('routines'); triggerCharacter(CHARACTER_EVENTS.ROUTINE_START) }}><span className="option-icon"><Icon name="routine" size={28}/></span><small>MY ROUTINE</small><h2>루틴으로 시작</h2><p>저장한 운동과 목표 세트를 바로 불러옵니다.</p><Icon name="chevron"/></button><button className="start-option" onClick={startFree}><span className="option-icon"><Icon name="plus" size={28}/></span><small>FREE WORKOUT</small><h2>자유 운동</h2><p>빈 운동에서 원하는 종목을 직접 추가합니다.</p><Icon name="chevron"/></button><button className="start-option start-option-wide" onClick={() => setView('records')}><span className="option-icon"><Icon name="calendar" size={28}/></span><small>PAST RECORD</small><h2>지난 기록 불러오기</h2><p>달력에서 고른 운동을 그대로 불러옵니다. 루틴 기록이면 오늘 운동도 그 루틴에 포함됩니다.</p><Icon name="chevron"/></button></div></div>

  if (view === 'records') return <RecordCalendar workoutDate={newWorkoutDate} onLoad={startFromSession} onBack={() => setView('hub')}/>

  if (view === 'routines') return <div className="page"><PageHeader back="/workout" navigate={() => setView('hub')} eyebrow={formatDate(newWorkoutDate)} title="루틴을 선택하세요"/><div className="routine-select-grid">{routines.map((routine) => <button className="routine-select-card" key={routine.id} onClick={() => startRoutine(routine)}><div><span className="routine-symbol"><Icon name="routine"/></span><small>{routine.exercises.length} EXERCISES</small></div><h2>{routine.name}</h2><p>{routine.exercises.slice(0, 3).map((exercise) => exercise.name).join(' · ')}</p><footer><span>목표 {targetSetCount(routine.exercises)}세트</span><Icon name="chevron"/></footer></button>)}{!routines.length && <EmptyState icon="routine" title="저장된 루틴이 없어요" description="루틴을 먼저 만들거나 자유 운동으로 시작하세요."/>}</div></div>

  const completed = countSets(draft.exercises, true)
  const totalSets = countSets(draft.exercises)
  const totalVolume = completedVolume(draft.exercises)
  return <div className="page workout-editor-page">
    <PageHeader back={sessionId ? `/workouts/${sessionId}` : null} navigate={navigate} eyebrow={formatDate(draft.workoutDate)} title={draft.routineName || '자유 운동'} action={<span className="save-status"><i/> {sessionId ? '기록 수정 중' : '임시 저장됨'}</span>}/>
    <div className="workout-progress"><div><b>{completed}</b><span> / {totalSets} 세트 완료 · 총 볼륨 {formatNumber(totalVolume, 2)}kg</span></div><div className="progress-track"><i style={{ width: `${totalSets ? completed / totalSets * 100 : 0}%` }}/></div></div>
    <ErrorMessage error={error}/>
    <div className="exercise-cards">{draft.exercises.map((exercise, exerciseIndex) => <ExerciseCard key={exerciseIndex} exercises={draft.exercises} index={exerciseIndex} dragIndex={dragIndex} onDragIndex={setDragIndex} onMove={moveExercise} onRemove={removeExercise} onUnbind={(groupNumber) => updateDraft((current) => ({ ...current, exercises: unbindGroup(current.exercises, groupNumber) }))} action={<button type="button" className="history-button" onClick={() => { setHistoryExercise(exercise); triggerCharacter(CHARACTER_EVENTS.RECORD_CHECK) }}><Icon name="history" size={17}/> 이전 기록</button>}>
      <SetTable exercises={draft.exercises} exercise={exercise} trackCompleted volume={<p className="exercise-volume"><span>운동 볼륨</span><b>{formatNumber(completedVolume([exercise]), 2)} kg</b></p>} onChange={(setIndex, field, value) => changeSetValue(exerciseIndex, setIndex, field, value)} onAdd={() => addSet(exerciseIndex)} onRemove={(setIndex) => removeSet(exerciseIndex, setIndex)}/>
    </ExerciseCard>)}</div>
    {!draft.exercises.length && <EmptyState title="운동 종목을 추가해 주세요" description="종목을 추가하면 세트 기록을 바로 시작할 수 있어요."/>}
    <BindExercisesButton exercises={draft.exercises} onBind={(indexes) => updateDraft((current) => ({ ...current, exercises: bindExercises(current.exercises, indexes) }))}/>
    <button className="add-exercise-button" onClick={() => setPickerOpen(true)}><Icon name="plus"/> 운동 종목 추가</button>
    <div className="sticky-save"><div><span>{draft.exercises.length}개 운동 · {formatNumber(totalVolume, 2)}kg</span><b>{completed}/{totalSets}세트 완료</b></div><button className="primary-button" onClick={save} disabled={saving}>{saving ? '저장 중...' : saveFailed ? '다시 시도' : sessionId ? '수정 저장' : '운동 저장'}</button></div>
    {pickerOpen && <ExercisePicker exercises={exercises} excluded={draft.exercises.map((exercise) => exercise.exerciseId)} onSelect={addExercise} onClose={() => setPickerOpen(false)}/>} 
    {historyExercise && <PreviousRecords exercise={historyExercise} onLoad={loadPrevious} onClose={() => setHistoryExercise(null)}/>} 
    {confirming && <ConfirmModal {...confirming} onConfirm={() => { confirming.onConfirm(); setConfirming(null) }} onClose={() => setConfirming(null)}/>}
  </div>
}
