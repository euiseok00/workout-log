import { useEffect, useState } from 'react'
import { api } from '../../lib/api'
import { targetSetCount } from '../../lib/setGroups'
import { EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from '../../components/ui'
import DeleteRoutineModal from './DeleteRoutineModal'

export default function RoutineList({ navigate }) {
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
      <Link to={`/routines/${routine.id}`} navigate={navigate} className="routine-card"><div className={`routine-number color-${index % 4}`}>{String(index + 1).padStart(2, '0')}</div><div className="routine-card-main"><p className="eyebrow">{routine.exercises.length} EXERCISES</p><h2>{routine.name}</h2><div className="routine-tags">{routine.exercises.slice(0, 4).map((exercise, exerciseIndex) => <span key={exerciseIndex}>{exercise.name}</span>)}</div></div><div className="routine-card-meta"><b>{targetSetCount(routine.exercises)}</b><small>목표 세트</small><Icon name="chevron"/></div></Link>
      <div className="routine-item-actions"><Link to={`/routines/${routine.id}/history`} navigate={navigate} className="item-action">기록 변화</Link><button type="button" className="item-action danger-action" onClick={() => setDeleting(routine)} aria-label={`${routine.name} 삭제`}>삭제</button></div>
    </div>)}</div> : <EmptyState icon="routine" title="아직 만든 루틴이 없어요" description="자주 하는 운동을 묶어 빠르게 시작해보세요." action={<Link to="/routines/new" navigate={navigate} className="primary-button compact">첫 루틴 만들기</Link>}/>}
    {deleting && <DeleteRoutineModal routine={deleting} onClose={() => setDeleting(null)} onDeleted={() => { setRoutines((current) => current.filter((routine) => routine.id !== deleting.id)); setDeleting(null) }}/>}
  </div>
}
