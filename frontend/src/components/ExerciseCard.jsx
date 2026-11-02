import { GroupBadge, SetTypeSelect } from './ExerciseGroups'
import { Icon } from './ui'
import { REPS_INPUT, setLabel, WEIGHT_INPUT } from '../lib/setGroups'
import { bodyPartLabel } from '../lib/utils'

export function ExerciseCard({ exercises, index, dragIndex, onDragIndex, onMove, onRemove, onUnbind, action, children }) {
  const exercise = exercises[index]
  return <article className="exercise-card" onDragOver={(event) => event.preventDefault()} onDrop={() => { onMove(dragIndex, index); onDragIndex(null) }}>
    <GroupBadge exercises={exercises} exercise={exercise} onUnbind={onUnbind}/>
    <header>
      <span className="drag-handle" title="드래그하여 순서 변경" draggable onDragStart={() => onDragIndex(index)}><Icon name="drag"/></span>
      <span className="exercise-symbol">{exercise.name.slice(0, 1)}</span>
      <div className="exercise-card-name"><h2>{exercise.name}</h2><p>{bodyPartLabel(exercise.bodyPart)}</p></div>
      <div className="exercise-card-tools">{action}<div className="card-actions"><button type="button" onClick={() => onMove(index, index - 1)} disabled={index === 0} aria-label="위로 이동">↑</button><button type="button" onClick={() => onMove(index, index + 1)} disabled={index === exercises.length - 1} aria-label="아래로 이동">↓</button><button type="button" onClick={() => onRemove(index)} aria-label="운동 삭제"><Icon name="close" size={17}/></button></div></div>
    </header>
    {children}
  </article>
}

// type="number"는 커서 위치를 지정할 수 없어 text 입력에 형식 검사를 직접 둔다.
function SetNumberInput({ label, unit, value, pattern, inputMode, onChange }) {
  const focus = (event) => {
    if (Number(value) === 0) { onChange(''); return }
    const input = event.target
    requestAnimationFrame(() => input.setSelectionRange(input.value.length, input.value.length))
  }
  const blur = () => { if (value === '' || String(value).endsWith('.')) onChange(Number(value)) }
  return <label><input type="text" inputMode={inputMode} autoComplete="off" value={value} aria-label={label} onFocus={focus} onBlur={blur} onChange={(event) => { if (pattern.test(event.target.value)) onChange(event.target.value) }}/><span>{unit}</span></label>
}

export function SetTable({ exercises, exercise, trackCompleted = false, volume, onChange, onAdd, onRemove }) {
  return <>
    <div className={`set-table ${trackCompleted ? '' : 'no-check'}`}>
      <div className="set-head"><span>세트</span><span>중량</span><span>횟수</span>{trackCompleted && <span>완료</span>}<span/></div>
      {exercise.sets.map((set, setIndex) => <div className={`set-row ${set.completed ? 'completed' : ''}`} key={setIndex}>
        <SetTypeSelect exercises={exercises} exercise={exercise} set={set} number={setIndex + 1} onChange={(value) => onChange(setIndex, 'setType', value)}/>
        <SetNumberInput label={`${exercise.name} ${setIndex + 1}세트 중량`} unit="kg" value={set.weight} pattern={WEIGHT_INPUT} inputMode="decimal" onChange={(value) => onChange(setIndex, 'weight', value)}/>
        <SetNumberInput label={`${exercise.name} ${setIndex + 1}세트 횟수`} unit="회" value={set.reps} pattern={REPS_INPUT} inputMode="numeric" onChange={(value) => onChange(setIndex, 'reps', value)}/>
        {trackCompleted && <button type="button" className={`check-button ${set.completed ? 'checked' : ''}`} onClick={() => onChange(setIndex, 'completed', !set.completed)} aria-label={`${setIndex + 1}세트 완료`}><Icon name="check" size={17}/></button>}
        <button type="button" className="remove-set" onClick={() => onRemove(setIndex)} aria-label={`${exercise.name} ${setIndex + 1}세트 삭제`}><Icon name="close" size={15}/></button>
      </div>)}
    </div>
    {volume}
    <button type="button" className="add-row-button" onClick={onAdd}><Icon name="plus" size={17}/> 세트 추가</button>
  </>
}

export function SetRecordList({ sets }) {
  return <table className="record-table set-record-list">
    <thead><tr><th scope="col">세트</th><th scope="col">중량</th><th scope="col">횟수</th><th scope="col">완료</th></tr></thead>
    <tbody>{sets.map((set) => <tr className={set.completed ? 'completed' : ''} key={set.setNumber}>
      <th scope="row">{setLabel(set)}</th>
      <td>{Number(set.weight) === 0 ? '맨몸' : `${set.weight}kg`}</td>
      <td>{set.reps}회</td>
      <td>{set.completed ? <i aria-label="완료"><Icon name="check" size={16}/></i> : '미완료'}</td>
    </tr>)}</tbody>
  </table>
}
