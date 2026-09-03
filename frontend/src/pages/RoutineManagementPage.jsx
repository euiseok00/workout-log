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
  { label: '워킹', value: 'WORKING' },
  { label: '탑세트', value: 'TOP' },
  { label: '실패', value: 'FAILURE' },
  { label: '백오프', value: 'BACKOFF' },
  { label: '드랍', value: 'DROP' },
]

const navItems = [
  { label: '대시보드', page: 'record' },
  { label: '기록추가', page: 'today' },
  { label: '통계', page: 'statistics' },
  { label: '루틴', page: 'routine' },
  { label: '운동관리', page: 'exercise' },
]

function categoryLabel(value) {
  return categories.find((category) => category.value === value)?.label ?? value
}

function countedSetNumber(sets, setIndex) {
  if (sets[setIndex].setType === 'WARMUP') return '-'

  return sets.slice(0, setIndex + 1).filter((set) => set.setType !== 'WARMUP').length
}

function toFormExercises(exercises) {
  return [...exercises]
    .sort((a, b) => a.exerciseOrder - b.exerciseOrder)
    .map((exercise) => ({
      id: exercise.exerciseId,
      name: exercise.exerciseName,
      category: exercise.exerciseCategory,
      memo: exercise.memo ?? '',
      sets: [...exercise.sets]
        .sort((a, b) => a.setOrder - b.setOrder)
        .map((set) => ({
          weight: set.weight,
          reps: set.reps,
          setType: set.setType,
        })),
    }))
}

function RoutineManagementPage({ headerAction = null, onNavigate = () => {} }) {
  const [routines, setRoutines] = useState([])
  const [availableExercises, setAvailableExercises] = useState([])
  const [mode, setMode] = useState('list')
  const [editingRoutineId, setEditingRoutineId] = useState(null)
  const [isExerciseSheetOpen, setIsExerciseSheetOpen] = useState(false)
  const [routineName, setRoutineName] = useState('')
  const [routineMemo, setRoutineMemo] = useState('')
  const [exerciseSearchText, setExerciseSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [selectedExercises, setSelectedExercises] = useState([])
  const [isRoutineLoading, setIsRoutineLoading] = useState(false)
  const [isExerciseLoading, setIsExerciseLoading] = useState(false)
  const [isDetailLoading, setIsDetailLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [listErrorMessage, setListErrorMessage] = useState('')
  const [detailErrorMessage, setDetailErrorMessage] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const savingRef = useRef(false)
  const deletingRef = useRef(false)

  const loadRoutines = useCallback(async () => {
    setIsRoutineLoading(true)
    setListErrorMessage('')

    try {
      const response = await apiFetch('/api/routines')
      if (!response.ok) throw new Error()
      setRoutines(await response.json())
    } catch {
      setListErrorMessage('루틴 목록을 불러오지 못했습니다.')
    } finally {
      setIsRoutineLoading(false)
    }
  }, [])

  const loadExercises = useCallback(async () => {
    setIsExerciseLoading(true)
    setFormMessage('')

    try {
      const query = selectedCategory ? `?category=${selectedCategory}` : ''
      const response = await apiFetch(`/api/exercises${query}`)
      if (!response.ok) throw new Error()
      setAvailableExercises(await response.json())
    } catch {
      setFormMessage('운동 목록을 불러오지 못했습니다.')
    } finally {
      setIsExerciseLoading(false)
    }
  }, [selectedCategory])

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect
    void loadRoutines()
  }, [loadRoutines])

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
        sets: [{ weight: 0, reps: 10, setType: 'WORKING' }],
      },
    ])
  }

  function addSet(exerciseIndex) {
    setSelectedExercises((items) =>
      items.map((exercise, index) =>
        index === exerciseIndex
          ? { ...exercise, sets: [...exercise.sets, { weight: 0, reps: 10, setType: 'WORKING' }] }
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

  function updateExerciseMemo(index, value) {
    setSelectedExercises((items) =>
      items.map((exercise, currentIndex) => (currentIndex === index ? { ...exercise, memo: value } : exercise)),
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

  function removeExercise(index) {
    setSelectedExercises((items) => items.filter((_, currentIndex) => currentIndex !== index))
  }

  function resetForm() {
    setRoutineName('')
    setRoutineMemo('')
    setSelectedExercises([])
    setEditingRoutineId(null)
    setDetailErrorMessage('')
    setFormMessage('')
  }

  function openCreateMode() {
    resetForm()
    setMode('create')
  }

  function backToList() {
    resetForm()
    setMode('list')
  }

  async function openEditMode(routineId) {
    resetForm()
    setMode('edit')
    setEditingRoutineId(routineId)
    setIsDetailLoading(true)

    try {
      const response = await apiFetch(`/api/routines/${routineId}`)
      if (!response.ok) throw new Error()

      const routine = await response.json()
      setRoutineName(routine.routineName)
      setRoutineMemo(routine.routineMemo ?? '')
      setSelectedExercises(toFormExercises(routine.exercises))
    } catch {
      setDetailErrorMessage('루틴 상세를 불러오지 못했습니다.')
    } finally {
      setIsDetailLoading(false)
    }
  }

  function buildPayload() {
    return {
      routineName: routineName.trim(),
      routineMemo: routineMemo.trim(),
      exercises: selectedExercises.map((exercise, exerciseIndex) => ({
        exerciseId: exercise.id,
        exerciseOrder: exerciseIndex + 1,
        memo: exercise.memo.trim(),
        sets: exercise.sets.map((set, setIndex) => ({
          setOrder: setIndex + 1,
          weight: Number(set.weight),
          reps: Number(set.reps),
          setType: set.setType,
        })),
      })),
    }
  }

  async function saveRoutine(event) {
    event.preventDefault()

    if (savingRef.current) return

    const name = routineName.trim()
    if (!name) {
      setFormMessage('루틴 이름을 입력해주세요.')
      return
    }
    if (selectedExercises.length === 0) {
      setFormMessage('운동을 1개 이상 추가해주세요.')
      return
    }

    savingRef.current = true
    setIsSaving(true)
    setFormMessage('')

    const isEditMode = mode === 'edit'
    const url = isEditMode ? `/api/routines/${editingRoutineId}` : '/api/routines'

    try {
      const response = await apiFetch(url, {
        method: isEditMode ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(buildPayload()),
      })

      if (!response.ok) throw new Error()

      resetForm()
      setMode('list')
      setFormMessage(isEditMode ? '루틴을 수정했습니다.' : '루틴을 저장했습니다.')
      await loadRoutines()
    } catch {
      setFormMessage(isEditMode ? '루틴을 수정하지 못했습니다.' : '루틴을 저장하지 못했습니다.')
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  async function deleteRoutine() {
    if (!editingRoutineId || deletingRef.current) return
    if (!window.confirm('루틴을 삭제할까요?')) return

    deletingRef.current = true
    setIsDeleting(true)
    setFormMessage('')

    try {
      const response = await apiFetch(`/api/routines/${editingRoutineId}`, { method: 'DELETE' })
      if (!response.ok) throw new Error()

      resetForm()
      setMode('list')
      setFormMessage('루틴을 삭제했습니다.')
      await loadRoutines()
    } catch {
      setFormMessage('루틴을 삭제하지 못했습니다.')
    } finally {
      deletingRef.current = false
      setIsDeleting(false)
    }
  }

  const isEditMode = mode === 'edit'
  const canShowForm = mode === 'create' || (isEditMode && !isDetailLoading && !detailErrorMessage)

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">ROUTINES</p>
          <h1>{mode === 'list' ? '루틴' : isEditMode ? '루틴 편집' : '새 루틴'}</h1>
        </div>
        <div className="page-header-actions">
          {mode === 'list' ? (
            <button type="button" className="add-button" onClick={openCreateMode}>
              루틴 생성
            </button>
          ) : (
            <button type="button" className="ghost-button" onClick={backToList}>
              목록
            </button>
          )}
          {headerAction}
        </div>
      </header>

      {mode === 'list' ? (
        <section className="exercise-list" aria-label="루틴 목록">
          {formMessage && <p className="success-message">{formMessage}</p>}
          {listErrorMessage && <p className="error-message">{listErrorMessage}</p>}
          {isRoutineLoading && <p className="empty-message">루틴 목록을 불러오는 중입니다.</p>}
          {!isRoutineLoading &&
            routines.map((routine) => (
              <button
                type="button"
                className="exercise-card routine-card routine-card-button"
                key={routine.routineId}
                onClick={() => openEditMode(routine.routineId)}
              >
                <div>
                  <div className="routine-card-title">
                    <h2>{routine.routineName}</h2>
                  </div>
                  <p>{routine.routineMemo || '메모 없음'}</p>
                  <p className="routine-exercise-names">
                    {routine.exerciseCount}개 운동 · {routine.exercises.join(', ')}
                  </p>
                </div>
              </button>
            ))}
          {!isRoutineLoading && !listErrorMessage && routines.length === 0 && (
            <p className="empty-message">저장된 루틴이 없습니다.</p>
          )}
        </section>
      ) : (
        <>
          {isDetailLoading && <p className="empty-message">루틴 상세를 불러오는 중입니다.</p>}
          {detailErrorMessage && (
            <section className="routine-section">
              <p className="error-message">{detailErrorMessage}</p>
              <button type="button" className="ghost-button" onClick={backToList}>
                목록으로 돌아가기
              </button>
            </section>
          )}
          {canShowForm && (
            <form className="routine-builder" onSubmit={saveRoutine}>
              {formMessage && <p className="error-message">{formMessage}</p>}
              <section className="routine-section" aria-label="루틴 기본 정보">
                <label>
                  루틴 이름
                  <input
                    type="text"
                    value={routineName}
                    placeholder="예: 월요일 상체"
                    onChange={(event) => setRoutineName(event.target.value)}
                  />
                </label>
                <label>
                  메모
                  <textarea
                    value={routineMemo}
                    placeholder="루틴 메모"
                    onChange={(event) => setRoutineMemo(event.target.value)}
                  />
                </label>
              </section>

              <section className="routine-section" aria-label="루틴 운동">
                <div className="section-header">
                  <h2>운동</h2>
                  <button type="button" onClick={() => setIsExerciseSheetOpen(true)}>
                    운동 추가
                  </button>
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
                          <button
                            type="button"
                            onClick={() => moveExercise(exerciseIndex, -1)}
                            disabled={exerciseIndex === 0}
                          >
                            ↑
                          </button>
                          <button
                            type="button"
                            onClick={() => moveExercise(exerciseIndex, 1)}
                            disabled={exerciseIndex === selectedExercises.length - 1}
                          >
                            ↓
                          </button>
                          <button
                            type="button"
                            onClick={() => removeExercise(exerciseIndex)}
                            aria-label={`${exercise.name} 삭제`}
                          >
                            ×
                          </button>
                        </div>
                      </div>

                      <label className="exercise-memo">
                        메모
                        <textarea
                          value={exercise.memo}
                          placeholder="운동 메모"
                          onChange={(event) => updateExerciseMemo(exerciseIndex, event.target.value)}
                        />
                      </label>

                      <div className="routine-sets">
                        {exercise.sets.map((set, setIndex) => (
                          <article className="routine-set-card" data-set-type={set.setType} key={setIndex}>
                            <div className="workout-set-grid routine-set-grid">
                              <label>
                                유형
                                <select
                                  value={set.setType}
                                  onChange={(event) =>
                                    updateSet(exerciseIndex, setIndex, 'setType', event.target.value)
                                  }
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
                </div>
              </section>

              <div className="routine-form-actions">
                {isEditMode && (
                  <button type="button" className="danger-outline-button" disabled={isDeleting} onClick={deleteRoutine}>
                    {isDeleting ? '삭제 중' : '루틴 삭제'}
                  </button>
                )}
                <button type="submit" className="primary-action routine-save-button" disabled={isSaving}>
                  {isSaving ? '저장 중' : isEditMode ? '변경사항 저장' : '루틴 저장'}
                </button>
              </div>
            </form>
          )}
        </>
      )}

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item.page === 'routine' ? 'active' : ''}
            key={item.page}
            onClick={() => onNavigate(item.page)}
          >
            {item.label}
          </button>
        ))}
      </nav>

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
              <h2 id="exercise-picker-title">운동 추가</h2>
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
              {formMessage && <p className="error-message">{formMessage}</p>}
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

export default RoutineManagementPage
