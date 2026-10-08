import { useMemo, useState } from 'react'
import { Icon, Modal } from './ui'
import { BODY_PARTS, bodyPartLabel } from './utils'

export default function ExercisePicker({ exercises, excluded = [], onSelect, onClose }) {
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const filtered = useMemo(() => exercises.filter((exercise) =>
    !excluded.includes(exercise.id)
    && (filter === 'ALL' || exercise.bodyPart === filter)
    && exercise.name.toLowerCase().includes(search.toLowerCase())), [exercises, excluded, filter, search])

  return <Modal title="운동 종목 추가" onClose={onClose} wide>
    <div className="search-field"><Icon name="search" size={19}/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="운동 검색..." autoFocus/></div>
    <div className="chip-row">{BODY_PARTS.map(([key, label]) => <button key={key} className={`chip ${filter === key ? 'active' : ''}`} onClick={() => setFilter(key)}>{label}</button>)}</div>
    <div className="picker-list">{filtered.map((exercise) => <button className="picker-item" key={exercise.id} onClick={() => onSelect(exercise)}><span className="exercise-symbol">{exercise.name.slice(0, 1)}</span><span><b>{exercise.name}</b><small>{bodyPartLabel(exercise.bodyPart)} · {exercise.custom ? '내 운동' : '기본 운동'}</small></span><Icon name="plus" size={20}/></button>)}{!filtered.length && <p className="list-empty">조건에 맞는 운동이 없습니다.</p>}</div>
  </Modal>
}
