#!/usr/bin/env node
// titan-tap.mjs - a tap on the Titan that lands only in KARTUN: reads the focused window first and refuses (exit 2) when
// it is not ours, so a script never taps the owner's launcher or his apps (EXPERIENCE EXP-0024: blind taps at fixed
// coordinates opened the owner's NDim three times). [TESTED: 2026-10-15 · Titan: refused on the launcher (exit 2), tapped in KARTUN]
//
// Usage: KARTUN_SERIAL=<ip:port> node tools/titan-tap.mjs <x> <y>
import { spawnSync } from 'node:child_process';

const ADB = 'C:/Android/Sdk/platform-tools/adb.exe';
const serial = process.env.KARTUN_SERIAL || '192.168.1.3:5555';
const [x, y] = process.argv.slice(2);
if (!/^\d+$/.test(x || '') || !/^\d+$/.test(y || '')) { console.error('usage: titan-tap.mjs <x> <y>'); process.exit(1); }
const adb = (...a) => spawnSync(ADB, ['-s', serial, ...a], { encoding: 'utf8' });
const focus = (adb('shell', 'dumpsys window | grep mCurrentFocus').stdout || '').trim();
if (!focus.includes('com.kriniks.kartun')) {
  console.error(`refused: focus is not KARTUN - ${focus || 'unknown'}`);
  process.exit(2);
}
const r = adb('shell', 'input', 'tap', x, y);
process.exit(r.status ?? 1);
