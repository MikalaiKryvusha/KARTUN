# Research 01 — KrinikCam as the style and stack reference for KARTUN

> **Создан:** 2026-10-14 (agent: Claude Opus 5.5, read-only study of a shallow clone of `MikalaiKryvusha/KrinikCam` HEAD `d33c055`, v0.8 (74))
> **Родитель:** `GOAL.md` — «проектом KrinikCam - для использования узнаваемого Криником стиля приложения, в его любимом темно розовом стиле»
> **Статус:** 🔬 living reference — recon done 2026-10-14; reuse decisions land in the MVP epic plan
> **Вовне:** —

All paths below are relative to the KrinikCam repository root. Hex values are verbatim from source.

## 1. Visual identity — "the Krinik style"

**Headline:** a near-black UI with exactly ONE brand accent — acid pink **`#FF1A8C`**; flat dark-grey surfaces, 10dp
corners, small pink UPPERCASE section headers, pink filled Material icons, solid pink primary buttons. No glows, blurs or
gradients in the app UI. The theme module exists, but screens mostly hardcode private color constants (drift to avoid).

### Palette tokens (`core/ui/src/main/kotlin/com/kriniks/kcam/core/ui/theme/Color.kt`)

| Token | Hex | Role |
|---|---|---|
| `AcidPink` | `#FF1A8C` | brand primary («кислый розовый») |
| `AcidPinkDark` | `#CC0070` | pressed / primaryContainer |
| `AcidPinkLight` | `#FF6BBD` | secondary / onPrimaryContainer |
| `DarkBackground` | `#0D0D0D` | background |
| `DarkSurface` | `#1A1A1A` | surface, cards, dialogs, sheets |
| `DarkSurfaceVariant` | `#2A2A2A` | surfaceVariant |
| `DarkOnSurface` / `DarkOnBackground` | `#F0F0F0` | text on dark |
| `ErrorRed` | `#CF6679` | error |
| `SuccessGreen` | `#4CAF50` | success |
| `WarningAmber` | `#FF9800` | warning |

De-facto colors hardcoded in screens: grey text tiers `#888888` (subtitles, 30 uses) · `#BBBBBB` (dialog body) ·
`#CCCCCC` · `#666666` (aux) · `#555555` (chevrons/disabled); inner cards `#232323`; dropdown `#3A3A3A`; unfocused field
border `#444444`; translucent rows `0x66151515` / `0xCC151515`; pink tints `0x33FF1A8C` (active row / chip border),
`AcidPink` α 0.4 (switch track); muted label `#E68FB4`.

### Theme, type, shape
- **Effectively dark-only:** `KrinikCamTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false)`; screens hardcode
  dark colors; window theme `android:Theme.Material.Light.NoActionBar` + black window background; `enableEdgeToEdge()`.
- **Font:** system default (`FontFamily.Default`), no custom fonts. Type scale in `core/ui/.../theme/Type.kt`
  (headlineLarge 28/36 Bold … labelSmall 10/14). Screens actually use: section header 11sp Bold, letterSpacing 1.2sp,
  UPPERCASE, pink · row title 15sp Medium white · subtitle 12sp `#888888` · aux 11sp `#666666` · sheet title 18sp Bold.
  Tabular figures for numbers (`fontFeatureSettings = "tnum"`).
- **Shape/spacing:** 10dp house radius (cards, rows); 8dp pills; `CircleShape` FABs; 16dp horizontal screen gutter;
  settings rows 16dp/14dp padding, 22dp icons.
- **Motion:** pulsing standby logo (α 0.6→1, 1200 ms); `AnimatedVisibility` dropdowns.
- **Icons:** Material Icons Extended, filled, tinted pink. Launcher icon: adaptive vector, solid `#FF1A8C` background,
  white camera glyph (self-described "temporary placeholder"); no monochrome layer. Icon pipeline `tools/graphics/`
  (SVG → PNG mipmaps).

## 2. Reusable Compose components

- `app/.../ui/screens/SettingsScreen.kt`: `SettingsSection` (pink caption + `DarkSurface` card, 10dp), `SettingsRow`
  (pink icon · title · subtitle · aux · chevron), `LanguageDialog` (radio list, platform `LocaleManager`),
  `ProjectInfoDialog` / `AuthorInfoDialog` (About, social links), `LinkRow`.
- `app/.../ui/overlay/FloatingPanelMenu.kt`: scrim + vertical list; `PanelActionRow` primary = solid pink.
- `feature/streaming/.../ui/`: `ModalBottomSheet` on `DarkSurface`; `KcamTextField` / `kcamTextFieldColors()` (pink focus
  border and cursor); `kcamChipColors()`; pink-thumb switches; `UiText.kt` (ViewModel → localizable text).
- `DevMenuScreen.kt` `DevToggleRow`: title + `[i]` info dialog + switch — the owner's "every setting has an [i]" rule.
- Snackbars at the TOP (bottom ones were hidden by FABs). **Absent:** Slider, progress indicators, Toasts — KARTUN must
  design these in the same style.

## 3. Build and toolchain (as of KrinikCam v0.8)

Gradle 8.10 · AGP 8.7.3 · Kotlin 2.1.0 (+ compose compiler plugin) · KSP 2.1.0-1.0.29 · Compose BOM 2024.12.01 ·
compileSdk/targetSdk 35 · minSdk 33 · Java 17 toolchain. Catalog: core-ktx 1.15.0, lifecycle 2.8.7, activity-compose
1.9.3, Hilt 2.53.1, navigation-compose 2.8.4, Room 2.6.1, DataStore 1.1.1, coroutines 1.9.0, Timber 5.0.1,
kotlinx-serialization 1.7.3. Modules `:app`, `:core:common`, `:core:ui`, `:core:logging`, `:data:*`, `:feature:*`;
rule: feature modules never depend on each other. No convention plugins; no NDK/CMake of its own; `proguard-rules.pro`
keeps JNI `native <methods>`. Versioning: `version.json` `{major, minor, build}`,
`versionCode = major*1_000_000 + minor*10_000 + build + 1`, `versionName "X.Y (build)"`, `BUILD_TIME` buildConfigField;
release signing from gitignored `keystore.properties`. CI: `build.yml` (assembleDebug) + `release.yml` (tag `v*`).
Testing: JVM JUnit4 only; UI is driven over ADB with `tools/ui.mjs`.

**For KARTUN (2026-10):** these versions are ~10 months old; KARTUN targets API 36 → re-pick current stable versions at
project creation, keep the SHAPE (catalog, module rule, versioning formula, signing).

## 4. Localization

EN default (`values/`) + RU (`values-ru/`), plurals; `res/xml/locales_config.xml` + `android:localeConfig`; custom
language dialog over the platform per-app language API (`LocaleManager`, Android 13+). No translation tooling or parity
check. KrinikCam's GOAL target list: RU, EN, ES, PT, ZH, HI, BN, FR, DE, AR (`MASTER_PLAN.md:413`).

## 5. Owner preferences quoted from KrinikCam that also apply to KARTUN

- Brand color: «Цвет бренда: `#FF1A8C` (acid pink)» — `AGENT_GUIDE.md`, code style.
- «Минимум легаси кода… Ниже Android 13 даже не берёмся. Только современное мощное железо и ОС» — `interviews/interview_001…:45`.
- MIT; «Не используем проприетарных библиотек… Пользуемся OpenSource» — `GOAL.md:12`.
- «делаем современный, красивый, лёгкий и удобный UI UX… для мобильных устройств с маленькими экранами, так и для больших
  планшетов. У каждой опции в настройках есть инфо кнопка [i]» — `GOAL.md:40`.
- «всё в список, радиалка сильно грузит» — `FloatingActionMenu.kt:3-4`.
- Selected chips must be brand pink; default M3 grey «выпадал из стиля приложения» — `ScenesManagerOverlay.kt:59`.
- «приложение само понимает язык системы… в два клика можно быстро переключить язык» — `GOAL.md:17`; RU quality first, EN second.
- «как обычный APK в релизах должен быть, так и возможность установки через… Obtainium» — `GOAL.md:25`; release only on
  the owner's explicit word.
- README EN + RU duplicate, README.pdf, «красивым, чтобы на Github выглядел шикарно» — `GOAL.md:19`; author credit
  «Mikalai Kryvusha aka KOT KRINIK (Николай Кривуша, Кот Криник)… проект — вайбкод» with social links — `GOAL.md:21`.
- He likes choosing from mockups («4 варианта мокапа» — interview_014).

## 6. Reuse verdict

**Reuse as-is:** palette tokens; system font + type scale; 10dp radius / 16dp gutter; `SettingsSection`/`SettingsRow`/
`LinkRow`/About dialogs/`LanguageDialog` (+ `locales_config.xml`); text-field/chip/switch/button colors; `UiText`;
top snackbars; `[i]` info dialogs; versioning formula, signing scheme, R8 JNI keep rule, MIT, EN+RU README layout.

**Adapt:** make KARTUN truly dark-only and token-driven (`MaterialTheme.colorScheme`, a `Shapes` object — no per-file
color copies); design pink progress indicators and the before/after slider (KrinikCam has none); new launcher icon for an
upscaler (+ monochrome layer); add a native `:engine` module (CMake/NDK, `abiFilters arm64-v8a`); add a translation
parity check for 10 languages; current library versions for API 36.
