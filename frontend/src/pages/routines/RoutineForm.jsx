import { useEffect, useState } from 'react'
import { api } from '../../lib/api'
import { ConfirmModal, EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from '../../components/ui'
import DeleteRoutineModal from './DeleteRoutineModal'
import ExercisePicker from '../../components/ExercisePicker'
import { ExerciseCard, SetTable } from '../../components/ExerciseCard'
import { BindExercisesButton } from '../../components/ExerciseGroups'
import { bindExercises, nextSet, normalizeGroups, targetSetCount, unbindGroup } from '../../lib/setGroups'

export default function RoutineForm({ id, navigate }) {
  const editing = Boolean(id)
  const [name, setName] = useState('')
  const [items, setRawItems] = useState([])
  const setItems = (update) => setRawItems((current) => normalizeGroups(update(current)))
  const [exercises, setExercises] = useState(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [removing, setRemoving] = useState(null)
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
        setRawItems(routine.exercises.map((exercise) => ({
          ...exercise,
          sets: exercise.sets?.length ? exercise.sets.map((set) => ({ weight: set.weight, reps: set.reps, setType: set.setType }))
            : Array.from({ length: exercise.targetSets || 1 }, () => ({ weight: 0, reps: exercise.targetReps || 0, setType: 'WORKING' })),
        })))
      }
    }).catch((requestError) => active && setError(requestError)).finally(() => active && setLoading(false))
    return () => { active = false }
  }, [editing, id])

  const add = (exercise) => {
    setItems((current) => [...current, { exerciseId: exercise.id, name: exercise.name, bodyPart: exercise.bodyPart, groupNumber: null, sets: Array.from({ length: 3 }, () => ({ weight: 0, reps: 10, setType: 'WORKING' })) }])
    setPickerOpen(false)
  }
  const updateSet = (exerciseIndex, setIndex, field, value) => setItems((current) => current.map((item, index) => index === exerciseIndex ? { ...item, sets: item.sets.map((set, index) => index === setIndex ? { ...set, [field]: value } : set) } : item))
  const addSet = (exerciseIndex) => setItems((current) => current.map((item, index) => index === exerciseIndex ? { ...item, sets: [...item.sets, nextSet(current, item)] } : item))
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
          groupNumber: item.groupNumber ?? null,
          targetSets: item.sets.length,
          targetReps: Number(item.sets[0].reps) || null,
          sets: item.sets.map((set, setIndex) => ({ setNumber: setIndex + 1, weight: Number(set.weight), reps: Number(set.reps), setType: set.setType || 'WORKING' })),
        })),
      } })
      navigate('/routines')
    } catch (requestError) { setError(requestError) } finally { setSaving(false) }
  }

  if (loading || !exercises) return <div className="page"><PageHeader back="/routines" navigate={navigate} title="루틴"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  return <><form className="page routine-form-page" onSubmit={save}>
    <PageHeader back="/routines" navigate={navigate} eyebrow={editing ? 'EDIT ROUTINE' : 'NEW ROUTINE'} title={editing ? '루틴 수정' : '새 루틴 만들기'}/>
    {editing && <div className="detail-actions"><Link to={`/routines/${id}/history`} navigate={navigate} className="secondary-button compact">기록 변화</Link><button type="button" className="text-button danger" onClick={() => setDeleteOpen(true)}>루틴 삭제</button></div>}
    <ErrorMessage error={error}/>
    {!editing && <aside className="routine-import-notice"><p>완료한 운동 기록을 루틴으로 저장할 수도 있어요.</p><Link to="/dashboard" navigate={navigate} className="secondary-button compact">운동 기록에서 가져오기</Link></aside>}
    <section className="card form-section"><label className="field-label" htmlFor="routine-name">루틴 이름</label><input id="routine-name" className="large-input" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 가슴 루틴" maxLength="100" required/></section>
    <section className="routine-builder"><header className="section-header"><div><p className="eyebrow">EXERCISE PLAN</p><h2>운동 구성</h2></div><span className="count-badge">{items.length}개</span></header>
      {items.map((item, exerciseIndex) => <ExerciseCard key={exerciseIndex} exercises={items} index={exerciseIndex} dragIndex={dragIndex} onDragIndex={setDragIndex} onMove={move} onRemove={setRemoving} onUnbind={(groupNumber) => setItems((current) => unbindGroup(current, groupNumber))}>
        <SetTable exercises={items} exercise={item} onChange={(setIndex, field, value) => updateSet(exerciseIndex, setIndex, field, value)} onAdd={() => addSet(exerciseIndex)} onRemove={(setIndex) => removeSet(exerciseIndex, setIndex)}/>
      </ExerciseCard>)}
      {!items.length && <EmptyState icon="routine" title="운동을 추가해 루틴을 구성하세요"/>}<BindExercisesButton exercises={items} onBind={(indexes) => setItems((current) => bindExercises(current, indexes))}/><button type="button" className="add-exercise-button" onClick={() => setPickerOpen(true)}><Icon name="plus"/> 운동 추가</button>
    </section>
    <div className="sticky-save"><div><span>{items.length}개 운동</span><b>목표 {targetSetCount(items)}세트</b></div><button className="primary-button" disabled={saving}>{saving ? '저장 중...' : '루틴 저장'}</button></div>
  </form>
    {pickerOpen && <ExercisePicker exercises={exercises} excluded={items.map((item) => item.exerciseId)} onSelect={add} onClose={() => setPickerOpen(false)}/>}
    {removing != null && <ConfirmModal title="운동 삭제" message="이 운동과 입력한 세트를 삭제할까요?" onConfirm={() => { setItems((current) => current.filter((_, index) => index !== removing)); setRemoving(null) }} onClose={() => setRemoving(null)}/>}
    {deleteOpen && <DeleteRoutineModal routine={{ id, name }} onClose={() => setDeleteOpen(false)} onDeleted={() => navigate('/routines')}/>}
  </>
}
