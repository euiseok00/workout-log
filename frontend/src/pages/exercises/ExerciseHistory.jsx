import { useEffect, useState } from 'react'
import { api } from '../../lib/api'
import { EmptyState, ErrorMessage, Loading, PageHeader, StatCards } from '../../components/ui'
import { bodyPartLabel, formatDate, formatNumber } from '../../lib/utils'

export default function ExerciseHistory({ id, navigate }) {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let active = true
    api(`/api/statistics/exercises/${id}`).then((value) => active && setStats(value)).catch((requestError) => active && setError(requestError))
    return () => { active = false }
  }, [id])
  if (!stats) return <div className="page"><PageHeader back="/exercises" navigate={navigate} title="운동 기록"/><ErrorMessage error={error}/>{!error && <Loading/>}</div>
  const cards = [['최대 중량', stats.maxWeight, 'kg'], ['총 볼륨', stats.totalVolume, 'kg'], ['완료 세트', stats.completedSetCount, '세트'], ['운동 횟수', stats.sessionCount, '회']]
  return <div className="page"><PageHeader back="/exercises" navigate={navigate} meta={bodyPartLabel(stats.bodyPart)} title={stats.name}/><StatCards cards={cards}/><section className="card history-section"><header className="section-header"><div><h2>날짜별 기록</h2></div><span className="count-badge">{stats.history.length}일</span></header>{stats.history.length ? <table className="record-table exercise-record-table"><thead><tr><th scope="col">날짜</th><th scope="col">최대(kg)</th><th scope="col">세트</th><th scope="col">볼륨(kg)</th></tr></thead><tbody>{stats.history.map((item) => <tr key={item.workoutDate}><th scope="row"><time>{formatDate(item.workoutDate)}</time></th><td>{formatNumber(item.maxWeight, 2)}</td><td>{item.completedSetCount}</td><td>{formatNumber(item.totalVolume, 2)}</td></tr>)}</tbody></table> : <EmptyState icon="history" title="아직 완료한 운동 기록이 없어요"/>}</section></div>
}
