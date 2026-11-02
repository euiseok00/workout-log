import { useState } from 'react'
import { groupLabel, groupTypeForSize, setTypeLabel, setTypeOptions } from '../lib/setGroups'
import { Icon, Modal } from './ui'

export function SetTypeSelect({ exercises, exercise, set, number, onChange }) {
  const type = set.setType || 'WORKING'
  return <label className={`set-type-cell ${type === 'WORKING' ? '' : 'special'}`}>
    <b>{number}</b>
    <small>{setTypeLabel(type)}</small>
    <select value={type} onChange={(event) => onChange(event.target.value)} aria-label={`${exercise.name} ${number}세트 유형`}>
      {setTypeOptions(exercises, exercise).map(([key, label]) => <option value={key} key={key}>{label}</option>)}
    </select>
  </label>
}

export function GroupBadge({ exercises, exercise, onUnbind }) {
  const label = groupLabel(exercises, exercise)
  if (!label) return null
  return <div className="exercise-group-badge"><Icon name="routine" size={15}/><b>{label}</b><button type="button" onClick={() => onUnbind(exercise.groupNumber)}>묶음 해제</button></div>
}

export function BindExercisesButton({ exercises, onBind }) {
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState([])
  const candidates = exercises.flatMap((exercise, index) => exercise.groupNumber == null ? [{ exercise, index }] : [])
  if (candidates.length < 2) return null
  const groupType = groupTypeForSize(selected.length)
  const toggle = (index) => setSelected((current) => current.includes(index) ? current.filter((item) => item !== index) : [...current, index])
  const close = () => { setOpen(false); setSelected([]) }
  return <>
    <button type="button" className="add-exercise-button" onClick={() => setOpen(true)}><Icon name="routine"/> 운동 묶기</button>
    {open && <Modal title="운동 묶기" onClose={close}>
      <div className="modal-form">
        <p className="group-hint">2개는 슈퍼, 3개는 트라이, 4개 이상은 자이언트 세트로 묶입니다.</p>
        <div className="group-options">{candidates.map(({ exercise, index }) => <label key={index}><input type="checkbox" checked={selected.includes(index)} onChange={() => toggle(index)}/><span>{exercise.name}</span></label>)}</div>
        <button type="button" className="primary-button full" disabled={!groupType} onClick={() => { onBind(selected); close() }}>{groupType ? `${setTypeLabel(groupType)} 세트로 묶기` : '운동을 2개 이상 선택하세요'}</button>
      </div>
    </Modal>}
  </>
}
