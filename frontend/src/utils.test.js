import assert from 'node:assert/strict'
import test from 'node:test'
import { volumeChange, volumeDifference } from './utils.js'

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
