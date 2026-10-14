# KAIF bug: the mid-turn owner-word hooks turn every short owner remark into a forced stop, print a service arrow «⏩» into the owner's chat, and reject a real answer that does not open with the message's literal first words

kaif-fp: hooks/owner-word :: forced-stall + service-text-to-owner :: v2.9
**Delivered upstream:** https://github.com/MikalaiKryvusha/KAIF/issues/144
**Autocapture** (from `.kaif/kaif.json`): KAIF 2.8 + prerelease 2.9 build `a63c651356bc` · project KARTUN · sphere programming ·
language ru · tracking origin · agent system claude-code (VS Code extension) · OS Windows 11 · Node v24.15.0
**Dedup attestation:** `ls bugs/KAIF/` → no directory (first ticket); `gh issue list -R MikalaiKryvusha/KAIF --search "owner word mid-turn"`,
`--search "stop-owner-answer"`, `--search "⏩"` → #138 (the mid-turn question never answered — the defect these hooks were built
to fix), #140, #142 — none describes the reverse effect below. No match found.

## Expected per canon

`AGENT_GUIDE.md` → «The owner's word mid-turn — the system signs its author», rule 2: «Answer it by its kind, AS TEXT, opening with
its first words in «…» … Answer in a response of its own — no tool call after it»; rule 3: «`pretool-owner-word.mjs` (2.8) refuses
ONE call after an unanswered mid-turn word; `stop-owner-answer.mjs` (2.9) asks ONCE for the answer and, after a last line
`⏩ <next step>`, resumes the work». The intent: the owner's word is never ignored.

## Got in the field

KARTUN, 2026-10-14 18:12–20:25 +03:00. The owner sent ~20 short remarks while the agent worked (UI corrections, «закрывай чат»,
etc.). Each one: the next tool call was refused («KAIF: the owner wrote while you were working … Answer it NOW AS TEXT»), the agent
had to end its response with a text and a last line «⏩ <next step>», then the Stop hook resumed it. The owner, verbatim, 20:24:

> «⏩ Отключаю лишнее подключение adb, ставлю сборку на Титан, проверяю хендл, жесты и уведомления, затем порядок моделей и «О
> приложении и авторе». откуда эта стрелка? кто это принус в каиф? почему ты стоишь и не делаешь, когда прошу?»

Then the agent answered, opening with a quote of his QUESTION, and `stop-owner-answer.mjs` rejected the answer:

> «KAIF: the owner wrote while you were working and this response does not answer it yet: «⏩ Отключаю лишнее подключение adb, …»»

— because the owner's message began with the agent's OWN pasted line «⏩ …», and the hook matches the literal first words.

## Repro (deterministic)

1. A project with the refresh-hooks module wired (`.kaif/hooks/pretool-owner-word.mjs`, `stop-owner-answer.mjs`).
2. Give the agent a multi-step task; while it works, send three short remarks a minute apart.
3. Observe: three forced stops, three responses ending with «⏩ …» in the owner's chat.
4. Send a message that starts by quoting the agent's own «⏩ …» line and then asks a question; answer it opening with the question's words → the Stop hook rejects the answer.

## Cost and violated invariant

The owner reads the forced pauses as the agent «standing and not doing what I ask», and sees a service marker he never asked for in
every reply — his trust in the agent's responsiveness drops (owner-work-safety / simplicity). One correct answer was rejected
(honest-green: the gate's red was false). Near-miss: the closing he ordered was delayed by the round-trips.

## What in KAIF led to this

1. The rule makes EVERY mid-turn word a hard stop, with no class for «a clarification of the current task» or «a backlog note» —
   the drive-by rule of the same guide says those are recorded and the work continues, but the hooks cannot tell them apart.
2. The resume protocol leaks into the owner's channel: «⏩» is a machine signal for the Stop hook, yet it is printed as the last line
   of a message to the owner.
3. The answer check matches the message's literal first words; a message that starts with quoted text fails a genuine answer.

## Local remediation (per the "defect in KAIF itself" contour, if applied)

None yet — the owner was offered to switch the two hooks off in KARTUN («Скажите «отключи» — сделаю»); waiting for his word.
Proposal for the origin: a hidden resume marker (not a visible arrow), a light path for remarks classified as clarification/backlog
(record + continue, no stop), and an answer check by intent (the reply names the message) rather than by its literal first words.
