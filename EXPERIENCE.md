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
>      field-dropped-in-rebuild, platform-layer-unseen, silent-noop-edit, replace-crossed-scope, stand-left-dirty, shallow-scan,
>      proxy-for-the-thing, stale-artifact, guessed-instead-of-looked, revert-wiped-work -->
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
| `replace-crossed-scope` | a scripted replace matched the same text in a neighbouring block (another language, another section) |
| `stand-left-dirty` | a harness left the owner's device, stand or tree in a state the owner does not expect |
| `shallow-scan` | a selection walked less of the tree than the action that follows it (top-level `ls` before a recursive delete) |
| `proxy-for-the-thing` | a check on a stand-in (the desktop GPU, the PC) taken as the check on the target (the phone's GPU) |
| `stale-artifact` | a build or file believed to carry a change that it does not carry (timestamps, caches) |
| `guessed-instead-of-looked` | a cause hunted on a guessed axis while the evidence (a journal, a thread name) already named it |
| `revert-wiped-work` | an undo through git (checkout, reset) took the tree back past uncommitted work it was not meant to touch |
>
> The `#tags` are **trigger-tags**: before a task, grep by the task's tags and QUOTE the relevant
> lessons in your report (id + one line) — or state "no relevant lessons". An unquoted recall is
> unverifiable; `/fable-judge` checks for this line.
>
> Skill: `/experience` (capture a lesson · recall relevant lessons).

## Entries

<!-- class-ok: claim-before-evidence — re-checked 2026-10-14 for the whole class: the cheap halves are mechanized (moments - tools/stamp-guard.mjs, EXP-0005; icon geometry - tools/make-icon.py, EXP-0006); what is left are counts and states written in prose and chat, with no single producing tool a guard could watch - the check is the Repro command run in the same turn as the claim -->

### EXP-0035 · 2026-10-16 · ❌→✅ · #crash #kotlin #release #guard
class: fix-without-guard
**Context:** RC4 final round; the first launch after `pm clear` died in the `UpscaleViewModel` constructor (bugs/04).
**Tried / did:** at 20:06 the same class (a property below `init`, null while init runs) was fixed by moving ONE field and a
comment; a later commit added `benchSaver` below `init`, marked `[NOT-TESTED]`, and RC4 was frozen on top of it.
**Result:** ❌ the release candidate crashed on every fresh install (Titan, Redmi). ✅ `init` moved to the end of the class +
`InitLastGuardTest` (red on the RC4 source).
**Lesson:** the second hit of one class in a day means the first fix closed the instance, not the class — close it by form and
a guard on the spot; and a `[NOT-TESTED]` commit is never frozen into an RC without `pm clear` + launch on one device.
**Repro:** `git show 3c8ca15:app/src/main/kotlin/com/kriniks/kartun/UpscaleViewModel.kt` → `benchSaver` at line 206, `init` at 189.
**Trigger:** writing an `init` block, or freezing an RC over a `[NOT-TESTED]` commit.
**Not for:** —
mechanized: app/src/test/kotlin/com/kriniks/kartun/InitLastGuardTest.kt

### EXP-0034 · 2026-10-16 · ❌→✅ · #owner-word #ui #layout
class: owner-word-retold
**Context:** the processing panel; the owner had asked (2026-10-15 ≈ 12:35) «Кнопку отменить меньше, ниже» · «чтобы высвободить
место для заголовка».
**Tried / did:** the agent kept «Отменить» in the title's row (only smaller); a later session retold the word as «поднять над
тайтлами метрик» and built from the retelling.
**Result:** ❌ on phones the title stayed cut («The network is enhancing your p…»); the owner, 2026-10-16 ≈ 21:0x: «я просил кнопку
Cancel ниже, чтобы она не мешала заголовку» · «давно просил» · «вниз её, где чимп GPU Vulcan». ✅ «Отменить» on the GPU chip's line.
**Lesson:** an owner's layout word is applied from his VERBATIM quote and checked against its PURPOSE («чтобы высвободить место
для заголовка») on the narrowest device — a retelling in a plan or a comment drops the purpose, and the purpose is the test.
**Repro:** `git log -S"ниже" -- app/src/main/kotlin/com/kriniks/kartun/ui/WorkingScreen.kt` shows the comment quoting the word; a
screenshot on a 360 dp phone shows whether the purpose holds.
**Trigger:** applying an owner's layout word, or reading «the owner asked …» in a code comment.
**Not for:** words with no stated purpose — then ask once.
mechanized: no - judgement; the narrow-device screenshot is the net

### EXP-0033 · 2026-10-16 · ❌→✅ · #mutation #git #test
class: revert-wiped-work
**Context:** proving a new unit test red by a mutation of a source file that also held this session's UNCOMMITTED edits.
**Tried / did:** `sed -i` the mutation, run the test, then `git checkout -- <file>` to undo the mutation.
**Result:** ❌ the checkout restored the last COMMIT, wiping the uncommitted feature edits too (journal note, openError);
saved only by a `cp` made before the mutation. A second try ran the mutant under a tool timeout - the restore step queued after
gradle never ran while the file sat mutated for minutes. ✅ a PowerShell script: read the file, write the mutant, run the test,
restore the read text in `finally` (`mutate-openerror.ps1` pattern, prints `restored: True`).
**Lesson:** a mutation is undone from a copy taken right before it, in a `finally`, never by `git checkout` - git knows only
the commit, not the work in the tree.
**Repro:** `git diff --stat <file>` before the mutation is non-empty → `git checkout -- <file>` would lose exactly that diff.
**Trigger:** about to mutate a file to prove a test red.
**Not for:** files with no uncommitted changes (`git diff --quiet <file>` exits 0) - there checkout is the same as the copy.
mechanized: no - one-off scripts; the finally-restore script shape is the net

### EXP-0032 · 2026-10-16 · ✅ · #docs #agent-api #fresh-agent
class: proxy-for-the-thing
**Context:** plans/07 step 10 — is `docs/agent-api.md` enough for an agent that never saw the repo?
**Tried / did:** a clean subagent got only the doc, the CLI and the serial: «get the device info, upscale this PNG 4×».
**Result:** ✅ it worked — and its FIRST upscale lost the result: `--out` into a missing folder, the pull failed, and the CLI
still wiped the device copy; `ok:false` next to `state:"succeeded"` was nowhere in the doc. Every one of these had passed
my own runs, because I always gave an existing folder.
**Lesson:** the author's runs prove the paths the author walks; a doc for strangers is checked by a stranger — a clean
instance with the doc alone — and every question or slip it has becomes a fix and a rerun.
**Repro:** `node tools/kartun-cli.mjs upscale <png> --out <missing-dir>/x.png` → `ok:true`, the folder made; `--out <a file>/x.png` → `pull_failed`, the device copy kept.
**Trigger:** a document or tool meant for other agents or people is about to be called done.
**Not for:** internal notes only the author reads.
none-cheap: the check is a clean model's reading, no script can be the stranger; the CLI paths it found are now handled in tools/kartun-cli.mjs

### EXP-0031 · 2026-10-16 · ❌→✅ · #android #permission #adb #agent-api
class: proxy-for-the-thing
**Context:** criterion A7 — another app must not start the agent door (`android:permission="android.permission.DUMP"`).
**Tried / did:** `adb shell run-as com.kriniks.kartun.debug am start-foreground-service …` against our own door (step 1),
then against the release build's door from the debug app (step 9).
**Result:** ❌ both «refusals» tested something else: `run-as <pkg>` runs as THAT package's uid, and a component's permission
never blocks its own uid (the refusal was the background-FGS rule); the debug app could not even SEE the release package
(«Error: Not found», Android package visibility). ✅ a probe app of our own with `<queries>` for KARTUN, started into the
foreground: `Permission Denial … uid=10302 requires android.permission.DUMP`.
**Lesson:** a permission guard is proved only by a caller of a DIFFERENT uid that can SEE the target and is otherwise
allowed to make the call; read the refusal text — it names the layer that refused.
**Repro:** `.\gradlew.bat :probe:assembleDebug`; `adb install probe/build/outputs/apk/debug/probe-debug.apk`; `adb shell am start -W -n com.kriniks.kartun.probe/.ProbeActivity`; `adb logcat -d | grep A7PROBE`; `adb uninstall com.kriniks.kartun.probe`.
**Trigger:** writing a check that «app X cannot call Y».
**Not for:** checks of our own app's behaviour.
mechanized: probe/ (the test-only caller module)

Also found in step 1: `adb push` keeps the PC file's mtime — a fresh input arrived dated two days back; the agent folder's
24 h sweep keys on the app-written status file for that reason (agent/AgentService.kt `sweep`).

### EXP-0030 · 2026-10-16 · ❌ · #titan #owner #device #lock
class: stand-left-dirty
**Context:** criterion A4 of the agent API («the job runs with the screen off») on the owner's Titan, then a UI run.
**Tried / did:** `adb shell input keyevent 223` to turn the screen off, the job, `keyevent 224` to wake it; later a UI run.
**Result:** ❌ the wake-up brought a SECURE lock screen (`isKeyguardShowing=true`, screenshots black, `wm dismiss-keyguard`
refused) — before the test the tablet was unlocked on the launcher; the UI case waited for the owner («я разлочил титан»),
and one UI-agent tap, one raw `input tap` and one `titan-tap` (its focus check still saw KARTUN) landed on the locked tablet.
**Lesson:** turning the owner's screen off locks his device; a screen-off test is the LAST device step of a run, and every
tap first reads that the Titan is awake and unlocked. The owner then set the lock delay to 30 min («или поставь на 30 минут»).
**Repro:** `adb shell dumpsys window | grep isKeyguardShowing` → `true` after `input keyevent 223` + `224` on the Titan.
**Trigger:** about to send `input keyevent 223` / `KEYCODE_SLEEP` / `POWER` to the owner's device, or to tap it.
**Not for:** a test device without a lock.
mechanized: tools/titan-tap.mjs (refuses asleep or locked; the refusal not yet seen red — that needs locking the owner's tablet)

### EXP-0029 · 2026-10-16 · ❌→✅ · #crash #openmp #ncnn #thread #device
class: guessed-instead-of-looked
**Context:** bugs/01 — the app aborted in libomp's affinity init on a model load; six repro variants (masks, cpusets, races) all ran clean.
**Tried / did:** varied the CPU mask of the ONE thread that initialises OpenMP; the owner's second crash then left the assert line and the thread name in the app's own crash journal.
**Result:** ❌ every mask variant was clean — the axis was wrong. ✅ both crashes ran on a NEW thread `pool-7-thread-1` in a process whose earlier loads used `pool-5`: the screen's own engine thread had exited (`onCleared` closed it), a static libomp shuts down with its last thread, and the next thread re-initialises it into the assert. Repro in one minute: Back out → reopen → pick a photo.
**Lesson:** for a native runtime crash "after a while", vary the THREAD's life (who initialises the runtime, who exits) before the CPU state; and a thread name in the journal is evidence — compare it across crashes.
**Repro:** old build: pick a photo, Back, Back, `am start …/MainActivity` (same pid), pick a photo → process gone, `kmp_affinity.cpp(4755)`.
**Trigger:** a native library (OpenMP, Vulkan, any runtime with global state) called from an executor owned by a screen or ViewModel.
**Not for:** pure Kotlin work on per-screen executors.
mechanized: app/src/test/kotlin/com/kriniks/kartun/EngineThreadGuardTest.kt

### EXP-0028 · 2026-10-16 · ❌ · #owner-reviews #contour #timeout
class: stand-left-dirty
**Context:** raising interview #005 to the owner with the review contour's page.
**Tried / did:** `timeout 60 node .kaif/tools/contour/review.mjs --queue` — the timeout was there to keep the tool call short.
**Result:** ❌ the page's local server lives in that process; `timeout` killed it after 60 s, and the owner's «send» failed
(«упал твой контур»); he pasted the page's answer JSON into the chat instead — the answers were applied from there.
**Lesson:** the queue command SERVES the page — run it in the background (or plain, without a timeout), never under `timeout`.
**Repro:** `timeout 60 node .kaif/tools/contour/review.mjs --queue`, answer on the page after 60 s → the send fails.
**Trigger:** about to start any command that serves a page the owner will use.
**Not for:** `--check`, `--list` and other one-shot queue commands.
mechanized: no — the command cannot see the wrapper that kills it

### EXP-0027 · 2026-10-16 · ❌→✅ · #shell #heredoc #bash #script
class: shell-lied
**Context:** editing Kotlin/Python files through a Bash-tool call that held a quoted heredoc (`<<'EOF'`) with a Python
script inside — triple quotes, Kotlin `'…'` chars, Cyrillic text.
**Tried / did:** four long heredocs of this shape in one session.
**Result:** ❌ each one died before running: `unexpected EOF while looking for matching `''` — the tool's command parser
counts quotes across the WHOLE command, the heredoc's quoting does not shield it; nothing was written, so no damage, only lost
turns. ✅ the same script written with the file tool (Write) into the scratchpad and run as `python <file>` worked first time.
**Lesson:** a script longer than a few lines, or one with quotes of its own, travels as a FILE (Write → run), never as a
heredoc inside a tool command — the text-through-files rule applies to the agent's own scripts, not only to documents.
**Repro:** a Bash-tool command `python - <<'EOF'` whose body holds `'''` or an odd number of `'` → the parse error above, exit 2.
**Trigger:** about to put a multi-line script with quotes into a single Bash-tool call.
**Not for:** short heredocs of plain text without quote characters (they pass).
mechanized: no — the parser is the tool's, not the project's; the rule is AGENT_GUIDE.md «TEXT TRAVELS THROUGH FILES»
Seen again 2026-10-16 ≈ 14:40 in a second face: a `<<'EOF'` Python script wrote `"\\n"` and Android strings got REAL line
breaks (the backslash pair came out single, and nothing failed); the file-tool script with `chr(92) + "n"` was right first
time. A script that must emit a backslash is written with the file tool, and the result is read back (`grep -vc '</string>$'`).

### EXP-0026 · 2026-10-16 · ❌→✅ · #adb #titan #harness #device
class: stand-left-dirty
**Context:** two sessions (KARTUN and Krinik Tribit) and the UI agent drove the same Titan over Wi-Fi adb.
**Tried / did:** called the UI agent's `shot`, which runs bare `adb`, from a shell whose PATH had `C:\adb` first.
**Result:** ❌ `adb server version (32) doesn't match this client (41); killing...` — C:\adb (32) and the SDK's adb (41) kill
each other's server on every call, the Titan's Wi-Fi connection drops (`device not found`, the agent's socket refused).
Both sessions did it once. ✅ the SDK's platform-tools first in PATH for every script that runs ui.py; `adb connect` again.
**Lesson:** on this PC only `C:\Android\Sdk\platform-tools\adb.exe` may touch the device — and tools that call bare `adb`
(the UI agent) inherit PATH, so the PATH is part of the call.
**Repro:** `C:\adb\adb.exe devices` then `C:\Android\Sdk\platform-tools\adb.exe devices` → the second prints the version
mismatch line and an empty device list.
**Trigger:** any script or tool that runs `adb` without a full path; a second session on the same device.
**Not for:** calls with the SDK adb's full path (they never meet C:\adb).
mechanized: no — HOUSE_RULES.md section 3/6 rows say «only the SDK adb»; the UI-agent row now names the PATH

### EXP-0025 · 2026-10-15 · ❌→✅ · #powershell #encoding #script #windows
class: shell-lied
**Context:** handing the owner a `.ps1` to run himself (the agent's own git operation had been refused), with progress messages written in his language.
**Tried / did:** wrote the script with Cyrillic `Write-Host` lines, as for any other document of this project.
**Result:** ❌ it died at parse time — `TerminatorExpectedAtEndOfString` on `Write-Host 'готово'`, a line correct in the source: PowerShell 5.1 reads a BOM-less `.ps1` as ANSI, the Cyrillic became mojibake and the mojibake ate the closing quote. ✅ rewritten ASCII-only (English messages), ran first try.
**Lesson:** the encoding rule binds a SCRIPT FILE, not only an argument and not only a write — the file on disk is correct and the READER is wrong, so "read the result back" never catches it. A `.ps1` is ASCII-only unless saved UTF-8 WITH BOM.
**Repro:** a `.ps1` holding a Cyrillic string, saved UTF-8 without BOM → `powershell -File` exits with a parser error naming a valid line; the same script in ASCII runs.
**Trigger:** about to write a `.ps1` (or any script a Windows-native interpreter parses) that contains non-ASCII.
**Not for:** files a UTF-8-aware reader consumes (`.md`, `.json`, `.mjs` via node) — those stay in the owner's language.
mechanized: no — one guard would have to parse every script this project may ever write; the cheap half is the canon line (AGENT_GUIDE.md, face 5 of the text-encoding class) plus the dossier row in HOUSE_RULES.md, both read before writing

### EXP-0024 · 2026-10-15 · ❌ · #titan #harness #adb #owner #device
class: stand-left-dirty
**Context:** a background bash loop drove the owner's path on the Titan with `input tap` at fixed coordinates (bugs/01 hunt).
**Tried / did:** stopped it with the harness's task stop when the owner said «баг я сам буду ловить»; did not check the PC
process list afterwards; the taps did not check which window was on screen.
**Result:** ❌ the task stop killed only the wrapper — Git Bash kept running `owner_path.sh` (PID 25240) for 7 more minutes;
`am start` did not always bring KARTUN to the front, so the tap for «Выберите фото» (800, 1142) hit the NDim icon on the
launcher and opened the owner's NDim three times (21:14:28, 21:16:49/21:17:31, 21:18:53), and two more taps per round landed
INSIDE NDim. The owner noticed: «кто запускает NDIM на моем девайсе? и зачем?». Found by `dumpsys usagestats` and the PC process list.
**Lesson:** a tap on the owner's device is legal only after reading that our window has the focus; a stopped loop is stopped
only when the process list shows it gone.
**Repro:** before every tap: `adb shell dumpsys window | grep mCurrentFocus` contains `com.kriniks.kartun` — else stop; after
stopping a loop: `powershell "Get-CimInstance Win32_Process | ? CommandLine -match 'scratchpad'"` prints nothing.
**Trigger:** writing any adb script with `input tap` / `input keyevent`, or stopping a background task that drives a device.
**Not for:** taps inside the debug probe screen launched by `am start -S … --es in` in the same call (focus is ours by construction).
mechanized: tools/titan-tap.mjs (taps only when KARTUN has the focus, else exit 2; proved on the Titan both ways)

### EXP-0023 · 2026-10-15 · ❌→✅ · #crash #logcat #android #forensics
class: stand-left-dirty
**Context:** the owner's crash at 19:00 (bugs/01) was looked for at 20:52.
**Tried / did:** the probe scripts of this session ran `adb logcat -c` before each run.
**Result:** ❌ the crash buffer and the OpenMP message were gone; only the dropbox entry survived (stack, no assert text —
libomp prints to stderr, which an Android app drops). ✅ the app now keeps its own journal (`files/crashes/`: exit records
with tombstones, native stderr, the cpuset before each model load).
**Lesson:** never clear a device's logs that may hold the owner's evidence; read `logcat -d -b crash` and the dropbox first.
**Repro:** `adb shell "dumpsys dropbox | grep -E 'data_app_(native_)?crash'"` and `adb shell ls /sdcard/Android/data/com.kriniks.kartun.debug/files/crashes/`.
**Trigger:** about to run `logcat -c` on the Titan → use `logcat -T <time>` or filter by pid instead.
**Not for:** a device nobody else uses.
mechanized: app/src/main/kotlin/com/kriniks/kartun/CrashLog.kt

### EXP-0022 · 2026-10-15 · ❌→✅ · #build #ninja #ncnn #device
class: stale-artifact
**Context:** proving an ncnn shader fix on the Titan: one APK on ncnn as-is, one on ncnn with the fixed shader.
**Tried / did:** copied the fixed shader into the tree with PowerShell `Copy-Item` and ran `ninja install`, then built the APK.
**Result:** ❌ the two APKs were byte-identical: `Copy-Item` kept the old file time, ninja saw nothing new, the "fixed" build was the
old one; caught by hashing both APKs before the device run. ✅ `touch` the file, rebuild, check the build by decoding the embedded
shader (`sdpa_cross.comp.hex.h` contains `min(gy4`), APK hashes differ.
**Lesson:** a build "with the change" is proved by the artifact's own content (decode it, hash it), never by the build exiting 0.
**Repro:** `sha256sum a.apk b.apk` must differ; `python -c "import re;…bytes(int(x,16) for x in re.findall(r'0x([0-9a-f]{2})',t))…"` on the generated header shows the change.
**Trigger:** building two variants to compare on a device → hash both before installing.
**Not for:** builds that differ in source the build system watches by content.
none-cheap: one-off A/B builds; the Repro hash is the net

### EXP-0021 · 2026-10-15 · ❌→✅ · #ncnn #psnr #png #engine
class: proxy-for-the-thing
**Context:** checking DAT2 on the Titan: the PNG output against a PyTorch reference gave 32-52 dB, the PC said 66.6 dB.
**Tried / did:** compared 8-bit PNGs (engine vs PyTorch rounded), then on a bare 108 px input against an engine that pads to 128.
**Result:** ❌ two gaps that were not the network: the engine's `to_pixels` CUT the fraction (every model's output half a step
darker, mean -0.46) and a transformer sees the whole padded tile (padding changes the middle too). ✅ float-vs-float on the
same input: 66.58 dB on the Mali; engine now writes x*255 + 0.5 (x4plus 39.2 -> 39.34 dB vs its reference).
**Lesson:** a network is compared float against float on the SAME input the engine feeds it; an 8-bit picture is capped near
59 dB by quantization alone, and a rounding mismatch alone caps it near 54 dB.
**Repro:** probe extra `blobs output` on a picture of exactly the input size → `blob_output.f32` → PSNR against PyTorch in float.
**Trigger:** a PSNR between the device and the reference looks low → check rounding and input padding before suspecting a layer.
**Not for:** taste comparisons of models — those stay the owner's eye.
none-cheap: the float dump is a debug-build path; the net is the Repro

### EXP-0020 · 2026-10-15 · ❌→✅ · #ncnn #gpu #mali #bisect #device
class: proxy-for-the-thing
**Context:** DAT2 gave noise on the Mali; the last session tested each SDPA kind alone on the device and found all correct.
**Tried / did:** the "channel" test net cut channels in heads of 5 (`Reshape 1=5`) while the real net uses 30 — another shape.
**Result:** ❌ the test proved a layer the net does not have. ✅ layer-by-layer stats of the REAL net on the device vs the PC
(`logBlobStats` + `tools/dat-blob-stats.py`, three bisection passes in seconds) found `sdpa_4671`: inputs equal to 6 digits,
output = the stale contents of a freed blob (its mean |x| matched blob 275 exactly) — the Vulkan kernel wrote nothing.
`31=16` on the 18 channel SDPAs: the whole net matches the PC, 66.58 dB vs PyTorch, 11.3 s per tile. Root cause found later the
same hour: ncnn's sdpa_cross.comp reads rows past its buffer when the sequence is not a multiple of 4 (30 here) - padding to 32
runs every layer on the GPU (10.9 s, 66.58 dB; ncnn #7120). A desktop GPU tolerates out-of-bounds reads, the Mali does not.
**Lesson:** bisect the real graph on the real device; an isolated reproduction proves only its own shape. A wrong blob whose
numbers equal another blob's is a kernel that did not write.
**Repro:** debug build → `--es model <m> --es blobs <b1,b2,…>` → logcat `blobstat` → `tools/dat-blob-stats.py <param> <bin> --blobs <…> --device <logcat.txt>`.
**Trigger:** a net correct on the PC is wrong on a phone → bisect blobs with the Repro before writing a single isolated test.
**Not for:** speed questions — use the `ms` timing of the same probe.
mechanized: tools/dat-blob-stats.py + logBlobStats in kartun_engine.cpp

### EXP-0019 · 2026-10-15 · ❌→✅ · #ncnn #pnnx #gpu #mali #device
class: proxy-for-the-thing
**Context:** bringing DAT2 (UltraSharp V2) to ncnn: the converted net matched PyTorch on the PC's RTX over Vulkan (66.6 dB).
**Tried / did:** took the desktop GPU run as the proof and pushed the model to the Titan.
**Result:** ❌ on the Mali one 128 px tile took 267 s and came out as noise (16 dB): ncnn's fp32 Vulkan Gemm ran ~0.6 GFLOP/s on
Mali (found by timing the net up to chosen blobs on the device), and some other layer computes differently there. ✅ Gemm →
1×1 conv made it 13.9 s; the wrong layer is still being bisected (STATUS item 1).
**Lesson:** a desktop GPU is a proxy for the phone's GPU, not the phone: every new graph runs ONE small tile on the Titan, its
time and its picture read, before anything is built on it; when it is slow or wrong, time and compare per layer on the device.
**Repro:** debug build → `adb shell am start … --es in test108.png --es model <m> --es blobs <b1,b2,…>` → logcat `{"blob":…,"ms":…}`.
**Trigger:** a new model or a rewritten graph → one small tile on the Titan through the probe screen first.
**Not for:** the PC-side numeric check of a conversion — it stays, it just is not the device.
mechanized: app/src/debug DebugTools (the probe screen and the `blobs` layer timing)

### EXP-0018 · 2026-10-15 · ❌→✅ · #titan #probe #owner #device
class: stand-left-dirty
**Context:** probing the first DAT2 build on the Titan with a 256 px picture through the phase 0 adb probe.
**Tried / did:** started the run and waited on it in the background (10-minute tool timeout), twice.
**Result:** ❌ the owner saw a black screen for many minutes («что-то приложение КАРТУН висит» · «долго висит черный экран»)
and stopped it. ✅ the probe now opens a screen with the spinner, live logs and device load; small pictures only (the owner:
«так ты на маленьких картинках пробуй, которым 10 минут не нужно»).
**Lesson:** a device probe the owner may look at shows what it is doing and finishes in seconds; anything slow is first cut
down (one tile, a tiny net) before it runs on his device.
**Repro:** `adb shell am start -S -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity --es in test108.png --es model <m> --ez gpu true --ei scale 4` → the probe screen, not black.
**Trigger:** about to run anything on the Titan that may take over a minute → shrink it first.
**Not for:** the owner's own long runs he asked for (a 100 MP photo).
mechanized: app/src/debug DebugTools (the probe screen replaces the black-screen probe)

### EXP-0017 · 2026-10-15 · ❌→✅ · #shell #python #paths #windows #docs
class: escaping-layer
**Context:** scripted edits of STATUS.md / HOUSE_RULES.md with a throwaway Python heredoc carrying Windows paths.
**Tried / did:** wrote `"C:\\Android\\Sdk\\platform-tools\\adb.exe"` inside a normal Python string literal (twice this session).
**Result:** ❌ once the assert stopped the write; once it went through: `\a` became a BEL byte and STATUS.md read
«platform-tools<BEL>db.exe». ✅ fixed with the Edit tool and a chr(7) replace; guard added.
**Lesson:** in a script, a Windows path inside a normal string literal is an escape minefield (`\a \b \f \v \t \A`); edit such
text with the Edit tool, or use raw strings / forward slashes — and let a commit hook refuse control bytes in text files.
**Repro:** `node tools/ctrl-char-guard.mjs <file>` → `CONTROL 0x07 …` on the broken file, exit 1.
**Trigger:** a scripted edit of a document that contains a backslash → Edit tool instead.
**Not for:** Markdown written by the Write/Edit tools — they pass backslashes through.
mechanized: tools/ctrl-char-guard.mjs (in .git/hooks/pre-commit)

### EXP-0016 · 2026-10-15 · ❌→✅ · #paths #move #git-hooks
class: twins-missed
**Context:** the first KAST commit after the 2026-10-15 00:16 move of `D:\Android` → `C:\Android`.
**Tried / did:** the move rewrote every path in the tracked files of KARTUN and KAST (`43b0123`, `8d730a20`).
**Result:** ❌ the commit failed: `.git/hooks/pre-commit` (not tracked) and the guard it runs, `C:\Android\private\kast-denylist-guard.mjs`
(outside every repo), still pointed at `D:/Android/...`; nothing showed it until the first commit. ✅ both paths fixed, the guard
ran green (`a318a875`).
**Lesson:** a path move also sweeps what git does not track: `.git/hooks/*`, private tools, scheduled tasks, shortcuts.
**Repro:** `grep -rln "D:/Android\|D:\\\\Android" /d/work/ai_sandbox/*/.git/hooks/ /c/Android/private/ 2>/dev/null | grep -v sample` → empty.
**Trigger:** moving or renaming a tool directory → run the Repro over hooks and private tools in the same step.
**Not for:** paths inside run reports and closed bugs — those record where a run happened.
none-cheap: one-off moves; the Repro grep is the net

### EXP-0015 · 2026-10-15 · ❌→✅ · #titan #ui-agent #harness #device
class: stand-left-dirty
**Context:** driving KARTUN on the Titan with the on-device UI agent through the system photo picker.
**Tried / did:** `am force-stop` + `am start` before each scenario; reading progress by polling `tree` every 50 ms.
**Result:** ❌ three stand artefacts read as app behaviour: the picker survived the force-stop and hung translucent over the
owner's Telegram; the agent answered `StaleObjectException` to every query after the picker closed (the app drew 0 frames);
a share sheet «took 16 s» under the polling and 2.4 s without it. ✅ `am start -S -W -f 0x10008000` (clears the task with the
picker), restart the agent with `ui.py --start ping` on a stale answer, time events by `dumpsys window | grep mCurrentFocus`.
**Lesson:** before calling something slow or broken on the device, rule out the harness: a cheaper probe (window focus,
`dumpsys gfxinfo` frame count) must agree with what the agent's polling showed.
**Repro:** `adb shell "dumpsys gfxinfo com.kriniks.kartun.debug reset >/dev/null; sleep 3; dumpsys gfxinfo com.kriniks.kartun.debug | grep 'Total frames'"` → 0 means the app is idle and the stale/slow reading is the harness.
**Trigger:** the agent says ERR, or a step takes far longer than in a previous run → run the cheap probe first.
**Not for:** crashes and logcat errors — those are the app's.
none-cheap: the harness is a throwaway driver in the session scratchpad; the net is the Repro probe

### EXP-0014 · 2026-10-15 · ❌→✅ · #owner #limits #reading
class: question-already-answered
**Context:** the owner asked «поддержку файлов больше 12 мб… до 100 мб»; the only «12» in the app was the 12,5 MP input limit.
**Tried / did:** read the number against the code before answering; said in chat that the limit is megapixels and that I read
«100 мб» as 100 MP, with the reason (the error line he saw said «12,5 Мп»).
**Result:** ✅ he went on to «давай поднимем макс лимит до 500 Мегапикселей» — the reading held; no round trip was spent.
**Lesson:** an owner's number about a limit is checked against every limit in the code first; the reading is said aloud in the
same answer, never silently applied.
**Repro:** `grep -rn "MAX_.*MP\|MAX_BYTES" app/src/main/kotlin` before answering a question about a size limit.
**Trigger:** the owner names a size, a count or a limit → grep the code's limits, state the reading in one line.
**Not for:** numbers he quotes from the screen verbatim.
none-cheap: the step is a reading of the owner's words, no tool can do it

### EXP-0013 · 2026-10-15 · ❌ · #titan #media #delete #device
class: shallow-scan
**Context:** the owner ordered «УДАЛИТЬ» on the Titan's `/sdcard/Download`; before deleting, the agent rescued the owner's own camera shots (`YYYYMMDD_HHMMSS.*`) to the PC.
**Tried / did:** listed candidates with `ls /sdcard/Download` (top level only), then `find Download -mindepth 1 -delete`.
**Result:** ❌ three camera videos in the subfolder `Download/Quick Share/` (20260820_230357, 20260822_221208, 20260823_160318, ≈ 860 MB) were deleted without a copy; the delete's own list file showed them.
**Lesson:** the rescue selection must walk the SAME tree the delete will walk — select with `find <dir> -type f`, never with `ls <dir>`.
**Repro:** compare `adb shell "find /sdcard/<dir> -type f | grep -cE '/[0-9]{8}_[0-9]{6}'"` with the count the rescue copied — they must be equal before `-delete`.
**Trigger:** about to delete a device folder after rescuing part of it → run the Repro count in the same call as the delete, stop on a mismatch.
**Not for:** deletes of a single flat folder with no subfolders (`find -mindepth 2 | wc -l` = 0).
none-cheap: the rescue scripts are throwaway; the net is the Repro count before every delete

### EXP-0012 · 2026-10-14 · ❌→✅ · #status #claim #state
class: claim-before-evidence
**Context:** answering the owner's «что там по беклогу?» about the Titan media offload (idea 01).
**Tried / did:** retold STATUS from memory: «скопированы, но сверка хешей не запускалась».
**Result:** ❌ wrong twice — the state files (`<inbox>\_state\verified.json`, `verify-bad.json`, 14:11) showed a check that ran mid-copy
(3 964 verified, 4 731 missing). ✅ corrected in chat and STATUS (`02aa118`); the resume then verified 8 716 of 8 716.
**Lesson:** a status line about an external process is a pointer, not evidence — read the process's own state files before repeating it.
**Repro:** `node -e "for(const f of ['verified','verify-bad'])console.log(f,JSON.parse(require('fs').readFileSync('J:/OLD_HDD/Pic/_titan_inbox_2026-10-14/_state/'+f+'.json','utf8')).length)"`
**Trigger:** about to tell the owner the state of a copy, a run or a sync → open its state file or log in the same turn.
**Not for:** facts this session measured itself a moment ago.
none-cheap: the claim lives in chat prose and in STATUS; no repository tool can know which external process a sentence describes

### EXP-0011 · 2026-10-14 · ❌→✅ · #i18n #script #edit
class: replace-crossed-scope
**Context:** rewriting positional entries of `tools/make-translations.py` (8 languages, one list each).
**Tried / did:** a global `text.replace(old_pair, new_pair)` per language.
**Result:** ❌ Spanish and Portuguese share some old values («Máximo · textura viva»), so the Spanish pass would have written Spanish
into the Portuguese list; ✅ caught by the next language's assert before any write, then replaced inside each language's own block.
**Lesson:** a value is unique per BLOCK, not per file — scope the replace to the block that owns it and assert count == 1 there.
**Repro:** in the edit script: find `    "<lang>": [` inside the right dict, slice to the next language, `assert seg.count(old) == 1`.
**Trigger:** writing a script that edits a multi-language table → per-block replace, never a file-wide one.
**Not for:** keys that are unique by construction (resource names in strings.xml).
none-cheap: the edit scripts are throwaway; the net is the per-block assert plus `python tools/make-translations.py --check`

### EXP-0010 · 2026-10-14 · ❌→✅ · #titan #probe #device
class: stand-left-dirty
**Context:** probing catalogue models on the Titan with `tools/titan-probe.mjs` while the owner held the tablet.
**Tried / did:** the probe starts MainActivity with extras that skip `setContent` and left it like that.
**Result:** ❌ the owner: «почему черный экран сейчас?»; ✅ the probe now restarts the normal interface on every exit.
**Lesson:** a harness that drives the owner's device leaves it the way the owner expects to find it.
**Repro:** `KARTUN_SERIAL=<serial> node tools/titan-probe.mjs --in test256.png --model realesr-animevideov3-x4 --gpu true --tile 200` → the home screen («Выберите фото») is back.
**Trigger:** writing any adb harness step that changes what the device shows → end it by restoring the screen.
**Not for:** runs on a device nobody looks at.
mechanized: tools/titan-probe.mjs (restoreUi on every exit)

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
**Repro:** `export MSYS_NO_PATHCONV=1; C:/Android/Sdk/platform-tools/adb.exe shell ls -la /data/local/tmp` — prints the device dir, not a Windows error.
**Trigger:** writing an `adb pull|push|shell <path>` command in the Bash tool → prefix `MSYS_NO_PATHCONV=1`.
**Not for:** PowerShell and Node scripts — they do not rewrite argv.
mechanized: tools/titan-probe.mjs (drives adb through Node spawnSync, so device paths never pass through Git Bash)

### EXP-0001 · 2026-01-01 · ✅ · #example #meta
**Context:** first task after KAIF was deployed into this project (example entry — replace with real ones).
**Tried / did:** wrote the first real lesson here in the canonical format.
**Result:** ✅ — the experience log is live and greppable.
**Lesson:** capture lessons at the level of *approach* (what worked / what to avoid), not defect detail
(that lives in `bugs/`); one short entry beats a long story.   → link: (none)
