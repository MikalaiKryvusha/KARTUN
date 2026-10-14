# KARTUN — External structure map

> **The EXTERNAL map: what the project looks like from the outside** — its directories, files, and the
> cross-references between them. Companion: `PROJECT_ARCHITECTURE_INTERNAL_MAP.md` (how the system thinks).
> **Living reference — never DONE-tagged.** Keep the tree honest in the same change that moves a file.

---

## The tree (2026-10-14: KAIF deployed, no Android code yet)

```
KARTUN/
├── GOAL.md                     # the owner's vision (verbatim original, committed first)
├── MASTER_PLAN.md · STATUS.md  # path to the vision · where we are now (owner-facing, Russian)
├── AGENT_GUIDE.md + 4 *_FRAMEWORK.md / PHILOSOPHY.md   # the KAIF canon (English)
├── HOUSE_RULES.md              # local law: owner rules, the Titan stand, env dossier, recipes (Russian)
├── EXPERIENCE.md · PROJECT_HISTORY.md · KAIF_FRAMEWORK.md
├── AUTHOR_STYLOMETRY.md        # owner's voice portrait, public snapshot — GITIGNORED, never ships
├── researches/                 # recon docs: 01 KrinikCam style · 02 Upscayl RE · 03 engine/licences/Android
├── plans/ ideas/ bugs/ interviews/ homeworks/ reports/   # KAIF knowledge dirs (README in each)
├── .kaif/                      # KAIF machinery: kaif-core.mjs, hooks/, tools/, spheres/, kaif.json marker
├── .claude/ .agents/ .grok/ .cline/ .clinerules/ .roo/ AGENTS.md CLAUDE.md   # skills for 5 agent systems
└── package.json                # kaif:* npm handles only
```

**Planned by the MVP epic (not created yet):** Gradle project — `app/` (Compose UI, MVVM), `core/ui` (Krinik theme
tokens), `engine/` (C++ + JNI over prebuilt ncnn, `src/main/cpp/`, CMake), `models/` or `app/src/main/assets/models/`
(BSD-3 Real-ESRGAN family), `tools/` (build/device harness), `docs/` licences/NOTICE.

## What each part is

| Path | What it is | Depends on / references |
|------|-----------|-------------------------|
| `GOAL.md` | owner's vision, verbatim | source for `MASTER_PLAN.md` |
| `MASTER_PLAN.md` | phased path to the vision | `GOAL.md`, `plans/` |
| `HOUSE_RULES.md` | Titan stand, build recipe, owner rules R1–R2 | `researches/`, KAST's house rules (read-only) |
| `researches/0N_*.md` | recon: style, Upscayl, engine/licences | feed `plans/` and `interviews/` |
| `.kaif/` | deployed framework machinery — never edit generated parts by hand | origin `MikalaiKryvusha/KAIF` |

## Cross-references & dependency rules

- Recon (`researches/`) → plans (`plans/`) → code; code cites the plan line it implements (`AGENT_GUIDE.md` step 8).
- Skill mirrors in `.agents/ .grok/ .cline/ .roo/` are generated from `.claude/skills/` (`node .kaif/kaif-core.mjs sync`) —
  edit the canonical copy only.
- Nothing under `C:\Program Files\Upscayl` or any Upscayl repo is copied into this tree (AGPL-3.0, research 02/03).

## Entry points

1. `STATUS.md` → 2. `GOAL.md` → 3. `MASTER_PLAN.md` → 4. `HOUSE_RULES.md` → 5. `researches/03_*` (engine) → 6. `plans/`.
