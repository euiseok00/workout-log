import { useEffect, useMemo, useRef, useState } from 'react'
import { api, query } from './api'
import { useCharacterEvent } from './themes/characterEventContext'
import { CHARACTER_EVENTS } from './themes/characterEvents'
import { themes } from './themes'
import { useTheme } from './themes/themeContext'
import { EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from './ui'
import { bodyPartLabel, formatDate, formatNumber, todayString, volumeChange, volumeDifference } from './utils'

function isoDate(year, month, day) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export default function Dashboard({ navigate, auth }) {
  const { triggerCharacter } = useCharacterEvent()
  const { currentTheme } = useTheme()
  const calendarFace = themes[currentTheme].calendarFace
  const entered = useRef(false)
  const initialDate = window.history.state?.selectedDate || todayString()
  const initialDay = new Date(`${initialDate}T00:00:00`)
  const [cursor, setCursor] = useState(new Date(initialDay.getFullYear(), initialDay.getMonth(), 1))
  const [selected, setSelected] = useState(initialDate)
  const [sessions, setSessions] = useState([])
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const lastDate = new Date(year, month + 1, 0).getDate()
  const from = isoDate(year, month, 1)
  const to = isoDate(year, month, lastDate)

  useEffect(() => {
    if (entered.current) return
    entered.current = true
    triggerCharacter(CHARACTER_EVENTS.DASHBOARD_ENTER)
  }, [triggerCharacter])

  const openRecord = (to) => {
    triggerCharacter(CHARACTER_EVENTS.WORKOUT_RECORD_OPEN)
    navigate(to)
  }

  useEffect(() => {
    let active = true
    Promise.all([
      api(`/api/workout-sessions${query({ from, to })}`),
      api(`/api/statistics/dashboard${query({ today: todayString() })}`),
    ]).then(([sessionData, dashboardData]) => {
      if (active) { setSessions(sessionData); setStats(dashboardData); setError(null) }
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
  return (
    <div className="page dashboard-page">
      <PageHeader eyebrow={`${auth.user.loginId}님의 기록`} title="오늘도 나답게, 한 세트씩" action={<span className="today-chip"><Icon name="calendar" size={17}/>{formatDate(todayString(), false)}</span>}/>
      <ErrorMessage error={error}/>
      {loading ? <Loading/> : <>
        <section className="stats-grid" aria-label="운동 통계">
          <article className="stat-card stat-1"><span>이번 달 운동</span><strong>{formatNumber(stats?.monthlyWorkoutDays)}<small>일</small></strong><i/></article>
          <article className="stat-card stat-2"><span>이번 주 완료 세트</span><strong>{formatNumber(stats?.currentWeekCompletedSetCount)}<small>세트</small></strong><p className="stat-note">지난주 {formatNumber(stats?.previousWeekCompletedSetCount)}세트</p><i/></article>
          <article className="stat-card stat-3"><span>이번 주 볼륨</span><strong>{formatNumber(stats?.currentWeekVolume, 2)}<small>kg</small></strong><p className="stat-note">지난주 {formatNumber(stats?.previousWeekVolume, 2)}kg · {volumeChange(stats?.currentWeekVolume, stats?.previousWeekVolume)}</p><i/></article>
          {stats?.recentSession ? <Link to={`/workouts/${stats.recentSession.id}`} navigate={openRecord} className="stat-card stat-4 stat-recent"><span>최근 운동</span><strong>{formatDate(stats.recentSession.workoutDate, false)}</strong><p className="stat-note">운동 {stats.recentSession.exerciseCount}개 · 기록 보기</p><i/></Link> : <article className="stat-card stat-4"><span>최근 운동</span><strong>기록 없음</strong><i/></article>}
        </section>
        <div className="dashboard-grid">
          <section className="card calendar-card">
            <header className="section-header"><div><p className="eyebrow">WORKOUT CALENDAR</p><h2>{year}년 {month + 1}월</h2></div><div className="month-buttons"><button className="icon-button" onClick={() => { const next = new Date(year, month - 1, 1); setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} aria-label="이전 달"><Icon name="back"/></button><button className="icon-button" onClick={() => { const next = new Date(year, month + 1, 1); setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} aria-label="다음 달"><Icon name="chevron"/></button></div></header>
            <div className="calendar-weekdays">{['일','월','화','수','목','금','토'].map((day) => <span key={day}>{day}</span>)}</div>
            <div className="calendar-grid">{days.map((day, index) => {
              if (!day) return <span key={`empty-${index}`} />
              const date = isoDate(year, month, day)
              const count = byDate[date]?.length || 0
              return <button key={date} className={`${selected === date ? 'selected' : ''} ${date === todayString() ? 'today' : ''}`} onClick={() => setSelected(date)}><span className="calendar-day">{day}</span><span className="calendar-marker">{count > 0 && (calendarFace ? <><img className="calendar-face" src={calendarFace} alt=""/>{count > 1 && <i className="calendar-face-count">{count}</i>}</> : <i>{count > 1 ? count : ''}</i>)}</span></button>
            })}</div>
          </section>
          <section className="day-sessions">
            <header className="section-header"><div><p className="eyebrow">DAILY LOG</p><h2>{formatDate(selected)}</h2></div><span className="count-badge">{selectedSessions.length}개</span></header>
            <div className="session-list">{selectedSessions.length ? selectedSessions.map((session) => {
              const completed = session.exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed).length
              return <Link to={`/workouts/${session.id}`} navigate={openRecord} className="session-card" key={session.id}><span className="session-icon"><Icon name="workout"/></span><div><h3>{session.routineName || '자유 운동'}</h3><p>운동 {session.exercises.length}개 · 완료 {completed}세트</p></div><Icon name="chevron"/></Link>
            }) : <EmptyState icon="calendar" title="이날 기록된 운동이 없어요" description="가볍게 오늘의 운동을 시작해보세요." action={<Link to="/workout" navigate={navigate} className="secondary-button">운동 시작</Link>}/>}</div>
          </section>
        </div>
        <section className="card body-volume-section" aria-label="부위별 주간 볼륨">
          <header className="section-header"><div><p className="eyebrow">WEEKLY VOLUME</p><h2>부위별 주간 볼륨</h2></div><span className="count-badge">월요일–일요일</span></header>
          <div className="body-volume-table-wrap"><table className="body-volume-table">
            <thead><tr><th scope="col">부위</th><th scope="col">이번 주</th><th scope="col">지난주</th><th scope="col">증감 중량</th></tr></thead>
            <tbody>{stats?.bodyPartVolumes.map((part) => {
              const difference = Number(part.currentWeekVolume) - Number(part.previousWeekVolume)
              return <tr key={part.bodyPart}>
                <th scope="row">{bodyPartLabel(part.bodyPart)}</th>
                <td>{formatNumber(part.currentWeekVolume, 2)} kg</td>
                <td>{formatNumber(part.previousWeekVolume, 2)} kg</td>
                <td className={difference > 0 ? 'volume-increase' : difference < 0 ? 'volume-decrease' : ''}>{volumeDifference(part.currentWeekVolume, part.previousWeekVolume)}</td>
              </tr>
            })}</tbody>
          </table></div>
        </section>
      </>}
    </div>
  )
}
