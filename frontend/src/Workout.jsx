import { useEffect, useMemo, useState } from 'react'
import { api } from './api'
import ExercisePicker from './ExercisePicker'
import { EmptyState, ErrorMessage, Icon, Loading, Modal, PageHeader } from './ui'
import { bodyPartLabel, formatDate, todayString } from './utils'

function readDraft(key) {
  try { return JSON.parse(localStorage.getItem(key)) } catch { return null }
}

function normalizeDraft(draft) {
  return { ...draft, exercises: draft.exercises.map((exercise, exerciseIndex) => ({ ...exercise, exerciseOrder: exerciseIndex + 1, sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1 })) })), updatedAt: new Date().toISOString() }
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
    {!records ? <Loading/> : <div className="record-list">{records.length ? records.map((record) => <article className="record-card" key={record.sessionId}><header><b>{formatDate(record.workoutDate)}</b><span>{record.sets.filter((set) => set.completed).length}세트</span></header><div>{record.sets.map((set) => <p key={set.setNumber}><span>{set.setNumber}세트</span><b>{Number(set.weight) === 0 ? '맨몸' : `${set.weight}kg`} × {set.reps}회</b>{set.completed && <Icon name="check" size={16}/>}</p>)}</div><button className="secondary-button full" onClick={() => onLoad(record.sets)}>이 기록 불러오기</button></article>) : <EmptyState icon="history" title="아직 이전 기록이 없어요" description="첫 기록을 완료하면 이곳에서 다시 불러올 수 있어요."/>}</div>}
  </Modal>
}

export default function Workout({ navigate, auth }) {
  const draftKey = `workout_draft:${auth.user.id}`
  const initialDraft = useMemo(() => readDraft(draftKey), [draftKey])
  const [draft, setDraft] = useState(initialDraft)
  const [view, setView] = useState(initialDraft ? 'resume' : 'hub')
  const [routines, setRoutines] = useState(null)
  const [exercises, setExercises] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [historyExercise, setHistoryExercise] = useState(null)
  const [dragIndex, setDragIndex] = useState(null)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveFailed, setSaveFailed] = useState(false)

  useEffect(() => {
    let active = true
    Promise.all([api('/api/routines'), api('/api/exercises')]).then(([routineData, exerciseData]) => {
      if (active) { setRoutines(routineData); setExercises(exerciseData) }
    }).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (draft) localStorage.setItem(draftKey, JSON.stringify(draft))
  }, [draft, draftKey])

  const updateDraft = (updater) => setDraft((current) => normalizeDraft(typeof updater === 'function' ? updater(current) : updater))
  const startFree = () => {
    const next = { workoutDate: todayString(), routineId: null, routineName: null, exercises: [], updatedAt: new Date().toISOString() }
    setDraft(next); setView('editor')
  }
  const startRoutine = (routine) => {
    const next = { workoutDate: todayString(), routineId: routine.id, routineName: routine.name, exercises: routine.exercises.map((exercise, index) => ({ exerciseId: exercise.exerciseId, name: exercise.name, bodyPart: exercise.bodyPart, exerciseOrder: index + 1, sets: Array.from({ length: exercise.targetSets || 1 }, (_, setIndex) => ({ setNumber: setIndex + 1, weight: 0, reps: exercise.targetReps || 0, completed: false })) })), updatedAt: new Date().toISOString() }
    setDraft(next); setView('editor')
  }
  const discard = () => {
    if (!window.confirm('작성 중인 운동을 삭제하고 새로 시작할까요?')) return
    localStorage.removeItem(draftKey); setDraft(null); setView('hub')
  }
  const addExercise = (exercise) => {
    updateDraft((current) => ({ ...current, exercises: [...current.exercises, { exerciseId: exercise.id, name: exercise.name, bodyPart: exercise.bodyPart, exerciseOrder: current.exercises.length + 1, sets: [{ setNumber: 1, weight: 0, reps: 0, completed: false }] }] })); setPickerOpen(false)
  }
  const removeExercise = (index) => {
    if (!window.confirm('이 운동과 입력한 세트를 삭제할까요?')) return
    updateDraft((current) => ({ ...current, exercises: current.exercises.filter((_, itemIndex) => itemIndex !== index) }))
  }
  const moveExercise = (from, to) => {
    if (to < 0 || to >= draft.exercises.length || from === to) return
    updateDraft((current) => { const items = [...current.exercises]; const [item] = items.splice(from, 1); items.splice(to, 0, item); return { ...current, exercises: items } })
  }
  const changeSet = (exerciseIndex, setIndex, field, value) => updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, currentExerciseIndex) => currentExerciseIndex === exerciseIndex ? { ...exercise, sets: exercise.sets.map((set, currentSetIndex) => currentSetIndex === setIndex ? { ...set, [field]: field === 'completed' ? value : Number(value) } : set) } : exercise) }))
  const addSet = (exerciseIndex) => updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, index) => index === exerciseIndex ? { ...exercise, sets: [...exercise.sets, { setNumber: exercise.sets.length + 1, weight: 0, reps: 0, completed: false }] } : exercise) }))
  const removeSet = (exerciseIndex, setIndex) => updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, index) => index === exerciseIndex ? { ...exercise, sets: exercise.sets.filter((_, currentSetIndex) => currentSetIndex !== setIndex) } : exercise) }))
  const loadPrevious = (sets) => {
    const index = draft.exercises.findIndex((exercise) => exercise.exerciseId === historyExercise.exerciseId)
    if (draft.exercises[index].sets.length && !window.confirm('현재 입력한 세트를 이전 기록으로 교체할까요?')) return
    updateDraft((current) => ({ ...current, exercises: current.exercises.map((exercise, exerciseIndex) => exerciseIndex === index ? { ...exercise, sets: sets.map((set) => ({ ...set, completed: false })) } : exercise) })); setHistoryExercise(null)
  }
  const save = async () => {
    setError(null)
    setSaveFailed(false)
    if (!draft.exercises.length) { setError(new Error('운동 종목을 하나 이상 추가해 주세요.')); return }
    setSaving(true)
    try {
      await api('/api/workout-sessions', { method: 'POST', body: { workoutDate: draft.workoutDate, routineId: draft.routineId, exercises: draft.exercises.map((exercise, index) => ({ exerciseId: exercise.exerciseId, exerciseOrder: index + 1, sets: exercise.sets.map((set, setIndex) => ({ setNumber: setIndex + 1, weight: Number(set.weight), reps: Number(set.reps), completed: Boolean(set.completed) })) })) } })
      localStorage.removeItem(draftKey); setDraft(null); navigate('/dashboard')
    } catch (requestError) { setError(requestError); setSaveFailed(true) } finally { setSaving(false) }
  }

  if (!routines || !exercises) return <div className="page"><PageHeader title="운동 시작"/><ErrorMessage error={error}/><Loading/></div>

  if (view === 'resume' && draft) {
    const allSets = draft.exercises.flatMap((exercise) => exercise.sets)
    return <div className="page narrow-page"><PageHeader eyebrow="DRAFT FOUND" title="작성 중인 운동이 있어요"/><section className="resume-card"><div className="resume-graphic"><span>{allSets.filter((set) => set.completed).length}</span><small>/{allSets.length} 세트</small></div><div><p>{formatDate(draft.workoutDate)}</p><h2>{draft.routineName || '자유 운동'}</h2><span>운동 {draft.exercises.length}개 · {new Date(draft.updatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 저장</span></div></section><button className="primary-button full" onClick={() => setView('editor')}>이어하기</button><button className="text-button danger" onClick={discard}>새로 시작</button></div>
  }

  if (view === 'hub') return <div className="page narrow-page"><PageHeader eyebrow="START WORKOUT" title="어떻게 시작할까요?"/><p className="page-lead">오늘의 컨디션에 맞는 방법을 선택하세요.</p><div className="start-options"><button className="start-option primary-option" onClick={() => setView('routines')}><span className="option-icon"><Icon name="routine" size={28}/></span><small>MY ROUTINE</small><h2>루틴으로 시작</h2><p>저장한 운동과 목표 세트를 바로 불러옵니다.</p><Icon name="chevron"/></button><button className="start-option" onClick={startFree}><span className="option-icon"><Icon name="plus" size={28}/></span><small>FREE WORKOUT</small><h2>자유 운동</h2><p>빈 운동에서 원하는 종목을 직접 추가합니다.</p><Icon name="chevron"/></button></div></div>

  if (view === 'routines') return <div className="page"><PageHeader back="/workout" navigate={() => setView('hub')} eyebrow="SELECT ROUTINE" title="루틴을 선택하세요"/><div className="routine-select-grid">{routines.map((routine) => <button className="routine-select-card" key={routine.id} onClick={() => startRoutine(routine)}><div><span className="routine-symbol"><Icon name="routine"/></span><small>{routine.exercises.length} EXERCISES</small></div><h2>{routine.name}</h2><p>{routine.exercises.slice(0, 3).map((exercise) => exercise.name).join(' · ')}</p><footer><span>목표 {routine.exercises.reduce((sum, exercise) => sum + (exercise.targetSets || 0), 0)}세트</span><Icon name="chevron"/></footer></button>)}{!routines.length && <EmptyState icon="routine" title="저장된 루틴이 없어요" description="루틴을 먼저 만들거나 자유 운동으로 시작하세요."/>}</div></div>

  const completed = draft.exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed).length
  const totalSets = draft.exercises.flatMap((exercise) => exercise.sets).length
  return <div className="page workout-editor-page">
    <PageHeader eyebrow={formatDate(draft.workoutDate)} title={draft.routineName || '자유 운동'} action={<span className="save-status"><i/> 임시 저장됨</span>}/>
    <div className="workout-progress"><div><b>{completed}</b><span> / {totalSets} 세트 완료</span></div><div className="progress-track"><i style={{ width: `${totalSets ? completed / totalSets * 100 : 0}%` }}/></div></div>
    <ErrorMessage error={error}/>
    <div className="exercise-cards">{draft.exercises.map((exercise, exerciseIndex) => <article className="exercise-card" key={exercise.exerciseId} onDragOver={(event) => event.preventDefault()} onDrop={() => { moveExercise(dragIndex, exerciseIndex); setDragIndex(null) }}>
      <header><span className="drag-handle" title="드래그하여 순서 변경" draggable onDragStart={() => setDragIndex(exerciseIndex)}><Icon name="drag"/></span><div><h2>{exercise.name}</h2><p>{bodyPartLabel(exercise.bodyPart)}</p></div><button className="history-button" onClick={() => setHistoryExercise(exercise)}><Icon name="history" size={17}/> 이전 기록</button><div className="card-actions"><button onClick={() => moveExercise(exerciseIndex, exerciseIndex - 1)} disabled={exerciseIndex === 0} aria-label="위로 이동">↑</button><button onClick={() => moveExercise(exerciseIndex, exerciseIndex + 1)} disabled={exerciseIndex === draft.exercises.length - 1} aria-label="아래로 이동">↓</button><button onClick={() => removeExercise(exerciseIndex)} aria-label="운동 삭제"><Icon name="close" size={17}/></button></div></header>
      <div className="set-table"><div className="set-head"><span>세트</span><span>중량</span><span>횟수</span><span>완료</span><span/></div>{exercise.sets.map((set, setIndex) => <div className={`set-row ${set.completed ? 'completed' : ''}`} key={setIndex}><b>{setIndex + 1}</b><label><input type="number" min="0" max="9999.99" step="0.01" value={set.weight} onChange={(event) => changeSet(exerciseIndex, setIndex, 'weight', event.target.value)}/><span>kg</span></label><label><input type="number" min="0" step="1" value={set.reps} onChange={(event) => changeSet(exerciseIndex, setIndex, 'reps', event.target.value)}/><span>회</span></label><button className={`check-button ${set.completed ? 'checked' : ''}`} onClick={() => changeSet(exerciseIndex, setIndex, 'completed', !set.completed)} aria-label={`${setIndex + 1}세트 완료`}><Icon name="check" size={17}/></button><button className="remove-set" onClick={() => removeSet(exerciseIndex, setIndex)} aria-label={`${setIndex + 1}세트 삭제`}><Icon name="close" size={15}/></button></div>)}</div>
      <button className="add-row-button" onClick={() => addSet(exerciseIndex)}><Icon name="plus" size={17}/> 세트 추가</button>
    </article>)}</div>
    {!draft.exercises.length && <EmptyState title="운동 종목을 추가해 주세요" description="종목을 추가하면 세트 기록을 바로 시작할 수 있어요."/>}
    <button className="add-exercise-button" onClick={() => setPickerOpen(true)}><Icon name="plus"/> 운동 종목 추가</button>
    <div className="sticky-save"><div><span>{draft.exercises.length}개 운동</span><b>{completed}/{totalSets}세트 완료</b></div><button className="primary-button" onClick={save} disabled={saving}>{saving ? '저장 중...' : saveFailed ? '다시 시도' : '운동 저장'}</button></div>
    {pickerOpen && <ExercisePicker exercises={exercises} excluded={draft.exercises.map((exercise) => exercise.exerciseId)} onSelect={addExercise} onClose={() => setPickerOpen(false)}/>} 
    {historyExercise && <PreviousRecords exercise={historyExercise} onLoad={loadPrevious} onClose={() => setHistoryExercise(null)}/>} 
  </div>
}
