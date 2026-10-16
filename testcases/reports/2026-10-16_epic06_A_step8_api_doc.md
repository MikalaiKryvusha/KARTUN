# Run report — 2026-10-16 · epic 06, phase A, step 8: `docs/agent-api.md` and the README line

## Work
`plans/07_epic06_A_adb_api.md` step 8 — the API document for agents (criterion A8 of `plans/06` §2: the doc describes the
commands, the JSON fields, the states and the error codes; every command in it, copy-pasted, works on the Titan) and one
line in the README features list, EN and RU.

## Contour
HEADWOLF Titan 1 at `100.99.111.48:5555`, the debug APK of the steps 4–6 commit; the CLI of step 7; Git Bash on the PC.

## Runs
1. 12:32 — the doc's «Raw adb protocol» block as a script (`<scratchpad>/a8-raw.sh`): `<pkg>` = `com.kriniks.kartun.debug`,
   `<job>` = `a8rawdoc01`, everything else as written — push, UPSCALE, poll, pull, `rm -f "<dir>/<job>.*"`, CANCEL, INFO.
2. 12:33 — the pull again into the current folder (`a8rawdoc02`; run 1's pull had a harness path, see Found).
3. 12:33 — the CLI commands of the doc not yet run in step 7: `status <job>` after `upscale … --keep`, `status nosuchjob1`.
4. 12:34 — README: `node .kaif/tools/kaif-voice-lint.mjs load`; the two items written; `check README.md --genre document`;
   an independent clean-instance pass (§7B) by a subagent with the portrait; its 6 hits fixed; `check` again.

## Checks
- Hygiene: `kaif-voice-lint check README.md` — 1 hit, line 256 «Что здесь делают» (a table header from 2026-10-14, the
  owner-accepted README; not touched); the new lines: 0 hits.
- Functional run: the doc's commands on the Titan, outputs read.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | raw block: submit and poll | pass | `"state":"succeeded"`, 1024 × 1024, `elapsed_ms 4181` |
| 2 | raw block: pull into the current folder | pass | `a8rawdoc02.out.png PNG 1024x1024` |
| 3 | raw block: clean-up, CANCEL, INFO | pass | the folder empty after `rm -f "<dir>/<job>.*"`; CANCEL of a finished job answers nothing and harms nothing; `info.json` written |
| 4 | CLI `status` | pass | `{"ok":true,…"state":"succeeded"…}` exit 0; unknown job → `unknown_job`, exit 1 (added to the doc's error list) |
| 5 | the CLI commands of the quick start (`info`, `upscale photo.jpg --scale 4`) | pass | run in step 7 (`testcases/reports/2026-10-16_epic06_A_step7_cli.md`, cases 1–2) |
| 6 | README items by the portrait | pass | §7B hits fixed: «picture» → «photo», «answers» → «responses», «with KARTUN closed» → «even while KARTUN is closed»; RU «результат на компьютер», a new sentence for «KARTUN при этом может быть закрыт», «JSON-ответы»; one borderline hit (expand «adb») not taken — the line's reader runs agents |

## Found
1. Run 1's `adb pull <dir>/<job>.out.png /d/work/…` failed: with `MSYS_NO_PATHCONV=1` the Git-Bash path `/d/…` reaches
   Windows adb unconverted — a harness slip, not the doc's; the doc's form (pull into the current folder) passed in run 2.
2. The speed line of the first draft («about 4 s per input megapixel») had no measurement behind it — replaced before the
   commit by the measured 3.7–5.5 s for 256 × 256 on the GPU, 39 s on the CPU, and the catalogue's 65 s/MP estimate.

## Traces
Outputs quoted above; `a8rawdoc02.out.png` in the session scratchpad; the device folder emptied.

## Verdict
pass — A8 observed: every command of the doc ran on the Titan. The README text waits for the owner's proofreading (the
storefront rule).
