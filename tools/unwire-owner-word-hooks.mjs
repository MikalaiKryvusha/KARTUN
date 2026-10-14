// Unwires the two KAIF "owner's word mid-turn" hooks from this project's Claude Code settings:
//   .kaif/hooks/pretool-owner-word.mjs  (PreToolUse - refuses a tool call after each owner message)
//   .kaif/hooks/stop-owner-answer.mjs   (Stop - demands the «⏩ <next step>» line and resumes the turn)
// Every other hook stays wired. A backup of the settings file is written next to it first.
// Run by the owner (the agent may not edit its own hook settings): node tools/unwire-owner-word-hooks.mjs
// Owner's order 2026-10-14 ≈ 20:38 +03:00: «Убрать это!!!! ИЗ КАИФ НАХУЙ УБРАТ!»; KAIF ticket MikalaiKryvusha/KAIF#144.
import { readFileSync, writeFileSync, copyFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const settingsPath = join(root, '.claude', 'settings.json');
const UNWANTED = ['pretool-owner-word.mjs', 'stop-owner-answer.mjs'];

const settings = JSON.parse(readFileSync(settingsPath, 'utf8'));
const hooks = settings.hooks || {};

// A hook entry's script path sits in `args` (current wiring) or in `command` (older wiring).
const isUnwanted = (h) => UNWANTED.some((name) => [h.command, ...(h.args || [])].join(' ').includes(name));

const removed = [];
for (const event of Object.keys(hooks)) {
  for (const group of hooks[event]) {
    const kept = (group.hooks || []).filter((h) => {
      if (!isUnwanted(h)) return true;
      removed.push(`${event}: ${[h.command, ...(h.args || [])].join(' ')}`);
      return false;
    });
    group.hooks = kept;
  }
  hooks[event] = hooks[event].filter((group) => group.hooks.length > 0);
  if (hooks[event].length === 0) delete hooks[event];
}

if (removed.length === 0) {
  console.log('Nothing to remove: neither hook is wired in .claude/settings.json.');
  process.exit(0);
}

const backup = `${settingsPath}.bak`;
copyFileSync(settingsPath, backup);
writeFileSync(settingsPath, JSON.stringify(settings, null, 2) + '\n');

console.log('Removed:');
for (const line of removed) console.log('  - ' + line);
console.log('Still wired:');
for (const [event, groups] of Object.entries(hooks)) {
  for (const group of groups) for (const h of group.hooks) console.log(`  + ${event}: ${(h.args || [h.command]).join(' ')}`);
}
console.log(`Backup: ${backup}`);
console.log('Restart the Claude Code session (or reload the window) so the change takes effect.');
