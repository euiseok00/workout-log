// Run with Vite running: node tests/workout-import.mjs (Playwright must be available).
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const { chromium } = createRequire(import.meta.url)('playwright')
const browser = await chromium.launch({ channel: 'chrome', headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } })
  page.setDefaultTimeout(5000)
  const errors = []
  page.on('pageerror', (error) => { errors.push(error.message); console.error(error.message) })
  const now = new Date()
  const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-02`
  const source = {
    workoutId: 81, workoutOrder: 7, workoutDate: date, workoutTitle: 'Source B', memo: 'Old memo',
    exercises: [{
      workoutExerciseId: 901, exerciseId: 12, exerciseOrder: 5,
      exerciseName: 'Bench press', exerciseCategory: 'CHEST', memo: 'Pause at bottom',
      sets: [
        { id: 902, setOrder: 9, weight: 80, reps: 5, rpe: 8, setType: 'TOP', completed: true },
        { id: 903, setOrder: 3, weight: 20, reps: 10, rpe: null, setType: 'WARMUP', completed: true },
      ],
    }],
  }
  const original = JSON.stringify(source)
  const requests = []
  let payload
  await page.route(/\/src\/main\.jsx(?:\?.*)?$/, (route) => route.fulfill({
    contentType: 'application/javascript',
    body: `import React from '/node_modules/.vite/deps/react.js';
      import ReactDOM from '/node_modules/.vite/deps/react-dom_client.js';
      import WorkoutCreatePage from '/src/pages/WorkoutCreatePage.jsx';
      import '/src/index.css'; import '/src/App.css';
      ReactDOM.createRoot(document.getElementById('root')).render(React.createElement(React.StrictMode, null, React.createElement(WorkoutCreatePage)));`,
  }))
  await page.route(/\/src\/lib\/apiClient\.js(?:\?.*)?$/, (route) => route.fulfill({
    contentType: 'application/javascript', body: 'export const apiFetch = (url, options) => fetch(url, options)',
  }))
  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    requests.push({ method: request.method(), path: url.pathname, search: url.search })
    let data
    if (url.pathname === '/api/workouts/calendar') data = [date]
    else if (url.pathname === '/api/workouts/81') data = source
    else if (url.pathname === '/api/workouts' && request.method() === 'POST') {
      payload = request.postDataJSON()
      data = { ...payload, workoutId: 82 }
    } else if (url.pathname === '/api/workouts') {
      data = url.searchParams.get('date') === date
        ? [80, 81].map((workoutId) => ({ workoutId, workoutTitle: workoutId === 80 ? 'Source A' : 'Source B', exerciseCount: 1, setCount: 2 }))
        : []
    } else throw new Error(`Unexpected request: ${request.method()} ${url}`)
    await route.fulfill({ json: data })
  })
  await page.goto(process.env.TEST_BASE_URL ?? 'http://localhost:5174')
  const title = page.getByLabel('기록 제목', { exact: true })
  await title.fill('New workout')
  const newDate = await page.getByLabel('운동 날짜', { exact: true }).inputValue()
  await page.getByPlaceholder('오늘 운동 메모').fill('Keep my memo')
  await page.getByRole('button', { name: '이전 기록 불러오기', exact: true }).click()
  await page.locator('.record-calendar button').filter({ hasText: /^2$/ }).click()
  await page.getByRole('button', { name: /Source B/ }).waitFor()
  assert.equal(await page.locator('.workout-record-card').count(), 2)
  assert(requests.some((request) => request.search === `?date=${date}`))
  await page.screenshot({ path: join(tmpdir(), 'workout-import-calendar-mobile.png'), fullPage: true })
  await page.getByRole('button', { name: /Source B/ }).click()
  await page.getByRole('heading', { name: 'Bench press' }).waitFor()
  assert.equal(await title.inputValue(), 'New workout')
  assert.equal(await page.getByLabel('운동 날짜', { exact: true }).inputValue(), newDate)
  assert.equal(await page.getByPlaceholder('오늘 운동 메모').inputValue(), 'Keep my memo')
  assert.equal(await page.locator('.exercise-memo textarea').inputValue(), 'Pause at bottom')
  assert.deepEqual(await page.getByLabel('중량', { exact: true }).evaluateAll((inputs) => inputs.map((input) => input.value)), ['20', '80'])
  assert.equal(await page.locator('input[type="checkbox"]:checked').count(), 0)
  await page.getByLabel('중량', { exact: true }).nth(1).fill('85')
  for (const width of [390, 1280]) {
    await page.setViewportSize({ width, height: 844 })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    await page.screenshot({ path: join(tmpdir(), `workout-import-form-${width}.png`), fullPage: true })
  }
  await page.getByRole('button', { name: '운동 기록 저장', exact: true }).click()
  await page.getByRole('status').filter({ hasText: '운동 기록이 저장되었습니다.' }).waitFor()
  assert.deepEqual(payload, {
    workoutDate: newDate, workoutTitle: 'New workout', memo: 'Keep my memo',
    exercises: [{ exerciseId: 12, exerciseOrder: 1, memo: 'Pause at bottom', sets: [
      { setOrder: 1, weight: 20, reps: 10, rpe: null, setType: 'WARMUP', completed: false },
      { setOrder: 2, weight: 85, reps: 5, rpe: 8, setType: 'TOP', completed: false },
    ] }],
  })
  assert.equal(JSON.stringify(source), original)
  assert.deepEqual(requests.filter((request) => request.method !== 'GET'), [
    { method: 'POST', path: '/api/workouts', search: '' },
  ])
  assert.deepEqual(errors, [])
  console.log('PASS: date lookup, multiple workouts, copy/reset, RPE, new POST, source unchanged, mobile/desktop layout')
} finally {
  await browser.close()
}
