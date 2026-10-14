# Plan 04 — Epic 01, phase 1: a reliable engine — large photos, 8× and 16×, the GPU fallback ladder

> **Создан:** 2026-10-14 (agent, guarded loop; the owner's order of phases 0 → 2 → 1 → 3 → 4, `plans/01_EPIC_mvp.md` §3)
> **Родитель:** `plans/01_EPIC_mvp.md` §3 row «1 — надёжный движок», gate: a series of 10 photos from 0.1 to 12 MP passes with no crash and memory back to its start; `MASTER_PLAN.md` phase 1
> **Статус:** 🔧 started 2026-10-14 ≈ 19:20 +03:00 — step 1 in work (no device: «работай автономно, без тестирования на клиенте»)
> **Вовне:** —

## Goal vector

Achieve: every scale of the owner's slider (1×, 2×, 4×, 8×, 16×) works within one stated output limit, and a GPU failure
never becomes a crash. Avoid: a result too large for memory reaching the engine at all.

## Acceptance criteria (Scale · Meter · Target)

1. 8× and 16×: Scale — output size; Meter — run on the Titan; Target — the 960×1280 photo gives 7680×10240 at 8×, and
   16× is refused for it with a notice (output over the limit).
2. Output limit: Scale — output megapixels; Meter — the setup screen; Target — above `MAX_OUTPUT_MP` the «Улучшить»
   button is off and a notice names the largest scale that fits.
3. Fallback ladder (plans/01 §5.1 risk 2): a forced GPU failure retries with a halved tile, then on the CPU, then shows
   the honest error — three screenshots.
4. Series: 10 photos 0.1–12 MP in a row, `dumpsys meminfo` of the app before and after within 10 %.

## Steps

1. [~] (built 2026-10-14 ≈ 19:21, `PassPlanTest` 5/5 mutation-proven; the device run of 8× and the refused 16× waits for the owner's «потом потестируем») 8× and 16× (`[AI]` how, a change from plans/01 §4.5.1): 8× = the 4× pass, scaled down to 2×, then a second 4× pass
   (the intermediate stays at 2× instead of 16× — a quarter of the memory); 16× = two 4× passes. Progress counts the
   tiles of both passes; the estimate adds the second pass's megapixels. Output limit `MAX_OUTPUT_MP = 200` — the size
   the 4× of a 12.5 MP photo already reaches in slice 1 of phase 2 — checked before the run.
2. [~] (built 2026-10-14 ≈ 19:22: `UpscaleViewModel.withFallback`, each step logged with tag `KARTUN`; the ring maps each pass's own done/total onto its share, so 100 px tiles do not overrun it; the forced-failure run on the device waits for the owner) Fallback ladder in `UpscaleViewModel`: on a native failure retry with tile 100, then CPU, then the error.
3. [ ] Tiles to disk for results over the limit (phase 1's large-photo goal) — research first (`researches/`): a PNG/JPEG
   encoder fed by rows, without one bitmap of the whole result.
4. [ ] The 10-photo series on the Titan with `dumpsys meminfo` → run report.

## Risks

- A second pass doubles the time (the estimate must say so — the owner's «эстимация»).
- 200 MP held as one bitmap (800 MB of native memory) may be killed by the low-memory killer on smaller phones — step 3
  is the real cure; until then the limit is a guess tested only on the 12 GB Titan.
