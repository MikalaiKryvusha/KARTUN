# Bug 01 — the app aborts in libomp's affinity init on the first model load (`__kmp_create_affinity_none_places`)

**Status:** ✅ DONE 2026-10-16 11:13 +03:00 — reproduced deterministically 11:08, fixed (one process-wide engine thread), the repro runs clean on the Titan
**Severity:** S2 — the owner's run was lost (a crash in front of the owner; no data harmed)
**Version/build:** debug build installed on the Titan by the 2026-10-15 16:15–18:23 session (spinner tweaks, `376dd03`); libkartun_engine.so BuildId `c474378f83ced813dd3eb24fc1ee6370fa09fd91`; NDK `29.0.14206865` (its libomp is linked into libkartun_engine.so — `__kmp_*` frames are inside it)
**When/context:** 2026-10-15 19:00:21 +03:00, the owner on the Titan («и я сегодня запускал сборку - и креш поймал» · «в часов 19 вечера +-»); reported 20:52, found in the dropbox 20:53
**Fixing:** this session — reproduction first, then the fix

## Goal vector and acceptance

Avoid: a model load never kills the app. Done when the reproduction below aborts on the old build and runs clean on
the fixed one (observed on the Titan), and the owner's path — open the app, pick a photo, wait for the model — passes.

## Symptom

KARTUN closed itself while preloading a model (`UpscaleViewModel.preload` → `ensureLoaded` → `NcnnUpscaler.nativeCreate`).
Process uptime 1442 s (started ≈ 18:36), foreground. SIGABRT from OpenMP's own debug assert.

## Forensics (dropbox `data_app_native_crash` 2026-10-15 19:00:21; logcat was cleared later by the probe scripts' `logcat -c`)

```
pid: 18196, tid: 24141, name: pool-7-thread-1  >>> com.kriniks.kartun.debug <<<
signal 6 (SIGABRT), code -1 (SI_QUEUE)
#00 libc.so (abort+156)
#01 libkartun_engine.so (__kmp_abort_process+52)
#02 (__kmp_fatal+128)
#03 (__kmp_debug_assert+140)
#04 (__kmp_create_affinity_none_places(kmp_affinity_t&)+152)
#05 (__kmp_aux_affinity_initialize(kmp_affinity_t&)+12964)
#06 (__kmp_affinity_initialize+60)
#07 (__kmp_do_middle_initialize()+76)
#08 (__kmp_parallel_initialize+276)
#09 (__kmp_fork_call+160)
#10 (__kmpc_fork_call+240)
#11 (ncnn::Cast_arm::forward(...)+736)
#12 (ncnn::Mat::from_float16(...)+248)
#13 (ncnn::ModelBinFromDataReader::load(int, int) const+1488)
#14 (ncnn::Convolution::load_model(...)+136)
#15 (ncnn::Net::load_model(...)+484)
#17 (Java_com_kriniks_kartun_engine_NcnnUpscaler_nativeCreate+448)
... UpscaleViewModel.ensureLoaded ← UpscaleViewModel$preload
```

So: the FIRST OpenMP parallel region of that process (24 minutes in) initialised libomp's affinity and failed its assert.
LLVM main, `openmp/runtime/src/kmp_affinity.cpp`:

```
static void __kmp_create_affinity_none_places(kmp_affinity_t &affinity) {
  KMP_ASSERT(__kmp_affin_fullMask != NULL);
  KMP_ASSERT(affinity.type == affinity_none);
  KMP_ASSERT(__kmp_avail_proc == __kmp_topology->get_num_hw_threads());
```

`__kmp_avail_proc` = bits in the affinity mask of the thread that initialises OpenMP (`__kmp_get_system_affinity`, `respect`);
the topology counts hardware threads. Titan: CPUs possible/present/online `0-7`, 8 in `/proc/cpuinfo` (probe 20:54).
Which of the three asserts fired is not known (the `OMP: Error #13 … kmp_affinity…(line)` line went to the cleared logcat).

## Reproduction hunt (TESTING_FRAMEWORK activity 6)

| # | Variant (axis) | Result |
|---|---|---|
| 1 | `test_cast` (ncnn tests, NDK r29 libomp static) on the Titan, full mask | exit 0 |
| 2 | same under `taskset 0f` (whole process on cores 0-3) | exit 0 |
| 3 | same under `taskset f0` (cores 4-7) | exit 0 |
| — | 2026-10-15 19:52–20:52 this session: dozens of fresh app processes loading models (GPU), no abort | — |

Not reproduced yet. Untried axes: a long-lived process that went background → foreground before its first model load
(Android moves the process between cpusets); the CPU path (`gpu=false`) vs GPU; a thread whose mask changes between the
mask read and the topology read.

The owner's path (chat 20:58–21:00): «мне кажется, что KARTUN был свёрнут» · «и я его развернул тогда» · «и выбрал фото в
галерее» · «и оно упало». Observed on the Titan 20:56–20:59: foreground `/top-app` cpus 0-7; 12 s after HOME `/background`
cpus 0-3 (every thread); with the system photo picker open `/foreground` cpus 0-7; back `/top-app` 0-7.

| # | Variant (axis) | Result |
|---|---|---|
| 4 | `omp_mask` (NDK r29, `-fstatic-openmp`): first `omp_*` call on one mask, first parallel region on another — ff→0f, 0f→ff, ff→f0, 0f→0f | all ok |
| 5 | `omp_race`: another thread flips the initialising thread's mask 0-3 ↔ 0-7 during the first parallel region, 300 runs | 300 ok |
| 6 | the same program as the app user (`run-as`, SELinux `runas_app`), `KMP_AFFINITY=verbose` | ok: libomp parses `/proc/cpuinfo` for the masked CPUs only, topology = mask |

Three+ variants failed → research, not more blind variants. Not tried yet: the owner's exact path (long background,
then restore, picker, preload) in the real app.

**Forensics for the next time (2026-10-15 21:04, commit below):** the app now keeps a crash journal
(`files/crashes/`, `CrashLog.kt`): system exit records with tombstones, native stderr in `engine-stderr.log` (libomp's
assert text lands there), and before every model load a line with the thread, cpuset and allowed CPUs. Verified with a
SIGSEGV control crash and the engine's witness line.

## Hypotheses (ranked; none proven)

1. The initialising thread's affinity mask differs from what libomp's topology counts at that moment — e.g. the process
   sat in a restricted cpuset (background) and was moved to top-app around the first OpenMP call. Whole-process `taskset`
   (variants 2–3) did not trigger it, so a plain restricted mask is not enough.
2. A libomp defect in NDK r29's runtime on this kernel/topology (MediaTek MT8792, 1+3+4 cores).

## Fix options (to choose after the reproduction)

- Initialise OpenMP once at engine start in a known state (a dummy parallel region in JNI_OnLoad) — the init then never
  races a cpuset move; does not help if the assert fires in any state.
- Build ncnn with `NCNN_SIMPLEOMP=ON` (ncnn's own small OpenMP runtime, no libomp affinity code) — the engine already
  can take a self-built ncnn (`KARTUN_NCNN_DIR`).
- `KMP_AFFINITY` env before the first OpenMP call — `disabled` still goes through `__kmp_create_affinity_none_places`
  (`__kmp_affinity_initialize` slams it to `affinity_none`), so it is not a fix by itself.

## Links

- ncnn FAQ, crash in `__kmp_abort_process` on Android (multiple OpenMP runtimes; static `-fstatic-openmp`):
  https://ide-kyxt.pcl.ac.cn/nihui/ncnn/raw/commit/f1bdc87478c64e0dfdba3d679e6e0dfd4b84df80/docs/how-to-use-and-FAQ/FAQ-ncnn-throw-error.md
- LLVM `kmp_affinity.cpp`: https://raw.githubusercontent.com/llvm/llvm-project/main/openmp/runtime/src/kmp_affinity.cpp

## The owner's second crash — 2026-10-16 10:23:32, the journal caught the text

`files/crashes/2026-10-16_10-23-32_native.txt` + `engine-stderr.log` (pulled 11:04):

```
2026-10-16_00-45-18 load 4x-ClearRealityV1-fp16 gpu=true thread=pool-5-thread-1 cpuset=/top-app cpus=0-7
native stderr -> this file, pid 981
2026-10-16_01-36-22 load 4xNomosWebPhoto-RealPLKSR-fp16 gpu=true thread=pool-5-thread-1 cpuset=/top-app cpus=0-7
2026-10-16_01-36-37 load realesrgan-x4plus gpu=true thread=pool-5-thread-1 cpuset=/top-app cpus=0-7
2026-10-16_10-23-31 load realesrgan-x4plus gpu=true thread=pool-7-thread-1 cpuset=/top-app cpus=0-7
OMP: Error #13: Assertion failure at kmp_affinity.cpp(4755).
```

The same process (pid 981) loaded models on `pool-5-thread-1` at night and died loading on a NEW thread `pool-7-thread-1`
in the morning — and the 2026-10-15 19:00 crash was on `pool-7-thread-1` too. Line 4755 of NDK r29's libomp (LLVM
`386af4a5`, `clang_source_info.md` of the NDK) is the third assert:
`KMP_ASSERT(__kmp_avail_proc == __kmp_topology->get_num_hw_threads())`.

## Root cause

`UpscaleViewModel` owned its engine thread (`Executors.newSingleThreadExecutor()`) and closed it in `onCleared` — when the
screen leaves for good (Back out of the app; the process stays). libomp is linked STATICALLY into the engine, and a static
libomp shuts itself down when the last thread that used it exits (`__kmp_internal_end_thread` in `kmp_runtime.cpp`: the
`KMP_DYNAMIC_LIB` early return is compiled out, all roots gone → `__kmp_internal_end`). The next screen's new thread
re-initialises libomp from that torn-down state and its affinity init fails the assert above. Variants 1–6 never stopped
the first OpenMP thread, so they could not see it.

**Deterministic repro (old build, Titan 11:06–11:08):** open KARTUN, pick a photo (the model loads) → Back, Back (the app
leaves, the process stays — `pidof` 10628 before and after) → open KARTUN from the launcher → pick a photo → the process is
gone; `engine-stderr.log`: `load realesr-general-x4v3 … thread=pool-7-thread-1` + `OMP: Error #13: Assertion failure at
kmp_affinity.cpp(4755)`; journal `2026-10-16_11-08-10_native.txt`.

## Fix

`EngineThread.kt`: one engine thread for the whole process (`kartun-engine`), never shut down; `UpscaleViewModel` and the
debug probe (`DebugTools`, it had a second engine thread of its own) both use it; `onCleared` releases the net on it and no
longer closes it. This is architecture invariant 4 ("all ncnn/Vulkan calls run on ONE dedicated thread") made true per
process instead of per screen. Guard: `EngineThreadGuardTest` (red on `eda6a40`'s sources — three offenders named; green
after the fix).

**Verified (new build, Titan 11:10–11:13):** the repro script (pick → Back out → reopen → pick, 3 rounds, twice) — the
process lived through every round (pid 12185, then 12615), loads after a Back-out on `thread=kartun-engine`
(11:10:38 → 11:11:08; 11:12:20 → 11:12:50), no new crash record. Run report:
`testcases/reports/2026-10-16_setup_cards_and_bug01.md`.

Hygiene: 43 JVM tests green (`.\gradlew.bat :app:testDebugUnitTest assembleDebug`) · Functional run: the owner's path on
the Titan, above.

TWINS: searched `Executors\.new\w+` in `app/src/main` and `app/src/debug` — 2 sites, both engine threads, both moved to
`EngineThread`; no other executor in the app.

## Decisions made without the owner

- `[AI]` The fix is the shared thread, not `NCNN_SIMPLEOMP` (a rebuilt ncnn) from the options above: the cause is the
  thread's exit, and one never-stopped thread removes it without touching the engine or its speed.
- `[AI]` The debug probe shares the same thread (two engine threads would let the second one's exit matter again).
- Hypotheses 1 and 2 above: refuted — the cpuset was `/top-app 0-7` at both crashes; the trigger is the thread's exit.

## Note 2026-10-16 01:05 +03:00 — the engine is stripped from now on

Since the app module names NDK 29 (commit after `e87c12a`), AGP strips `libkartun_engine.so` in debug and release (16.3 →
9.9 MB). A tombstone pulled after the next crash will show offsets without `__kmp_*` names: symbolize on the PC with
`ndk-stack -sym <unstripped dir> -dump <tombstone or _native.txt>` — the unstripped copy of each build stays in
`app/build/intermediates/merged_native_libs/<variant>/…/arm64-v8a/` (same BuildId as on the device).


## ✅ STATUS: DONE (2026-10-16 11:13 +03:00)

Reproduced on the old build, clean on the fixed one, both on the Titan by the owner's path; guard `EngineThreadGuardTest`.
