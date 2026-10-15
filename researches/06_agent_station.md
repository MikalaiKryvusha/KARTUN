# Research 06 — KARTUN as an upscaling station for AI agents: the ADB mode and the LAN (HTTPS) mode

> **Создан:** 2026-10-14 (agent; `/plan-epic` rung 1 for `ideas/02`)
> **Родитель:** `ideas/02_agent_api_adb_and_lan_workstation.md`; the owner's order verbatim, chat ≈ 14:15 +03:00: «в KARTUN должен быть понятный API, CLI, чтобы ИИ агенты могли им пользоваться по проводу через ADB, или еще-как-то, или в локальной вай-фай сети по HTTPS, чтобы андроид устройство с KARTUN могло выступать в качестве рабочей станции по апскейлингу»; ≈ 22:41: «оба режима сейчас наинаем делать»
> **Статус:** ✅ written 2026-10-14 22:51 +03:00 — feeds `plans/06_EPIC_agent_api.md`; deepened and corrected (Ktor CIO has no server TLS; the FGS start from adb) by `researches/10_agent_api_recon.md` 2026-10-16
> **Вовне:** the forks at the end → the owner (chat / interview)

## 1. Industry sweep (web) — findings with sources

**A. Driving an app over adb.**
- A `ContentProvider` exposes `call(method, arg, extras) → Bundle`; adb reaches it with
  `content call --uri <URI> --method <METHOD> [--arg <ARG>] [--extra <key>:<type>:<value>]` (AOSP `Content.java`,
  https://code.yawk.at/android/android-9.0.0_r35/com/android/commands/content/Content.java ; OWASP MASTG-TECH-0148,
  https://mas.owasp.org/MASTG/techniques/android/MASTG-TECH-0148/). Commands run as the shell user, which holds more
  permissions than an ordinary app.
- **Observed on the Titan (2026-10-14 ≈ 22:50):** `adb shell content call --uri content://settings/system --method GET_system
  --arg screen_off_timeout` printed `Result: Bundle[{value=2147483000}]` and exited 0 — a synchronous request/response,
  no logcat polling (today's probe polls logcat: `tools/titan-probe.mjs`).
- Anti-pattern seen in our own code: an exported entry point anyone can call. The provider must refuse every caller but
  the shell (calling UID 2000) and the app itself.

**B. A server inside an Android app.**
- Ktor runs `embeddedServer(CIO, …)` on Android (Kotlin Slack, https://slack-chats.kotlinlang.org/t/4744599/ ; Ktor
  engines, https://ktor.io/docs/2.3.12/server-engines.html). NanoHTTPD is a tiny HTTP/1.0 server with no auth or TLS of its
  own and was dropped by Orbot (https://git.danballard.com/dan/tor-android/commit/33bd25a33df8834ab8b850c97c613585145e9435).
- TLS: Ktor generates a self-signed keystore with `buildKeyStore` (`ktor-network-tls-certificates`,
  https://ktor.io/docs/server-ssl.html); on Android the walkthrough converts to BKS via Bouncy Castle
  (https://proandroiddev.com/running-tls-protected-http-server-on-android-using-ktor-49bdbc7f5e1f); known pitfall — a null
  alias crash with BKS (Kotlin Slack thread above).
- Foreground service type for a LAN server: `connectedDevice` ("interactions with external devices that require a …
  network connection"), prerequisite — declare `CHANGE_NETWORK_STATE` (or `CHANGE_WIFI_STATE` …), manifest permission
  `FOREGROUND_SERVICE_CONNECTED_DEVICE`; fallback `specialUse` with a reviewed subtype
  (https://developer.android.com/develop/background-work/services/fgs/service-types).

**C. Android 17 local network permission — binding for KARTUN (targetSdk 37).**
https://developer.android.com/privacy-and-security/local-network-permission : "Accepting an incoming TCP connection:
yes" needs the permission; the permission is `ACCESS_LOCAL_NETWORK` (group `NEARBY_DEVICES`), "required only if your app
targets Android 17 (SDK 37) or higher", requested at runtime with the standard prompt; `NsdManager` and `.local`
resolution are covered too. Android 16 devices (the Titan) behave as before unless the `RESTRICT_LOCAL_NETWORK` appcompat
flag is set — that flag is how we test the Android 17 behaviour on the Titan.

**D. HTTPS on a LAN device — trust.** No public CA can sign a LAN address; the field practice is trust-on-first-use
with an out-of-band check: pin the certificate fingerprint at first contact, show the fingerprint (or a PIN) on the
device to compare, alert loudly when it changes, offer an explicit re-pair (W3C "HTTPS for Local Networks" 2024,
https://www.w3.org/2024/09/HTTPSForLocalNetworks_2024-09-25.html ; HPE OneView automatic initial trust,
https://hpe.com/psnow/resources/ebooks/a00113385en_us_v5/s_auto-initial-trst.html ; WICG local devices idea,
https://discourse.WICG.io/t/idea-local-devices-api-lan-services/5056). PAKE pairing is the stronger, newer option.

**E. How agents consume tools.** The Model Context Protocol is the common way to give Claude Code and others a tool;
existing servers wrap CLIs (eirikb/any-cli-mcp-server, https://enterprisedna.co/directories/mcp/eirikb-any-cli-mcp-server)
or wrap image APIs (mcp-nano-banana, https://igorstechnoclub.com/mcp-nano-banana/). A thin MCP server over our CLI is the
idiomatic last mile.

## 2. Local recon (read 2026-10-14)

- `EngineProbe` (`app/.../EngineProbe.kt`): debug-only, `am start` extras, result as a logcat JSON line, input/output in
  `getExternalFilesDir(null)` — the seed of the ADB mode, but async-by-logcat and debug-only.
- `UpscaleService` (foreground `mediaProcessing`, `plans/03` step 11) already keeps a run alive with a notification — the
  job runner both modes can share.
- `UpscaleViewModel` owns the pass plan, the GPU→CPU ladder, cancel, the 200 MP limit, the learned estimate — logic the
  agent modes must reuse, not copy (DRY): it has to move into a UI-free job layer.
- Network: `INTERNET` exists only for «Вставить ссылку» (interview #001, Q1 = A — «сеть работает только по кнопке
  „Вставить ссылку“»). A LAN server is a second network use: the owner's 14:15 order covers it, the interview answer
  pre-dates it — recorded as a fork (§4).
- Lessons: EXP-0002 (adb argv mangling in Git Bash — the CLI runs adb through Node `spawnSync`), EXP-0010 (a harness
  leaves the owner's screen as he expects — an agent job must not take over the screen).

## 3. Implications for this epic

1. **One job layer, two doors.** Extract a `JobRunner` (input file → model, scale, format → output file + report JSON)
   from the ViewModel; the UI, the ADB provider and the HTTPS server all submit to it. One engine thread, one queue.
2. **ADB door = `ContentProvider.call`**: methods `info`, `models`, `submit`, `status`, `cancel`; files under the app's
   external files dir (`agent/in`, `agent/out`) moved by `adb push/pull`; caller check UID == shell. Synchronous answers,
   long work as a job id + polling.
3. **CLI on the PC** (`tools/kartun-cli.mjs`, Node, adb via `spawnSync`): `kartun upscale <file> --model --scale
   --format` → push, submit, poll, pull, print one JSON line; the same CLI later talks HTTPS.
4. **HTTPS door = Ktor CIO + self-signed TLS** inside a `connectedDevice` foreground service, off by default, a switch
   in settings; a random access key and the certificate fingerprint shown on the device; the CLI pins the fingerprint
   on first contact (TOFU) and refuses a changed one. Android 17: `ACCESS_LOCAL_NETWORK` requested when the switch turns on.
5. **MCP last mile**: a small MCP server over the CLI so Claude Code calls `kartun_upscale` as a tool.
6. **Privacy invariant holds**: the image goes only between the agent's machine and the device it chose.

## 4. Forks for the owner

1. **The ADB door on by default?** Recommended: yes — only the shell user (USB/Wi-Fi debugging the owner enabled himself)
   can call it. Alternative: behind a settings switch too.
2. **The LAN server**: off by default with a settings switch «Станция для ИИ-агентов по Wi-Fi», a key and a fingerprint on
   screen (recommended); or always on while the app is open.
3. **Network beyond «Вставить ссылку»**: the LAN server is a second network use; interview #001 Q1 limited the network
   to the link button — the owner's 14:15 / 22:41 orders supersede it for the LAN only (recorded, not decided silently).
4. **An MCP server** for Claude Code over the CLI — in this epic (recommended, small) or later.
