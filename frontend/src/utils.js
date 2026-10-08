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

export function formatDate(value, withYear = true) {
  if (!value) return ''
  const [year, month, day] = value.split('-').map(Number)
  return withYear ? `${year}년 ${month}월 ${day}일` : `${month}월 ${day}일`
}

export function todayString() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}
