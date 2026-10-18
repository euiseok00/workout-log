import { useEffect, useState } from 'react'
import { api } from './api'
import ExercisePicker from './ExercisePicker'
import { EmptyState, ErrorMessage, Icon, Link, Loading, Modal, PageHeader } from './ui'
import { bodyPartLabel } from './utils'

function DeleteRoutineModal({ routine, onClose, onDeleted }) {
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState(null)
  const remove = async () => {
    setDeleting(true); setError(null)
    try {
      await api(`/api/routines/${routine.id}`, { method: 'DELETE' })
      onDeleted()
    } catch (requestError) { setError(requestError); setDeleting(false) }
  }
  return <Modal title="루틴 삭제" onClose={() => !deleting && onClose()}><p>“{routine.name}” 루틴을 삭제하시겠습니까? 완료한 운동 기록은 유지됩니다.</p><ErrorMessage error={error}/><div className="detail-delete-actions"><button type="button" className="secondary-button" onClick={onClose} disabled={deleting}>취소</button><button type="button" className="primary-button delete-confirm-button" onClick={remove} disabled={deleting}>{deleting ? '삭제 중...' : '삭제'}</button></div></Modal>
}

export function RoutineList({ navigate }) {
  const [routines, setRoutines] = useState(null)
  const [error, setError] = useState(null)
  const [deleting, setDeleting] = useState(null)
  useEffect(() => {
    let active = true
    api('/api/routines').then((value) => active && setRoutines(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [])
  return <div className="page">
    <PageHeader eyebrow="MY ROUTINES" title="나만의 운동 루틴" action={<Link to="/routines/new" navigate={navigate} className="primary-button compact"><Icon name="plus" size={18}/> 새 루틴</Link>}/>
    <ErrorMessage error={error}/>
    {!routines ? <Loading/> : routines.length ? <div className="routine-list">{routines.map((routine, index) => <div className="routine-list-item" key={routine.id}>
      <Link to={`/routines/${routine.id}`} navigate={navigate} className="routine-card"><div className={`routine-number color-${index % 4}`}>{String(index + 1).padStart(2, '0')}</div><div className="routine-card-main"><p className="eyebrow">{routine.exercises.length} EXERCISES</p><h2>{routine.name}</h2><div className="routine-tags">{routine.exercises.slice(0, 4).map((exercise, exerciseIndex) => <span key={exerciseIndex}>{exercise.name}</span>)}</div></div><div className="routine-card-meta"><b>{routine.exercises.reduce((sum, exercise) => sum + (exercise.sets?.length || exercise.targetSets || 0), 0)}</b><small>목표 세트</small><Icon name="chevron"/></div></Link>
      <button type="button" className="routine-delete-button" onClick={() => setDeleting(routine)} aria-label={`${routine.name} 삭제`}>삭제</button>
    </div>)}</div> : <EmptyState icon="routine" title="아직 만든 루틴이 없어요" description="자주 하는 운동을 묶어 빠르게 시작해보세요." action={<Link to="/routines/new" navigate={navigate} className="primary-button compact">첫 루틴 만들기</Link>}/>}
    {deleting && <DeleteRoutineModal routine={deleting} onClose={() => setDeleting(null)} onDeleted={() => { setRoutines((current) => current.filter((routine) => routine.id !== deleting.id)); setDeleting(null) }}/>}
  </div>
}

export function RoutineForm({ id, navigate }) {
  const editing = Boolean(id)
  const [name, setName] = useState('')
  const [items, setItems] = useState([])
  const [exercises, setExercises] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
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
          sets: exercise.sets?.length ? exercise.sets.map((set) => ({ weight: set.weight, reps: set.reps }))
            : Array.from({ length: exercise.targetSets || 1 }, () => ({ weight: 0, reps: exercise.targetReps || 0 })),
        })))
      }
    }).catch((requestError) => active && setError(requestError)).finally(() => active && setLoading(false))
    return () => { active = false }
  }, [editing, id])

  const add = (exercise) => {
    setItems((current) => [...current, { exerciseId: exercise.id, name: exercise.name, bodyPart: exercise.bodyPart, sets: Array.from({ length: 3 }, () => ({ weight: 0, reps: 10 })) }])
    setPickerOpen(false)
  }
  const updateSet = (exerciseIndex, setIndex, field, value) => setItems((current) => current.map((item, index) => index === exerciseIndex ? { ...item, sets: item.sets.map((set, index) => index === setIndex ? { ...set, [field]: value } : set) } : item))
  const addSet = (exerciseIndex) => setItems((current) => current.map((item, index) => index === exerciseIndex ? { ...item, sets: [...item.sets, { weight: 0, reps: 0 }] } : item))
  const removeSet = (exerciseIndex, setIndex) => setItems((current) => current.map((item, index) => index === exerciseIndex ? { ...item, sets: item.sets.filter((_, index) => index !== setIndex) } : item))
  const move = (from, to) => {
    if (from == null || to < 0 || to >= items.length || from === to) return
    setItems((current) => { const next = [...current]; const [item] = next.splice(from, 1); next.splice(to, 0, item); return next })
  }
  const save = async (event) => {
    event.preventDefault(); setError(null)
    if (!items.length || items.some((item) => !item.sets.length)) { setError(new Error('각 운동에 세트를 하나 이상 추가해 주세요.')); return }
    setSaving(true)
    try {
      await api(editing ? `/api/routines/${id}` : '/api/routines', { method: editing ? 'PUT' : 'POST', body: {
        name,
        exercises: items.map((item, index) => ({
          exerciseId: item.exerciseId,
          exerciseOrder: index + 1,
          targetSets: item.sets.length,
          targetReps: Number(item.sets[0].reps) || null,
          sets: item.sets.map((set, setIndex) => ({ setNumber: setIndex + 1, weight: Number(set.weight), reps: Number(set.reps) })),
        })),
      } })
      navigate('/routines')
    } catch (requestError) { setError(requestError) } finally { setSaving(false) }
  }

  if (loading || !exercises) return <div className="page"><PageHeader back="/routines" navigate={navigate} title="루틴"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  return <><form className="page routine-form-page" onSubmit={save}>
    <PageHeader back="/routines" navigate={navigate} eyebrow={editing ? 'EDIT ROUTINE' : 'NEW ROUTINE'} title={editing ? '루틴 수정' : '새 루틴 만들기'}/>
    {editing && <div className="detail-actions"><button type="button" className="text-button danger" onClick={() => setDeleteOpen(true)}>루틴 삭제</button></div>}
    <ErrorMessage error={error}/>
    {!editing && <aside className="routine-import-notice"><p>완료한 운동 기록을 루틴으로 저장할 수도 있어요.</p><Link to="/dashboard" navigate={navigate} className="secondary-button compact">운동 기록에서 가져오기</Link></aside>}
    <section className="card form-section"><label className="field-label" htmlFor="routine-name">루틴 이름</label><input id="routine-name" className="large-input" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 가슴 루틴" maxLength="100" required/></section>
    <section className="routine-builder"><header className="section-header"><div><p className="eyebrow">EXERCISE PLAN</p><h2>운동 구성</h2></div><span className="count-badge">{items.length}개</span></header>
      {items.map((item, exerciseIndex) => <article className="routine-exercise-card" key={exerciseIndex} onDragOver={(event) => event.preventDefault()} onDrop={() => { move(dragIndex, exerciseIndex); setDragIndex(null) }}>
        <header><span className="drag-handle" draggable onDragStart={() => setDragIndex(exerciseIndex)}><Icon name="drag"/></span><span className="exercise-symbol">{item.name.slice(0, 1)}</span><div className="routine-exercise-name"><b>{item.name}</b><small>{bodyPartLabel(item.bodyPart)}</small></div><div className="row-actions"><button type="button" onClick={() => move(exerciseIndex, exerciseIndex - 1)} disabled={exerciseIndex === 0} aria-label="위로 이동">↑</button><button type="button" onClick={() => move(exerciseIndex, exerciseIndex + 1)} disabled={exerciseIndex === items.length - 1} aria-label="아래로 이동">↓</button><button type="button" onClick={() => setItems((current) => current.filter((_, index) => index !== exerciseIndex))} aria-label="운동 삭제"><Icon name="close" size={17}/></button></div></header>
        <div className="routine-set-table"><div className="routine-set-head"><span>세트</span><span>중량</span><span>횟수</span><span/></div>{item.sets.map((set, setIndex) => <div className="routine-set-row" key={setIndex}><b>{setIndex + 1}</b><label><input type="number" min="0" max="9999.99" step="0.01" required value={set.weight} onChange={(event) => updateSet(exerciseIndex, setIndex, 'weight', event.target.value)} aria-label={`${item.name} ${setIndex + 1}세트 중량`}/><span>kg</span></label><label><input type="number" min="0" step="1" required value={set.reps} onChange={(event) => updateSet(exerciseIndex, setIndex, 'reps', event.target.value)} aria-label={`${item.name} ${setIndex + 1}세트 횟수`}/><span>회</span></label><button type="button" className="remove-set" onClick={() => removeSet(exerciseIndex, setIndex)} aria-label={`${item.name} ${setIndex + 1}세트 삭제`}><Icon name="close" size={15}/></button></div>)}</div>
        <button type="button" className="add-row-button" onClick={() => addSet(exerciseIndex)}><Icon name="plus" size={17}/> 세트 추가</button>
      </article>)}
      {!items.length && <EmptyState icon="routine" title="운동을 추가해 루틴을 구성하세요"/>}<button type="button" className="add-exercise-button" onClick={() => setPickerOpen(true)}><Icon name="plus"/> 운동 추가</button>
    </section>
    <div className="form-actions"><Link to="/routines" navigate={navigate} className="secondary-button">취소</Link><button className="primary-button" disabled={saving}>{saving ? '저장 중...' : '루틴 저장'}</button></div>
  </form>
    {pickerOpen && <ExercisePicker exercises={exercises} excluded={items.map((item) => item.exerciseId)} onSelect={add} onClose={() => setPickerOpen(false)}/>}
    {deleteOpen && <DeleteRoutineModal routine={{ id, name }} onClose={() => setDeleteOpen(false)} onDeleted={() => navigate('/routines')}/>}
  </>
}
