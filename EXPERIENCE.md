# EXPERIENCE — the agent's accumulated experience

> The agent's growing log of lessons. **Externalized memory of *what works and what doesn't*** — so a
> fresh, context-less session (or an autonomous loop) never repeats a dead end. Consult it BEFORE a task;
> append to it AFTER a meaningful attempt (success **or** failure). Grep, don't scroll.
>
> **Tags live inline on every entry** (not in a central list) — so one grep finds the experiences directly:
> `grep '#loop' EXPERIENCE.md` · `grep -i '#context\|#build' EXPERIENCE.md` · `grep '❌' -A4 EXPERIENCE.md`
> · `grep 'EXP-0007' EXPERIENCE.md`. Reuse an existing tag where one fits (grep to see what's in use).
>
> **Entry format (keep it short and grep-friendly).** Newest on top. Every entry starts with a stable id,
> an ISO date, an outcome marker (`✅` / `❌` / `❌→✅`), and inline `#tags`:
>
> ```
> ### EXP-0001 · 2026-01-01 · ✅ · #tag #area
> class: <slug from the class list below — the UNIT OF RECURRENCE>
> **Context:** one line — what was being done.
> **Tried / did:** the approach, briefly.
> **Result:** ✅/❌ — what happened.
> **Lesson:** the reusable takeaway (the reason this entry exists).   → link: bugs/NN · ideas/NN · plans/NN
> **Repro:** the ready-to-run command/check that verifies or applies the lesson — a weak session
>   executes a pasted command reliably, an essay it won't act on. REQUIRED since 2.1: a lesson
>   with no Repro line is not accepted (field-proven: lessons with a Repro command get executed,
>   essay-lessons get read and ignored). If the lesson genuinely has no command, say what to
>   OBSERVE instead — but say it as an action.
> **Trigger:** for class-level lessons — the decision point that must invoke this lesson, as
>   "writing X → run Y" (the lesson names WHERE it applies, instead of hoping to be remembered).
> **Not for:** the lesson's validity range — where it does NOT apply. A documented lesson is still a
>   hypothesis; applied outside its range it kills good ideas.
> ```
>
> **A lesson that repeats is a lesson that failed as text.** When the same class recurs in NEW code
> after its entry was recorded, the journal has proven insufficient — the lesson MUST become
> executable (a linter rule, a guard, a gate), and the entry gains the line
> `mechanized: <the tool>`. Two strikes → a mechanism, never a third reminder.
>
> **The deadline is RUN, not remembered** (2.7, epic EL; origin issue #69 — a field audit of one
> project's journal: 14 of 15 failure classes recurred AFTER their lesson was written, five lessons
> written 6–17 times in different words, 5.8 % mechanized): `node .kaif/tools/kaif-experience-lint.mjs
> check` reads the `class:` field as the UNIT of recurrence and reddens on the SECOND failure entry of
> one class with no `mechanized:`, naming the class and both entries by id. Two fates clear it, both
> WRITTEN: name the guard in the entry (`mechanized: <the tool>`), or re-check the price once for the
> WHOLE class and declare it — `<!-- class-ok: <slug> — <why it is not cheaply possible> -->` (an empty
> declaration is itself a finding; the declared classes are printed on the summary line and that list
> only shrinks). A third record is never a fate. It also warns when
> `mechanized:` names a command this project does not contain, and when a slug is outside the list
> below; `--shrink EXP-NNNN` collapses a MECHANIZED entry to one line pointing at its guard (shows by
> default, `--yes` writes — the text itself stays in the git history). The command belongs in the
> closing ritual (`/end-chat-soft`).
>
> **The class list of this journal** — a CONTROLLED list, not a closed one: pick a slug from it, and
> when a lesson genuinely brings a new class, add the slug here in the same write (the linter warns
> about an unlisted slug, it never refuses). The starter list below is what a field audit had already
> measured (origin issue #69) — replace and grow it with your project's own classes.
>
> <!-- classes: question-already-answered, guard-not-proven-against-threat, shown-as-link,
>      claim-before-evidence, owner-decision-not-applied, text-in-agents-world,
>      etalon-from-dirty-tree, shell-lied, escaping-layer, twins-missed,
>      field-dropped-in-rebuild -->
>
> | Class slug | The failure it names |
> |---|---|
> | `question-already-answered` | the owner is asked what his own past word, the goal doc or a run already decided |
> | `guard-not-proven-against-threat` | a guard shipped without being seen red on the threat it claims to stop |
> | `shown-as-link` | showing replaced by a link or a path instead of the thing itself |
> | `claim-before-evidence` | a claim written wider than the observation behind it |
> | `owner-decision-not-applied` | a decision the owner gave is recorded and not carried into the artifact |
> | `text-in-agents-world` | text written for the agent's own world instead of the owner's |
> | `etalon-from-dirty-tree` | a reference/etalon captured from a tree that was not clean |
> | `shell-lied` | the shell or the tool swallowed/rewrote what was passed to it |
> | `escaping-layer` | one escaping level lost between the tool and the file |
> | `twins-missed` | one of two layers/copies moved and the twin stayed behind |
> | `field-dropped-in-rebuild` | a field or section silently lost when an artifact was regenerated |
>
> The `#tags` are **trigger-tags**: before a task, grep by the task's tags and QUOTE the relevant
> lessons in your report (id + one line) — or state "no relevant lessons". An unquoted recall is
> unverifiable; `/fable-judge` checks for this line.
>
> Skill: `/experience` (capture a lesson · recall relevant lessons).

## Entries

### EXP-0004 · 2026-10-14 · ❌→✅ · #report #time #testrun
class: claim-before-evidence
**Context:** writing the phase 0 run report right after eight probe runs on the Titan.
**Tried / did:** filled the Runs table moments (14:08–14:16) from a sense of elapsed work, without a clock probe.
**Result:** ❌ the first probe after the commit showed 14:10:06 — the moments were in the future. ✅ corrected from the
device output files' mtimes (`ls --full-time`), runs without an own file marked `≈`; the correction is written in the report.
**Lesson:** a session's sense of time comes from the volume of work, not from the clock; every moment in a report is a
probe in the same call as the write, or a mtime/log line — never typed by feel.   → link: testcases/reports/2026-10-14_F0_engine_probe.md
**Repro:** `date '+%Y-%m-%d %H:%M:%S %z'` in the same tool call that writes the report row; for past runs `adb shell ls -la --full-time <outputs>`.
**Trigger:** writing a `Moment` cell or a `Created:` stamp → run `date` in that same call.
**Not for:** dates of external events (a vendor release) — those are dates, not moments of our work.
none-cheap: a moment typed by feel and a probed one look the same to a script; the Trigger line at the write is the carrier

### EXP-0003 · 2026-10-14 · ❌→✅ · #shell #heredoc #windows #paths
class: escaping-layer
**Context:** writing `tools/fetch-ncnn.mjs` through a Bash heredoc with a JS string `'C:\\Windows\\System32\\tar.exe'`.
**Tried / did:** typed doubled backslashes in the Bash tool's command parameter.
**Result:** ❌ the file got single backslashes (`'C:\Windows\System32\tar.exe'`) → JS read `\t` as a tab, `spawnSync` ENOENT,
"unpack failed". ✅ path built without escapes: `join(process.env.SystemRoot || 'C:/Windows', 'System32', 'tar.exe')`.
**Lesson:** a doubled backslash typed into a tool parameter can arrive single; write code files with the Write/Edit tools
and build Windows paths with `path.join` / forward slashes, never with escaped backslashes.   → link: tools/fetch-ncnn.mjs
**Repro:** `grep -n "\\\\\\\\" tools/*.mjs` after writing a script — any `\\` you intended must be there; better, `grep -n "C:\\\\" tools/*.mjs` shows none.
**Trigger:** writing a code file that contains a Windows path → Write tool + `path.join`, not a heredoc.
**Not for:** prose in Markdown, where a single backslash in a path is the intended text.
none-cheap: the loss happens in the tool layer before the file exists; the Write tool and `path.join` avoid the class

### EXP-0002 · 2026-10-14 · ❌→✅ · #adb #device #shell #titan
class: shell-lied
**Context:** measuring `adb pull` speed to the Titan from Git Bash with a device path `/data/local/tmp/…`.
**Tried / did:** `adb pull /data/local/tmp/x.bin …` and `adb shell rm -f /data/local/tmp/x.bin` as separate argv words.
**Result:** ❌ Git Bash (MSYS) rewrote the device path into `C:/Program Files/Git/data/local/tmp/…` — the pull failed and
the `rm` silently missed the file; paths INSIDE one quoted `adb shell "…"` string were untouched. ✅ with `MSYS_NO_PATHCONV=1`.
**Lesson:** any argv word that starts with `/` is a Windows path to Git Bash; device paths passed to `adb` from Git Bash
need `MSYS_NO_PATHCONV=1` (or call adb from PowerShell / Node `spawnSync`, which pass argv untouched). Measured speed over
Tailscale (DERP relay): 4.2 MB/s (20 MB in 4.8 s, 2026-10-14).   → link: ideas/01, HOUSE_RULES.md §3
**Repro:** `export MSYS_NO_PATHCONV=1; D:/Android/Sdk/platform-tools/adb.exe shell ls -la /data/local/tmp` — prints the device dir, not a Windows error.
**Trigger:** writing an `adb pull|push|shell <path>` command in the Bash tool → prefix `MSYS_NO_PATHCONV=1`.
**Not for:** PowerShell and Node scripts — they do not rewrite argv.
mechanized: tools/titan-probe.mjs (drives adb through Node spawnSync, so device paths never pass through Git Bash)

### EXP-0001 · 2026-01-01 · ✅ · #example #meta
**Context:** first task after KAIF was deployed into this project (example entry — replace with real ones).
**Tried / did:** wrote the first real lesson here in the canonical format.
**Result:** ✅ — the experience log is live and greppable.
**Lesson:** capture lessons at the level of *approach* (what worked / what to avoid), not defect detail
(that lives in `bugs/`); one short entry beats a long story.   → link: (none)
