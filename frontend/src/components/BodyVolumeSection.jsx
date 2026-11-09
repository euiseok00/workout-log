import { useEffect, useState } from 'react'
import { api, query } from '../lib/api'
import { addDays, bodyPartLabel, changeClass, formatNumber, formatRange, mondayOf, todayString, volumeDifference } from '../lib/utils'
import { ErrorMessage, Icon } from './ui'

const MAX_DAYS = 365

function readDays(key) {
  const stored = Number(localStorage.getItem(key))
  return Number.isInteger(stored) && stored >= 1 && stored <= MAX_DAYS ? stored : null
}

export default function BodyVolumeSection({ auth }) {
  const storageKey = `volume_period_days:${auth.user.id}`
  const today = todayString()
  const [customDays, setCustomDays] = useState(() => readDays(storageKey))
  const [mode, setMode] = useState(customDays ? 'custom' : 'week')
  const [daysInput, setDaysInput] = useState(String(customDays || 5))
  const [from, setFrom] = useState(() => customDays ? addDays(today, 1 - customDays) : mondayOf(today))
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const days = mode === 'week' ? 7 : customDays || 5

  useEffect(() => {
    let active = true
    api(`/api/statistics/volume-periods${query({ from, days })}`)
      .then((value) => { if (active) { setData(value); setError(null) } })
      .catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [from, days])

  const showWeek = () => {
    setMode('week'); setFrom(mondayOf(today))
    localStorage.removeItem(storageKey)
  }
  const showCustom = () => {
    const next = customDays || 5
    setMode('custom'); setCustomDays(next); setDaysInput(String(next)); setFrom(addDays(today, 1 - next))
    localStorage.setItem(storageKey, String(next))
  }
  const changeDays = (value) => {
    setDaysInput(value)
    const next = Number(value)
    if (!Number.isInteger(next) || next < 1 || next > MAX_DAYS) return
    setCustomDays(next)
    localStorage.setItem(storageKey, String(next))
  }

  const nextIsFuture = data ? data.next.from > today : true
  return <section className="card body-volume-section" aria-label="부위별 기간 볼륨">
    <header className="section-header"><div><h2>{mode === 'week' ? '부위별 주간 볼륨' : `부위별 ${days}일 볼륨`}</h2></div><div className="chip-row"><button className={`chip ${mode === 'week' ? 'active' : ''}`} onClick={showWeek}>주별</button><button className={`chip ${mode === 'custom' ? 'active' : ''}`} onClick={showCustom}>일수 지정</button></div></header>
    <div className="volume-controls">
      <button className="icon-button" onClick={() => setFrom(addDays(from, -days))} aria-label={`이전 ${days}일`}><Icon name="back"/></button>
      {mode === 'custom'
        ? <><label>시작일<input type="date" value={from} max={today} onChange={(event) => event.target.value && setFrom(event.target.value)}/></label><label>기간<input type="number" min="1" max={MAX_DAYS} step="1" value={daysInput} onChange={(event) => changeDays(event.target.value)} onBlur={() => setDaysInput(String(days))}/>일</label></>
        : <span className="count-badge">월요일부터 일요일까지</span>}
      <button className="icon-button" onClick={() => setFrom(addDays(from, days))} disabled={nextIsFuture} aria-label={`다음 ${days}일`}><Icon name="chevron"/></button>
    </div>
    <ErrorMessage error={error}/>
    {data && <div className="body-volume-table-wrap"><table className="body-volume-table">
      <thead><tr><th scope="col">부위<small>단위 kg</small></th><th scope="col">이전<small>{formatRange(data.previous.from, data.previous.to)}</small></th><th scope="col">선택<small>{formatRange(data.selected.from, data.selected.to)}</small></th><th scope="col">다음<small>{formatRange(data.next.from, data.next.to)}</small></th><th scope="col">이전 대비</th></tr></thead>
      <tbody>
        {[...data.selected.bodyParts.map((part, index) => ({ key: part.bodyPart, label: bodyPartLabel(part.bodyPart), selected: part.volume, previous: data.previous.bodyParts[index].volume, next: data.next.bodyParts[index].volume })),
          { key: 'TOTAL', label: '합계', selected: data.selected.totalVolume, previous: data.previous.totalVolume, next: data.next.totalVolume }].map((row) => {
          return <tr key={row.key}>
            <th scope="row">{row.label}</th>
            <td>{formatNumber(row.previous, 2)}</td>
            <td>{formatNumber(row.selected, 2)}</td>
            <td>{nextIsFuture ? '—' : formatNumber(row.next, 2)}</td>
            <td className={changeClass(row.selected, row.previous)}>{volumeDifference(row.selected, row.previous).replace(' kg', '')}</td>
          </tr>
        })}
      </tbody>
    </table></div>}
  </section>
}
