# KARTUN — KAIF 2.8 install report (pre-release 2.9 build a63c651356bc)

**Delivered upstream:** https://github.com/MikalaiKryvusha/KAIF/issues/143

Fresh install into a greenfield project (only the owner's `GOAL.md`, 29 059 bytes) on Windows 11, Claude Code (VS Code
extension), model Claude Opus 5.5. Source pinned to the origin commit of the 2.9 work in progress:
`--source https://raw.githubusercontent.com/MikalaiKryvusha/KAIF/610b18abac0739f6e5de539f67694d32e1c3c4b0/dist`
(origin `main` = local `D:\work\ai_sandbox\KAIF` HEAD `610b18a`, checked with `git ls-remote`).

## 1. Chronology with numbers

- 13:16 (+03:00) target is not a git repo → `git init -b main` by the agent (the bootstrap ends with a commit).
- 13:17:19 `KAIF-LOADER.mjs` written verbatim from `KAIF.md` (extracted by a script, 9 386 bytes); 13:17:25 core in
  place; loader exit 0. Printed: "PRE-RELEASE of KAIF 2.9 … version stays 2.8 … recorded as "prerelease": "2.9"".
- Deployed: 37 skills (`find .claude/skills -name SKILL.md | wc -l` → 37), 5 agent systems, `KAIF_ADAPTATION_TASK.md` with 11 items.
- Owner's original `GOAL.md` committed verbatim first (`b47a812`), untouched by the installer (size 29 059 before and after).
- `project-name KARTUN`, `sphere programming`, `wire-hooks` → "6 of 6 KAIF hooks added; a new file".
- `owner-voice`: owner chose the public snapshot (copied from the KAIF repo root, md5 `97b65d32…`), gitignored in the same step.
- `checkpoint placeholders` → "re-synced 175 system skill copies from the canon"; scan clean.
- 8 of 11 checkpoints recorded by 13:40; owner-facing texts (`MASTER_PLAN.md`, epic plan, interview) written after
  `kaif-voice-lint load`; `kaif-voice-lint check` → 0 findings in 3 files, 282 lines.
- 303 files created or changed in the tree since `GOAL.md` (`find . -newer GOAL.md`, excluding `.git`).

## 2. Friction and rakes

1. **Non-git target.** The bootstrap's last step is `Commit chore: deploy KAIF`, but nothing in `KAIF.md` or the loader
   says what to do when the project is not a git repository. The agent ran `git init` on its own. Suggest one line in
   `KAIF.md` step 3 (or a loader notice): "no `.git` → `git init` first (the owner's originals are committed before the deploy)".
2. **Refresh-timer hook right after install.** `prompt-refresh-timer.mjs` fired on the first owner message after
   `wire-hooks` ("no refresh witness found this session, interval 60 min") and repeated on every mid-turn owner message
   until a full nine-document re-read — although the session had read the whole canon ~15 minutes earlier while adapting.
   A fresh install has no `.kaif/refresh-marker.json` by construction. Suggest: the adaptation task's `study` item (or
   `verify-final`) stamps the marker from the agent's actual reads, so the first hour after install does not demand a
   re-read the agent just did.
3. **`kaif-voice-lint load` output size.** ~58.6 KB printed to stdout for the public snapshot; a client that truncates
   tool output by bytes (`head -c`) cuts a UTF-8 sequence and the loaded text ends in U+FFFD. Self-inflicted (the agent
   piped through `head -c`), but a `--out <file>` option would make the load witness safe for clients with output caps.
4. **Mid-turn owner messages + Stop hook.** The owner sent 12 messages mid-turn in ~30 minutes; `stop-owner-answer.mjs`
   correctly forced a separate answer for each (one earlier message had been answered only between tool calls). Worked
   as designed; noted because the cost is one extra turn per message.

## 3. What confused a cold agent (top 3)

1. Version label: `KAIF.md` header says "v2.8" while the pinned commit is the 2.9 work; only the loader's PRE-RELEASE
   line resolves it. Clear once printed.
2. The `owner-voice` item says "a PUBLIC repository takes the public snapshot … add it to .gitignore" — two editions
   exist on the owner's machine (`krinik_voice/AUTHOR_STYLOMETRY.md` private core and the generated public snapshot in
   the KAIF repo root); which file IS the public snapshot had to be found by comparing first lines across sibling
   deployments (KAST uses the snapshot, KAGO the private core).
3. Where to deliver this report on a pre-release install (the origin issue tracker vs local only) — resolved by the
   standing authorization text in `reports/README.md`.

## 4. Final state and judge verdict

- Checkpoints at the time of writing: 8 of 11 recorded (`grep -c "KAIF-ADAPT" KAIF_ADAPTATION_TASK.md` → 8); remaining
  `goal-plan`, `field-report`, `verify` are closed after this report.
- Judge pass: self-audit against the claims above — every number is a command output quoted in §1; no separate
  `/fable-judge` subagent run yet (the owner switched the session to another task mid-install; the verdict line will be
  appended after `verify-final`).

## 5. Judge verdict (appended 2026-10-14 14:34 +0300)

- `/fable-judge` pass by a separate subagent over this session's claims: the install itself — **VERIFIED WITH CAVEATS**
  (`gh issue view 143` OPEN; upstream `kaif-manifest.json` at `610b18a` → sourceTree `a63c651356bc`, prerelease 2.9;
  `git hash-object .kaif/kaif-core.mjs` = upstream blob `967c670f…`; installer files absent and never committed;
  caveat: `verify-final` self-cleans, so its pass is evidenced only indirectly — `kaif-core check` → "manifest
  satisfied: 104 files + 152 agent artifacts").
- The same pass REFUTED other session claims (stale status lines, a cited run report missing runs, a voice-lint miss);
  all were corrected in the project before this append. No new KAIF defect found by the judge.
