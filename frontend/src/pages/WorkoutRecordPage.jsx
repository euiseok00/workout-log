import { useEffect, useMemo, useRef, useState } from 'react'

import workoutFlame from '../assets/workout-flame.png'
import { apiFetch } from '../lib/apiClient.js'
import { isDbWeightInput } from '../utils/numberInputs.js'

const navItems = [
  { label: '대시보드', page: 'record' },
  { label: '기록추가', page: 'today' },
  { label: '통계', page: 'statistics' },
  { label: '루틴', page: 'routine' },
  { label: '운동관리', page: 'exercise' },
]
const weekdays = ['일', '월', '화', '수', '목', '금', '토']
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)
const categories = [
  { label: '가슴', value: 'CHEST' },
  { label: '등', value: 'BACK' },
  { label: '하체', value: 'LEGS' },
  { label: '어깨', value: 'SHOULDER' },
  { label: '이두', value: 'BICEPS' },
  { label: '삼두', value: 'TRICEPS' },
  { label: '유산소', value: 'CARDIO' },
  { label: '기타', value: 'ETC' },
]
const setTypeLabels = {
  WARMUP: '웜업',
  WORKING: '워킹',
  TOP: '탑세트',
  FAILURE: '실패',
  BACKOFF: '백오프',
  DROP: '드랍',
}
const setTypes = Object.entries(setTypeLabels).map(([value, label]) => ({ value, label }))
const startYear = 2020
const endYear = Math.max(new Date().getFullYear() + 5, 2030)
const yearOptions = Array.from({ length: endYear - startYear + 1 }, (_, index) => startYear + index)

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDisplayDate(dateText) {
  const [year, month, day] = dateText.split('-').map(Number)
  const weekday = weekdays[new Date(year, month - 1, day).getDay()]
  return `${month}월 ${day}일 ${weekday}요일`
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
  return workout.exercises.reduce(
    (total, exercise) => total + exercise.sets.filter((set) => set.setType !== 'WARMUP').length,
    0,
  )
}

function categoryLabel(value) {
  return categories.find((category) => category.value === value)?.label ?? value
}

function countedSetNumber(sets, setIndex) {
  if (sets[setIndex].setType === 'WARMUP') return '-'

  return sets.slice(0, setIndex + 1).filter((set) => set.setType !== 'WARMUP').length
}

function toEditForm(workout) {
  return {
    workoutDate: workout.workoutDate,
    memo: workout.memo ?? '',
    exercises: workout.exercises
      .slice()
      .sort((a, b) => a.exerciseOrder - b.exerciseOrder)
      .map((exercise) => ({
        exerciseId: exercise.exerciseId,
        exerciseName: exercise.exerciseName,
        exerciseCategory: exercise.exerciseCategory,
        exerciseOrder: exercise.exerciseOrder,
        memo: exercise.memo ?? '',
        completed: Boolean(exercise.completed),
        sets: exercise.sets
          .slice()
          .sort((a, b) => a.setOrder - b.setOrder)
          .map((set) => ({
            weight: set.weight,
            reps: set.reps,
            rpe: set.rpe,
            setType: set.setType,
            completed: Boolean(set.completed),
          })),
      })),
  }
}

function WorkoutRecordPage({ headerAction = null, onNavigate = () => {} }) {
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
  const [isDeleting, setIsDeleting] = useState(false)
  const [isUpdating, setIsUpdating] = useState(false)
  const [calendarError, setCalendarError] = useState('')
  const [listError, setListError] = useState('')
  const [detailError, setDetailError] = useState('')
  const [isMonthPickerOpen, setIsMonthPickerOpen] = useState(false)
  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState(null)
  const deletingRef = useRef(false)
  const updatingRef = useRef(false)

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
        const response = await apiFetch(`/api/workouts/calendar?year=${calendarMonth.year}&month=${calendarMonth.month}`)
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
        const response = await apiFetch(`/api/workouts?date=${selectedDate}`)
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

  function selectMonth(year, month) {
    setCalendarMonth({ year, month })
    setSelectedDate(`${year}-${String(month).padStart(2, '0')}-01`)
  }

  function goToday() {
    const now = new Date()
    setCalendarMonth({ year: now.getFullYear(), month: now.getMonth() + 1 })
    setSelectedDate(formatDate(now))
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
    setIsEditing(false)
    setEditForm(null)
    setIsDetailLoading(true)

    try {
      const response = await apiFetch(`/api/workouts/${workoutId}`)
      if (!response.ok) throw new Error()
      setDetail(await response.json())
    } catch {
      setDetailError('운동 기록 상세를 불러오지 못했습니다.')
    } finally {
      setIsDetailLoading(false)
    }
  }

  function startEdit() {
    if (!detail) return
    setEditForm(toEditForm(detail))
    setDetailError('')
    setIsEditing(true)
  }

  function cancelEdit() {
    setEditForm(null)
    setDetailError('')
    setIsEditing(false)
  }

  function updateWorkoutField(field, value) {
    setEditForm((current) => ({ ...current, [field]: value }))
  }

  function updateExerciseField(exerciseIndex, field, value) {
    setEditForm((current) => ({
      ...current,
      exercises: current.exercises.map((exercise, index) =>
        index === exerciseIndex ? { ...exercise, [field]: value } : exercise,
      ),
    }))
  }

  function updateSetField(exerciseIndex, setIndex, field, value) {
    if (field === 'weight' && !isDbWeightInput(value)) return

    setEditForm((current) => ({
      ...current,
      exercises: current.exercises.map((exercise, index) => {
        if (index !== exerciseIndex) return exercise

        return {
          ...exercise,
          sets: exercise.sets.map((set, currentSetIndex) =>
            currentSetIndex === setIndex ? { ...set, [field]: value } : set,
          ),
        }
      }),
    }))
  }

  function addSet(exerciseIndex) {
    setEditForm((current) => ({
      ...current,
      exercises: current.exercises.map((exercise, index) =>
        index === exerciseIndex
          ? {
              ...exercise,
              sets: [...exercise.sets, { weight: 0, reps: 10, rpe: null, setType: 'WORKING', completed: false }],
            }
          : exercise,
      ),
    }))
  }

  function removeSet(exerciseIndex, setIndex) {
    setEditForm((current) => ({
      ...current,
      exercises: current.exercises.map((exercise, index) => {
        if (index !== exerciseIndex || exercise.sets.length === 1) return exercise

        return {
          ...exercise,
          sets: exercise.sets.filter((_, currentSetIndex) => currentSetIndex !== setIndex),
        }
      }),
    }))
  }

  function buildEditPayload() {
    return {
      workoutDate: editForm.workoutDate,
      memo: editForm.memo.trim(),
      exercises: editForm.exercises.map((exercise) => ({
        exerciseId: exercise.exerciseId,
        exerciseOrder: exercise.exerciseOrder,
        memo: exercise.memo.trim(),
        completed: exercise.completed,
        sets: exercise.sets.map((set, setIndex) => ({
          setOrder: setIndex + 1,
          weight: Number(set.weight),
          reps: Number(set.reps),
          rpe: set.rpe,
          setType: set.setType,
          completed: set.completed,
        })),
      })),
    }
  }

  async function saveEdit(event) {
    event.preventDefault()
    if (!detail || !editForm || updatingRef.current) return

    updatingRef.current = true
    setIsUpdating(true)
    setDetailError('')

    try {
      const response = await apiFetch(`/api/workouts/${detail.workoutId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(buildEditPayload()),
      })
      if (!response.ok) throw new Error()

      const updated = await response.json()
      const [updatedYear, updatedMonth] = updated.workoutDate.split('-').map(Number)
      setDetail(updated)
      setSelectedDate(updated.workoutDate)
      setCalendarMonth({
        year: updatedYear,
        month: updatedMonth,
      })

      const listResponse = await apiFetch(`/api/workouts?date=${updated.workoutDate}`)
      if (listResponse.ok) {
        setWorkouts(await listResponse.json())
      }

      const calendarResponse = await apiFetch(`/api/workouts/calendar?year=${updatedYear}&month=${updatedMonth}`)
      if (calendarResponse.ok) {
        setRecordDates(await calendarResponse.json())
      }

      setEditForm(null)
      setIsEditing(false)
    } catch {
      setDetailError('운동 기록을 수정하지 못했습니다.')
    } finally {
      updatingRef.current = false
      setIsUpdating(false)
    }
  }

  async function deleteWorkout() {
    if (!detail || deletingRef.current) return
    if (!window.confirm('운동 기록을 삭제할까요?')) return

    deletingRef.current = true
    setIsDeleting(true)
    setDetailError('')

    try {
      const response = await apiFetch(`/api/workouts/${detail.workoutId}`, { method: 'DELETE' })
      if (!response.ok) throw new Error()

      const hasOtherWorkoutOnDate = workouts.some((workout) => workout.workoutId !== detail.workoutId)
      setWorkouts((items) => items.filter((workout) => workout.workoutId !== detail.workoutId))
      if (!hasOtherWorkoutOnDate) {
        setRecordDates((dates) => dates.filter((date) => date !== detail.workoutDate))
      }
      setDetail(null)
      setView('calendar')
    } catch {
      setDetailError('운동 기록을 삭제하지 못했습니다.')
    } finally {
      deletingRef.current = false
      setIsDeleting(false)
    }
  }

  if (view === 'detail') {
    const totalSets = detail ? countSets(detail) : 0

    return (
      <main className="exercise-page">
        <header className="page-header">
          <div>
            <p className="eyebrow">RECORDS</p>
            <h1>{formatDisplayDate(detail?.workoutDate ?? selectedDate)}</h1>
          </div>
          <div className="page-header-actions">
            <button type="button" className="ghost-button" onClick={() => setView('calendar')}>
              뒤로
            </button>
            {headerAction}
          </div>
        </header>

        {isDetailLoading && <p className="empty-message">상세 기록을 불러오는 중입니다.</p>}
        {detailError && <p className="error-message">{detailError}</p>}
        {detail && !isEditing && (
          <section className="routine-builder" aria-label="운동 기록 상세">
            <section className="routine-section record-detail-header-card">
              <div>
                <p>운동 기록 {detail.workoutOrder}</p>
                <div className="record-detail-summary">
                  <strong>운동 {detail.exercises.length}개</strong>
                  <span>총 {totalSets}세트</span>
                </div>
              </div>
              {detail.memo && <p className="record-memo">{detail.memo}</p>}
              <div className="record-edit-actions">
                <button type="button" className="ghost-button" onClick={startEdit}>
                  운동 기록 수정
                </button>
                <button
                  type="button"
                  className="danger-outline-button record-delete-button"
                  disabled={isDeleting}
                  onClick={deleteWorkout}
                >
                  {isDeleting ? '삭제 중' : '운동 기록 삭제'}
                </button>
              </div>
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
                      <p>{categoryLabel(exercise.exerciseCategory)}</p>
                    </div>
                  </div>
                  {exercise.memo && <p className="record-memo">{exercise.memo}</p>}
                  <div className="record-set-list" aria-label={`${exercise.exerciseName} 세트`}>
                    <div className="record-set-header" aria-hidden="true">
                      <span>세트</span>
                      <span>유형</span>
                      <span>중량</span>
                      <span>횟수</span>
                      <span>완료</span>
                    </div>
                    {exercise.sets
                      .slice()
                      .sort((a, b) => a.setOrder - b.setOrder)
                      .map((set, setIndex, sets) => (
                        <div className="record-set-row" key={set.setOrder}>
                          <span className="record-set-number">{countedSetNumber(sets, setIndex)}</span>
                          <strong className={`record-set-type set-type-${set.setType.toLowerCase()}`}>
                            {setTypeLabels[set.setType] ?? set.setType}
                          </strong>
                          <span className="record-set-weight">{set.weight}kg</span>
                          <span className="record-set-reps">{set.reps}</span>
                          <span className={set.completed ? 'record-set-status complete' : 'record-set-status'}>
                            {set.completed ? '완료' : '-'}
                          </span>
                        </div>
                      ))}
                  </div>
                </article>
              ))}
          </section>
        )}

        {detail && isEditing && editForm && (
          <form className="routine-builder" aria-label="운동 기록 수정" onSubmit={saveEdit}>
            <section className="routine-section record-detail-header-card">
              <label>
                운동 날짜
                <input
                  type="date"
                  value={editForm.workoutDate}
                  onChange={(event) => updateWorkoutField('workoutDate', event.target.value)}
                />
              </label>
              <label>
                전체 메모
                <textarea
                  value={editForm.memo}
                  placeholder="운동 기록 메모"
                  onChange={(event) => updateWorkoutField('memo', event.target.value)}
                />
              </label>
              <div className="record-detail-summary">
                <strong>운동 {editForm.exercises.length}개</strong>
                <span>운동 기록 {detail.workoutOrder}</span>
              </div>
              <div className="record-edit-actions">
                <button type="button" className="ghost-button" disabled={isUpdating} onClick={cancelEdit}>
                  취소
                </button>
                <button type="submit" className="primary-action" disabled={isUpdating}>
                  {isUpdating ? '저장 중' : '변경사항 저장'}
                </button>
              </div>
            </section>

            {editForm.exercises.map((exercise, exerciseIndex) => (
              <article className="routine-exercise-card record-exercise-detail" key={exercise.exerciseOrder}>
                <div className="routine-exercise-top">
                  <div>
                    <span>{exercise.exerciseOrder}</span>
                    <h3>{exercise.exerciseName}</h3>
                    <p>{categoryLabel(exercise.exerciseCategory)}</p>
                  </div>
                </div>
                <label className="exercise-memo" aria-label={`${exercise.exerciseName} 메모`}>
                  <textarea
                    value={exercise.memo}
                    placeholder="운동 메모"
                    onChange={(event) => updateExerciseField(exerciseIndex, 'memo', event.target.value)}
                  />
                </label>
                <div className="routine-sets">
                  {exercise.sets.map((set, setIndex) => (
                    <article className="routine-set-card" data-set-type={set.setType} key={setIndex}>
                      <div className="workout-set-grid">
                        <label>
                          유형
                          <select
                            value={set.setType}
                            onChange={(event) => updateSetField(exerciseIndex, setIndex, 'setType', event.target.value)}
                          >
                            {setTypes.map((setType) => (
                              <option value={setType.value} key={setType.value}>
                                {setType.label}
                              </option>
                            ))}
                          </select>
                        </label>
                        <div className="workout-set-number">
                          <span>세트</span>
                          <strong>{countedSetNumber(exercise.sets, setIndex)}</strong>
                        </div>
                        <label>
                          중량
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            value={set.weight}
                            onChange={(event) => updateSetField(exerciseIndex, setIndex, 'weight', event.target.value)}
                          />
                        </label>
                        <label>
                          횟수
                          <input
                            type="number"
                            min="0"
                            value={set.reps}
                            onChange={(event) => updateSetField(exerciseIndex, setIndex, 'reps', event.target.value)}
                          />
                        </label>
                        <label className="workout-set-complete">
                          <span>완료</span>
                          <input
                            type="checkbox"
                            checked={set.completed}
                            onChange={(event) => updateSetField(exerciseIndex, setIndex, 'completed', event.target.checked)}
                          />
                        </label>
                        <button
                          type="button"
                          className="set-remove-button workout-set-delete"
                          aria-label={`${setIndex + 1}세트 삭제`}
                          disabled={exercise.sets.length === 1}
                          onClick={() => removeSet(exerciseIndex, setIndex)}
                        >
                          ×
                        </button>
                      </div>
                    </article>
                  ))}
                  <button type="button" className="set-add-button" onClick={() => addSet(exerciseIndex)}>
                    세트 추가
                  </button>
                </div>
              </article>
            ))}

            <button type="submit" className="primary-action routine-save-button" disabled={isUpdating}>
              {isUpdating ? '저장 중' : '변경사항 저장'}
            </button>
          </form>
        )}

        <nav className="bottom-nav" aria-label="하단 메뉴">
          {navItems.map((item) => (
            <button
              type="button"
              className={item.page === 'record' ? 'active' : ''}
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

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">RECORDS</p>
          <h1>운동 기록</h1>
        </div>
        {headerAction}
      </header>

      <section className="routine-builder" aria-label="운동 기록 달력">
        <section className="routine-section">
          <div className="record-month-header">
            <button type="button" className="record-today-button" onClick={goToday}>
              오늘
            </button>
            <button type="button" className="record-month-arrow" aria-label="이전 달" onClick={() => moveMonth(-1)}>
              ‹
            </button>
            <div className="record-month-picker">
              <span>
                {calendarMonth.year}.{String(calendarMonth.month).padStart(2, '0')}
              </span>
              <button
                type="button"
                className="record-month-toggle"
                aria-label="월 선택"
                aria-expanded={isMonthPickerOpen}
                onClick={() => setIsMonthPickerOpen((open) => !open)}
              >
                {isMonthPickerOpen ? '▴' : '▾'}
              </button>
              {isMonthPickerOpen && (
                <div className="record-month-dropdown">
                  <div className="record-month-dropdown-header">
                    <strong>직접 선택</strong>
                    <button type="button" aria-label="닫기" onClick={() => setIsMonthPickerOpen(false)}>
                      ×
                    </button>
                  </div>
                  <div className="record-month-options">
                    <div>
                      {yearOptions.map((year) => (
                        <button
                          type="button"
                          className={year === calendarMonth.year ? 'active' : ''}
                          key={year}
                          onClick={() => selectMonth(year, calendarMonth.month)}
                        >
                          {year}년
                        </button>
                      ))}
                    </div>
                    <div>
                      {monthOptions.map((month) => (
                        <button
                          type="button"
                          className={month === calendarMonth.month ? 'active' : ''}
                          key={month}
                          onClick={() => {
                            selectMonth(calendarMonth.year, month)
                            setIsMonthPickerOpen(false)
                          }}
                        >
                          {month}월
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
            <button type="button" className="record-month-arrow" aria-label="다음 달" onClick={() => moveMonth(1)}>
              ›
            </button>
          </div>
          {calendarError && <p className="error-message">{calendarError}</p>}
          {isCalendarLoading && <p className="empty-message">월간 기록을 불러오는 중입니다.</p>}
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
                    <img src={workoutFlame} alt="운동 기록 있음" />
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
            <h2>{formatDisplayDate(selectedDate)}</h2>
          </div>
          {listError && <p className="error-message">{listError}</p>}
          {isListLoading && <p className="empty-message">운동 기록을 불러오는 중입니다.</p>}
          {!isListLoading && !listError && workouts.length === 0 && (
            <p className="empty-message">이 날짜에는 운동 기록이 없습니다.</p>
          )}
          <div className="exercise-list">
            {workouts.map((workout) => (
              <button
                type="button"
                className="exercise-card routine-card-button workout-record-card"
                key={workout.workoutId}
                onClick={() => openDetail(workout.workoutId)}
              >
                <div className="workout-record-card-body">
                  <h2>운동 기록 {workout.workoutOrder}</h2>
                  <p>
                    {workout.exerciseCount}개 운동 · {workout.setCount}세트
                  </p>
                  {workout.memo && <p className="routine-exercise-names">{workout.memo}</p>}
                </div>
                <span aria-hidden="true">›</span>
              </button>
            ))}
          </div>
        </section>
      </section>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item.page === 'record' ? 'active' : ''}
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

export default WorkoutRecordPage
