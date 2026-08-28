import { useMemo, useState } from 'react'

const categories = ['전체', '가슴', '등', '하체', '어깨', '이두', '삼두', '유산소', '기타']

const initialExercises = [
  { id: 1, name: '벤치프레스', category: '가슴', isUserExercise: false, isActive: true },
  { id: 2, name: '인클라인 벤치프레스', category: '가슴', isUserExercise: false, isActive: true },
  { id: 3, name: '체스트프레스', category: '가슴', isUserExercise: false, isActive: true },
  { id: 4, name: '머신 체스트프레스', category: '가슴', isUserExercise: true, isActive: true },
  { id: 5, name: '케이블 플라이', category: '가슴', isUserExercise: true, isActive: true },
  { id: 6, name: '펙덱', category: '가슴', isUserExercise: false, isActive: true },
  { id: 7, name: '덤벨 풀오버', category: '가슴', isUserExercise: true, isActive: true },
]

const navItems = ['오늘', '기록', '루틴', '운동']

function ExerciseManagementPage() {
  const [exercises, setExercises] = useState(initialExercises)
  const [searchText, setSearchText] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('전체')
  const [editingExercise, setEditingExercise] = useState(undefined)
  const [exerciseToDisable, setExerciseToDisable] = useState(null)
  const [form, setForm] = useState({ name: '', category: '가슴' })

  const filteredExercises = useMemo(() => {
    const keyword = searchText.trim()

    return exercises.filter((exercise) => {
      if (!exercise.isActive) return false
      if (selectedCategory !== '전체' && exercise.category !== selectedCategory) return false
      return !keyword || exercise.name.includes(keyword)
    })
  }, [exercises, searchText, selectedCategory])

  function openAddSheet() {
    setEditingExercise(null)
    setForm({ name: '', category: '가슴' })
  }

  function openEditSheet(exercise) {
    if (!exercise.isUserExercise) return
    setEditingExercise(exercise)
    setForm({ name: exercise.name, category: exercise.category })
  }

  function closeSheet() {
    setEditingExercise(undefined)
    setForm({ name: '', category: '가슴' })
  }

  function saveExercise(event) {
    event.preventDefault()

    const name = form.name.trim()
    if (!name) return

    if (editingExercise) {
      setExercises((items) =>
        items.map((item) =>
          item.id === editingExercise.id ? { ...item, name, category: form.category } : item,
        ),
      )
    } else {
      setExercises((items) => [
        ...items,
        {
          id: Date.now(),
          name,
          category: form.category,
          isUserExercise: true,
          isActive: true,
        },
      ])
    }

    closeSheet()
  }

  function confirmDisable() {
    if (!exerciseToDisable?.isUserExercise) return

    setExercises((items) =>
      items.map((item) => (item.id === exerciseToDisable.id ? { ...item, isActive: false } : item)),
    )
    setExerciseToDisable(null)
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
            className={category === selectedCategory ? 'active' : ''}
            aria-pressed={category === selectedCategory}
            key={category}
            onClick={() => setSelectedCategory(category)}
          >
            {category}
          </button>
        ))}
      </nav>

      <section className="exercise-list" aria-label="운동 목록">
        {filteredExercises.map((exercise) => (
          <article className="exercise-card" key={exercise.id}>
            <div>
              <h2>{exercise.name}</h2>
              <p>{exercise.category}</p>
            </div>
            {exercise.isUserExercise && (
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
        {filteredExercises.length === 0 && <p className="empty-message">표시할 운동이 없습니다.</p>}
      </section>

      <nav className="bottom-nav" aria-label="하단 메뉴">
        {navItems.map((item) => (
          <button type="button" className={item === '운동' ? 'active' : ''} key={item}>
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
                    .filter((category) => category !== '전체')
                    .map((category) => (
                      <option value={category} key={category}>
                        {category}
                      </option>
                    ))}
                </select>
              </label>
              <button type="submit" className="primary-action">
                저장
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
              <button type="button" className="danger-fill" onClick={confirmDisable}>
                비활성화
              </button>
            </div>
          </section>
        </div>
      )}
    </main>
  )
}

export default ExerciseManagementPage
