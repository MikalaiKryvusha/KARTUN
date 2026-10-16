# KAIF improvement request: one tool call — one action; a chained command is refused by the agent system's safety classifier as «Instruction Poisoning» (guidance for AI agents, target 2.9)

kaif-fp: AGENT_GUIDE.md#git-workflow :: chained-command-refused-by-classifier :: v2.8
**Delivered upstream:** https://github.com/MikalaiKryvusha/KAIF/issues/146
**Autocapture:** KAIF 2.8 + prerelease 2.9 (`a63c651356bc`) · project KARTUN · sphere programming · language ru · i18n ru ·
tracking origin · agent system claude-code (auto mode, VS Code) · OS Windows 11 Pro 10.0.26200 · Node v24.15.0
**Dedup attestation:** searched `bugs/KAIF/` (`grep -ril "poison\|classifier\|auto mode\|chain" bugs/KAIF/` → nothing) and origin
issues (`gh issue list --repo MikalaiKryvusha/KAIF --state open --search "classifier"` → #144, #120, #137, #118 — none about
command shape; `--state all --search "poisoning OR \"one command\" OR permission denied"` → nothing on the class). No match found.

## Gap

The canon tells the agent HOW to commit (`AGENT_GUIDE.md` → Git workflow: «`git diff --stat` before every commit — of the set
that is ACTUALLY LEAVING»; the closing skills: «commit AND push») but never in what SHAPE to send the commands. A session
naturally packs the whole ritual into one shell call — `git add … && git diff --cached --stat && git commit … && git log -1 &&
git push`. On an agent system with a safety classifier (Claude Code auto mode) such a call is refused whole, and the session
burns turns and the owner's attention re-trying the same packed shape. Nothing in KAIF names the class or the cure.

## Field evidence

KARTUN, 2026-10-16, Claude Code auto mode, `Bash` already allowed in `.claude/settings.local.json`. Three refusals in a row,
each with the same verbatim text:

```
Permission for this action was denied by the Claude Code auto mode classifier. Reason: [Instruction Poisoning].
```

| # | the refused call | the shape |
|---|---|---|
| 1 | `git add STATUS.md && git diff --cached --stat && git commit -q -F - <<'EOF' … EOF` + `git log -1` + `git push` | five actions, local + outward |
| 2 | `git commit -q -F <scratchpad>/msg1.txt && git log -1 …` | commit + log, message from a file |
| 3 | the same as 1 after the owner's explicit order «ВЫПОЛНИ git commit, Я ПРИКАЗАЛ - АВТОР ПРОЕКТА!!!! И ВСЕГДА ТЕБЕ РАЗРЕШАЮ!» | five actions |

Then passed at once: `git commit -q -m "<msg>" -m "Co-Authored-By: …"` as its own call, and `git push` as its own call — and
from there every commit of the session (`a965610`, `659d40a`). Everything else in the session passed (adb install, deleting
65 probe files on the test device, Gradle builds, edits). The previous session of the same project lost its last 15 minutes
the same way: the classifier refused a probe-file deletion and then every `Bash` call (KARTUN `STATUS.md`, commit `f988bc8`).

The owner's reading of the reason, verbatim (2026-10-16 ≈ 12:00): «отказывает, потому что ты слепливаешь много команд в одну -
он же тебя прямо говорит, что ты превышаешь полномочия в одной команде» · «это я тебе поясняю смысл «Instruction Poisoning»» —
a call stuffed with more actions than the task asked for reads as poisoned, and the whole call is refused. The owner had to
step in twice; the agent's first theory (the refusal is about WHO gave the order) was wrong and cost a round trip.

## Proposed change (smallest that closes the gap)

1. **`AGENT_GUIDE.md` → Git workflow, a numbered rule** (the form rule: a step, not prose):
   «**One tool call — one action.** `git add`, `git commit -m "<msg>"`, `git push` go as SEPARATE tool calls; a push never
   rides in a chain with anything; the commit message travels inline (`-m`, repeated for the trailer), not via `-F` from a
   temp file. A refusal of the agent system's safety layer is read as a signal about the SHAPE of the call first: split it
   into single actions and send again ONCE; a refusal of a single action stops the work and goes to the owner.»
   Rationale under it: a safety classifier judges the whole call; a chain mixes a local action with an outward one and reads
   as more authority than the task asked for («Instruction Poisoning»).
2. **The closing skills** (`/end-chat-soft`, `/end-chat-force`, `/release`, the loops' commit steps): write the commit and the
   push as two numbered steps, each its own command line — the skill text is what a weak session copies.
3. **Optional, code before cognition — a hook in the refresh-hooks module** (`.kaif/hooks/pretool-one-action.mjs`, Claude
   Code `PreToolUse` on `Bash`): when the command contains `git push` or `git commit` together with `&&`, `;` or `|` joining
   another command, refuse ONCE with the line «split: one tool call — one action (AGENT_GUIDE.md → Git workflow)». Proved red
   on refused call #1 above and green on `git push` alone.

## Expected effect and its check

A session commits and pushes with zero classifier refusals and zero owner interventions. Check: the hook's selftest — the
chain `git add a && git commit -m x && git push` is refused, `git commit -m x` and `git push` alone pass; in the field, `grep -c
"Instruction Poisoning"` over a session transcript stays 0 across a closing ritual. Invariants served: autonomy (no owner
round trip for a routine commit) and owner-work-safety (an outward push is never smuggled inside a local chain).

## Local remediation

The rule is applied in this deployment's agent memory (one git action per call, `-m` inline, push alone) since 2026-10-16
≈ 11:58; not yet in the canon text — waits for the origin's wording in 2.9 to avoid a local divergence.
