import assert from 'node:assert/strict'
import test from 'node:test'
import { query } from './api.js'

test('query skips empty values and prefixes with ?', () => {
  assert.equal(query({ from: '2026-01-01', to: '', page: undefined, size: null }), '?from=2026-01-01')
  assert.equal(query({}), '')
})
