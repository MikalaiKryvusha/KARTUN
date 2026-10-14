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
>      field-dropped-in-rebuild, platform-layer-unseen, silent-noop-edit -->
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
> | `platform-layer-unseen` | the platform renders an asset in a place the agent never looked at (splash, themed icon, dark mode) |
| `silent-noop-edit` | a scripted edit whose anchor did not match changed nothing, and the work was reported as done |
>
> The `#tags` are **trigger-tags**: before a task, grep by the task's tags and QUOTE the relevant
> lessons in your report (id + one line) — or state "no relevant lessons". An unquoted recall is
> unverifiable; `/fable-judge` checks for this line.
>
> Skill: `/experience` (capture a lesson · recall relevant lessons).

## Entries

### EXP-0009 · 2026-10-14 · ❌→✅ · #docs #numbers #claim
class: claim-before-evidence
**Context:** writing a device test case (tile counts of an 8× run) and an interview's archaeology line (grep hit count) late in a loop.
**Tried / did:** typed the numbers from a mental estimate: «175 / 140 tiles», «14 hits».
**Result:** ❌ both wrong — 165 (35 + 130) and 33; ✅ caught by recomputing with a one-line script before anyone ran them (`03bdfdd`, interview 004).
**Lesson:** a number in a document is a measurement, not a recollection — print it with a script in the same call that writes it.
**Repro:** `python -c "import math;print(math.ceil(960/200)*math.ceil(1280/200), math.ceil(1920/200)*math.ceil(2560/200))"` → `35 130`; `grep … | wc -l` for hit counts.
**Trigger:** about to write a count, a size or a duration into a test case, a plan or an interview → run the command first.
**Not for:** numbers copied verbatim from a run report that already printed them.
none-cheap: a count in prose has no single producing tool to guard - the stamp guard covers moments only and the review door attests the archaeology FORM, not its number; the Repro command run in the same call is the check

### EXP-0008 · 2026-10-14 · ❌→✅ · #docs #script #edit #claim
class: silent-noop-edit
**Context:** inserting rows into a test-case document with a throwaway Python `t.replace(anchor, rows + anchor, 1)`.
**Tried / did:** the anchor was typed from memory (`| C14 debug build:`), the real text was `| C14 | debug build:`; no assert.
**Result:** ❌ the replace changed nothing, the script printed nothing, and two commit messages (`2caef1e`, `7a3ac42`) claimed
the rows were added. ✅ caught by reading the commit's own `--stat`; rows added and the claim corrected in `9ed34b4`.
**Lesson:** a scripted text edit must FAIL when its anchor is absent; a replace without an assert is a claim without evidence.
**Repro:** in every edit script: `assert t.count(anchor) == 1, anchor` before `t.replace(...)`; then `git diff --cached --stat` lists every file the commit message names.
**Trigger:** writing a script that edits a document → the `rep()` helper with the count assert, never a bare `replace`.
**Not for:** the Edit tool — it already refuses a missing `old_string`.
none-cheap: the edit scripts are throwaway files in the session scratchpad, no repository guard sees them; the second net is reading `git diff --cached --stat` against the commit message before every commit

### EXP-0007 · 2026-10-14 · ❌→✅ · #android #icon #splash #titan
class: platform-layer-unseen
**Context:** adaptive launcher icon whose foreground layer carries the picture with its glass faded to transparent.
**Tried / did:** checked the icon on the home screen only; the owner opened the app and saw the launch splash.
**Result:** ❌ "на сплеш экране ужас… нет фона под сердцем" — the Android 12+ splash draws only the FOREGROUND layer on the
theme's window colour (grey), so the faded glass became a dark blob. ✅ `Theme.KARTUN` sets `windowSplashScreenBackground`,
`windowSplashScreenIconBackgroundColor` and `windowBackground` to `@color/ic_launcher_background`.   → link: app/src/main/res/values/themes.xml
**Lesson:** an icon is seen in three places — home screen, splash, themed (monochrome) — check all three on the device.
**Repro:** `adb shell "am force-stop com.kriniks.kartun.debug; am start -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity >/dev/null; sleep 0.15; screencap -p /data/local/tmp/s.png"` then pull and look (MSYS_NO_PATHCONV=1 in Git Bash).
**Trigger:** changing anything in `app/src/main/res/mipmap-*` or the theme → capture the splash as above.
**Not for:** icons that are opaque edge to edge (no fade) — there the default splash is merely plain, not broken.
none-cheap: the splash exists only on a running device for ~0.5 s; the Repro command above is the check, a PC-side guard cannot see it

### EXP-0006 · 2026-10-14 · ❌→✅ · #icon #geometry #report
class: claim-before-evidence
**Context:** fitting the owner's `icon_v3` (heart on a glass plate) into the Android circle mask.
**Tried / did:** estimated the heart's farthest pixel by eye (463 px) and told the owner the circle could not both crop inside
the plate and keep the heart whole.
**Result:** ❌ the measurement printed by `tools/make-icon.py` was 386 px — both held at full size; the claim was withdrawn in chat.
**Lesson:** a geometric "impossible" is a number; say it only after the script printed it.   → link: tools/make-icon.py
**Repro:** `python tools/make-icon.py --preview-only --preview <png>` prints "heart farthest pixel = N dp from the centre".
**Trigger:** about to tell the owner a layout/crop is impossible or will clip → run the measuring command first.
**Not for:** taste judgements (how it looks) — those stay the owner's eye.
mechanized: tools/make-icon.py (prints the measured heart extent in dp on every run)

### EXP-0005 · 2026-10-14 · ❌→✅ · #report #time #guard
class: claim-before-evidence
**Context:** recording the owner's acceptance of the mockups in MASTER_PLAN, STATUS and interview 002.
**Tried / did:** typed "≈ 15:05" for the moment; the commit carrying it went out at 15:04 — the second time this session
(EXP-0004 was the first) a moment was written ahead of the clock.
**Result:** ❌ a future moment stood in pushed files; ✅ replaced by the recording commit's time (15:02) and the class mechanized.
**Lesson:** a lesson written as text did not stop the second slip within the hour; a guard at commit time does.   → link: tools/stamp-guard.mjs
**Repro:** `node tools/stamp-guard.mjs --staged` (runs in .git/hooks/pre-commit); a stamp later than now reddens with exit 1.
**Trigger:** any commit that carries a "YYYY-MM-DD HH:MM" moment.
**Not for:** past moments typed by feel (earlier than the truth) — the guard cannot see those; the Trigger of EXP-0004 still applies.
mechanized: tools/stamp-guard.mjs

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
mechanized: tools/stamp-guard.mjs (future moments only; see EXP-0005)

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
