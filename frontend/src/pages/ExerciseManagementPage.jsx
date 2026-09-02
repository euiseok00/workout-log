import { useEffect, useMemo, useRef, useState } from 'react'

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

const navItems = ['오늘', '기록', '루틴', '운동']

function ExerciseManagementPage({ onNavigate = () => {} }) {
  const [exercises, setExercises] = useState([])
  const [searchText, setSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [editingExercise, setEditingExercise] = useState(undefined)
  const [exerciseToDisable, setExerciseToDisable] = useState(null)
  const [form, setForm] = useState({ name: '', category: 'CHEST' })
  const [errorMessage, setErrorMessage] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isDisabling, setIsDisabling] = useState(false)
  const savingRef = useRef(false)
  const disablingRef = useRef(false)

  useEffect(() => {
    let ignore = false

    async function loadExercises() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const query = selectedCategory ? `?category=${selectedCategory}` : ''
        const response = await fetch(`/api/exercises${query}`)

        if (!response.ok) {
          throw new Error('운동 목록을 불러오지 못했습니다.')
        }

        const data = await response.json()
        if (!ignore) {
          setExercises(data)
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error.message)
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
        }
      }
    }

    loadExercises()

    return () => {
      ignore = true
    }
  }, [selectedCategory])

  const filteredExercises = useMemo(() => {
    const keyword = searchText.trim()

    return exercises.filter((exercise) => {
      if (!exercise.active) return false
      return !keyword || exercise.name.includes(keyword)
    })
  }, [exercises, searchText])

  async function reloadExercises() {
    const query = selectedCategory ? `?category=${selectedCategory}` : ''
    const response = await fetch(`/api/exercises${query}`)

    if (!response.ok) {
      throw new Error('운동 목록을 불러오지 못했습니다.')
    }

    setExercises(await response.json())
  }

  function openAddSheet() {
    setEditingExercise(null)
    setForm({ name: '', category: 'CHEST' })
  }

  function openEditSheet(exercise) {
    if (exercise.type !== 'CUSTOM') return
    setEditingExercise(exercise)
    setForm({ name: exercise.name, category: exercise.category })
  }

  function closeSheet() {
    setEditingExercise(undefined)
    setForm({ name: '', category: 'CHEST' })
  }

  async function saveExercise(event) {
    event.preventDefault()
    if (savingRef.current) return

    const name = form.name.trim()
    if (!name) return

    const url = editingExercise ? `/api/exercises/${editingExercise.id}` : '/api/exercises'
    savingRef.current = true
    setIsSaving(true)

    try {
      const response = await fetch(url, {
        method: editingExercise ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, category: form.category }),
      })

      if (!response.ok) {
        throw new Error('운동을 저장하지 못했습니다.')
      }

      await reloadExercises()
      closeSheet()
    } catch {
      setErrorMessage('운동을 저장하지 못했습니다.')
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  async function confirmDisable() {
    if (exerciseToDisable?.type !== 'CUSTOM' || disablingRef.current) return

    disablingRef.current = true
    setIsDisabling(true)

    try {
      const response = await fetch(`/api/exercises/${exerciseToDisable.id}/inactive`, {
        method: 'PATCH',
      })

      if (!response.ok) {
        throw new Error('운동을 비활성화하지 못했습니다.')
      }

      await reloadExercises()
      setExerciseToDisable(null)
    } catch {
      setErrorMessage('운동을 비활성화하지 못했습니다.')
    } finally {
      disablingRef.current = false
      setIsDisabling(false)
    }
  }

  const isSheetOpen = editingExercise !== undefined

  return (
    <main className="exercise-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">EXERCISES</p>
          <h1>운동 관리</h1>
        </div>
        <button type="button" className="add-button" onClick={openAddSheet}>
          운동 추가
        </button>
      </header>

      <section className="search-section" aria-label="운동 검색">
        <input
          type="search"
          value={searchText}
          placeholder="운동 이름 검색"
          aria-label="운동 이름 검색"
          onChange={(event) => setSearchText(event.target.value)}
        />
      </section>

      <nav className="category-tabs" aria-label="운동 카테고리">
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

      <section className="exercise-list" aria-label="운동 목록">
        {errorMessage && <p className="error-message">{errorMessage}</p>}
        {isLoading && <p className="empty-message">운동 목록을 불러오는 중입니다.</p>}
        {filteredExercises.map((exercise) => (
          <article className="exercise-card" key={exercise.id}>
            <div>
              <h2>{exercise.name}</h2>
              <p>{categories.find((category) => category.value === exercise.category)?.label ?? exercise.category}</p>
            </div>
            {exercise.type === 'CUSTOM' && (
              <div className="exercise-actions" aria-label={`${exercise.name} 관리`}>
                <button type="button" onClick={() => openEditSheet(exercise)}>
                  수정
                </button>
                <button type="button" className="danger" onClick={() => setExerciseToDisable(exercise)}>
                  비활성화
                </button>
              </div>
            )}
          </article>
        ))}
        {!isLoading && filteredExercises.length === 0 && <p className="empty-message">표시할 운동이 없습니다.</p>}
      </section>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button
            type="button"
            className={item === '운동' ? 'active' : ''}
            key={item}
            onClick={() => {
              if (item === '오늘') onNavigate('today')
              if (item === '기록') onNavigate('record')
              if (item === '루틴') onNavigate('routine')
            }}
          >
            {item}
          </button>
        ))}
      </nav>

      {isSheetOpen && (
        <div className="sheet-backdrop" role="presentation" onClick={closeSheet}>
          <section
            className="bottom-sheet"
            role="dialog"
            aria-modal="true"
            aria-labelledby="exercise-form-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="sheet-header">
              <h2 id="exercise-form-title">{editingExercise ? '운동 수정' : '운동 추가'}</h2>
              <button type="button" onClick={closeSheet}>
                닫기
              </button>
            </div>

            <form className="exercise-form" onSubmit={saveExercise}>
              <label>
                운동 이름
                <input
                  type="text"
                  value={form.name}
                  placeholder="운동 이름"
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                />
              </label>
              <label>
                카테고리
                <select
                  value={form.category}
                  onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
                >
                  {categories
                    .filter((category) => category.value)
                    .map((category) => (
                      <option value={category.value} key={category.value}>
                        {category.label}
                      </option>
                    ))}
                </select>
              </label>
              <button type="submit" className="primary-action" disabled={isSaving}>
                {isSaving ? '저장 중' : '저장'}
              </button>
            </form>
          </section>
        </div>
      )}

      {exerciseToDisable && (
        <div className="sheet-backdrop" role="presentation" onClick={() => setExerciseToDisable(null)}>
          <section
            className="confirm-sheet"
            role="dialog"
            aria-modal="true"
            aria-labelledby="disable-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="disable-title">운동을 비활성화할까요?</h2>
            <p>{exerciseToDisable.name}은 목록에서 숨겨집니다.</p>
            <div className="confirm-actions">
              <button type="button" onClick={() => setExerciseToDisable(null)}>
                취소
              </button>
              <button type="button" className="danger-fill" disabled={isDisabling} onClick={confirmDisable}>
                {isDisabling ? '처리 중' : '비활성화'}
              </button>
            </div>
          </section>
        </div>
      )}
    </main>
  )
}

export default ExerciseManagementPage
