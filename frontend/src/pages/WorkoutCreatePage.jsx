import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

import { apiFetch } from '../lib/apiClient.js'
import { isDbWeightInput } from '../utils/numberInputs.js'

const categories = [
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

const setTypes = [
  { label: '웜업', value: 'WARMUP' },
  { label: '본세트', value: 'WORKING' },
  { label: '탑세트', value: 'TOP' },
  { label: '실패', value: 'FAILURE' },
]

const navItems = ['오늘', '기록', '루틴', '운동']
const draftStorageKey = 'workout-log:workout-draft'

function today() {
  return new Date().toISOString().slice(0, 10)
}

function categoryLabel(value) {
  return categories.find((category) => category.value === value)?.label ?? value
}

function countedSetNumber(sets, setIndex) {
  if (sets[setIndex].setType === 'WARMUP') return '-'

  return sets.slice(0, setIndex + 1).filter((set) => set.setType !== 'WARMUP').length
}

function readDraft() {
  try {
    const draft = JSON.parse(localStorage.getItem(draftStorageKey))
    return Array.isArray(draft?.selectedExercises) ? draft : null
  } catch {
    return null
  }
}

function toWorkoutExercises(exercises) {
  return [...exercises]
    .sort((a, b) => a.exerciseOrder - b.exerciseOrder)
    .map((exercise) => ({
      id: exercise.exerciseId,
      name: exercise.exerciseName,
      category: exercise.exerciseCategory ?? '',
      memo: exercise.memo ?? '',
      sets: [...exercise.sets]
        .sort((a, b) => a.setOrder - b.setOrder)
        .map((set) => ({
          weight: set.weight,
          reps: set.reps,
          setType: set.setType,
          completed: Boolean(set.completed),
        })),
    }))
}

async function readErrorMessage(response) {
  try {
    const error = await response.json()
    return error.message
  } catch {
    return ''
  }
}

function WorkoutCreatePage({ headerAction = null, onNavigate = () => {} }) {
  const initialDraft = useMemo(() => readDraft(), [])
  const [workoutDate, setWorkoutDate] = useState(initialDraft?.workoutDate ?? today)
  const [memo, setMemo] = useState(initialDraft?.memo ?? '')
  const [selectedExercises, setSelectedExercises] = useState(initialDraft?.selectedExercises ?? [])
  const [routines, setRoutines] = useState([])
  const [availableExercises, setAvailableExercises] = useState([])
  const [selectedCategory, setSelectedCategory] = useState('')
  const [exerciseSearchText, setExerciseSearchText] = useState('')
  const [isRoutineSheetOpen, setIsRoutineSheetOpen] = useState(false)
  const [isExerciseSheetOpen, setIsExerciseSheetOpen] = useState(false)
  const [isRoutineLoading, setIsRoutineLoading] = useState(false)
  const [isExerciseLoading, setIsExerciseLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [savedWorkout, setSavedWorkout] = useState(null)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const savingRef = useRef(false)
  const loadingRoutineRef = useRef(false)

  const loadRoutines = useCallback(async () => {
    setIsRoutineLoading(true)
    setErrorMessage('')

    try {
      const response = await apiFetch('/api/routines')
      if (!response.ok) throw new Error()
      setRoutines(await response.json())
    } catch {
      setErrorMessage('루틴 목록을 불러오지 못했습니다.')
    } finally {
      setIsRoutineLoading(false)
    }
  }, [])

  const loadExercises = useCallback(async () => {
    setIsExerciseLoading(true)
    setErrorMessage('')

    try {
      const query = selectedCategory ? `?category=${selectedCategory}` : ''
      const response = await apiFetch(`/api/exercises${query}`)
      if (!response.ok) throw new Error()
      setAvailableExercises(await response.json())
    } catch {
      setErrorMessage('운동 목록을 불러오지 못했습니다.')
    } finally {
      setIsExerciseLoading(false)
    }
  }, [selectedCategory])

  useEffect(() => {
    if (savedWorkout) {
      localStorage.removeItem(draftStorageKey)
      return
    }

    const hasDraft = memo.trim() || selectedExercises.length > 0 || workoutDate !== today()
    if (!hasDraft) {
      localStorage.removeItem(draftStorageKey)
      return
    }

    localStorage.setItem(draftStorageKey, JSON.stringify({ workoutDate, memo, selectedExercises }))
  }, [memo, savedWorkout, selectedExercises, workoutDate])

  useEffect(() => {
    if (isRoutineSheetOpen) {
      // oxlint-disable-next-line react/set-state-in-effect
      void loadRoutines()
    }
  }, [isRoutineSheetOpen, loadRoutines])

  useEffect(() => {
    if (isExerciseSheetOpen) {
      // oxlint-disable-next-line react/set-state-in-effect
      void loadExercises()
    }
  }, [isExerciseSheetOpen, loadExercises])

  const selectedExerciseIds = useMemo(
    () => new Set(selectedExercises.map((exercise) => exercise.id)),
    [selectedExercises],
  )

  const filteredExercises = useMemo(() => {
    const keyword = exerciseSearchText.trim()

    return availableExercises.filter((exercise) => {
      if (!exercise.active) return false
      return !keyword || exercise.name.includes(keyword)
    })
  }, [availableExercises, exerciseSearchText])

  function addExercise(exercise) {
    if (selectedExerciseIds.has(exercise.id)) return

    setSelectedExercises((items) => [
      ...items,
      {
        id: exercise.id,
        name: exercise.name,
        category: exercise.category,
        memo: '',
        sets: [{ weight: 0, reps: 10, setType: 'WORKING', completed: false }],
      },
    ])
  }

  async function loadRoutineDraft(routine) {
    if (loadingRoutineRef.current) return

    loadingRoutineRef.current = true
    setIsRoutineLoading(true)
    setErrorMessage('')
    setMessage('')

    try {
      const response = await apiFetch(`/api/routines/${routine.routineId}`)
      if (!response.ok) throw new Error(await readErrorMessage(response))

      const routineDetail = await response.json()
      setSavedWorkout(null)
      setSelectedExercises(toWorkoutExercises(routineDetail.exercises))
      setIsRoutineSheetOpen(false)
      setMessage('루틴을 운동 기록에 불러왔습니다.')
    } catch {
      setErrorMessage('루틴 상세를 불러오지 못했습니다.')
    } finally {
      loadingRoutineRef.current = false
      setIsRoutineLoading(false)
    }
  }

  function updateExercise(index, field, value) {
    setSelectedExercises((items) =>
      items.map((exercise, currentIndex) =>
        currentIndex === index ? { ...exercise, [field]: value } : exercise,
      ),
    )
  }

  function updateSet(exerciseIndex, setIndex, field, value) {
    if (field === 'weight' && !isDbWeightInput(value)) return

    setSelectedExercises((items) =>
      items.map((exercise, currentIndex) => {
        if (currentIndex !== exerciseIndex) return exercise

        return {
          ...exercise,
          sets: exercise.sets.map((set, currentSetIndex) =>
            currentSetIndex === setIndex ? { ...set, [field]: value } : set,
          ),
        }
      }),
    )
  }

  function addSet(exerciseIndex) {
    setSelectedExercises((items) =>
      items.map((exercise, index) =>
        index === exerciseIndex
          ? {
              ...exercise,
              sets: [...exercise.sets, { weight: 0, reps: 10, setType: 'WORKING', completed: false }],
            }
          : exercise,
      ),
    )
  }

  function removeSet(exerciseIndex, setIndex) {
    setSelectedExercises((items) =>
      items.map((exercise, currentIndex) => {
        if (currentIndex !== exerciseIndex || exercise.sets.length === 1) return exercise

        return {
          ...exercise,
          sets: exercise.sets.filter((_, currentSetIndex) => currentSetIndex !== setIndex),
        }
      }),
    )
  }

  function moveExercise(index, direction) {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= selectedExercises.length) return

    setSelectedExercises((items) => {
      const next = [...items]
      const current = next[index]
      next[index] = next[nextIndex]
      next[nextIndex] = current
      return next
    })
  }

  function removeExercise(index) {
    setSelectedExercises((items) => items.filter((_, currentIndex) => currentIndex !== index))
  }

  function buildPayload() {
    return {
      workoutDate,
      memo: memo.trim(),
      exercises: selectedExercises.map((exercise, exerciseIndex) => ({
        exerciseId: exercise.id,
        exerciseOrder: exerciseIndex + 1,
        memo: exercise.memo.trim(),
        sets: exercise.sets.map((set, setIndex) => ({
          setOrder: setIndex + 1,
          weight: Number(set.weight),
          reps: Number(set.reps),
          rpe: null,
          setType: set.setType,
          completed: set.completed,
        })),
      })),
    }
  }

  async function saveWorkout(event) {
    event.preventDefault()

    if (savingRef.current) return
    if (savedWorkout) {
      setErrorMessage('이미 저장된 운동 기록입니다.')
      return
    }
    if (selectedExercises.length === 0) {
      setErrorMessage('운동을 1개 이상 추가해주세요.')
      return
    }

    savingRef.current = true
    setIsSaving(true)
    setMessage('')
    setErrorMessage('')

    try {
      const response = await apiFetch('/api/workouts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(buildPayload()),
      })

      if (!response.ok) throw new Error(await readErrorMessage(response))

      const createdWorkout = await response.json()
      const savedResponse = await apiFetch(`/api/workouts/${createdWorkout.workoutId}`)
      if (!savedResponse.ok) throw new Error(await readErrorMessage(savedResponse))

      const saved = await savedResponse.json()
      setWorkoutDate(saved.workoutDate)
      setMemo(saved.memo ?? '')
      setSelectedExercises(toWorkoutExercises(saved.exercises))
      setSavedWorkout(saved)
      setMessage('운동 기록을 저장했습니다.')
    } catch (error) {
      const reason = error.message ? ` (${error.message})` : ''
      setErrorMessage(`운동 기록을 저장하지 못했습니다.${reason}`)
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">TODAY</p>
          <h1>오늘 운동 기록</h1>
        </div>
        {headerAction}
      </header>

      <form className="routine-builder" onSubmit={saveWorkout}>
        {message && <p className="success-message">{message}</p>}
        {errorMessage && <p className="error-message">{errorMessage}</p>}

        <section className="routine-section" aria-label="운동 기록 기본 정보">
          <label>
            운동 날짜
            <input type="date" value={workoutDate} onChange={(event) => setWorkoutDate(event.target.value)} />
          </label>
          <label>
            전체 메모
            <textarea value={memo} placeholder="오늘 운동 메모" onChange={(event) => setMemo(event.target.value)} />
          </label>
          <div className="workout-action-grid">
            <button type="button" className="ghost-button" onClick={() => setIsRoutineSheetOpen(true)}>
              루틴 불러오기
            </button>
            <button type="button" className="add-button" onClick={() => setIsExerciseSheetOpen(true)}>
              운동 직접 추가
            </button>
          </div>
          {savedWorkout && (
            <p className="saved-workout-summary">
              저장 확인 · ID {savedWorkout.workoutId} · {savedWorkout.workoutOrder}번째 기록
            </p>
          )}
        </section>

        <section className="routine-section" aria-label="운동 목록">
          <div className="section-header">
            <h2>운동 목록</h2>
          </div>

          <div className="routine-exercises">
            {selectedExercises.map((exercise, exerciseIndex) => (
              <article className="routine-exercise-card" key={exercise.id}>
                <div className="routine-exercise-top">
                  <div>
                    <span>{exerciseIndex + 1}</span>
                    <h3>{exercise.name}</h3>
                    <p>{categoryLabel(exercise.category)}</p>
                  </div>
                  <div className="order-actions" aria-label={`${exercise.name} 순서 변경`}>
                    <button type="button" onClick={() => moveExercise(exerciseIndex, -1)} disabled={exerciseIndex === 0}>
                      ↑
                    </button>
                    <button
                      type="button"
                      onClick={() => moveExercise(exerciseIndex, 1)}
                      disabled={exerciseIndex === selectedExercises.length - 1}
                    >
                      ↓
                    </button>
                    <button type="button" onClick={() => removeExercise(exerciseIndex)} aria-label={`${exercise.name} 삭제`}>
                      ×
                    </button>
                  </div>
                </div>

                <label className="exercise-memo" aria-label={`${exercise.name} 메모`}>
                  <textarea
                    value={exercise.memo}
                    placeholder="운동 메모"
                    onChange={(event) => updateExercise(exerciseIndex, 'memo', event.target.value)}
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
                            onChange={(event) => updateSet(exerciseIndex, setIndex, 'setType', event.target.value)}
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
                            onChange={(event) => updateSet(exerciseIndex, setIndex, 'weight', event.target.value)}
                          />
                        </label>
                        <label>
                          횟수
                          <input
                            type="number"
                            min="0"
                            value={set.reps}
                            onChange={(event) => updateSet(exerciseIndex, setIndex, 'reps', event.target.value)}
                          />
                        </label>
                        <label className="workout-set-complete">
                          <span>완료</span>
                          <input
                            type="checkbox"
                            checked={set.completed}
                            onChange={(event) => updateSet(exerciseIndex, setIndex, 'completed', event.target.checked)}
                          />
                        </label>
                        <button
                          type="button"
                          className="set-remove-button workout-set-delete"
                          aria-label={`${setIndex + 1}세트 삭제`}
                          disabled={exercise.sets.length === 1}
                          onClick={() => {
                            if (window.confirm(`${setIndex + 1}세트를 삭제할까요?`)) {
                              removeSet(exerciseIndex, setIndex)
                            }
                          }}
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
            {selectedExercises.length === 0 && <p className="empty-message">추가된 운동이 없습니다.</p>}
          </div>
        </section>

        <button type="submit" className="primary-action routine-save-button" disabled={isSaving}>
          {isSaving ? '저장 중' : '운동 기록 저장'}
        </button>
      </form>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item === '오늘' ? 'active' : ''}
            key={item}
            onClick={() => {
              if (item === '기록') onNavigate('record')
              if (item === '루틴') onNavigate('routine')
              if (item === '운동') onNavigate('exercise')
            }}
          >
            {item}
          </button>
        ))}
      </nav>

      {isRoutineSheetOpen && (
        <div className="sheet-backdrop" role="presentation" onClick={() => setIsRoutineSheetOpen(false)}>
          <section
            className="bottom-sheet"
            role="dialog"
            aria-modal="true"
            aria-labelledby="routine-picker-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="sheet-header">
              <h2 id="routine-picker-title">루틴 불러오기</h2>
              <button type="button" onClick={() => setIsRoutineSheetOpen(false)}>
                닫기
              </button>
            </div>
            <div className="exercise-picker-list">
              {isRoutineLoading && <p className="empty-message">루틴 목록을 불러오는 중입니다.</p>}
              {!isRoutineLoading &&
                routines.map((routine) => (
                  <button
                    type="button"
                    key={routine.routineId}
                    disabled={isRoutineLoading}
                    onClick={() => loadRoutineDraft(routine)}
                  >
                    <span>{routine.routineName}</span>
                    <small>{routine.exerciseCount}개</small>
                  </button>
                ))}
              {!isRoutineLoading && routines.length === 0 && <p className="empty-message">저장된 루틴이 없습니다.</p>}
            </div>
          </section>
        </div>
      )}

      {isExerciseSheetOpen && (
        <div className="sheet-backdrop" role="presentation" onClick={() => setIsExerciseSheetOpen(false)}>
          <section
            className="bottom-sheet"
            role="dialog"
            aria-modal="true"
            aria-labelledby="exercise-picker-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="sheet-header">
              <h2 id="exercise-picker-title">운동 직접 추가</h2>
              <button type="button" onClick={() => setIsExerciseSheetOpen(false)}>
                닫기
              </button>
            </div>
            <section className="search-section sheet-search" aria-label="운동 검색">
              <input
                type="search"
                value={exerciseSearchText}
                placeholder="운동 이름 검색"
                onChange={(event) => setExerciseSearchText(event.target.value)}
              />
            </section>
            <nav className="category-tabs sheet-tabs" aria-label="운동 카테고리">
              {categories.map((category) => (
                <button
                  type="button"
                  className={category.value === selectedCategory ? 'active' : ''}
                  aria-pressed={category.value === selectedCategory}
                  key={category.value || 'ALL'}
                  onClick={() => setSelectedCategory(category.value)}
                >
                  {category.label}
                </button>
              ))}
            </nav>
            <div className="exercise-picker-list">
              {isExerciseLoading && <p className="empty-message">운동 목록을 불러오는 중입니다.</p>}
              {!isExerciseLoading &&
                filteredExercises.map((exercise) => (
                  <button
                    type="button"
                    disabled={selectedExerciseIds.has(exercise.id)}
                    key={exercise.id}
                    onClick={() => addExercise(exercise)}
                  >
                    <span>{exercise.name}</span>
                    <small>{selectedExerciseIds.has(exercise.id) ? '추가됨' : categoryLabel(exercise.category)}</small>
                  </button>
                ))}
              {!isExerciseLoading && filteredExercises.length === 0 && (
                <p className="empty-message">표시할 운동이 없습니다.</p>
              )}
            </div>
          </section>
        </div>
      )}
    </main>
  )
}

export default WorkoutCreatePage
