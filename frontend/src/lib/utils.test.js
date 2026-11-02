import assert from 'node:assert/strict'
import test from 'node:test'
import { addDays, formatRange, mondayOf, volumeChange, volumeDifference } from './utils.js'

test('period helpers cross month boundaries and start weeks on Monday', () => {
  assert.equal(addDays('2026-10-30', 5), '2026-11-04')
  assert.equal(addDays('2026-03-01', -1), '2026-02-28')
  assert.equal(mondayOf('2026-11-01'), '2026-10-26')
  assert.equal(mondayOf('2026-11-02'), '2026-11-02')
  assert.equal(formatRange('2026-10-21', '2026-10-25'), '10.21–10.25')
  assert.equal(formatRange('2026-10-21', '2026-10-21'), '10.21')
})

test('weekly volume change handles increases, decreases, and zero baselines', () => {
  assert.equal(volumeChange(500, 300), '+66.7%')
  assert.equal(volumeChange(0, 300), '-100%')
  assert.equal(volumeChange(100, 0), '신규')
  assert.equal(volumeChange(0, 0), '—')
})

test('weekly volume difference formats increases, decreases, unchanged values, and empty weeks', () => {
  assert.equal(volumeDifference(1000, 300), '+700 kg')
  assert.equal(volumeDifference(600, 1000), '-400 kg')
  assert.equal(volumeDifference(500, 500), '0 kg')
  assert.equal(volumeDifference(0, 0), '—')
})
