const SET_TYPES = [
  ['WORKING', '워킹'],
  ['WARMUP', '웜업'],
  ['SUPER', '슈퍼'],
  ['TRI', '트라이'],
  ['GIANT', '자이언트'],
  ['DROP', '드랍'],
  ['TOP', '탑'],
  ['FAILURE', '실패'],
  ['ASSIST', '보조'],
  ['BACKOFF', '백오프'],
]

const GROUP_TYPES = ['SUPER', 'TRI', 'GIANT']

export function completedVolume(exercises) {
  return exercises.flatMap((exercise) => exercise.sets).filter((set) => set.completed && set.setType !== 'WARMUP').reduce((sum, set) => sum + Number(set.weight) * Number(set.reps), 0)
}

// 웜업·드랍은 세지 않고, 슈퍼·트라이·자이언트는 묶음의 한 라운드를 한 세트로 센다. 백엔드 StatisticsService.Totals와 규칙이 같다.
export function countSets(exercises, onlyCompleted = false) {
  const groupRounds = new Map()
  let count = 0
  for (const exercise of exercises) {
    const sets = exercise.sets.filter((set) => (!onlyCompleted || set.completed) && set.setType !== 'WARMUP' && set.setType !== 'DROP')
    const rounds = exercise.groupNumber == null ? 0 : sets.filter((set) => GROUP_TYPES.includes(set.setType)).length
    count += sets.length - rounds
    groupRounds.set(exercise.groupNumber, Math.max(groupRounds.get(exercise.groupNumber) || 0, rounds))
  }
  return count + [...groupRounds.values()].reduce((sum, rounds) => sum + rounds, 0)
}

// 세트 없이 목표 세트 수만 가진 예전 루틴 운동은 목표 값을 그대로 쓴다.
export function targetSetCount(exercises) {
  return countSets(exercises.filter((exercise) => exercise.sets?.length)) + exercises.reduce((sum, exercise) => sum + (exercise.sets?.length ? 0 : exercise.targetSets || 0), 0)
}

export function setTypeLabel(value) {
  return SET_TYPES.find(([key]) => key === value)?.[1] || '워킹'
}

export function setLabel(set) {
  const type = set.setType || 'WORKING'
  return type === 'WORKING' ? `${set.setNumber}세트` : `${set.setNumber}세트 · ${setTypeLabel(type)}`
}

export function groupTypeForSize(size) {
  if (size < 2) return null
  return size === 2 ? 'SUPER' : size === 3 ? 'TRI' : 'GIANT'
}

function groupSize(exercises, groupNumber) {
  return groupNumber == null ? 0 : exercises.filter((exercise) => exercise.groupNumber === groupNumber).length
}

// 운동 삭제·이동 뒤에도 묶음 번호와 세트 유형이 서버 검증 규칙을 만족하도록 맞춘다.
export function normalizeGroups(exercises) {
  const numbers = new Map()
  return exercises.map((exercise) => {
    const size = groupSize(exercises, exercise.groupNumber)
    const groupType = groupTypeForSize(size)
    if (groupType && !numbers.has(exercise.groupNumber)) numbers.set(exercise.groupNumber, numbers.size + 1)
    return {
      ...exercise,
      groupNumber: groupType ? numbers.get(exercise.groupNumber) : null,
      sets: exercise.sets.map((set) => GROUP_TYPES.includes(set.setType) ? { ...set, setType: groupType || 'WORKING' } : set),
    }
  })
}

export function bindExercises(exercises, indexes) {
  const selected = [...indexes].sort((a, b) => a - b)
  const groupNumber = Math.max(0, ...exercises.map((exercise) => exercise.groupNumber || 0)) + 1
  const groupType = groupTypeForSize(selected.length)
  const members = selected.map((index) => ({
    ...exercises[index],
    groupNumber,
    sets: exercises[index].sets.map((set) => (set.setType || 'WORKING') === 'WORKING' ? { ...set, setType: groupType } : set),
  }))
  const rest = exercises.flatMap((exercise, index) => selected[0] === index ? members : selected.includes(index) ? [] : [exercise])
  return normalizeGroups(rest)
}

export function unbindGroup(exercises, groupNumber) {
  return normalizeGroups(exercises.map((exercise) => exercise.groupNumber === groupNumber ? { ...exercise, groupNumber: null } : exercise))
}

export const WEIGHT_INPUT = /^(\d{1,4}(\.\d{0,2})?)?$/
export const REPS_INPUT = /^\d{0,5}$/

export function nextSet(exercises, exercise) {
  const previous = exercise.sets.at(-1)
  return { weight: previous?.weight ?? 0, reps: previous?.reps ?? 0, setType: newSetType(exercises, exercise) }
}

export function newSetType(exercises, exercise) {
  return groupTypeForSize(groupSize(exercises, exercise.groupNumber)) || 'WORKING'
}

export function setTypeOptions(exercises, exercise) {
  const groupType = groupTypeForSize(groupSize(exercises, exercise.groupNumber))
  return SET_TYPES.filter(([key]) => !GROUP_TYPES.includes(key) || key === groupType)
}

export function groupLabel(exercises, exercise) {
  const groupType = groupTypeForSize(groupSize(exercises, exercise.groupNumber))
  return groupType ? `${setTypeLabel(groupType)} 세트 ${String.fromCharCode(64 + exercise.groupNumber)}` : null
}
