import assert from 'node:assert/strict'
import test from 'node:test'
import { draftFromSession, workoutRequest } from './workoutSession.js'

test('draft loaded from a routine session stays in that routine on the new date with sets unchecked', () => {
  const session = {
    id: 7,
    workoutDate: '2026-10-01',
    routineId: 3,
    routineName: '상체',
    exercises: [{ exerciseId: 8, groupNumber: 1, sets: [{ setNumber: 4, weight: 60, reps: 8, completed: true, setType: 'DROP' }] }],
  }

  const draft = draftFromSession(session, '2026-11-02')

  assert.equal(draft.id, undefined)
  assert.deepEqual(workoutRequest(draft), {
    workoutDate: '2026-11-02',
    routineId: 3,
    exercises: [{ exerciseId: 8, exerciseOrder: 1, groupNumber: 1, sets: [{ setNumber: 1, weight: 60, reps: 8, completed: false, setType: 'DROP' }] }],
  })
  assert.equal(draft.routineName, '상체')
})

test('draft loaded from a session whose routine was deleted starts as a free workout', () => {
  const draft = draftFromSession({ routineId: null, routineName: '삭제된 루틴', exercises: [] }, '2026-11-02')
  assert.equal(draft.routineId, null)
  assert.equal(draft.routineName, null)
})

test('edited session keeps its date and sends only the current exercises and sets in order', () => {
  const draft = {
    id: 42,
    workoutDate: '2026-10-15',
    routineId: null,
    exercises: [
      { exerciseId: 8, sets: [{ setNumber: 3, weight: '140.50', reps: '4', completed: true }] },
      { exerciseId: 9, sets: [] },
    ],
  }
  draft.exercises[0].sets.push({ setNumber: 7, weight: '', reps: 0, completed: false })

  assert.deepEqual(workoutRequest(draft), {
    workoutDate: '2026-10-15',
    routineId: null,
    exercises: [
      { exerciseId: 8, exerciseOrder: 1, groupNumber: null, sets: [
        { setNumber: 1, weight: 140.5, reps: 4, completed: true, setType: 'WORKING' },
        { setNumber: 2, weight: 0, reps: 0, completed: false, setType: 'WORKING' },
      ] },
      { exerciseId: 9, exerciseOrder: 2, groupNumber: null, sets: [] },
    ],
  })
})
