import { themes } from '../themes'
import { useTheme } from '../themes/context/themeContext'
import { Icon } from './ui'
import { isoDate, todayString } from '../lib/utils'

export default function WorkoutCalendar({ cursor, selected, byDate, onMonthChange, onSelect }) {
  const { currentTheme } = useTheme()
  const calendarFace = themes[currentTheme].calendarFace
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const lastDate = new Date(year, month + 1, 0).getDate()
  const days = []
  const firstDay = new Date(year, month, 1).getDay()
  for (let index = 0; index < firstDay; index += 1) days.push(null)
  for (let day = 1; day <= lastDate; day += 1) days.push(day)
  return <section className="card calendar-card">
    <header className="section-header"><div><h2>{year}년 {month + 1}월</h2></div><div className="month-buttons"><button className="icon-button" onClick={() => onMonthChange(new Date(year, month - 1, 1))} aria-label="이전 달"><Icon name="back"/></button><button className="icon-button" onClick={() => onMonthChange(new Date(year, month + 1, 1))} aria-label="다음 달"><Icon name="chevron"/></button></div></header>
    <div className="calendar-weekdays">{['일','월','화','수','목','금','토'].map((day) => <span key={day}>{day}</span>)}</div>
    <div className="calendar-grid">{days.map((day, index) => {
      if (!day) return <span key={`empty-${index}`} />
      const date = isoDate(year, month, day)
      const count = byDate[date]?.length || 0
      return <button key={date} className={`${selected === date ? 'selected' : ''} ${date === todayString() ? 'today' : ''}`} onClick={() => onSelect(date)}><span className="calendar-day">{day}</span><span className="calendar-marker">{count > 0 && (calendarFace ? <><img className="calendar-face" src={calendarFace} alt=""/>{count > 1 && <i className="calendar-face-count">{count}</i>}</> : <i>{count > 1 ? count : ''}</i>)}</span></button>
    })}</div>
  </section>
}
