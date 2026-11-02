import { useEffect, useState } from 'react'
import { api } from '../../lib/api'
import { EmptyState, ErrorMessage, Link, Loading, PageHeader, StatCards } from '../../components/ui'
import { changeClass, formatDate, formatNumber, volumeDifference } from '../../lib/utils'

const WIDTH = 640
const HEIGHT = 200
const PAD = { top: 34, right: 20, bottom: 26, left: 20 }

function VolumeTrend({ sessions }) {
  const [hovered, setHovered] = useState(null)
  const volumes = sessions.map((session) => Number(session.totalVolume))
  const max = Math.max(...volumes)
  const min = Math.min(...volumes)
  const x = (index) => PAD.left + index * (WIDTH - PAD.left - PAD.right) / (sessions.length - 1)
  const y = (value) => max === min ? HEIGHT / 2 : PAD.top + (max - value) * (HEIGHT - PAD.top - PAD.bottom) / (max - min)
  const bandWidth = (WIDTH - PAD.left - PAD.right) / (sessions.length - 1)
  const active = hovered == null ? sessions.length - 1 : hovered
  const anchor = active === 0 ? 'start' : active === sessions.length - 1 ? 'end' : 'middle'
  return <svg className="volume-trend" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`세션별 총 볼륨 추이. 최저 ${formatNumber(min, 2)}kg, 최고 ${formatNumber(max, 2)}kg`} onMouseLeave={() => setHovered(null)}>
    <line className="trend-grid" x1={PAD.left} x2={WIDTH - PAD.right} y1={y(max)} y2={y(max)}/>
    <line className="trend-grid" x1={PAD.left} x2={WIDTH - PAD.right} y1={y(min)} y2={y(min)}/>
    <line className="trend-cross" x1={x(active)} x2={x(active)} y1={PAD.top - 6} y2={HEIGHT - PAD.bottom}/>
    <polyline className="trend-line" points={volumes.map((value, index) => `${x(index)},${y(value)}`).join(' ')}/>
    {volumes.map((value, index) => <circle className="trend-point" cx={x(index)} cy={y(value)} r={index === active ? 6 : 4} key={sessions[index].sessionId}/>)}
    <text className="trend-value" x={x(active)} y={18} textAnchor={anchor}>{formatDate(sessions[active].workoutDate, false)} · {formatNumber(volumes[active], 2)} kg</text>
    <text className="trend-axis" x={PAD.left} y={HEIGHT - 6}>{formatDate(sessions[0].workoutDate, false)}</text>
    <text className="trend-axis" x={WIDTH - PAD.right} y={HEIGHT - 6} textAnchor="end">{formatDate(sessions[sessions.length - 1].workoutDate, false)}</text>
    {sessions.map((session, index) => <rect key={session.sessionId} x={x(index) - bandWidth / 2} y={0} width={bandWidth} height={HEIGHT} fill="transparent" onMouseEnter={() => setHovered(index)} onTouchStart={() => setHovered(index)}/>)}
  </svg>
}

export default function RoutineHistory({ id, navigate }) {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/statistics/routines/${id}`).then((value) => active && setStats(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [id])
  if (!stats) return <div className="page"><PageHeader back="/routines" navigate={navigate} title="루틴 기록"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  const cards = [['운동 횟수', stats.sessionCount, '회'], ['총 볼륨', stats.totalVolume, 'kg'], ['완료 세트', stats.completedSetCount, '세트'], ['최대 중량', stats.maxWeight, 'kg']]
  return <div className="page"><PageHeader back="/routines" navigate={navigate} eyebrow="ROUTINE PROGRESS" title={stats.name}/>
    <StatCards cards={cards}/>
    {stats.sessions.length > 1 && <section className="card history-section"><header className="section-header"><div><p className="eyebrow">TREND</p><h2>세션별 총 볼륨</h2></div></header><VolumeTrend sessions={[...stats.sessions].reverse()}/></section>}
    <section className="card history-section"><header className="section-header"><div><p className="eyebrow">HISTORY</p><h2>회차별 변화</h2></div><span className="count-badge">{stats.sessions.length}회</span></header>
      {stats.sessions.length ? <div className="routine-history-list">{stats.sessions.map((session, index) => {
        const previous = stats.sessions[index + 1]
        return <Link to={`/workouts/${session.sessionId}`} navigate={navigate} className="routine-history-row" key={session.sessionId}>
          <header><time>{formatDate(session.workoutDate)}</time><b>{formatNumber(session.totalVolume, 2)} kg</b>{previous && <span className={changeClass(session.totalVolume, previous.totalVolume)}>{volumeDifference(session.totalVolume, previous.totalVolume)}</span>}</header>
          <table className="record-table routine-record-table">
            <thead><tr><th scope="col">운동</th><th scope="col">세트</th><th scope="col">최대(kg)</th><th scope="col">볼륨(kg)</th><th scope="col">변화</th></tr></thead>
            <tbody>{session.exercises.map((exercise) => {
              const before = previous?.exercises.find((item) => item.exerciseId === exercise.exerciseId)
              return <tr key={exercise.exerciseId}>
                <th scope="row">{exercise.name}</th>
                <td>{exercise.completedSetCount}</td>
                <td>{formatNumber(exercise.maxWeight, 2)}</td>
                <td>{formatNumber(exercise.totalVolume, 2)}</td>
                <td className={before ? changeClass(exercise.totalVolume, before.totalVolume) : ''}>{before ? volumeDifference(exercise.totalVolume, before.totalVolume).replace(' kg', '') : '—'}</td>
              </tr>
            })}</tbody>
          </table>
        </Link>
      })}</div> : <EmptyState icon="history" title="아직 이 루틴으로 운동한 기록이 없어요" description="루틴으로 운동을 시작하면 회차별 변화를 확인할 수 있어요."/>}
    </section>
  </div>
}
