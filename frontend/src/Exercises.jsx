import { useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { EmptyState, ErrorMessage, Icon, Link, Loading, Modal, PageHeader } from './ui'
import { BODY_PARTS, bodyPartLabel, formatDate, formatNumber } from './utils'

function NewExercise({ onCreated, onClose }) {
  const [name, setName] = useState('')
  const [bodyPart, setBodyPart] = useState('CHEST')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const save = async (event) => {
    event.preventDefault(); setSaving(true); setError(null)
    try { onCreated(await api('/api/exercises', { method: 'POST', body: { name, bodyPart } })) } catch (requestError) { setError(requestError) } finally { setSaving(false) }
  }
  return <Modal title="내 운동 추가" onClose={onClose}><form className="modal-form" onSubmit={save}><ErrorMessage error={error}/><label className="field-label">운동 이름</label><input className="large-input" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 원암 케이블 로우" maxLength="100" required/><label className="field-label">운동 부위</label><div className="body-part-grid">{BODY_PARTS.slice(1).map(([key, label]) => <button type="button" className={`chip ${bodyPart === key ? 'active' : ''}`} onClick={() => setBodyPart(key)} key={key}>{label}</button>)}</div><button className="primary-button full" disabled={saving}>{saving ? '저장 중...' : '운동 저장'}</button></form></Modal>
}

export function ExerciseList({ navigate }) {
  const [exercises, setExercises] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [newOpen, setNewOpen] = useState(false)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api('/api/exercises').then((value) => active && setExercises(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [])
  const filtered = useMemo(() => (exercises || []).filter((exercise) => (filter === 'ALL' || exercise.bodyPart === filter) && exercise.name.toLowerCase().includes(search.toLowerCase())), [exercises, filter, search])
  return <div className="page"><PageHeader eyebrow="EXERCISE LIBRARY" title="운동 종목" action={<button className="primary-button compact" onClick={() => setNewOpen(true)}><Icon name="plus" size={18}/> 운동 추가</button>}/><p className="page-lead">기본 운동과 직접 추가한 운동의 기록을 확인하세요.</p><ErrorMessage error={error}/><div className="exercise-toolbar"><div className="search-field"><Icon name="search" size={19}/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="운동 검색..."/></div><div className="chip-row">{BODY_PARTS.map(([key, label]) => <button key={key} className={`chip ${filter === key ? 'active' : ''}`} onClick={() => setFilter(key)}>{label}</button>)}</div></div>{!exercises ? <Loading/> : filtered.length ? <div className="exercise-list-grid">{filtered.map((exercise) => <Link to={`/exercises/${exercise.id}/history`} navigate={navigate} className="exercise-list-card" key={exercise.id}><span className={`exercise-symbol body-${exercise.bodyPart.toLowerCase()}`}>{exercise.name.slice(0, 1)}</span><div><h3>{exercise.name}</h3><p>{bodyPartLabel(exercise.bodyPart)} <i/> {exercise.custom ? '내 운동' : '기본 운동'}</p></div><Icon name="chevron"/></Link>)}</div> : <EmptyState icon="search" title="조건에 맞는 운동이 없어요" description="검색어나 부위 필터를 변경해 보세요."/>}{newOpen && <NewExercise onClose={() => setNewOpen(false)} onCreated={(exercise) => { setExercises((current) => [...current, exercise]); setNewOpen(false) }}/>}</div>
}

export function ExerciseHistory({ id, navigate }) {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/statistics/exercises/${id}`).then((value) => active && setStats(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [id])
  if (!stats) return <div className="page"><PageHeader back="/exercises" navigate={navigate} title="운동 기록"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  const cards = [['최대 중량', stats.maxWeight, 'kg'], ['총 볼륨', stats.totalVolume, 'kg'], ['완료 세트', stats.completedSetCount, '세트'], ['운동 횟수', stats.sessionCount, '회']]
  return <div className="page"><PageHeader back="/exercises" navigate={navigate} eyebrow={bodyPartLabel(stats.bodyPart)} title={stats.name}/><section className="stats-grid detail-stats">{cards.map(([label, value, unit], index) => <article className={`stat-card stat-${index + 1}`} key={label}><span>{label}</span><strong>{formatNumber(value, 2)}<small>{unit}</small></strong><i/></article>)}</section><section className="card history-section"><header className="section-header"><div><p className="eyebrow">HISTORY</p><h2>날짜별 기록</h2></div><span className="count-badge">{stats.history.length}일</span></header>{stats.history.length ? <div className="history-list">{stats.history.map((item) => <article key={item.workoutDate}><time>{formatDate(item.workoutDate)}</time><div><span>최대 <b>{formatNumber(item.maxWeight, 2)}kg</b></span><span><b>{item.completedSetCount}</b>세트</span><span><b>{formatNumber(item.totalVolume, 2)}</b>kg</span></div></article>)}</div> : <EmptyState icon="history" title="아직 완료한 운동 기록이 없어요"/>}</section></div>
}
