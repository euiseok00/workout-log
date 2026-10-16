import assert from 'node:assert/strict'
import test from 'node:test'
import { workoutRequest } from './workoutSession.js'

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
      { exerciseId: 8, exerciseOrder: 1, sets: [
        { setNumber: 1, weight: 140.5, reps: 4, completed: true },
        { setNumber: 2, weight: 0, reps: 0, completed: false },
      ] },
      { exerciseId: 9, exerciseOrder: 2, sets: [] },
    ],
  })
})
