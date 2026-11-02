import assert from 'node:assert/strict'
import test from 'node:test'
import { bindExercises, completedVolume, countSets, groupLabel, newSetType, normalizeGroups, nextSet, REPS_INPUT, setLabel, setTypeOptions, WEIGHT_INPUT, targetSetCount, unbindGroup } from './setGroups.js'

const exercise = (id, groupNumber = null, setType = 'WORKING') => ({ exerciseId: id, groupNumber, sets: [{ setNumber: 1, setType }] })
const types = (exercises) => exercises.map((item) => item.sets[0].setType)

test('binding picks the type from group size, moves members together, and keeps special sets', () => {
  const warmup = { ...exercise(3), sets: [{ setNumber: 1, setType: 'WARMUP' }, { setNumber: 2, setType: 'WORKING' }] }
  const bound = bindExercises([exercise(1), exercise(2), warmup], [0, 2])
  assert.deepEqual(bound.map((item) => item.exerciseId), [1, 3, 2])
  assert.deepEqual(bound.map((item) => item.groupNumber), [1, 1, null])
  assert.deepEqual(bound[1].sets.map((set) => set.setType), ['WARMUP', 'SUPER'])
  assert.deepEqual(types(bindExercises([exercise(1), exercise(2), exercise(3)], [0, 1, 2])), ['TRI', 'TRI', 'TRI'])
  assert.deepEqual(types(bindExercises([exercise(1), exercise(2), exercise(3), exercise(4)], [0, 1, 2, 3])), ['GIANT', 'GIANT', 'GIANT', 'GIANT'])
})

test('removing a member shrinks the group type and dissolves a single leftover', () => {
  const tri = [exercise(1, 1, 'TRI'), exercise(2, 1, 'TRI'), exercise(3, 1, 'TRI')]
  const superSet = normalizeGroups(tri.slice(0, 2))
  assert.deepEqual(types(superSet), ['SUPER', 'SUPER'])
  const single = normalizeGroups(superSet.slice(0, 1))
  assert.equal(single[0].groupNumber, null)
  assert.deepEqual(types(single), ['WORKING'])
})

test('loaded group sets without a group fall back to working, and groups renumber in order', () => {
  assert.deepEqual(types(normalizeGroups([exercise(1, null, 'SUPER')])), ['WORKING'])
  const renumbered = normalizeGroups([exercise(1, 5), exercise(2, 5), exercise(3, 2), exercise(4, 2)])
  assert.deepEqual(renumbered.map((item) => item.groupNumber), [1, 1, 2, 2])
})

test('unbinding restores working sets but keeps other set types', () => {
  const items = [exercise(1, 1, 'SUPER'), exercise(2, 1, 'DROP'), exercise(3, 2, 'SUPER'), exercise(4, 2, 'SUPER')]
  const result = unbindGroup(items, 1)
  assert.deepEqual(types(result), ['WORKING', 'DROP', 'SUPER', 'SUPER'])
  assert.deepEqual(result.map((item) => item.groupNumber), [null, null, 1, 1])
})

test('warmups add nothing, drops add volume only, and a group round is one set', () => {
  const set = (setType, completed = true) => ({ weight: 50, reps: 10, completed, setType })
  const items = [
    { groupNumber: 1, sets: [set('WARMUP'), set('SUPER'), set('SUPER'), set('DROP')] },
    { groupNumber: 1, sets: [set('SUPER'), set('SUPER'), set('SUPER', false)] },
    { groupNumber: null, sets: [set('WORKING'), set('TOP', false)] },
  ]
  assert.equal(countSets(items, true), 3)
  assert.equal(countSets(items), 5)
  assert.equal(completedVolume(items), 3000)
  assert.equal(targetSetCount([...items, { groupNumber: null, targetSets: 4, sets: [] }]), 9)
})

test('a new set copies the previous weight and reps but takes its type from the group', () => {
  const warmup = { groupNumber: null, sets: [{ weight: 40, reps: 12, setType: 'WARMUP' }] }
  assert.deepEqual(nextSet([warmup], warmup), { weight: 40, reps: 12, setType: 'WORKING' })
  const empty = { groupNumber: null, sets: [] }
  assert.deepEqual(nextSet([empty], empty), { weight: 0, reps: 0, setType: 'WORKING' })
})

test('set inputs accept only values the server can store, including partial typing', () => {
  for (const value of ['', '0', '102.', '102.5', '9999.99']) assert.ok(WEIGHT_INPUT.test(value), value)
  for (const value of ['.', '.5', '10000', '1.234', '-1', '1e2', 'a']) assert.ok(!WEIGHT_INPUT.test(value), value)
  assert.ok(REPS_INPUT.test('12'))
  assert.ok(!REPS_INPUT.test('1.5'))
})

test('option list and labels follow the group', () => {
  const items = [exercise(1, 1, 'SUPER'), exercise(2, 1, 'SUPER'), exercise(3)]
  const keys = (target) => setTypeOptions(items, target).map(([key]) => key)
  assert.ok(keys(items[0]).includes('SUPER'))
  assert.ok(!keys(items[0]).includes('TRI'))
  assert.ok(!keys(items[2]).some((key) => ['SUPER', 'TRI', 'GIANT'].includes(key)))
  assert.equal(newSetType(items, items[0]), 'SUPER')
  assert.equal(newSetType(items, items[2]), 'WORKING')
  assert.equal(groupLabel(items, items[0]), '슈퍼 세트 A')
  assert.equal(groupLabel(items, items[2]), null)
  assert.equal(setLabel({ setNumber: 2, setType: 'DROP' }), '2세트 · 드랍')
  assert.equal(setLabel({ setNumber: 2 }), '2세트')
})
