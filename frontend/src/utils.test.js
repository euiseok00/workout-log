import assert from 'node:assert/strict'
import test from 'node:test'
import { volumeChange } from './utils.js'

test('weekly volume change handles increases, decreases, and zero baselines', () => {
  assert.equal(volumeChange(500, 300), '+66.7%')
  assert.equal(volumeChange(0, 300), '-100%')
  assert.equal(volumeChange(100, 0), '신규')
  assert.equal(volumeChange(0, 0), '—')
})
