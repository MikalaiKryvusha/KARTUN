# Bug 01 — the app aborts in libomp's affinity init on the first model load (`__kmp_create_affinity_none_places`)

**Status:** 🔴 OPEN — reproduction being hunted
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

## Decisions made without the owner

(filled at closing)
