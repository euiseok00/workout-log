import { useMemo, useState } from 'react'
import ExerciseFilter from './ExerciseFilter'
import { Icon, Modal } from './ui'
import { bodyPartLabel, filterExercises } from '../lib/utils'

export default function ExercisePicker({ exercises, excluded = [], onSelect, onClose }) {
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const filtered = useMemo(() => filterExercises(exercises.filter((exercise) => !excluded.includes(exercise.id)), filter, search), [exercises, excluded, filter, search])

  return <Modal title="운동 종목 추가" onClose={onClose} wide>
    <ExerciseFilter search={search} filter={filter} autoFocus onSearch={setSearch} onFilter={setFilter}/>
    <div className="picker-list">{filtered.map((exercise) => <button className="picker-item" key={exercise.id} onClick={() => onSelect(exercise)}><span className="exercise-symbol">{exercise.name.slice(0, 1)}</span><span><b>{exercise.name}</b><small>{bodyPartLabel(exercise.bodyPart)} · {exercise.custom ? '내 운동' : '기본 운동'}</small></span><Icon name="plus" size={20}/></button>)}{!filtered.length && <p className="list-empty">조건에 맞는 운동이 없습니다.</p>}</div>
  </Modal>
}
