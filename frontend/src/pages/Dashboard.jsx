import { useEffect, useMemo, useRef, useState } from 'react'
import { api, query } from '../lib/api'
import { useCharacterEvent } from '../themes/context/characterEventContext'
import { CHARACTER_EVENTS } from '../themes/context/characterEvents'
import BodyVolumeSection from '../components/BodyVolumeSection'
import WorkoutCalendar from '../components/WorkoutCalendar'
import { EmptyState, ErrorMessage, Icon, Link, Loading, PageHeader } from '../components/ui'
import { countSets } from '../lib/setGroups'
import { formatDate, formatNumber, isoDate, todayString, volumeChange } from '../lib/utils'

export default function Dashboard({ navigate, auth }) {
  const { triggerCharacter } = useCharacterEvent()
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
  const selectedSessions = byDate[selected] || []
  const addRecord = selected <= todayString() && <Link to="/workout" navigate={() => navigate('/workout', false, { workoutDate: selected })} className="secondary-button">{selected === todayString() ? '운동 시작' : '이 날짜에 기록 추가'}</Link>
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
          <WorkoutCalendar cursor={cursor} selected={selected} byDate={byDate} onMonthChange={(next) => { setCursor(next); setSelected(isoDate(next.getFullYear(), next.getMonth(), 1)) }} onSelect={setSelected}/>
          <section className="day-sessions">
            <header className="section-header"><div><p className="eyebrow">DAILY LOG</p><h2>{formatDate(selected)}</h2></div><span className="count-badge">{selectedSessions.length}개</span></header>
            <div className="session-list">{selectedSessions.length ? selectedSessions.map((session) => {
              const completed = countSets(session.exercises, true)
              return <Link to={`/workouts/${session.id}`} navigate={openRecord} className="session-card" key={session.id}><span className="session-icon"><Icon name="workout"/></span><div><h3>{session.routineName || '자유 운동'}</h3><p>운동 {session.exercises.length}개 · 완료 {completed}세트</p></div><Icon name="chevron"/></Link>
            }) : <EmptyState icon="calendar" title="이날 기록된 운동이 없어요" description={selected > todayString() ? '미래 날짜에는 기록할 수 없어요.' : selected === todayString() ? '가볍게 오늘의 운동을 시작해보세요.' : '지난 운동을 이 날짜에 기록할 수 있어요.'} action={addRecord}/>}</div>
            {selectedSessions.length > 0 && addRecord && <div className="day-add-record">{addRecord}</div>}
          </section>
        </div>
        <BodyVolumeSection auth={auth}/>
      </>}
    </div>
  )
}
