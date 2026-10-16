# Bug 04 — RC4: первый запуск падает в конструкторе UpscaleViewModel (поле объявлено ниже `init`)

**Status:** 🔧 fix pending verification · **Severity:** S1 · **Fixing:** this session (RC5)
**Version/build:** release `0.1 (275)`, commit `3c8ca15` (RC4) · **When/context:** 2026-10-16 22:32 +03:00, the final RC4
round — the owner on the Titan: «КРЕШ НА ТИТАНЕ!!!!!!»; the Redmi agent hit the same crash at the same time after `pm clear`.

## Goal vector

Avoid: a fresh install (or `pm clear`) of KARTUN never dies on its first launch. Fit criterion — Situation: KARTUN freshly
installed, no bench done yet. Action: the user opens KARTUN. Result: the first-launch check dialog appears and closes itself;
`logcat -b crash` stays empty. Check: `adb shell pm clear com.kriniks.kartun; adb shell am start -n com.kriniks.kartun/.MainActivity`,
then `adb logcat -d -b crash` prints nothing and `logcat -d | grep '"bench":true'` prints one line.

## Symptom

The app closes right after the start, every start, until data is cleared — the bench is never recorded, so every launch takes the
same path. `logcat -b crash` on the Titan (2026-10-16 22:32:49):

```
FATAL EXCEPTION: main · Process: com.kriniks.kartun
java.lang.RuntimeException: Cannot create an instance of class jn1          (jn1 = com.kriniks.kartun.UpscaleViewModel)
Caused by: java.lang.reflect.InvocationTargetException
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'void ft0.setValue(java.lang.Object)' on a null object reference
    at jn1.<init>(:643)     → mapping (pg_map_id 9e1335b4…, = the RC4 build): setBenchSaver():768 ← startBenchIfNeeded():213 ← <init>:199
```

## Repro (deterministic)

Release RC4 on any device, bench not done: `pm clear com.kriniks.kartun` → launch → crash. Seen on the Titan and the Redmi
Note 12 Pro+.

## Root cause

Kotlin runs property initializers and `init` blocks in source order. `init` (line 189) ends with `startBenchIfNeeded()`;
it writes `benchSaver` (with saver on) or calls `runBench()` → `stopSaverWatch()` → `benchSaver = false` (saver off). `benchSaver`
was declared at line 206, BELOW `init`, so its state delegate was still null → NPE in the constructor → the ViewModel factory fails.
Commit `0e418fd` (battery saver warning) added `benchSaver` below `init`; it was `[NOT-TESTED]` and RC4 was frozen on top of it.

The SAME class already crashed the first launch at 20:06 the same day (`benchRunning`, Redmi): that fix moved one property
above `init` and left a comment — no guard for the class, so the next property walked into it.

## Fix

- `init` moved to the END of the class (below every property), so no property can be read before its initializer — the class is
  closed by form, not by moving one more field. All properties that were below `init` have constant initializers (`false`, `null`,
  `Screen.Settings`, `AtomicBoolean(false)`), so their order relative to `init` changes nothing else.
- Guard `app/src/test/kotlin/com/kriniks/kartun/InitLastGuardTest.kt` (`@guard init-last`): any class-level `val`/`var` below a
  class-level `init {` in app sources reddens the unit tests. Proven red against the `3c8ca15` source of `UpscaleViewModel.kt`:
  8 offenders named, `benchSaver:206` and `saverWatch:207` first; green on the fix; full suite 78 tests, 0 failures.

TWINS: searched `^\s*init {` in `app/src` — found 0 other sites (UpscaleViewModel is the only class with an init block).

## Verification

- Hygiene: guard red on `3c8ca15`, green after; `:app:testDebugUnitTest assembleDebug` green (2026-10-16 22:3x).
- Functional run: RC5 — `pm clear` + launch on the devices (without the Titan until the owner returns it; the battery-saver path
  H2 is Titan-only and waits for it).

## Decisions made without the owner

- `[AI]` Fix by form (init last + guard) instead of moving the two fields above `init` — the second occurrence of the class in one day.
- `[AI]` The RC4 round was stopped on all four devices; the round is re-run on RC5.

## Links

`testcases/TC_release_1_0_rc4.md` H1/H2 · commit `0e418fd` (where `benchSaver` came in) · EXP entry for the class
