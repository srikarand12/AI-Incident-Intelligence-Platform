import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
describe('dashboard source', () => {
 it('includes incident workflow controls', () => {
  const source = readFileSync(new URL('./App.jsx', import.meta.url), 'utf8');
  expect(source).toContain('Create incident');
  expect(source).toContain('Investigate');
  expect(source).toContain('Recommended checks');
 });
});