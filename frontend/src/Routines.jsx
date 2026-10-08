import { useEffect, useState } from 'react'
import { api } from './api'
import ExercisePicker from './ExercisePicker'
import { EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from './ui'
import { bodyPartLabel } from './utils'

export function RoutineList({ navigate }) {
  const [routines, setRoutines] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api('/api/routines').then((value) => active && setRoutines(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [])
  return <div className="page"><PageHeader eyebrow="MY ROUTINES" title="나만의 운동 루틴" action={<Link to="/routines/new" navigate={navigate} className="primary-button compact"><Icon name="plus" size={18}/> 새 루틴</Link>}/><ErrorMessage error={error}/>{!routines ? <Loading/> : routines.length ? <div className="routine-list">{routines.map((routine, index) => <Link to={`/routines/${routine.id}`} navigate={navigate} className="routine-card" key={routine.id}><div className={`routine-number color-${index % 4}`}>{String(index + 1).padStart(2, '0')}</div><div className="routine-card-main"><p className="eyebrow">{routine.exercises.length} EXERCISES</p><h2>{routine.name}</h2><div className="routine-tags">{routine.exercises.slice(0, 4).map((exercise) => <span key={exercise.exerciseId}>{exercise.name}</span>)}</div></div><div className="routine-card-meta"><b>{routine.exercises.reduce((sum, exercise) => sum + (exercise.targetSets || 0), 0)}</b><small>목표 세트</small><Icon name="chevron"/></div></Link>)}</div> : <EmptyState icon="routine" title="아직 만든 루틴이 없어요" description="자주 하는 운동을 묶어 빠르게 시작해보세요." action={<Link to="/routines/new" navigate={navigate} className="primary-button compact">첫 루틴 만들기</Link>}/>}</div>
}

export function RoutineForm({ id, navigate }) {
  const editing = Boolean(id)
  const [name, setName] = useState('')
  const [items, setItems] = useState([])
  const [exercises, setExercises] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [dragIndex, setDragIndex] = useState(null)
  const [loading, setLoading] = useState(editing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    const requests = [api('/api/exercises')]
    if (editing) requests.push(api(`/api/routines/${id}`))
    Promise.all(requests).then(([exerciseData, routine]) => {
      if (!active) return
      setExercises(exerciseData)
      if (routine) {
        setName(routine.name)
        setItems(routine.exercises.map((exercise) => ({
          ...exercise,
          targetSets: exercise.targetSets || 1,
          targetReps: exercise.targetReps || 1,
        })))
      }
    }).catch((requestError) => active && setError(requestError)).finally(() => active && setLoading(false))
    return () => { active = false }
  }, [editing, id])

  const add = (exercise) => {
    setItems((current) => [...current, { exerciseId: exercise.id, name: exercise.name, bodyPart: exercise.bodyPart, targetSets: 3, targetReps: 10 }]); setPickerOpen(false)
  }
  const update = (index, field, value) => setItems((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, [field]: Number(value) } : item))
  const move = (from, to) => {
    if (to < 0 || to >= items.length || from === to) return
    setItems((current) => { const next = [...current]; const [item] = next.splice(from, 1); next.splice(to, 0, item); return next })
  }
  const save = async (event) => {
    event.preventDefault(); setError(null)
    if (!items.length) { setError(new Error('운동을 하나 이상 추가해 주세요.')); return }
    setSaving(true)
    try {
      await api(editing ? `/api/routines/${id}` : '/api/routines', { method: editing ? 'PUT' : 'POST', body: { name, exercises: items.map((item, index) => ({ exerciseId: item.exerciseId, exerciseOrder: index + 1, targetSets: item.targetSets, targetReps: item.targetReps })) } })
      navigate('/routines')
    } catch (requestError) { setError(requestError) } finally { setSaving(false) }
  }

  if (loading || !exercises) return <div className="page"><Loading/></div>
  return <form className="page routine-form-page" onSubmit={save}><PageHeader back="/routines" navigate={navigate} eyebrow={editing ? 'EDIT ROUTINE' : 'NEW ROUTINE'} title={editing ? '루틴 수정' : '새 루틴 만들기'}/><ErrorMessage error={error}/><section className="card form-section"><label className="field-label" htmlFor="routine-name">루틴 이름</label><input id="routine-name" className="large-input" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 가슴 루틴" maxLength="100" required/></section><section className="routine-builder"><header className="section-header"><div><p className="eyebrow">EXERCISE PLAN</p><h2>운동 구성</h2></div><span className="count-badge">{items.length}개</span></header>{items.map((item, index) => <article className="routine-exercise-row" key={item.exerciseId} onDragOver={(event) => event.preventDefault()} onDrop={() => { move(dragIndex, index); setDragIndex(null) }}><span className="drag-handle" draggable onDragStart={() => setDragIndex(index)}><Icon name="drag"/></span><span className="exercise-symbol">{item.name.slice(0, 1)}</span><div className="routine-exercise-name"><b>{item.name}</b><small>{bodyPartLabel(item.bodyPart)}</small></div><label><span>목표 세트</span><input type="number" min="1" value={item.targetSets} onChange={(event) => update(index, 'targetSets', event.target.value)} required/></label><label><span>목표 횟수</span><input type="number" min="1" value={item.targetReps} onChange={(event) => update(index, 'targetReps', event.target.value)} required/></label><div className="row-actions"><button type="button" onClick={() => move(index, index - 1)} disabled={index === 0}>↑</button><button type="button" onClick={() => move(index, index + 1)} disabled={index === items.length - 1}>↓</button><button type="button" onClick={() => setItems((current) => current.filter((_, currentIndex) => currentIndex !== index))}><Icon name="close" size={17}/></button></div></article>)}{!items.length && <EmptyState icon="routine" title="운동을 추가해 루틴을 구성하세요"/>}<button type="button" className="add-exercise-button" onClick={() => setPickerOpen(true)}><Icon name="plus"/> 운동 추가</button></section><div className="form-actions"><Link to="/routines" navigate={navigate} className="secondary-button">취소</Link><button className="primary-button" disabled={saving}>{saving ? '저장 중...' : '루틴 저장'}</button></div>{pickerOpen && <ExercisePicker exercises={exercises} excluded={items.map((item) => item.exerciseId)} onSelect={add} onClose={() => setPickerOpen(false)}/>}</form>
}
