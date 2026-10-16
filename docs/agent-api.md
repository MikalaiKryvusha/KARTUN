# KARTUN agent API — upscale pictures on an Android device from your agent

KARTUN is an Android app that enlarges photos 1×–16× with a neural network (Real-ESRGAN family, ncnn + Vulkan) on the
phone itself. This API lets an AI agent on a PC send a picture to a device with KARTUN over adb and get the upscaled file
back. The picture goes only to the device you send it to; nothing goes to the internet.

## Quick start

Requirements: a device with KARTUN installed and reachable by `adb` (USB, or Wi-Fi debugging), Node 18+ on the PC.

```
node tools/kartun-cli.mjs info
node tools/kartun-cli.mjs upscale photo.jpg --scale 4
```

The second command writes `photo_x4.png` next to `photo.jpg` and prints one JSON line:

```json
{"ok":true,"file":"photo.jpg","job":"kmvarkp6y10e16e","state":"succeeded","model":"4xNomosWebPhoto-RealPLKSR-fp16","scale":4,"format":"png","gpu":true,"progress":1,"tiles_done":4,"tiles_total":4,"elapsed_ms":5517,"estimate_ms":4250,"in":{"w":256,"h":256},"out":{"file":"kmvarkp6y10e16e.out.png","w":1024,"h":1024,"bytes":1569580},"error":null,"output":"C:\\work\\photo_x4.png"}
```

KARTUN does not need to be open, and the device screen may be off. The CLI never opens the app on the screen.

## CLI reference

`node tools/kartun-cli.mjs <command> [args] [--serial <ip:port>] [--pkg <package>]`

| Command | What it does |
|---|---|
| `info` | device, app, GPU, models, limits — one JSON line |
| `models` | `{"ok":true,"models":[…]}` — every model of the catalogue, `installed` true for the ones on the device |
| `upscale <file…>` | upscales each file in turn; one JSON line per file |
| `status <job>` | the job's status (see Status file) |
| `cancel <job>` | cancels a queued or running job; waits up to 10 s for `canceled` |

`upscale` options:

| Option | Values | Default |
|---|---|---|
| `--model` | a model `id` from `models` with `installed: true`, or `default` | `default` |
| `--scale` | `1`, `2`, `4`, `8`, `16` | `4` |
| `--format` | `png`, `jpg` (`jpeg`), `webp` | `png` |
| `--quality` | `1`–`100` (jpg and webp) | `92` |
| `--gpu` / `--cpu` | force the backend | the app's «GPU acceleration» setting |
| `--out` | a file path, or a directory (ending with `/`, existing, or several inputs); missing folders are created | `<name>_x<scale>.<format>` next to the input |
| `--timeout` | seconds; on expiry the job is canceled | `3600` |
| `--keep` | leave the job's files on the device (they also stay when the copy to the PC fails) | off |

Device selection: `--serial` or the `KARTUN_SERIAL` environment variable; without either, adb's single connected device.
adb: `KARTUN_ADB`, else `C:/Android/Sdk/platform-tools/adb.exe` if present, else `adb` on `PATH`. Package: `--pkg`, else
the installed one of `com.kriniks.kartun` (release) and `com.kriniks.kartun.debug`.

`ok` is the verdict for you: true only when the result file is on the PC. `state` is the job's state on the device — a
line can carry `"state":"succeeded"` with `"ok":false` when the result was made but could not be copied to the PC
(`pull_failed`; the file then stays on the device and the message gives the `adb pull` command).

Progress goes to stderr: first `job <id> · <file> · <scale>x <format>` (use that id with `cancel` from another shell),
then `photo.jpg: 42% · tiles 21/50 · 3.2 s of ≈ 8 s · GPU`. Ctrl+C cancels the running job on the device before exiting.

Exit codes:

| Code | Meaning |
|---|---|
| `0` | every job succeeded |
| `1` | a job failed or was canceled (see its `error`) |
| `2` | bad arguments |
| `3` | no device, no adb, or `--timeout` expired |

## Raw adb protocol (agents without Node)

`<pkg>` is `com.kriniks.kartun` or `com.kriniks.kartun.debug`; `<dir>` is `/sdcard/Android/data/<pkg>/files/agent`.
A job id is yours to choose: `[A-Za-z0-9_-]{8,64}`.

```
# submit
adb push photo.jpg <dir>/<job>.in.jpg
adb shell am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService \
  -a com.kriniks.kartun.agent.UPSCALE --es job <job> --es in <job>.in.jpg \
  --es model default --ei scale 4 --es format png --ei quality 92 [--ez gpu true|false]
# poll (every 0.5 s is enough; the file is replaced atomically)
adb exec-out cat <dir>/<job>.json
# result
adb pull <dir>/<job>.out.png
# clean up (the app also removes a job's files 24 h after its last status)
adb shell rm -f "<dir>/<job>.*"
# cancel
adb shell am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService -a com.kriniks.kartun.agent.CANCEL --es job <job>
# device info and models -> <dir>/info.json
adb shell am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService -a com.kriniks.kartun.agent.INFO
```

Only the adb shell can start the agent service: it is guarded by `android.permission.DUMP`, which apps on the device do
not hold. A request whose `job` is not a valid id gets no status file; the reason is in `adb logcat -s KARTUN`
(`agent: bad request …`).

## Status file `<job>.json`

| Field | Meaning |
|---|---|
| `v` | protocol version, `1` |
| `job` | the job id |
| `state` | `queued` · `running` · `succeeded` · `failed` · `canceled` |
| `model` | the model id actually used (`default` resolved) |
| `scale`, `format` | as requested |
| `gpu` | true while the GPU does the work; false on `--cpu` or after a GPU failure fell back to the CPU |
| `progress` | `0`–`1` over all tiles of all passes (8× and 16× run two passes) |
| `tiles_done`, `tiles_total` | tiles of the network |
| `elapsed_ms` | time since the job started running |
| `estimate_ms` | the expected run time, learned from this device's earlier runs |
| `in` | `{"w","h"}` of the input, once decoded |
| `out` | on `succeeded`: `{"file","w","h","bytes"}` |
| `error` | on `failed`: `{"code","message"}` |

Jobs run one at a time, in the order they were sent; at most 8 wait. A running job is canceled after its current tile.

## `info.json`

```json
{"v":1,"app":{"package":"com.kriniks.kartun.debug","version":"0.1 (1)","build":"debug"},
 "device":{"manufacturer":"HEADWOLF","model":"Titan 1","sdk":36},
 "gpu":"gpu_count=1 name=Mali-G615 MC6 fp16_storage=1 fp16_arith=1 heap_budget_mb=8038",
 "gpu_default":true,"charging":false,"queue":0,
 "models":[{"id":"4xNomosWebPhoto-RealPLKSR-fp16","name":"4xNomosWebPhoto RealPLKSR","scale":4,"installed":true,"default":true,"licence":"CC BY 4.0","commercial":true}],
 "limits":{"max_input_mp":100,"max_output_mp":500,"scales":[1,2,4,8,16],"formats":["png","jpg","webp"]}}
```

(`models` is shortened here; it lists the whole catalogue.) Models not installed are downloaded by the user in the app,
«All models».

## Error codes

| Code | Meaning | What to do |
|---|---|---|
| `bad_request` | a field outside the contract (job id, a path in `in`, scale, format, quality), or a WebP result over 16 383 px per side | fix the request; for big results use png or jpg |
| `input_missing` | no input file under that name in `<dir>` | push it first |
| `decode_failed` | the input is not a picture the device can read | send a format Android decodes: JPEG, PNG, WebP, HEIC, AVIF, GIF, BMP |
| `too_big` | input over 100 MP, or the result over 500 MP | a smaller scale or a smaller picture |
| `model_not_installed` | the model is not on the device | `models` lists the installed ones |
| `model_load_failed` | the model's files did not load | reinstall the model in the app |
| `engine_failed` | the network failed on GPU and CPU | retry with `--cpu`, or a smaller picture |
| `busy` | 8 jobs are already waiting | retry later |
| `quota` | Android's limit of 6 h of media processing per 24 h is used up | retry tomorrow |
| `internal` | writing the result on the device failed | check the device's free space |

The CLI adds `bad_arguments` (exit 2), `no_device`, `adb_missing`, `timeout` (exit 3), `unknown_job` (`status` of a job
with no status file, exit 1), `pull_failed` (the result is on the device, the copy to the PC failed — a folder or a disk
problem on the PC; exit 1), and `canceled` on Ctrl+C.

## Limits

- Input up to 100 MP; result up to 500 MP (so 1× for a 100 MP photo, up to 2× for 50 MP).
- WebP results up to 16 383 px per side.
- Scales 1×, 2×, 4× take one pass of the network, 8× and 16× two passes.
- Android allows a background job of this kind 6 hours per 24 hours (`quota`).
- Speed on a MediaTek Dimensity 8300 class tablet (Mali-G615), default model, 4×: a 256 × 256 picture in 3.7–5.5 s on the
  GPU and 39 s on the CPU; the app's estimate for this model is 65 s per input megapixel on the GPU. `estimate_ms` gives
  the figure for your device and picture before the run.
