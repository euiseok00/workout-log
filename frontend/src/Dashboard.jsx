import { useEffect, useMemo, useState } from 'react'
import { api, query } from './api'
import { EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from './ui'
import { formatDate, formatNumber, todayString } from './utils'

function isoDate(year, month, day) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export default function Dashboard({ navigate, auth }) {
  const today = new Date(`${todayString()}T00:00:00`)
  const [cursor, setCursor] = useState(new Date(today.getFullYear(), today.getMonth(), 1))
  const [selected, setSelected] = useState(todayString())
  const [sessions, setSessions] = useState([])
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const lastDate = new Date(year, month + 1, 0).getDate()
  const from = isoDate(year, month, 1)
  const to = isoDate(year, month, lastDate)

  useEffect(() => {
    let active = true
    Promise.all([
      api(`/api/workout-sessions${query({ from, to })}`),
      api(`/api/statistics/summary${query({ from, to })}`),
    ]).then(([sessionData, summaryData]) => {
      if (active) { setSessions(sessionData); setSummary(summaryData); setError(null) }
    }).catch((requestError) => active && setError(requestError)).finally(() => active && setLoading(false))
    return () => { active = false }
  }, [from, to])

  const byDate = useMemo(() => sessions.reduce((map, session) => {
    map[session.workoutDate] = [...(map[session.workoutDate] || []), session]
    return map
  }, {}), [sessions])
  const days = []
  const firstDay = new Date(year, month, 1).getDay()
  for (let index = 0; index < firstDay; index += 1) days.push(null)
  for (let day = 1; day <= lastDate; day += 1) days.push(day)
  const selectedSessions = byDate[selected] || []
  const stats = summary ? [
    ['이번 달 운동', summary.workoutCount, '회'],
    ['완료 세트', summary.completedSetCount, '세트'],
    ['총 반복', summary.totalReps, '회'],
    ['총 볼륨', summary.totalVolume, 'kg'],
  ] : []

  return (
    <div className="page dashboard-page">
      <PageHeader eyebrow={`${auth.user.loginId}님의 기록`} title="오늘도 나답게, 한 세트씩" action={<span className="today-chip"><Icon name="calendar" size={17}/>{formatDate(todayString(), false)}</span>}/>
      <ErrorMessage error={error}/>
      {loading ? <Loading/> : <>
        <section className="stats-grid" aria-label="이번 달 통계">
          {stats.map(([label, value, unit], index) => <article className={`stat-card stat-${index + 1}`} key={label}><span>{label}</span><strong>{formatNumber(value, 2)}<small>{unit}</small></strong><i/></article>)}
        </section>
        <div className="dashboard-grid">
          <section className="card calendar-card">
            <header className="section-header"><div><p className="eyebrow">WORKOUT CALENDAR</p><h2>{year}년 {month + 1}월</h2></div><div className="month-buttons"><button className="icon-button" onClick={() => { const next = new Date(year, month - 1, 1); setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} aria-label="이전 달"><Icon name="back"/></button><button className="icon-button" onClick={() => { const next = new Date(year, month + 1, 1); setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} aria-label="다음 달"><Icon name="chevron"/></button></div></header>
            <div className="calendar-weekdays">{['일','월','화','수','목','금','토'].map((day) => <span key={day}>{day}</span>)}</div>
            <div className="calendar-grid">{days.map((day, index) => {
              if (!day) return <span key={`empty-${index}`} />
              const date = isoDate(year, month, day)
              const count = byDate[date]?.length || 0
              return <button key={date} className={`${selected === date ? 'selected' : ''} ${date === todayString() ? 'today' : ''}`} onClick={() => setSelected(date)}><span>{day}</span>{count > 0 && <i>{count > 1 ? count : ''}</i>}</button>
            })}</div>
          </section>
          <section className="day-sessions">
            <header className="section-header"><div><p className="eyebrow">DAILY LOG</p><h2>{formatDate(selected)}</h2></div><span className="count-badge">{selectedSessions.length}개</span></header>
            <div className="session-list">{selectedSessions.length ? selectedSessions.map((session) => {
              const completed = session.exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed).length
              return <Link to={`/workouts/${session.id}`} navigate={navigate} className="session-card" key={session.id}><span className="session-icon"><Icon name="workout"/></span><div><h3>{session.routineName || '자유 운동'}</h3><p>운동 {session.exercises.length}개 · 완료 {completed}세트</p></div><Icon name="chevron"/></Link>
            }) : <EmptyState icon="calendar" title="이날 기록된 운동이 없어요" description="가볍게 오늘의 운동을 시작해보세요." action={<Link to="/workout" navigate={navigate} className="secondary-button">운동 시작</Link>}/>}</div>
          </section>
        </div>
      </>}
    </div>
  )
}
