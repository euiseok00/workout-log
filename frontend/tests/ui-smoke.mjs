// Run against Vite with Playwright available: node tests/ui-smoke.mjs
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const { chromium } = createRequire(import.meta.url)('playwright')
const browser = await chromium.launch({ channel: 'chrome', headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } })
  page.setDefaultTimeout(7000)
  const errors = []
  const writes = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('dialog', (dialog) => dialog.accept())
  const now = new Date()
  const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const exercises = [{ id: 12, name: '긴운동이름테스트벤치프레스'.repeat(3), category: 'CHEST', active: true, type: 'CUSTOM' }]
  const sets = [{ setOrder: 1, weight: 999.99, reps: 100, rpe: 8, setType: 'BACKOFF', completed: true }]
  const detailExercises = [{ exerciseId: 12, exerciseName: exercises[0].name, exerciseCategory: 'CHEST', exerciseOrder: 1, memo: '운동 메모', sets }]
  let workout = { workoutId: 1, workoutOrder: 1, workoutDate: date, workoutTitle: '기록제목'.repeat(10), memo: '메모'.repeat(40), exercises: detailExercises }
  let routine = { routineId: 1, routineName: '테스트 루틴', routineMemo: '', exercises: detailExercises }
  await page.route(/\/src\/lib\/supabaseClient\.js(?:\?.*)?$/, (route) => route.fulfill({
    contentType: 'application/javascript',
    body: `let session = null; const listeners = new Set();
      const publish = (value) => { session = value; listeners.forEach(fn => fn('', session)); };
      export const supabase = { auth: {
        getSession: async () => ({data:{session}}),
        onAuthStateChange: fn => { listeners.add(fn); return {data:{subscription:{unsubscribe:()=>listeners.delete(fn)}}}; },
        signInWithPassword: async () => { publish({access_token:'test-only'}); return {data:{session}}; },
        signUp: async () => ({data:{session:null}}),
        signOut: async () => { publish(null); return {}; }
      }};`,
  }))
  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()
    assert.equal(request.headers().authorization, 'Bearer test-only')
    if (method !== 'GET') writes.push(`${method} ${path}`)
    let data = []
    if (path.startsWith('/api/exercises')) {
      if (method === 'PUT') Object.assign(exercises[0], request.postDataJSON())
      if (method === 'PATCH') exercises[0].active = path.endsWith('/active')
      if (method === 'POST') exercises.push({ ...request.postDataJSON(), id: 13, active: true, type: 'CUSTOM' })
      data = exercises.filter((exercise) => url.searchParams.get('status') === 'INACTIVE' ? !exercise.active : exercise.active)
    } else if (path === '/api/routines') {
      if (method === 'POST') routine = { ...routine, ...request.postDataJSON(), exercises: detailExercises }
      data = [{ ...routine, exerciseCount: 1, exerciseNames: [exercises[0].name] }]
    } else if (path === '/api/routines/1') {
      if (method === 'PUT') routine = { ...routine, ...request.postDataJSON(), exercises: detailExercises }
      data = routine
    } else if (path === '/api/workouts/calendar') data = [date]
    else if (path === '/api/workouts/1') {
      if (method === 'PUT') workout = { ...workout, ...request.postDataJSON(), exercises: detailExercises }
      data = workout
    } else if (path === '/api/workouts') data = [{ ...workout, exerciseCount: 1, setCount: 1 }]
    else if (path.startsWith('/api/statistics/')) data = [{ date, volume: 99999, workoutCount: 1, workouts: [{ workoutId: 1, workoutOrder: 1, sets }] }]
    else errors.push(`Unexpected API: ${path}`)
    await route.fulfill({ json: data })
  })
  async function layout(name) {
    await page.evaluate(() => Promise.all(document.getAnimations().map((animation) => animation.finished.catch(() => {}))))
    for (const width of [320, 360, 390, 430, 480, 1280]) {
      await page.setViewportSize({ width, height: 844 })
      const overflow = await page.evaluate(() => [...document.querySelectorAll('main *, [role="dialog"] *')]
        .filter((node) => {
          const rect = node.getBoundingClientRect()
          return rect.width && (rect.right > innerWidth + 1 || rect.left < -1)
        }).map((node) => `${node.tagName}.${node.className}`))
      assert.deepEqual(overflow, [], `${name} at ${width}px`)
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), name)
      if (width === 390 || width === 1280) await page.screenshot({ path: join(tmpdir(), `workout-ui-${name}-${width}.png`), fullPage: true })
    }
    await page.setViewportSize({ width: 390, height: 844 })
  }
  const navigate = (name) => page.getByRole('navigation', { name: '하단 메뉴' }).getByRole('button', { name, exact: true }).click()
  await page.goto(process.env.TEST_BASE_URL ?? 'http://127.0.0.1:5174')
  await page.getByPlaceholder('name@example.com').waitFor()
  await layout('auth')
  await page.getByRole('button', { name: '회원가입', exact: true }).click()
  await page.getByPlaceholder('name@example.com').fill('test@example.com')
  await page.locator('input[type="password"]').fill('test-password')
  await page.locator('form button[type="submit"]').click()
  await page.getByText('가입 확인 후 로그인해주세요.').waitFor()
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await page.locator('form button[type="submit"]').click()
  await page.getByRole('heading', { name: '오늘 운동 기록' }).waitFor()
  await layout('create')
  await page.getByRole('button', { name: '운동 직접 추가' }).click()
  await page.getByRole('dialog').getByRole('button', { name: /긴운동이름/ }).click()
  await layout('picker')
  await page.getByRole('dialog').getByRole('button', { name: '닫기' }).click()
  assert.equal(await page.locator('.sheet-backdrop[data-exiting="true"][inert]').count(), 1)
  await page.locator('.sheet-backdrop').waitFor({ state: 'detached' })
  await layout('sets')
  await page.getByRole('button', { name: '세트 추가', exact: true }).click()
  assert.equal(await page.locator('.routine-set-card').count(), 2)
  await page.locator('input[type="checkbox"]').first().check()
  await navigate('루틴')
  await page.getByRole('dialog').getByRole('button', { name: '예', exact: true }).click()
  assert(await page.evaluate(() => localStorage.getItem('workout-log:workout-draft')))
  await page.getByRole('button', { name: /테스트 루틴/ }).waitFor()
  await layout('routines')
  await page.getByRole('button', { name: /테스트 루틴/ }).click()
  await page.locator('.routine-set-card').waitFor()
  await layout('routine-edit')
  await page.getByRole('button', { name: /변경사항 저장|루틴 저장/ }).click()
  await page.getByText('루틴을 수정했습니다.').waitFor()
  await navigate('대시보드')
  await page.locator('.workout-record-card').waitFor()
  await layout('dashboard')
  await page.getByRole('button', { name: '월 선택', exact: true }).click()
  await layout('month-picker')
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await page.locator('.record-month-dropdown').waitFor({ state: 'detached' })
  await page.locator('.workout-record-card').click()
  await page.getByRole('button', { name: '운동 기록 수정', exact: true }).waitFor()
  await layout('record-detail')
  await page.getByRole('button', { name: '운동 기록 수정', exact: true }).click()
  await layout('record-edit')
  await page.getByRole('button', { name: '변경사항 저장', exact: true }).last().click()
  await page.getByRole('button', { name: '운동 기록 수정', exact: true }).waitFor()
  await navigate('통계')
  await page.getByRole('img', { name: '볼륨 추이 그래프' }).waitFor()
  await layout('statistics')
  await page.getByRole('button', { name: '부위별', exact: true }).click()
  await page.getByRole('button', { name: '1개월', exact: true }).click()
  await layout('category-statistics')
  await navigate('운동관리')
  await page.getByRole('button', { name: '수정', exact: true }).waitFor()
  await layout('exercises')
  await page.getByRole('button', { name: '수정', exact: true }).click()
  await layout('exercise-edit')
  await page.getByRole('dialog').locator('input').fill('수정한 운동')
  await page.getByRole('dialog').locator('button[type="submit"]').click()
  await page.locator('.sheet-backdrop').waitFor({ state: 'detached' })
  await page.getByRole('button', { name: '비활성화', exact: true }).click()
  await layout('confirm')
  await page.getByRole('dialog').getByRole('button', { name: '비활성화', exact: true }).click()
  await page.locator('.sheet-backdrop').waitFor({ state: 'detached' })
  await page.getByRole('button', { name: '비활성', exact: true }).click()
  await page.getByRole('button', { name: '활성화', exact: true }).click()
  await navigate('기록추가')
  assert.equal(await page.locator('.routine-set-card').count(), 2)
  assert.equal(await page.locator('input[type="checkbox"]:checked').count(), 1)
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.getByRole('button', { name: '루틴 불러오기', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: '닫기' }).click()
  await page.locator('.sheet-backdrop').waitFor({ state: 'detached' })
  await page.getByRole('button', { name: '로그아웃', exact: true }).click()
  await page.getByPlaceholder('name@example.com').waitFor()
  assert(writes.includes('PUT /api/routines/1'))
  assert(writes.includes('PUT /api/workouts/1'))
  assert(writes.includes('PUT /api/exercises/12'))
  assert(writes.includes('PATCH /api/exercises/12/inactive'))
  assert(writes.includes('PATCH /api/exercises/12/active'))
  assert.deepEqual(errors, [])
  console.log('PASS: six viewports, auth, workout draft, routine/record edits, statistics, exercise management, modal transitions')
} finally {
  await browser.close()
}
