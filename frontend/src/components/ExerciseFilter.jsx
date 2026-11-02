import { BODY_PARTS } from '../lib/utils'
import { Icon } from './ui'

export default function ExerciseFilter({ search, filter, autoFocus = false, onSearch, onFilter }) {
  return <>
    <div className="search-field"><Icon name="search" size={19}/><input value={search} onChange={(event) => onSearch(event.target.value)} placeholder="운동 검색..." autoFocus={autoFocus}/></div>
    <div className="chip-row">{BODY_PARTS.map(([key, label]) => <button type="button" key={key} className={`chip ${filter === key ? 'active' : ''}`} onClick={() => onFilter(key)}>{label}</button>)}</div>
  </>
}
