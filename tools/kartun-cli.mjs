#!/usr/bin/env node
// kartun-cli.mjs - the PC side of the KARTUN agent API (plans/06 §4.5, plans/07 step 7): an AI agent on the PC upscales a
// picture on an Android device with KARTUN over adb and gets the file back plus ONE JSON line on stdout.
// The device side is the app's AgentService (the protocol block of plans/07, docs/agent-api.md). Node built-ins only;
// every adb call is spawnSync(ADB, argv) with no shell, so device paths never pass through Git Bash (EXP-0002); the CLI
// never starts an activity - the owner's screen stays as it is (EXP-0010). [TESTED: 2026-10-16 · Titan: A1-A6, A9 -
// testcases/reports/2026-10-16_epic06_A_step7_cli.md; not yet: A10 airplane mode, Ctrl+C]
//
// Usage: node tools/kartun-cli.mjs <command> [args] [--serial <ip:port>] [--pkg <package>]
//   info                      device, app, GPU, models, limits
//   models                    the model list (installed ones marked)
//   upscale <file…>           [--model id|default] [--scale 1|2|4|8|16] [--format png|jpg|webp] [--quality N]
//                             [--gpu | --cpu] [--out path|dir] [--timeout s] [--keep]
//   status <job>              the job's status file
//   cancel <job>              cancel a queued or running job
// Exit codes: 0 ok · 1 the job failed or was canceled · 2 bad arguments · 3 no device, no adb, timeout.

import { spawnSync } from 'node:child_process';
import { existsSync, statSync } from 'node:fs';
import { basename, dirname, extname, join, resolve } from 'node:path';
import { randomBytes } from 'node:crypto';

const HELP = `kartun-cli - upscale pictures on an Android device with KARTUN, over adb.

  node tools/kartun-cli.mjs info
  node tools/kartun-cli.mjs models
  node tools/kartun-cli.mjs upscale <file...> [--model id|default] [--scale 1|2|4|8|16] [--format png|jpg|webp]
                                     [--quality 1-100] [--gpu | --cpu] [--out path|dir] [--timeout seconds] [--keep]
  node tools/kartun-cli.mjs status <job>
  node tools/kartun-cli.mjs cancel <job>

Options for every command: --serial <ip:port> (or KARTUN_SERIAL), --pkg <package> (default: the installed KARTUN,
release first). adb: KARTUN_ADB, else C:/Android/Sdk/platform-tools/adb.exe, else adb on PATH.
stdout: one JSON line per result ("ok": true|false). Progress goes to stderr.
Exit codes: 0 ok, 1 job failed or canceled, 2 bad arguments, 3 no device / no adb / timeout.`;

const POLL_MS = 500;
const INFO_WAIT_MS = 10_000;
const DEFAULT_TIMEOUT_S = 3600;
const PACKAGES = ['com.kriniks.kartun', 'com.kriniks.kartun.debug'];
const SERVICE = 'com.kriniks.kartun.agent.AgentService';
const ACTION = (a) => `com.kriniks.kartun.agent.${a}`;

// ---- arguments -----------------------------------------------------------------------------------------------------
const FLAGS_WITH_VALUE = new Set(['serial', 'pkg', 'model', 'scale', 'format', 'quality', 'out', 'timeout']);
function parseArgs(argv) {
  const pos = [], opt = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '-h' || a === '--help') { opt.help = true; continue; }
    if (a.startsWith('--')) {
      const name = a.slice(2);
      if (FLAGS_WITH_VALUE.has(name)) {
        if (i + 1 >= argv.length) usage(`--${name} needs a value`);
        opt[name] = argv[++i];
      } else opt[name] = true;
    } else pos.push(a);
  }
  return { cmd: pos[0], args: pos.slice(1), opt };
}

function out(obj) { process.stdout.write(JSON.stringify(obj) + '\n'); }
function usage(message) { out({ ok: false, error: { code: 'bad_arguments', message } }); process.stderr.write(HELP + '\n'); process.exit(2); }
function fatal(code, message) { out({ ok: false, error: { code, message } }); process.exit(3); }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---- adb -----------------------------------------------------------------------------------------------------------
const { cmd, args, opt } = parseArgs(process.argv.slice(2));
if (opt.help || !cmd) { process.stdout.write(HELP + '\n'); process.exit(cmd || opt.help ? 0 : 2); }

const ADB = process.env.KARTUN_ADB || (existsSync('C:/Android/Sdk/platform-tools/adb.exe') ? 'C:/Android/Sdk/platform-tools/adb.exe' : 'adb');
const SERIAL = opt.serial || process.env.KARTUN_SERIAL || '';

/** One adb call, argv as a list; `binary` keeps stdout as a Buffer. */
function adb(argv, binary = false) {
  const full = SERIAL ? ['-s', SERIAL, ...argv] : argv;
  const r = spawnSync(ADB, full, { encoding: binary ? null : 'utf8', maxBuffer: 64 * 1024 * 1024 });
  if (r.error?.code === 'ENOENT') fatal('adb_missing', `adb not found (${ADB}); set KARTUN_ADB`);
  return r;
}

function device() {
  const r = adb(['get-state']);
  if (r.status !== 0 || String(r.stdout).trim() !== 'device') {
    fatal('no_device', `no device${SERIAL ? ` at ${SERIAL}` : ''}: ${String(r.stderr || r.stdout).trim() || 'adb get-state failed'}`);
  }
}

function packageName() {
  if (opt.pkg) return opt.pkg;
  const listed = String(adb(['shell', 'pm', 'list', 'packages', 'com.kriniks.kartun']).stdout).split(/\r?\n/).map((l) => l.replace('package:', '').trim());
  const pkg = PACKAGES.find((p) => listed.includes(p));
  if (!pkg) fatal('no_device', 'KARTUN is not installed on the device');
  return pkg;
}

device();
const PKG = packageName();
const DIR = `/sdcard/Android/data/${PKG}/files/agent`;

/** Starts the agent service with an action and extras (`--es`/`--ei`/`--ez` triples). Never an activity. */
function send(action, extras = []) {
  const r = adb(['shell', 'am', 'start-foreground-service', '-n', `${PKG}/${SERVICE}`, '-a', ACTION(action), ...extras]);
  const text = `${r.stdout}${r.stderr}`;
  if (r.status !== 0 || /Error|Exception/.test(text)) fatal('no_device', `the agent service did not start: ${text.trim()}`);
}

/** The parsed JSON of a file in the agent folder, or null while it is not there (or half-written - never, the app renames). */
function readJson(name) {
  const r = adb(['exec-out', 'cat', `${DIR}/${name}`]);
  if (r.status !== 0 || !String(r.stdout).trim().startsWith('{')) return null;
  try { return JSON.parse(r.stdout); } catch { return null; }
}

// ---- commands ------------------------------------------------------------------------------------------------------
async function info() {
  adb(['shell', 'rm', '-f', `${DIR}/info.json`]);
  send('INFO');
  for (let t = 0; t < INFO_WAIT_MS; t += POLL_MS) {
    const j = readJson('info.json');
    // R1: the answer is read, the file leaves the device.
    if (j) { adb(['shell', 'rm', '-f', `${DIR}/info.json`]); return j; }
    await sleep(POLL_MS);
  }
  fatal('timeout', `no info.json after ${INFO_WAIT_MS / 1000} s`);
}

function newJob() { return `k${Date.now().toString(36)}${randomBytes(3).toString('hex')}`; }

function outputPath(input, scale, format) {
  const name = `${basename(input, extname(input))}_x${scale}.${format}`;
  if (!opt.out) return join(dirname(resolve(input)), name);
  const o = resolve(opt.out);
  const isDir = (existsSync(o) && statSync(o).isDirectory()) || /[\\/]$/.test(opt.out) || args.length > 1;
  return isDir ? join(o, name) : o;
}

let current = null; // the job in flight, for Ctrl+C

async function upscaleOne(file, settings) {
  if (!existsSync(file) || !statSync(file).isFile()) return { ok: false, file, error: { code: 'input_missing', message: `no file ${file}` } };
  const job = newJob();
  const ext = (extname(file).slice(1) || 'img').toLowerCase();
  const input = `${job}.in.${ext}`;
  adb(['shell', 'mkdir', '-p', DIR]);
  const pushed = adb(['push', file, `${DIR}/${input}`]);
  if (pushed.status !== 0) fatal('no_device', `push failed: ${String(pushed.stderr).trim()}`);
  const extras = ['--es', 'job', job, '--es', 'in', input, '--es', 'model', settings.model, '--ei', 'scale', String(settings.scale),
    '--es', 'format', settings.format, '--ei', 'quality', String(settings.quality)];
  if (settings.gpu !== null) extras.push('--ez', 'gpu', String(settings.gpu));
  current = job;
  send('UPSCALE', extras);
  // The id goes to stderr at once, so another shell can `cancel <job>` while this one waits.
  process.stderr.write(`job ${job} · ${basename(file)} · ${settings.scale}x ${settings.format}\n`);
  const deadline = Date.now() + settings.timeout * 1000;
  let s = null;
  let shown = '';
  while (true) {
    s = readJson(`${job}.json`);
    if (s && ['succeeded', 'failed', 'canceled'].includes(s.state)) break;
    if (s && s.state === 'running') {
      const pct = Math.round((s.progress || 0) * 100);
      const line = `${basename(file)}: ${pct}% · tiles ${s.tiles_done}/${s.tiles_total} · ${(s.elapsed_ms / 1000).toFixed(1)} s` +
        (s.estimate_ms ? ` of ≈ ${Math.round(s.estimate_ms / 1000)} s` : '') + (s.gpu ? ' · GPU' : ' · CPU');
      if (line !== shown) { process.stderr.write(`\r${line}   `); shown = line; }
    }
    if (Date.now() > deadline) {
      send('CANCEL', ['--es', 'job', job]);
      current = null;
      return { ok: false, job, file, error: { code: 'timeout', message: `no result after ${settings.timeout} s; the job was canceled` } };
    }
    await sleep(POLL_MS);
  }
  process.stderr.write('\n');
  current = null;
  let result = { ok: s.state === 'succeeded', file, ...s };
  if (s.state === 'succeeded') {
    const dest = outputPath(file, settings.scale, settings.format);
    const pulled = adb(['pull', `${DIR}/${s.out.file}`, dest]);
    if (pulled.status !== 0) result = { ...result, ok: false, error: { code: 'internal', message: `pull failed: ${String(pulled.stderr).trim()}` } };
    else result.output = dest;
  }
  // R1: nothing of the job stays on the device unless --keep.
  if (!opt.keep) adb(['shell', `rm -f ${DIR}/${job}.*`]);
  return result;
}

async function upscale() {
  if (args.length === 0) usage('upscale needs at least one file');
  const format = (opt.format || 'png').toLowerCase().replace('jpeg', 'jpg');
  if (!['png', 'jpg', 'webp'].includes(format)) usage('--format must be png, jpg or webp');
  const scale = Number(opt.scale ?? 4);
  if (![1, 2, 4, 8, 16].includes(scale)) usage('--scale must be 1, 2, 4, 8 or 16');
  const quality = Number(opt.quality ?? 92);
  if (!Number.isInteger(quality) || quality < 1 || quality > 100) usage('--quality must be 1..100');
  const timeout = Number(opt.timeout ?? DEFAULT_TIMEOUT_S);
  if (!(timeout > 0)) usage('--timeout must be a number of seconds');
  if (opt.gpu && opt.cpu) usage('--gpu and --cpu exclude each other');
  const settings = { model: opt.model || 'default', scale, format, quality, timeout, gpu: opt.gpu ? true : opt.cpu ? false : null };
  // Ctrl+C: the running job is canceled on the device before the CLI leaves.
  process.on('SIGINT', () => {
    if (current) send('CANCEL', ['--es', 'job', current]);
    out({ ok: false, job: current, state: 'canceled', error: { code: 'canceled', message: 'interrupted' } });
    process.exit(1);
  });
  let failed = false;
  for (const f of args) {
    const r = await upscaleOne(f, settings);
    out(r);
    if (!r.ok) failed = true;
    if (r.error?.code === 'timeout') process.exit(3);
  }
  process.exit(failed ? 1 : 0);
}

async function main() {
  switch (cmd) {
    case 'info': { const j = await info(); out({ ok: true, ...j }); break; }
    case 'models': { const j = await info(); out({ ok: true, models: j.models }); break; }
    case 'upscale': await upscale(); break;
    case 'status': {
      if (!args[0]) usage('status needs a job id');
      const s = readJson(`${args[0]}.json`);
      if (!s) { out({ ok: false, job: args[0], error: { code: 'unknown_job', message: 'no status file for this job' } }); process.exit(1); }
      out({ ok: s.state !== 'failed', ...s });
      break;
    }
    case 'cancel': {
      if (!args[0]) usage('cancel needs a job id');
      send('CANCEL', ['--es', 'job', args[0]]);
      for (let t = 0; t < INFO_WAIT_MS; t += POLL_MS) {
        const s = readJson(`${args[0]}.json`);
        if (s && ['canceled', 'succeeded', 'failed'].includes(s.state)) { out({ ok: s.state === 'canceled', ...s }); process.exit(s.state === 'canceled' ? 0 : 1); }
        await sleep(POLL_MS);
      }
      fatal('timeout', 'the job did not stop within 10 s');
      break;
    }
    default: usage(`unknown command '${cmd}'`);
  }
}

main();
