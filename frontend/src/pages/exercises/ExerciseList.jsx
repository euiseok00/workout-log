import { useEffect, useMemo, useState } from 'react'
import { api } from '../../lib/api'
import { EmptyState, ErrorMessage, Icon, Link, Loading, Modal, PageHeader } from '../../components/ui'
import ExerciseFilter from '../../components/ExerciseFilter'
import { BODY_PARTS, bodyPartLabel, filterExercises } from '../../lib/utils'

function ExerciseEditor({ exercise, onSaved, onClose }) {
  const [name, setName] = useState(exercise?.name || '')
  const [bodyPart, setBodyPart] = useState(exercise?.bodyPart || 'CHEST')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const save = async (event) => {
    event.preventDefault(); setSaving(true); setError(null)
    try { onSaved(await api(exercise ? `/api/exercises/${exercise.id}` : '/api/exercises', { method: exercise ? 'PUT' : 'POST', body: { name, bodyPart } })) } catch (requestError) { setError(requestError) } finally { setSaving(false) }
  }
  return <Modal title={exercise ? '내 운동 수정' : '내 운동 추가'} onClose={onClose}><form className="modal-form" onSubmit={save}><ErrorMessage error={error}/><label className="field-label">운동 이름</label><input className="large-input" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 원암 케이블 로우" maxLength="100" required/><label className="field-label">운동 부위</label><div className="body-part-grid">{BODY_PARTS.slice(1).map(([key, label]) => <button type="button" className={`chip ${bodyPart === key ? 'active' : ''}`} onClick={() => setBodyPart(key)} key={key}>{label}</button>)}</div><button className="primary-button full" disabled={saving}>{saving ? '저장 중…' : '운동 저장'}</button></form></Modal>
}

export default function ExerciseList({ navigate }) {
  const [exercises, setExercises] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [editor, setEditor] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api('/api/exercises').then((value) => active && setExercises(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [])
  const filtered = useMemo(() => filterExercises(exercises || [], filter, search), [exercises, filter, search])
  return <div className="page"><PageHeader title="운동 종목" action={<button className="primary-button compact" onClick={() => setEditor({})}><Icon name="plus" size={18}/> 운동 추가</button>}/><p className="page-lead">기본 운동과 직접 추가한 운동의 기록을 확인하세요.</p><ErrorMessage error={error}/><div className="exercise-toolbar"><ExerciseFilter search={search} filter={filter} onSearch={setSearch} onFilter={setFilter}/></div>{!exercises ? <Loading/> : filtered.length ? <div className="exercise-list-grid">{filtered.map((exercise) => <div className="exercise-list-item" key={exercise.id}><Link to={`/exercises/${exercise.id}/history`} navigate={navigate} className="exercise-list-card"><span className="exercise-symbol">{exercise.name.slice(0, 1)}</span><div><h3>{exercise.name}</h3><p className="meta-row"><span>{bodyPartLabel(exercise.bodyPart)}</span><span>{exercise.custom ? '내 운동' : '기본 운동'}</span></p></div><Icon name="chevron"/></Link>{exercise.custom && <button className="item-action" onClick={() => setEditor(exercise)} aria-label={`${exercise.name} 수정`}>수정</button>}</div>)}</div> : <EmptyState icon="search" title="조건에 맞는 운동이 없어요" description="검색어나 부위 필터를 변경해 보세요."/>}{editor && <ExerciseEditor exercise={editor.id ? editor : null} onClose={() => setEditor(null)} onSaved={(saved) => { setExercises((current) => editor.id ? current.map((item) => item.id === saved.id ? saved : item) : [...current, saved]); setEditor(null) }}/>}</div>
}
