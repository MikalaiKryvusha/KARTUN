# Research 10 — The agent API recon: the ADB door, the LAN door, the job API (deepens research 06)

> **Создан:** 2026-10-16 (agent; `/plan-epic` rung 1 on the owner's order in chat 2026-10-15: «API для ИИ-агентов по ADB и HTTPS (ideas/02); Сейчас делаем!»; owner's LAN requirement ≈ 23:58: «можно чтобы просто в локальной сети умело работать» · «не обязательно по интернету» · «чтобы IP девайса и порт приложения можно было дать, и этого хватало для работы по локальной сети»)
> **Родитель:** `ideas/02_agent_api_adb_and_lan_workstation.md`; `researches/06_agent_station.md` (the first sweep — this doc closes its open questions and corrects §1.B on Ktor CIO); `plans/01_EPIC_mvp.md` §4.9; `interviews/interview_001_mvp_forks.md` Q1
> **Статус:** ✅ written 2026-10-16 — feeds `plans/06_EPIC_agent_api.md` and `plans/07_epic06_A_adb_api.md`; device facts marked `[UNVERIFIED]` are checked in step 1 of plan 07
> **Вовне:** the owner-level forks (§5) → the owner through `plans/06_EPIC_agent_api.md` §6

Numbering note: the task named this file `09_…`; `09_lossless_scaling_and_video.md` already holds that number, so this is 10.

## 1. The ADB door — how proven projects drive an app from adb

### 1.1 Transport options

| Option | Invocation | Sync answer | Can start a foreground service (FGS) from the background | Takes the screen | Guard against other apps | Precedent | Verdict |
|---|---|---|---|---|---|---|---|
| A. `am start` an activity | `adb shell am start -n pkg/.Act --es …` | no (logcat) | yes, the activity is visible | **yes** | exported activity, no guard | our phase 0 probe (`tools/titan-probe.mjs`) | rejected for jobs: EXP-0010 («почему черный экран сейчас?») |
| B. `am broadcast` to a receiver | `adb shell am broadcast -n pkg/.Rcv -a … --es …` | no; ~10 s receiver budget | **no** — the receiver calls `startForegroundService` under the APP's uid, and a background app has no exemption (§1.4) | no | `android:permission="android.permission.DUMP"` | androidx WorkManager `DiagnosticsReceiver` and androidx ProfileInstaller `ProfileInstallReceiver`: both `exported="true"` + `android:permission="android.permission.DUMP"` [S3][S4] | fine for quick commands, not for a long job |
| C. `am start-foreground-service` to a service | `adb shell am start-foreground-service -n pkg/.Svc -a … --es …` | no (status file) | **yes** — AOSP `ActiveServices` treats `SHELL_UID` callers as `REASON_SYSTEM_UID` [S1] | no | `android:permission="android.permission.DUMP"` (shell holds DUMP [S2]) | same DUMP guard as row B | **recommended door** |
| D. `content call` to a provider | `adb shell content call --uri content://… --method …` | **yes** (Bundle, observed on the Titan — research 06 §1.A) | `[UNVERIFIED]` — the provider runs the call under the app's uid; the shell exemption covers only the shell's own binder call | no | `call()` is not permission-checked by the framework — check the calling uid in code | AOSP `content` command, OWASP MASTG-TECH-0148 (research 06) | alternative for synchronous queries; not needed in v1 |
| E. `adb forward` to an in-app HTTP server | `adb forward tcp:H tcp:D`, then HTTP to `127.0.0.1:H` | yes | n/a (server already in an FGS) | no | loopback is reachable by every app on the device → needs its own guard | Appium UiAutomator2: device server on 6790, host port 8200 by `adb forward` [S5] | comes free once the LAN server exists (phase B); too heavy for phase A |
| F. Instrumentation | `adb shell am instrument -w …` | yes | runs inside the test harness | no | test APK signature | Android test runner | for tests, not a product door (needs a test APK, restarts the target process) |

### 1.2 Files in and out

- `adb push` / `adb pull` into `/sdcard/Android/data/<pkg>/files/…` — already the phase 0 route on the Titan
  (`tools/titan-probe.mjs`, STATUS «Вход и модели — `adb push` в `/sdcard/Android/data/com.kriniks.kartun.debug/files/`»).
  Scoped storage hides that folder from other apps; the shell user reaches it.
- `adb exec-out cat <file>` returns a small file byte-exact (no CR/LF rewriting of `adb shell`) — the read path for JSON.
- `content read` / `content write` over a provider's `openFile` is the alternative when a folder is not reachable; slower
  and adds a provider — not needed.
- `[UNVERIFIED]` the app writes into a sub-folder that the SHELL created by `adb push`; plan 07 step 1 checks it, the
  fallback is a flat layout in an `agent/` folder the app creates itself.
- EXP-0002: from Git Bash, device paths in argv are rewritten into Windows paths — the CLI calls adb through Node
  `spawnSync` with an argv array (no shell), as `tools/titan-probe.mjs` already does.

### 1.3 Reporting progress and results back

| Channel | Price |
|---|---|
| logcat tag (today's probe) | needs `logcat -c` (clears the owner's log), the ring buffer can wrap on a long run, lines must be grepped out of noise |
| status file written atomically (`tmp` + `rename`) next to the result | one `adb exec-out cat` per poll; the agent can read it by hand; survives the app's death as the last known state |
| provider `call` (row D) | synchronous, but the Bundle prints as `Result: Bundle[{…}]`, which the CLI must parse |

Recommended: the status file. One mechanism (files) carries input, progress, result and errors.

### 1.4 Android 14–16 limits that bind the ADB door

- **Background activity launch (BAL).** The shell holds `START_ACTIVITIES_FROM_BACKGROUND` [S2], so row A works — but
  it takes the owner's screen (EXP-0010). Android 14/15 tightened only `PendingIntent` senders and creators [S6].
- **FGS start from the background (Android 12+).** Exemptions are listed in [S7]; none covers "an app answering a
  broadcast or a provider call". AOSP source [S1], `shouldAllowFgsWhileInUsePermissionLocked`:
  `case ROOT_UID: case SYSTEM_UID: case NFC_UID: case SHELL_UID: isCallerSystem = true;` → `ret = REASON_SYSTEM_UID;`,
  and `shouldAllowFgsStartForegroundNoBindingCheckLocked` starts from that value. So `am start-foreground-service` run
  by the shell is allowed while KARTUN is in the background or not running.
- **The 10-second promise.** A service started with `startForegroundService` must call `startForeground` within a few
  seconds or the system throws `ForegroundServiceDidNotStartInTimeException` [S7 troubleshooting]. Every command
  (UPSCALE, CANCEL, INFO) therefore calls `startForeground` first.
- **`mediaProcessing` type.** No runtime prerequisite; **6 hours out of every 24, shared by all of the app's
  mediaProcessing services**; at the limit `onTimeout` gives a few seconds to `stopSelf` or the app gets an ANR [S8].
  `UpscaleService` already handles `onTimeout`. A busy station hits this limit — a job then fails with `quota`.
- **Cached apps freezer.** On Android 14 a cached process is frozen 10 s after it becomes cached [S9]. Work that runs
  outside an FGS in a background process stalls → the job runs only inside the FGS.
- **Stopped state after install.** `[UNVERIFIED]` an explicit `am start-foreground-service` starts a freshly installed,
  never-opened app; plan 07 step 1 checks it.
- **Notifications.** Without `POST_NOTIFICATIONS` the FGS still runs, the notification is hidden. Interview #004 already
  asks for the permission on the first «Улучшить».

## 2. The LAN door — a server inside an Android app

### 2.1 What proven LAN tools do (the owner's «IP и порт — и этого хватало»)

| Tool | Default | Transport on the LAN | Auth | Source |
|---|---|---|---|---|
| Ollama | binds `127.0.0.1:11434`; `OLLAMA_HOST` opens it to the network | plain HTTP | none built in | [S10] |
| ComfyUI | `127.0.0.1`; `--listen 0.0.0.0` opens it | plain HTTP | none built in; docs advise firewall rules | [S11] |
| AUTOMATIC1111 web UI | `--listen` opens it | plain HTTP | optional `--api-auth user:pass` | [S12] |
| LocalSend (phone ↔ PC file transfer) | port 53317, multicast discovery | HTTPS with a self-signed certificate, its SHA-256 fingerprint is the device id; an unencrypted HTTP mode exists, and the browser download API is HTTP "because browsers reject self-signed certificates" | none (the receiver accepts on screen) | [S13] |
| Appium UiAutomator2 | device port 6790 | plain HTTP over `adb forward` | none | [S5] |

The common pattern for "give the address and it works": **opening to the network is an explicit opt-in on the serving
side, then plain HTTP with no client setup; auth is optional**. Encryption appears where files go between personal
devices (LocalSend), and even there the client does no certificate setup.

### 2.2 The price of zero setup — named honestly

With plain HTTP and no key, **anyone on the same Wi-Fi can use the station**: send images, load the GPU, drain the
battery. Images travel unencrypted on the Wi-Fi: on an open network anyone nearby reads them; on a WPA2-Personal home
network anyone who knows the Wi-Fi password and captures the handshake can. Mitigations that cost the client nothing:
the server is off until the user turns it on; it answers only local addresses; job ids are random 128-bit values and
there is no "list all jobs" endpoint, so one client cannot fetch another's results; results are deleted after download.
An **optional access key** (off by default) closes "anyone on the Wi-Fi" at the price of one header on the client
(`Authorization: Bearer <key>`). **TLS** (self-signed, fingerprint) closes sniffing at the price of `curl -k` or a pinned
fingerprint on the client — the setup the owner asked to avoid.

### 2.3 Embedded server candidates (the app is MIT — GPL/AGPL excluded)

| Candidate | Licence | TLS | Maintenance / known issues | Notes |
|---|---|---|---|---|
| Ktor server, CIO engine | Apache-2.0 | **server TLS not supported** — `UnsupportedOperationException` from `CIOApplicationEngine` reported [S14]; corrects research 06 §1.B | maintained by JetBrains | runs on Android (research 06); routes are engine-agnostic, so a later HTTPS move is an engine swap (Netty), routes unchanged |
| Ktor server, Netty engine | Apache-2.0 | yes (`sslConnector`, JKS keystore) [S15] | maintained | heavy for an APK; JKS is not an Android keystore type → PKCS12/BKS conversion (research 06 §1.B) |
| NanoHTTPD 2.3.1 | BSD-3 (modified BSD) [S16] | yes, `makeSecure(SSLServerSocketFactory)` [S16] | last release 2.3.1; CVE-2020-13697 (XSS in `GeneralHandler`) [S17], CVE-2022-21230 (request bodies > 1 KB spill into world-readable temp files) [S18] | tiny; on Android `java.io.tmpdir` is the app's cache, which softens CVE-2022-21230, but the project carries open CVEs |
| Own HTTP/1.1 over a `ServerSocket` | ours (MIT) | via JSSE `SSLServerSocket` | ours to keep | a hand-written request parser on a network port is attack surface — rejected |

### 2.4 TLS on a LAN without a public CA (only if the owner picks HTTPS)

- No public CA signs a private IP; field practice is trust-on-first-use with an out-of-band fingerprint check (W3C
  "HTTPS for Local Networks", research 06 §1.D) and LocalSend's fingerprint-as-identity [S13].
- Key: the Android Keystore generates a key pair with a self-signed certificate [S19]; `[UNVERIFIED]` Conscrypt's server
  `SSLContext` accepts that non-exportable key — fallback: a PKCS12 file in `filesDir`.
- Client side: `curl --pinnedpubkey sha256//…` stays enforced even with `--insecure` [S20]; Node's documented pinning
  pattern is a `checkServerIdentity` callback that compares the certificate's public-key hash [S21].

### 2.5 Android restrictions for the LAN door

- **Android 17 local network permission.** KARTUN has `targetSdk = 37` (`app/build.gradle.kts`), so on Android 17
  accepting an incoming TCP connection needs `ACCESS_LOCAL_NETWORK`, a runtime permission [S22]; NSD/mDNS is covered
  too. On the Titan (Android 16) test it with `adb shell am compat enable RESTRICT_LOCAL_NETWORK <pkg>` + reboot [S22].
  Whether loopback (`adb forward`) counts as local network is not stated [S22] — `[UNVERIFIED]`.
- **Cleartext policy.** The network security config governs the app's OUTGOING connections; a server socket is not a
  client connection, so plain HTTP serving needs no `usesCleartextTraffic`. `[UNVERIFIED]` on the device — one curl
  in phase B's first step proves it.
- **Keeping the server alive.** An FGS: `connectedDevice` ("interactions with external devices that require a …
  network connection"; prerequisite: declare `CHANGE_NETWORK_STATE` or `CHANGE_WIFI_STATE`; no time limit) [S8]. The
  processing itself stays `mediaProcessing` with its 6 h / 24 h budget [S8].
- **Doze.** Doze starts when the device is "unplugged and stationary … with the screen off" and then "suspends network
  access"; foreground services are not listed as exempt [S23]. A station on the charger never enters Doze → the
  settings row says «держите устройство на зарядке», and the info endpoint reports `charging`.
- **Discovery.** NSD needs `ACCESS_LOCAL_NETWORK` on Android 17 and an mDNS library on the PC side; the owner's
  requirement is "give the IP and the port" — so v1 shows the address on screen and skips discovery.
- **The house rule.** Interview #001 Q1 = A: «сеть работает только по кнопке „Вставить ссылку“». The LAN server is a
  second network use; the owner's orders 2026-10-14 ≈ 14:15 / 22:41 and 2026-10-15 cover it. To keep the spirit of Q1 the
  server is an explicit switch in settings, off by default, never started by the system (no boot autostart — Android 15
  also bars several FGS types from `BOOT_COMPLETED` [S7]), with an always-visible notification while on.

## 3. The API shape

- **Job pattern.** Replicate: create → poll → result, states `starting · processing · succeeded · failed · canceled ·
  aborted` [S24]; Google AIP-151 long-running operations: the call returns an operation id at once, the client polls it
  [S25]. KARTUN states: `queued · running · succeeded · failed · canceled`.
- **One job document for both doors.** The status JSON is the same in the ADB status file and in the HTTP response, so
  `docs/agent-api.md` describes one schema and two transports.
- **Self-describing root (for «IP и порт — и хватало»).** `GET /` answers with a short JSON: what KARTUN is, the
  endpoints, one `curl` example. An agent that knows only the address discovers the API without any document.
- **A one-command path for agents.** `POST /v1/upscale?scale=4&wait=1` with the image as the body returns the result
  image directly (Replicate offers the same "wait" mode); without `wait` it returns the job JSON for polling. Long runs
  (DAT2 ≈ 29 min on a large photo) should poll.
- **Concurrency.** ncnn/Vulkan objects must stay on one thread (research 03 §B lesson 7; `NcnnUpscaler` KDoc). All
  doors and the UI share ONE engine thread; jobs queue FIFO behind it (queue cap, then `busy`).
- **CLI.** Node built-ins only (the project's tools are Node; no npm dependency), adb through `spawnSync`. Contract:
  one JSON line on stdout per result, progress on stderr, exit codes 0 ok · 1 job failed · 2 usage · 3 no device /
  transport.
- **MCP.** A thin MCP server over the CLI is the idiomatic last mile for Claude Code (research 06 §1.E); an agent with a
  shell already uses the CLI from the doc — MCP waits for the owner's word.

## 4. Local recon (read 2026-10-16, read-only; another agent is editing the catalogue code)

- `UpscaleViewModel` owns the engine thread (`Executors.newSingleThreadExecutor()`), the loaded net (`loaded`,
  `loadedModel`), the pass plan (`secondPassInput`, two 4× passes for 8×/16×), the fallback ladder (`withFallback`:
  200 px → 100 px → CPU → error), the limits (`MAX_INPUT_MP = 100`, `MAX_OUTPUT_MP = 500`), the learned estimate and
  `decode` from a `Uri`. An agent job must reuse this, not copy it → extract a process-wide engine object.
- `ProcessingState` is one global (progress + one `cancelRequested`), mirrored by `UpscaleService` (not exported,
  `mediaProcessing`). An agent job needs its own cancel flag and its own notification.
- `ModelStore` copies APK assets to `filesDir/models`; `Model` is an enum of two — the catalogue work (`plans/05`) is
  changing both right now. The agent's model list must come from whatever the catalogue lands.
- `ResultStore` encodes to the gallery (`saveToGallery`) or a share copy; an agent job needs "encode to this file" —
  a small addition reusing its `Format` and cancellable stream.
- `DebugTools.onIntent` (debug build only) is the phase 0 probe: `am start` + logcat line `KARTUN_PROBE`. It stays for
  engine probes; the agent door is separate.
- Manifest: `INTERNET` present (Q1 = A), no exported service, `targetSdk 37`.

## 5. Forks

Engineering forks (decided by the agent, cited by the plans):

- `FORK: ADB door` — (a) `am start-foreground-service` to a DUMP-guarded exported service + files ✅ · (b) provider
  `content call` · (c) `adb forward` to an HTTP server. Price of error: (b) may fail to start the FGS from the background
  (§1.4, unverified) — a job that dies on the freezer; (c) drags the whole LAN server into phase A. (a) is proven by
  AOSP source [S1] and by androidx's DUMP-guarded components [S3][S4].
- `FORK: status channel` — status file ✅ · logcat · provider Bundle (§1.3).
- `FORK: concurrency` — one engine thread, FIFO queue with a cap, `busy` above it ✅ · reject while busy · parallel
  engines (excluded: Mali lesson 7).
- `FORK: LAN server library` — with plain HTTP: Ktor CIO ✅ (maintained, Apache-2.0, HTTPS later = engine swap) ·
  NanoHTTPD (open CVEs) · own parser (rejected). With HTTPS: Ktor Netty vs NanoHTTPD `makeSecure` — a spike measures
  the APK delta first.

Owner-level forks (vision; listed in `plans/06_EPIC_agent_api.md` §6 with recommendations):

1. LAN transport: plain HTTP (zero setup, the owner's wording) vs HTTPS.
2. Access key: none by default with an optional switch vs mandatory.
3. The ADB door in the release build «KARTUN» (no switch, adb-only guard) vs debug only.
4. The LAN switch: survives app restarts vs turns off with the app.
5. "Local" = private Wi-Fi addresses only vs also VPN overlays such as Tailscale (`100.64.0.0/10`).
6. MCP server: in this epic vs later.

## 6. Sources

- [S1] AOSP `ActiveServices.java` (main): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/am/ActiveServices.java — read 2026-10-16 via `?format=TEXT`
- [S2] AOSP Shell manifest (`uses-permission android.permission.DUMP`, `START_ACTIVITIES_FROM_BACKGROUND`, `START_FOREGROUND_SERVICES_FROM_BACKGROUND`): https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/Shell/AndroidManifest.xml ; DUMP is `signature|privileged|development`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml
- [S3] androidx WorkManager manifest (`DiagnosticsReceiver`, DUMP): https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/work/work-runtime/src/main/AndroidManifest.xml ; usage: https://developer.android.com/guide/background/testing/persistent/debug
- [S4] androidx ProfileInstaller manifest (`ProfileInstallReceiver`, DUMP): https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/profileinstaller/profileinstaller/src/main/AndroidManifest.xml
- [S5] Appium UiAutomator2 driver (device server port, `adb forward`, systemPort 8200–8299): https://cdn.jsdelivr.net/npm/appium-uiautomator2-driver@8.0.1/README.md ; https://discuss.appium.io/t/systemport-on-uiautomator2-doesnt-have-effect-solved/23380
- [S6] Background activity starts: https://developer.android.com/guide/components/activities/background-starts
- [S7] FGS background-start restrictions and exemptions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start ; troubleshooting: https://developer.android.com/develop/background-work/services/fgs/troubleshooting
- [S8] FGS types (`mediaProcessing` 6 h / 24 h, `connectedDevice` prerequisites, `specialUse`): https://developer.android.com/develop/background-work/services/fgs/service-types
- [S9] Cached apps freezer: https://source.android.com/docs/core/perf/cached-apps-freezer
- [S10] Ollama FAQ (binds 127.0.0.1:11434, `OLLAMA_HOST`): https://docs.ollama.com/faq
- [S11] ComfyUI on the LAN (`--listen`, firewall advice): https://comfyui-wiki.com/en/faq/how-to-access-comfyui-on-lan ; https://www.mintlify.com/Comfy-Org/ComfyUI/guides/headless-mode
- [S12] AUTOMATIC1111 command-line arguments (`--listen`, `--api-auth`): https://github.com/AUTOMATIC1111/stable-diffusion-webui/wiki/Command-Line-Arguments-and-Settings/8fc36525ce0000f708956130eb35c8ee7f3bd66d
- [S13] LocalSend protocol: https://github.com/localsend/protocol/blob/main/README.md
- [S14] Ktor CIO server and HTTPS (Kotlin Slack): https://slack-chats.kotlinlang.org/t/13368481/does-server-side-cio-support-https-i-got-this-exception-when
- [S15] Ktor SSL: https://ktor.io/docs/server-ssl.html
- [S16] NanoHTTPD (AOSP mirror, licence and `makeSecure`): https://android.googlesource.com/platform/external/nanohttpd/+/android-7.1.2_r6 ; Android + self-signed BKS: https://community.letsencrypt.org/t/secured-webpage-fails-to-get-a-valid-response-from-an-android-nanohttpd-web-server-due-to-self-signed-certificate-localhost/51667
- [S17] CVE-2020-13697: https://nvd.nist.gov/vuln/detail/CVE-2020-13697
- [S18] CVE-2022-21230: https://nvd.nist.gov/vuln/detail/CVE-2022-21230
- [S19] Android Keystore self-signed certificate on key generation: https://developer.android.com/reference/android/security/KeyPairGeneratorSpec (successor `KeyGenParameterSpec`)
- [S20] curl pinning independent of `--insecure`: https://everything.curl.dev/usingcurl/tls/pinning ; https://android.googlesource.com/platform/external/curl/+/398c59ae6%5E%21/
- [S21] Node.js `https.request` pinning example: https://nodejs.org/api/https.html#httpsrequesturl-options-callback
- [S22] Android local network permission: https://developer.android.com/privacy-and-security/local-network-permission
- [S23] Doze and App Standby: https://developer.android.com/training/monitoring-device-state/doze-standby
- [S24] Replicate prediction lifecycle: https://replicate.com/docs/topics/predictions/lifecycle
- [S25] Google AIP-151 long-running operations: https://google.aip.dev/151
