export const BODY_PARTS = [
  ['ALL', '전체'],
  ['CHEST', '가슴'],
  ['BACK', '등'],
  ['SHOULDERS', '어깨'],
  ['ARMS', '팔'],
  ['LEGS', '하체'],
]

export function bodyPartLabel(value) {
  return BODY_PARTS.find(([key]) => key === value)?.[1] || value
}

export function formatNumber(value, maximumFractionDigits = 0) {
  return Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits })
}

export function volumeChange(current, previous) {
  if (Number(previous) === 0) return Number(current) > 0 ? '신규' : '—'
  const percent = (Number(current) - Number(previous)) / Number(previous) * 100
  return `${percent > 0 ? '+' : ''}${formatNumber(percent, 1)}%`
}

export function volumeDifference(current, previous) {
  const currentVolume = Number(current || 0)
  const previousVolume = Number(previous || 0)
  if (currentVolume === 0 && previousVolume === 0) return '—'
  const difference = currentVolume - previousVolume
  return `${difference > 0 ? '+' : ''}${formatNumber(difference, 2)} kg`
}

export function isoDate(year, month, day) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export function formatDate(value, withYear = true) {
  if (!value) return ''
  const [year, month, day] = value.split('-').map(Number)
  return withYear ? `${year}년 ${month}월 ${day}일` : `${month}월 ${day}일`
}

export function addDays(value, days) {
  const date = new Date(`${value}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

export function mondayOf(value) {
  const weekday = new Date(`${value}T00:00:00Z`).getUTCDay()
  return addDays(value, -((weekday + 6) % 7))
}

export function formatRange(from, to) {
  const short = (value) => value.slice(5).replace('-', '.')
  return from === to ? short(from) : `${short(from)}–${short(to)}`
}

export function todayString() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

export function changeClass(current, previous) {
  const difference = Number(current) - Number(previous)
  return difference > 0 ? 'volume-increase' : difference < 0 ? 'volume-decrease' : ''
}

export function filterExercises(exercises, filter, search) {
  return exercises.filter((exercise) => (filter === 'ALL' || exercise.bodyPart === filter) && exercise.name.toLowerCase().includes(search.toLowerCase()))
}
