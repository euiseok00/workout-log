import { useEffect, useMemo, useState } from 'react'

const navItems = ['오늘', '기록', '루틴', '운동']
const weekdays = ['일', '월', '화', '수', '목', '금', '토']
const setTypeLabels = {
  WARMUP: '웜업',
  WORKING: '본세트',
  TOP: '탑세트',
  FAILURE: '실패',
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function currentMonth() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1 }
}

function buildCalendarDays(year, month) {
  const firstDay = new Date(year, month - 1, 1).getDay()
  const lastDate = new Date(year, month, 0).getDate()
  return [
    ...Array.from({ length: firstDay }, () => null),
    ...Array.from({ length: lastDate }, (_, index) => index + 1),
  ]
}

function countSets(workout) {
  return workout.exercises.reduce((total, exercise) => total + exercise.sets.length, 0)
}

function WorkoutRecordPage({ onNavigate = () => {} }) {
  const initialMonth = useMemo(() => currentMonth(), [])
  const [calendarMonth, setCalendarMonth] = useState(initialMonth)
  const [recordDates, setRecordDates] = useState([])
  const [selectedDate, setSelectedDate] = useState(formatDate(new Date()))
  const [workouts, setWorkouts] = useState([])
  const [detail, setDetail] = useState(null)
  const [view, setView] = useState('calendar')
  const [isCalendarLoading, setIsCalendarLoading] = useState(false)
  const [isListLoading, setIsListLoading] = useState(false)
  const [isDetailLoading, setIsDetailLoading] = useState(false)
  const [calendarError, setCalendarError] = useState('')
  const [listError, setListError] = useState('')
  const [detailError, setDetailError] = useState('')

  const calendarDays = useMemo(
    () => buildCalendarDays(calendarMonth.year, calendarMonth.month),
    [calendarMonth],
  )
  const recordDateSet = useMemo(() => new Set(recordDates), [recordDates])

  useEffect(() => {
    let ignore = false

    async function loadCalendarDates() {
      setIsCalendarLoading(true)
      setCalendarError('')

      try {
        const response = await fetch(`/api/workouts/calendar?year=${calendarMonth.year}&month=${calendarMonth.month}`)
        if (!response.ok) throw new Error()

        const dates = await response.json()
        if (!ignore) setRecordDates(dates)
      } catch {
        if (!ignore) setCalendarError('월간 기록을 불러오지 못했습니다.')
      } finally {
        if (!ignore) setIsCalendarLoading(false)
      }
    }

    void loadCalendarDates()

    return () => {
      ignore = true
    }
  }, [calendarMonth])

  useEffect(() => {
    let ignore = false

    async function loadWorkouts() {
      setIsListLoading(true)
      setListError('')
      setWorkouts([])

      try {
        const response = await fetch(`/api/workouts?date=${selectedDate}`)
        if (!response.ok) throw new Error()

        const data = await response.json()
        if (!ignore) setWorkouts(data)
      } catch {
        if (!ignore) setListError('운동 기록을 불러오지 못했습니다.')
      } finally {
        if (!ignore) setIsListLoading(false)
      }
    }

    void loadWorkouts()

    return () => {
      ignore = true
    }
  }, [selectedDate])

  function moveMonth(offset) {
    const next = new Date(calendarMonth.year, calendarMonth.month - 1 + offset, 1)
    setCalendarMonth({ year: next.getFullYear(), month: next.getMonth() + 1 })
    setSelectedDate(formatDate(next))
  }

  function selectDate(day) {
    const date = `${calendarMonth.year}-${String(calendarMonth.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (date !== selectedDate) {
      setSelectedDate(date)
    }
  }

  async function openDetail(workoutId) {
    setView('detail')
    setDetail(null)
    setDetailError('')
    setIsDetailLoading(true)

    try {
      const response = await fetch(`/api/workouts/${workoutId}`)
      if (!response.ok) throw new Error()
      setDetail(await response.json())
    } catch {
      setDetailError('운동 기록 상세를 불러오지 못했습니다.')
    } finally {
      setIsDetailLoading(false)
    }
  }

  if (view === 'detail') {
    const totalSets = detail ? countSets(detail) : 0

    return (
      <main className="exercise-page">
        <header className="page-header">
          <div>
            <p className="eyebrow">RECORDS</p>
            <h1>{detail?.workoutDate ?? selectedDate}</h1>
          </div>
          <button type="button" className="ghost-button" onClick={() => setView('calendar')}>
            뒤로
          </button>
        </header>

        {isDetailLoading && <p className="empty-message">상세 기록을 불러오는 중입니다.</p>}
        {detailError && <p className="error-message">{detailError}</p>}
        {detail && (
          <section className="routine-builder" aria-label="운동 기록 상세">
            <section className="routine-section">
              <div className="record-detail-summary">
                <strong>운동 {detail.exercises.length}개</strong>
                <span>총 {totalSets}세트</span>
              </div>
              {detail.memo && <p className="record-memo">{detail.memo}</p>}
            </section>

            {detail.exercises
              .slice()
              .sort((a, b) => a.exerciseOrder - b.exerciseOrder)
              .map((exercise) => (
                <article className="routine-exercise-card record-exercise-detail" key={exercise.exerciseOrder}>
                  <div className="routine-exercise-top">
                    <div>
                      <span>{exercise.exerciseOrder}</span>
                      <h3>{exercise.exerciseName}</h3>
                      <p>{exercise.completed ? '완료' : '미완료'}</p>
                    </div>
                  </div>
                  {exercise.memo && <p className="record-memo">{exercise.memo}</p>}
                  <div className="record-set-table" aria-label={`${exercise.exerciseName} 세트`}>
                    <div className="record-set-row record-set-head">
                      <span>세트</span>
                      <span>중량</span>
                      <span>횟수</span>
                      <span>RPE</span>
                      <span>유형</span>
                      <span>완료</span>
                    </div>
                    {exercise.sets
                      .slice()
                      .sort((a, b) => a.setOrder - b.setOrder)
                      .map((set) => (
                        <div className="record-set-row" key={set.setOrder}>
                          <span>{set.setOrder}</span>
                          <span>{set.weight}</span>
                          <span>{set.reps}</span>
                          <span>{set.rpe ?? '-'}</span>
                          <span>{setTypeLabels[set.setType] ?? set.setType}</span>
                          <span>{set.completed ? '완료' : '-'}</span>
                        </div>
                      ))}
                  </div>
                </article>
              ))}
          </section>
        )}

        <nav className="bottom-nav" aria-label="하단 메뉴">
          {navItems.map((item) => (
            <button
              type="button"
              className={item === '기록' ? 'active' : ''}
              key={item}
              onClick={() => {
                if (item === '오늘') onNavigate('today')
                if (item === '루틴') onNavigate('routine')
                if (item === '운동') onNavigate('exercise')
              }}
            >
              {item}
            </button>
          ))}
        </nav>
      </main>
    )
  }

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">RECORDS</p>
          <h1>운동 기록</h1>
        </div>
      </header>

      <section className="routine-builder" aria-label="운동 기록 달력">
        <section className="routine-section">
          <div className="record-month-header">
            <button type="button" className="ghost-button" onClick={() => moveMonth(-1)}>
              이전
            </button>
            <strong>
              {calendarMonth.year}. {String(calendarMonth.month).padStart(2, '0')}
            </strong>
            <button type="button" className="ghost-button" onClick={() => moveMonth(1)}>
              다음
            </button>
          </div>
          {calendarError && <p className="error-message">{calendarError}</p>}
          {isCalendarLoading && <p className="empty-message">월간 기록을 불러오는 중입니다.</p>}
          {!isCalendarLoading && !calendarError && recordDates.length === 0 && (
            <p className="empty-message">이 달에는 운동 기록이 없습니다.</p>
          )}
          <div className="record-calendar">
            {weekdays.map((weekday) => (
              <div className="record-weekday" key={weekday}>
                {weekday}
              </div>
            ))}
            {calendarDays.map((day, index) =>
              day ? (
                <button
                  type="button"
                  className={formatDate(new Date(calendarMonth.year, calendarMonth.month - 1, day)) === selectedDate ? 'active' : ''}
                  key={day}
                  onClick={() => selectDate(day)}
                >
                  <span>{day}</span>
                  {recordDateSet.has(formatDate(new Date(calendarMonth.year, calendarMonth.month - 1, day))) && (
                    <i aria-label="운동 기록 있음" />
                  )}
                </button>
              ) : (
                <div className="record-calendar-empty" key={`empty-${index}`} />
              ),
            )}
          </div>
        </section>

        <section className="routine-section" aria-label="날짜별 운동 기록">
          <div className="section-header">
            <h2>{selectedDate}</h2>
          </div>
          {listError && <p className="error-message">{listError}</p>}
          {isListLoading && <p className="empty-message">운동 기록을 불러오는 중입니다.</p>}
          {!isListLoading && !listError && workouts.length === 0 && (
            <p className="empty-message">운동 기록이 없습니다.</p>
          )}
          <div className="exercise-list">
            {workouts.map((workout) => (
              <button
                type="button"
                className="exercise-card routine-card-button workout-record-card"
                key={workout.workoutId}
                onClick={() => openDetail(workout.workoutId)}
              >
                <div>
                  <h2>운동 기록 {workout.workoutOrder}</h2>
                  <p>
                    운동 {workout.exerciseCount}개 · {workout.setCount}세트
                  </p>
                  {workout.memo && <p className="routine-exercise-names">{workout.memo}</p>}
                </div>
                <span>상세 &gt;</span>
              </button>
            ))}
          </div>
        </section>
      </section>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item === '기록' ? 'active' : ''}
            key={item}
            onClick={() => {
              if (item === '오늘') onNavigate('today')
              if (item === '루틴') onNavigate('routine')
              if (item === '운동') onNavigate('exercise')
            }}
          >
            {item}
          </button>
        ))}
      </nav>
    </main>
  )
}

export default WorkoutRecordPage
