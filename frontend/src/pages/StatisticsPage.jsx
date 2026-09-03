import { useEffect, useMemo, useState } from 'react'

import { apiFetch } from '../lib/apiClient.js'

const navItems = [
  { label: '대시보드', page: 'record' },
  { label: '기록추가', page: 'today' },
  { label: '통계', page: 'statistics' },
  { label: '루틴', page: 'routine' },
  { label: '운동관리', page: 'exercise' },
]

const modeOptions = [
  { label: '종목별', value: 'exercise' },
  { label: '부위별', value: 'category' },
]

const periodOptions = [
  { label: '1개월', value: '1m', months: 1 },
  { label: '3개월', value: '3m', months: 3 },
  { label: '6개월', value: '6m', months: 6 },
  { label: '전체', value: 'all', months: null },
]

const exerciseCategories = [
  { label: '전체', value: '' },
  { label: '가슴', value: 'CHEST' },
  { label: '등', value: 'BACK' },
  { label: '하체', value: 'LEGS' },
  { label: '어깨', value: 'SHOULDER' },
  { label: '이두', value: 'BICEPS' },
  { label: '삼두', value: 'TRICEPS' },
  { label: '유산소', value: 'CARDIO' },
  { label: '기타', value: 'ETC' },
]

const categories = [
  { label: '가슴', value: 'CHEST' },
  { label: '등', value: 'BACK' },
  { label: '하체', value: 'LEGS' },
  { label: '어깨', value: 'SHOULDER' },
  { label: '이두', value: 'BICEPS' },
  { label: '삼두', value: 'TRICEPS' },
]

const setTypeLabels = {
  WARMUP: '웜업',
  WORKING: '워킹',
  TOP: '탑세트',
  FAILURE: '실패',
  BACKOFF: '백오프',
  DROP: '드랍',
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDisplayDate(dateText) {
  const [, month, day] = dateText.split('-').map(Number)
  return `${month}월 ${day}일`
}

function formatWeight(value) {
  return Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

function formatVolume(value) {
  return `${Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 0 })}kg`
}

function countedSetNumber(sets, setIndex) {
  if (sets[setIndex].setType === 'WARMUP') return '-'

  return sets.slice(0, setIndex + 1).filter((set) => set.setType !== 'WARMUP').length
}

function dateRange(period) {
  const to = new Date()
  const option = periodOptions.find((item) => item.value === period)
  if (!option?.months) return { from: '', to: formatDate(to) }

  const from = new Date(to)
  from.setMonth(from.getMonth() - option.months)
  return { from: formatDate(from), to: formatDate(to) }
}

function queryString(period) {
  const range = dateRange(period)
  return `${range.from ? `from=${range.from}&` : ''}to=${range.to}`
}

function VolumeChart({ points }) {
  const [activeIndex, setActiveIndex] = useState(null)
  const width = 320
  const height = 166
  const padding = { top: 18, right: 14, bottom: 28, left: 42 }
  const values = points.map((point) => Number(point.volume))
  const maxValue = Math.max(...values, 1)
  const active = points[activeIndex] ?? points.at(-1)
  const chartPoints = points.map((point, index) => {
    const x =
      points.length === 1
        ? width / 2
        : padding.left + (index / (points.length - 1)) * (width - padding.left - padding.right)
    const y = height - padding.bottom - (Number(point.volume) / maxValue) * (height - padding.top - padding.bottom)
    return { ...point, x, y }
  })
  const linePoints = chartPoints.map((point) => `${point.x},${point.y}`).join(' ')

  function selectNearestPoint(event) {
    const box = event.currentTarget.getBoundingClientRect()
    const ratio = Math.min(1, Math.max(0, (event.clientX - box.left) / box.width))
    setActiveIndex(Math.round(ratio * (points.length - 1)))
  }

  return (
    <div className="statistics-chart">
      <svg
        role="img"
        aria-label="볼륨 추이 그래프"
        viewBox={`0 0 ${width} ${height}`}
        onPointerDown={selectNearestPoint}
        onPointerMove={selectNearestPoint}
      >
        <line x1={padding.left} y1={height - padding.bottom} x2={width - padding.right} y2={height - padding.bottom} />
        <line x1={padding.left} y1={padding.top} x2={padding.left} y2={height - padding.bottom} />
        <text x={padding.left - 8} y={padding.top + 4} textAnchor="end">
          {formatVolume(maxValue)}
        </text>
        <text x={padding.left - 8} y={height - padding.bottom + 4} textAnchor="end">
          0
        </text>
        <polyline points={linePoints} />
        {chartPoints.map((point, index) => (
          <circle
            cx={point.x}
            cy={point.y}
            r={point === active ? 5 : 3.5}
            className={point === active ? 'active' : ''}
            key={point.date}
            onClick={() => setActiveIndex(index)}
          />
        ))}
      </svg>
      {active && (
        <div className="statistics-chart-tooltip">
          <div>
            <strong>{formatDisplayDate(active.date)}</strong>
            {active.workoutCount > 1 && <small>운동 기록 {active.workoutCount}회 합산</small>}
          </div>
          <span>{formatVolume(active.volume)}</span>
        </div>
      )}
    </div>
  )
}

function ExerciseHistory({ days }) {
  return (
    <section className="statistics-history" aria-label="종목별 상세 기록">
      {days.map((day) => (
        <article className="statistics-history-day" key={day.date}>
          <div className="statistics-history-header">
            <div>
              <h2>{formatDisplayDate(day.date)}</h2>
              <p>
                {formatVolume(day.volume)} · 운동 기록 {day.workoutCount}개
              </p>
            </div>
          </div>
          {day.workouts.map((workout) => (
            <div className="statistics-workout-block" key={workout.workoutId}>
              <h3>운동 기록 {workout.workoutOrder}</h3>
              <div className="record-set-list" aria-label={`운동 기록 ${workout.workoutOrder} 세트`}>
                <div className="record-set-header" aria-hidden="true">
                  <span>세트</span>
                  <span>유형</span>
                  <span>중량</span>
                  <span>횟수</span>
                  <span>완료</span>
                </div>
                {workout.sets.map((set, setIndex, sets) => (
                  <div className="record-set-row" key={set.setOrder}>
                    <span className="record-set-number">{countedSetNumber(sets, setIndex)}</span>
                    <strong className={`record-set-type set-type-${set.setType.toLowerCase()}`}>
                      {setTypeLabels[set.setType] ?? set.setType}
                    </strong>
                    <span className="record-set-weight">{formatWeight(set.weight)}kg</span>
                    <span className="record-set-reps">{set.reps}</span>
                    <span className={set.completed ? 'record-set-status complete' : 'record-set-status'}>
                      {set.completed ? '완료' : '-'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </article>
      ))}
    </section>
  )
}

function StatisticsPage({ headerAction = null, onNavigate = () => {} }) {
  const [mode, setMode] = useState('exercise')
  const [period, setPeriod] = useState('3m')
  const [exercises, setExercises] = useState([])
  const [selectedExerciseId, setSelectedExerciseId] = useState('')
  const [selectedExerciseCategory, setSelectedExerciseCategory] = useState('')
  const [exerciseSearchText, setExerciseSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('CHEST')
  const [exerciseStats, setExerciseStats] = useState([])
  const [categoryStats, setCategoryStats] = useState([])
  const [isExerciseLoading, setIsExerciseLoading] = useState(false)
  const [isStatsLoading, setIsStatsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const selectedExerciseName = useMemo(
    () => exercises.find((exercise) => String(exercise.id) === selectedExerciseId)?.name ?? '',
    [exercises, selectedExerciseId],
  )
  const filteredExercises = useMemo(() => {
    const keyword = exerciseSearchText.trim()

    return exercises.filter((exercise) => {
      if (selectedExerciseCategory && exercise.category !== selectedExerciseCategory) return false
      return !keyword || exercise.name.includes(keyword)
    })
  }, [exerciseSearchText, exercises, selectedExerciseCategory])
  const currentPoints = mode === 'exercise' ? exerciseStats : categoryStats
  const emptyMessage =
    mode === 'exercise'
      ? selectedExerciseId
        ? '해당 기간의 운동 기록이 없습니다.'
        : '운동 종목을 선택해주세요.'
      : '해당 기간의 부위별 기록이 없습니다.'

  useEffect(() => {
    let ignore = false

    async function loadExercises() {
      setIsExerciseLoading(true)
      setErrorMessage('')

      try {
        const response = await apiFetch('/api/exercises')
        if (!response.ok) throw new Error()

        const data = await response.json()
        if (!ignore) {
          const activeExercises = data.filter((exercise) => exercise.active)
          setExercises(activeExercises)
          setSelectedExerciseId((current) => current || String(activeExercises[0]?.id ?? ''))
        }
      } catch {
        if (!ignore) setErrorMessage('운동 목록을 불러오지 못했습니다.')
      } finally {
        if (!ignore) setIsExerciseLoading(false)
      }
    }

    void loadExercises()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    let ignore = false

    async function loadStatistics() {
      if (mode === 'exercise' && !selectedExerciseId) return

      setIsStatsLoading(true)
      setErrorMessage('')

      try {
        const endpoint =
          mode === 'exercise'
            ? `/api/statistics/exercises/${selectedExerciseId}?${queryString(period)}`
            : `/api/statistics/categories/${selectedCategory}?${queryString(period)}`
        const response = await apiFetch(endpoint)
        if (!response.ok) throw new Error()

        const data = await response.json()
        if (!ignore) {
          if (mode === 'exercise') setExerciseStats(data)
          if (mode === 'category') setCategoryStats(data)
        }
      } catch {
        if (!ignore) setErrorMessage('통계를 불러오지 못했습니다.')
      } finally {
        if (!ignore) setIsStatsLoading(false)
      }
    }

    void loadStatistics()

    return () => {
      ignore = true
    }
  }, [mode, period, selectedCategory, selectedExerciseId])

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">STATISTICS</p>
          <h1>통계</h1>
        </div>
        {headerAction}
      </header>

      <section className="statistics-layout" aria-label="운동 통계">
        <nav className="statistics-mode-tabs" aria-label="통계 종류">
          {modeOptions.map((option) => (
            <button
              type="button"
              className={mode === option.value ? 'active' : ''}
              aria-pressed={mode === option.value}
              key={option.value}
              onClick={() => setMode(option.value)}
            >
              {option.label}
            </button>
          ))}
        </nav>

        <section className="routine-section statistics-filter-panel" aria-label="통계 조건">
          {mode === 'exercise' ? (
            <>
              <div className="statistics-selected-exercise">
                <span>종목</span>
                <strong>{selectedExerciseName || '운동 종목을 선택해주세요.'}</strong>
              </div>
              <section className="search-section statistics-search" aria-label="통계 운동 검색">
                <input
                  type="search"
                  value={exerciseSearchText}
                  placeholder="운동 이름 검색"
                  aria-label="운동 이름 검색"
                  onChange={(event) => setExerciseSearchText(event.target.value)}
                />
              </section>
              <nav className="category-tabs statistics-category-tabs" aria-label="운동 카테고리">
                {exerciseCategories.map((category) => (
                  <button
                    type="button"
                    className={category.value === selectedExerciseCategory ? 'active' : ''}
                    aria-pressed={category.value === selectedExerciseCategory}
                    key={category.value || 'ALL'}
                    onClick={() => setSelectedExerciseCategory(category.value)}
                  >
                    {category.label}
                  </button>
                ))}
              </nav>
              <div className="exercise-picker-list statistics-exercise-picker">
                {filteredExercises.map((exercise) => (
                  <button
                    type="button"
                    className={String(exercise.id) === selectedExerciseId ? 'active' : ''}
                    aria-pressed={String(exercise.id) === selectedExerciseId}
                    key={exercise.id}
                    onClick={() => setSelectedExerciseId(String(exercise.id))}
                  >
                    <span>{exercise.name}</span>
                    <small>
                      {exerciseCategories.find((category) => category.value === exercise.category)?.label ?? exercise.category}
                    </small>
                  </button>
                ))}
                {!isExerciseLoading && filteredExercises.length === 0 && (
                  <p className="empty-message">표시할 운동이 없습니다.</p>
                )}
              </div>
            </>
          ) : (
            <label>
              부위
              <select value={selectedCategory} onChange={(event) => setSelectedCategory(event.target.value)}>
                {categories.map((category) => (
                  <option value={category.value} key={category.value}>
                    {category.label}
                  </option>
                ))}
              </select>
            </label>
          )}
          <nav className="statistics-period-tabs" aria-label="기간 선택">
            {periodOptions.map((option) => (
              <button
                type="button"
                className={period === option.value ? 'active' : ''}
                aria-pressed={period === option.value}
                key={option.value}
                onClick={() => setPeriod(option.value)}
              >
                {option.label}
              </button>
            ))}
          </nav>
        </section>

        {errorMessage && <p className="error-message">{errorMessage}</p>}
        {(isExerciseLoading || isStatsLoading) && <p className="empty-message">통계를 불러오는 중입니다.</p>}
        {!isExerciseLoading && !isStatsLoading && !errorMessage && currentPoints.length === 0 && (
          <p className="empty-message">{emptyMessage}</p>
        )}
        {!isStatsLoading && currentPoints.length > 0 && (
          <section className="routine-section statistics-chart-panel" aria-label="볼륨 그래프">
            <div className="statistics-chart-heading">
              <div>
                <h2>{mode === 'exercise' ? selectedExerciseName : categories.find((item) => item.value === selectedCategory)?.label}</h2>
                <p>완료한 본세트 볼륨</p>
              </div>
              <strong>{formatVolume(currentPoints.at(-1).volume)}</strong>
            </div>
            <VolumeChart points={currentPoints} />
          </section>
        )}
        {mode === 'exercise' && !isStatsLoading && exerciseStats.length > 0 && <ExerciseHistory days={exerciseStats} />}
      </section>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item.page === 'statistics' ? 'active' : ''}
            key={item.page}
            onClick={() => onNavigate(item.page)}
          >
            {item.label}
          </button>
        ))}
      </nav>
    </main>
  )
}

export default StatisticsPage
