// spinner-engine.js - the living pixel spinner of KARTUN for design mockups: the palette and life of a square, the scenes
// (S.heart, S.net, S.conv, S.attn, S.quad, S.life, S.sort, S.fourier ...) and the regroup tour S.tour, ported from
// app/src/main/kotlin/com/kriniks/kartun/ui/PixelSpinner.kt. Shared by design/spinner-states.html and
// design/processing-spinner.html (one copy - DRY). A design copy: the app's own spinner is PixelSpinner.kt.
// drawSpinner(canvas, t, pad) draws the tour into a square canvas without the caption; pad (share of the side) leaves room
// for the squares that fly outside the figure during a regroup. [NOT-TESTED]
// --- the palette and the life of a square, ported from PixelSpinner.kt
const PINK = [255, 26, 140];
const DEEP = PINK.map(v => Math.round(v * .47));                    // lerp(pink, black, .53)
const LIGHT = PINK.map(v => Math.round(v + (255 - v) * .45));        // lerp(pink, white, .45)
const PLUM = [52, 8, 30], GREY = [44, 44, 48], DIM = [26, 24, 28];
const mix = (a, b, k) => { k = Math.min(1, Math.max(0, k)); return a.map((v, i) => Math.round(v + (b[i] - v) * k)); };
const rgb = (c, a = 1) => `rgba(${c.join(',')},${a})`;
const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
const bump = (x, a, b) => Math.sin(Math.PI * clamp((x - a) / (b - a)));
const hash = (a, b) => { const v = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453; return v - Math.floor(v); };
const TWO_PI = Math.PI * 2;

/** Fig.Heart life of PixelSpinner.kt: beat, play, flare on the 1.5x calmer clock. */
function play(i, t) {
  const tt = t / 1.5, beat = Math.max(0, Math.sin(tt / 420)) ** 8;
  const h1 = (i * .6180339) % 1, h2 = (i * .4142135) % 1, h3 = (i * .7320508) % 1;
  const p = .5 + .5 * Math.sin(tt / (380 + h1 * 700) + h2 * 6.283) * Math.sin(tt / (900 + h3 * 900) + h1 * 6.283);
  const flare = Math.max(0, Math.sin(tt / (1700 + h2 * 2300) + h3 * 6.283) - .93) / .07;
  return { k: 1 + beat * .1 + flare * .08, p, flare };
}
/** The heart's colour for square i, lit by the scene's meaning v (0 = resting, dark plum playing; 1 = hot, light playing). */
function glow(i, t, v) {
  const l = play(i, t);
  const rest = mix(mix(PLUM, DEEP, l.p), PINK, .2 * l.p), hot = mix(PINK, LIGHT, .25 + .6 * l.p);
  return { c: mix(mix(rest, hot, v), LIGHT, l.flare * .8), k: l.k };
}
/** Now and then a square sways a little (the owner, ≈ 17:50: «изменение размеров ты уже слишком сильное сделал, и
 *  покачивание… иногда какие-то пиксели увеличиваются… иногда что-то из них покачивается: такого чуть-чуть хватит»):
 *  a rare per-square event, the same shape as the heart's flare; the size changes only by the heart's beat and flares. */
function sway(i, t, a = .006) {
  const h1 = (i * .618) % 1, h2 = (i * .414) % 1, h3 = (i * .732) % 1;
  const ev = Math.max(0, Math.sin(t / (2300 + h1 * 2600) + h3 * 6.283) - .8) / .2;   // on about 1/5 of the time, per square
  return [a * ev * Math.sin(t / 140 + h2 * 6.283), a * ev * .5 * Math.cos(t / 170 + h1 * 6.283), 1];
}
/** One square of a scene: centre x, y and side s in unit space, colour c. */
function Q(i, t, x, y, s, v, swayA = .006) { const g = glow(i, t, v), w = sway(i, t, swayA); return { x: x + w[0], y: y + w[1], s: s * g.k * w[2], c: g.c }; }

function line(g, x1, y1, x2, y2, c, a, w = .006) { const W = g.W, x = g.ctx; x.strokeStyle = rgb(c, a); x.lineWidth = w * W; x.beginPath(); x.moveTo(x1 * W, y1 * W); x.lineTo(x2 * W, y2 * W); x.stroke(); }
function frame(g, x, y, s, c, a) { const W = g.W, x2 = g.ctx; x2.strokeStyle = rgb(c, a); x2.lineWidth = .012 * W; x2.strokeRect((x - s / 2) * W, (y - s / 2) * W, s * W, s * W); }
function label(g, text) { const W = g.W, x = g.ctx; x.fillStyle = 'rgba(154,154,160,.9)'; x.font = `700 ${Math.round(W * .045)}px Manrope`; x.fillText(text, W * .04, W * .965); }

// --- scenes: sq(t) gives the squares, extra(g, t) draws what is not a square (links, the kernel frame, a caption)
const S = {};

// The app's own figures, for the regroup card: heart (Fig.Heart), ring with a running wave (Fig.Ring), tiles (Fig.Tiles).
{ const HEART = ['0110110', '1111111', '1111111', '0111110', '0011100', '0001000'], P = [];
  HEART.forEach((r, y) => [...r].forEach((c, x) => c === '1' && P.push([(x + .5) / 7 * .8 + .1, (y + .5) / 6 * .7 + .16])));
  S.heart = { title: 'Сердце', sq: t => P.map((p, i) => Q(i, t, p[0], p[1], .1, .5, .003)) }; }
S.ring = { title: 'Кольцо', sq: t => Array.from({ length: 18 }, (_, i) => { const a = i / 18 * TWO_PI - Math.PI / 2, head = (t / 1.5 / 1300) % 1 * 18, d = ((head - i) + 18) % 18;
  return Q(i, t, .5 + Math.cos(a) * .36, .5 + Math.sin(a) * .36, .085, Math.max(0, 1 - d / 7), .002); }) };
{ const SQ0 = .13, SEAM = .025, TILE = (1 - 2 * SQ0 - 2 * SEAM) / 3, SUB = TILE / 2;
  S.tiles = { title: 'Квадрат', sq: t => Array.from({ length: 36 }, (_, j) => { const tile = j >> 2, sub = j & 3, tx = tile % 3, ty = (tile / 3) | 0;
    return Q(j, t, SQ0 + tx * (TILE + SEAM) + (sub % 2 + .5) * SUB, SQ0 + ty * (TILE + SEAM) + ((sub >> 1) + .5) * SUB, SUB * .88, .5 + .5 * Math.sin(t / 500 + j * 2.17), .002); }) }; }

// 0. Neural network: 5-7-5 neurons, faint links, the excitation wave layer by layer; neurons sway, breathe and play.
{ const L = [5, 7, 5], P = []; L.forEach((n, l) => { for (let j = 0; j < n; j++) P.push([.18 + l * .32, .5 + (j - (n - 1) / 2) * .7 / 6, l]); });
  const E = []; let b = 0; for (let l = 0; l < 2; l++) { const nx = b + L[l]; for (let a = b; a < nx; a++) for (let c = nx; c < nx + L[l + 1]; c++) E.push([a, c]); b = nx; }
  const acts = w => { const A = P.map((p, i) => p[2] === 0 ? hash(w, i) ** 2 : 0); let b = 0;
    for (let l = 0; l < 2; l++) { const nx = b + L[l]; for (let c = nx; c < nx + L[l + 1]; c++) { let s = 0; for (let a = b; a < nx; a++) s += hash(a, c) * A[a]; A[c] = clamp(s / L[l] * 2.2) ** 1.5; } b = nx; } return A; };
  const WAVE = 1800, lit = (i, t) => { const ph = (t % WAVE) / WAVE, l = P[i][2]; return acts(Math.floor(t / WAVE))[i] * bump(ph, l * .3, l * .3 + .4); };
  S.net = { title: 'Нейросеть из трёх слоёв', own: true,
    text: 'Нейроны покачиваются, дышат размером и переливаются, как сердце; возбуждение бежит по едва заметным связям от слоя к слою.',
    sq: t => P.map((p, i) => { const v = lit(i, t); return Q(i, t, p[0], p[1], .075 * (.94 + .12 * v), .15 + .85 * v, .007); }),
    extra(g, t) { const ph = (t % WAVE) / WAVE, A = acts(Math.floor(t / WAVE));
      for (const [a, c] of E) { const k = (ph - (P[a][2] * .3 + .15)) / .3, s = A[a] * hash(a, c), gl = k >= 0 && k <= 1 ? s * Math.sin(Math.PI * k) : 0;
        line(g, P[a][0], P[a][1], P[c][0], P[c][1], mix(GREY, PINK, gl), .16 + .5 * gl);
        if (gl > .05) { const x = P[a][0] + (P[c][0] - P[a][0]) * k, y = P[a][1] + (P[c][1] - P[a][1]) * k; g.ctx.fillStyle = rgb(LIGHT, gl); g.ctx.fillRect((x - .011) * g.W, (y - .011) * g.W, .022 * g.W, .022 * g.W); } } } }; }

// 2. Convolution: a 3x3 kernel window slides over a 5x5 picture; at each stop the result pixel jumps out of the window
// and lands in the 3x3 output.
{ const STEP = 380, inp = (x, y) => .2 + .8 * hash(x + 1, y + 7), cx = i => .2 + (i % 3) * .1, cy = i => .4 + ((i / 3) | 0) * .1;
  const out = i => { let s = 0; for (let a = 0; a < 3; a++) for (let b = 0; b < 3; b++) s += inp(i % 3 + a, ((i / 3) | 0) + b); return s / 9; };
  S.conv = { title: 'Свёртка 3×3', star: true, dur: 13 * STEP,
    text: 'Окно-ядро ползёт по картинке; на каждой остановке пиксель результата выпрыгивает из окна и встаёт на своё место в выходе.',
    sq(t) { const step = Math.floor(t / STEP) % 13, f = (t % STEP) / STEP, pos = Math.min(step, 8), list = [];
      for (let y = 0; y < 5; y++) for (let x = 0; x < 5; x++) { const under = step < 9 && x >= pos % 3 && x < pos % 3 + 3 && y >= ((pos / 3) | 0) && y < ((pos / 3) | 0) + 3;
        list.push(Q(y * 5 + x, t, .1 + x * .1, .3 + y * .1, .085, under ? .45 + .55 * inp(x, y) : .12 * inp(x, y), .003)); }
      for (let i = 0; i < 9; i++) { const ox = .72 + (i % 3) * .1, oy = .4 + ((i / 3) | 0) * .1;
        if (step >= 9 || i < step) { list.push(Q(40 + i, t, ox, oy, .085, .55 + .45 * out(i) + (step >= 9 ? .3 * bump((t % (4 * STEP)) / (4 * STEP), 0, 1) : 0), .004)); continue; }
        if (i > step) { list.push({ x: ox, y: oy, s: .07, c: DIM }); continue; }
        const e = ease(clamp(f / .8)), x = cx(i) + (ox - cx(i)) * e, y = cy(i) + (oy - cy(i)) * e - .16 * Math.sin(Math.PI * e);
        const q = Q(40 + i, t, x, y, .26 + (.085 - .26) * ease(clamp(f / .35)), 1, .002); q.c = mix(LIGHT, q.c, e); list.push(q); }
      return list; },
    extra(g, t) { const step = Math.floor(t / STEP) % 13; if (step < 9) frame(g, .2 + (step % 3) * .1, .4 + ((step / 3) | 0) * .1, .31, LIGHT, .85); } }; }

// 4. Attention: 6 queries x 6 keys; each query in turn pops its row by the softmax weights, earlier rows keep an afterglow.
{ const STEP = 420;
  S.attn = { title: 'Внимание (attention)',
    text: 'Таблица «запрос × ключ»: запросы по очереди решают, на что смотреть, — клетки подпрыгивают по силе внимания и долго светятся следом.',
    sq(t) { const n = Math.floor(t / STEP), q = n % 6, f = (t % STEP) / STEP, round = Math.floor(n / 6), list = [];
      for (let i = 0; i < 6; i++) { list.push(Q(60 + i, t, .08, .22 + i * .12, .06, i === q ? 1 : .15)); list.push(Q(70 + i, t, .22 + i * .12, .08, .06, .15 + .6 * hash(round, i))); }
      for (let r = 0; r < 6; r++) { const age = (q - r + 6) % 6, rr = age > q ? round - 1 : round; let w = [], s = 0;
        for (let c = 0; c < 6; c++) { const e = Math.exp(3.2 * hash(r * 7 + rr * 13, c)); w.push(e); s += e; }
        for (let c = 0; c < 6; c++) { const v = w[c] / s * 2.6, fresh = age === 0 ? ease(clamp(f * 2.5)) : Math.max(0, 1 - (age - 1 + f) / 4);
          const pop = age === 0 ? 1 + .35 * v * bump(f, 0, .7) : 1;
          list.push(Q(r * 6 + c, t, .22 + c * .12, .22 + r * .12, .1 * pop, clamp(v * fresh + .08), .004)); } }
      return list; } }; }

// 5. Quadtree: the square splits into 4, 16, and one corner deeper still - a different corner every round - and merges back.
{ const CYC = 3200;
  S.quad = { title: 'Квадродерево',
    text: 'Квадрат делится на четыре, на шестнадцать, а один угол — ещё глубже; каждый раунд другой угол, всё переливается и сливается обратно.',
    sq(t) { const ph = (t % CYC) / CYC, corner = Math.floor(t / CYC) % 4, lvl = ease(ph < .5 ? ph * 2 : (1 - ph) * 2) * 3, list = [];
      const cx = corner % 2 ? 1 : 0, cyq = corner > 1 ? 1 : 0;
      const go = (x, y, s, d, path) => { const want = clamp(lvl - d), deeper = d < 2 || (d === 2 && (x > .5) === !!cx && (y > .5) === !!cyq);
        if (deeper && want > .02) { const sp = s / 4 * want; [[-1, -1], [1, -1], [-1, 1], [1, 1]].forEach(([a, b], k) => go(x + a * sp, y + b * sp, s * (1 - .5 * want), d + 1, path * 4 + k)); }
        else list.push(Q(path, t, x, y, s * .9, .3 + .2 * d + .25 * Math.sin(t / 300 + path), .003)); };
      go(.5, .5, .78, 0, 1); return list; } }; }

// 6. Conway's Game of Life on a 16x16 torus, seeded with the real patterns: gliders, pulsar, a spaceship, pentadecathlon,
// R-pentomino. Newborn cells flash, living cells play like the heart.
{ const G = 16, STEP = 110;
  const rows = (r, ox, oy) => { const c = []; r.forEach((s, y) => [...s].forEach((ch, x) => ch === 'O' && c.push([x + ox, y + oy]))); return c; };
  const GL = ['.O.', '..O', 'OOO'];
  const pulsar = []; for (const r of [0, 5, 7, 12]) for (const c of [2, 3, 4, 8, 9, 10]) pulsar.push([c + 1, r + 1]);
  for (const r of [2, 3, 4, 8, 9, 10]) for (const c of [0, 5, 7, 12]) pulsar.push([c + 1, r + 1]);
  const SEEDS = [
    { name: 'Глайдеры', dur: 5200, cells: [...rows(GL, 1, 1), ...rows(GL, 9, 2), ...rows(GL, 2, 9), ...rows(GL, 10, 10)] },
    { name: 'Пульсар', dur: 4000, cells: pulsar },
    { name: 'Космический корабль', dur: 4800, cells: rows(['.O..O', 'O....', 'O...O', 'OOOO.'], 2, 6) },
    { name: 'Пентадекатлон', dur: 4400, cells: rows(['..O....O..', 'OO.OOOO.OO', '..O....O..'], 3, 7) },
    { name: 'R-пентамино', dur: 7000, cells: rows(['.OO', 'OO.', '.O.'], 7, 7) },
  ];
  const TOTAL = SEEDS.reduce((s, x) => s + x.dur, 0);
  const life = c => c.map((v, i) => { const x = i % G, y = (i / G) | 0; let n = 0;
    for (let a = -1; a <= 1; a++) for (let b = -1; b <= 1; b++) if (a || b) n += c[((y + b + G) % G) * G + (x + a + G) % G]; return n === 3 || (v && n === 2) ? 1 : 0; });
  let cache = { seed: -1, gen: -1, cur: null, prev: null };
  const state = t => { let tt = t % TOTAL, k = 0; while (tt >= SEEDS[k].dur) { tt -= SEEDS[k].dur; k++; }
    const gen = Math.floor(tt / STEP);
    if (cache.seed !== k || gen < cache.gen) { const c = Array(G * G).fill(0); SEEDS[k].cells.forEach(([x, y]) => c[((y + G) % G) * G + (x + G) % G] = 1); cache = { seed: k, gen: 0, cur: c, prev: c }; }
    while (cache.gen < gen) { cache.prev = cache.cur; cache.cur = life(cache.cur); cache.gen++; }
    return { k, f: (tt % STEP) / STEP, ...cache }; };
  S.life = { title: 'Игра «Жизнь»',
    text: 'Поле 16×16 и настоящие узоры Конвея: глайдеры, пульсар, космический корабль, пентадекатлон, хаос R-пентамино.',
    sq(t) { const st = state(t), cell = .9 / G, list = [];
      for (let i = 0; i < G * G; i++) { const x = .05 + (i % G + .5) * cell, y = .05 + (((i / G) | 0) + .5) * cell, on = st.cur[i], was = st.prev[i];
        if (on) { const q = Q(i, t, x, y, cell * .86, .6 + .4 * Math.sin(t / 250 + i), .0015); if (!was) q.c = mix(LIGHT, q.c, st.f); list.push(q); }
        else list.push({ x, y, s: cell * (was ? .86 * (1 - st.f) + .5 * st.f : .5), c: was ? mix(DEEP, DIM, st.f) : DIM }); }
      return list; },
    extra(g, t) { label(g, SEEDS[state(t).k].name); } }; }

// 8. Sorting: 8 columns of 1..8 squares (36 in all); bubble sort, the swapped columns hop over each other; a victory wave
// runs when sorted, then the columns jump into a new shuffle.
{ const STEP = 120, CMP = 28, WAVE = 900, HOP = 450, ROUND = CMP * STEP + WAVE + HOP;
  const perm = r => { const a = [1, 2, 3, 4, 5, 6, 7, 8]; for (let i = 7; i > 0; i--) { const j = Math.floor(hash(r, i) * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  const plan = a => { const b = a.slice(), s = []; for (let i = 0; i < 7; i++) for (let j = 0; j < 7 - i; j++) { const sw = b[j] > b[j + 1]; s.push([j, sw]); if (sw) [b[j], b[j + 1]] = [b[j + 1], b[j]]; } return s; };
  S.sort = { title: 'Сортировка',
    text: 'Восемь столбиков из 1–8 квадратиков (ровно 36): соседи сравниваются и перепрыгивают друг через друга; в конце — волна победы.',
    sq(t) { const r = Math.floor(t / ROUND), tau = t % ROUND, a = perm(r), sw = plan(a), list = [];
      const idx = Math.min(CMP, Math.floor(tau / STEP)), f = (tau % STEP) / STEP, order = a.slice();
      for (let i = 0; i < idx; i++) if (sw[i][1]) { const j = sw[i][0]; [order[j], order[j + 1]] = [order[j + 1], order[j]]; }
      const cmp = idx < CMP ? sw[idx] : null, waveT = tau - CMP * STEP, hopT = waveT - WAVE;
      const colX = c => .12 + c * .108;
      order.forEach((h, c) => { let x = colX(c), hop = 0, v = .25 + .5 * h / 8;
        if (cmp && (c === cmp[0] || c === cmp[0] + 1)) { v = 1; if (cmp[1]) { const e = ease(f), dir = c === cmp[0] ? 1 : -1; x += dir * .108 * e; hop = .09 * Math.sin(Math.PI * e) * (dir > 0 ? 1 : .4); } }
        if (waveT >= 0 && hopT < 0) v = Math.max(v, bump(waveT / WAVE * 1.6 - c / 8 * .6, 0, .5));
        if (hopT >= 0) hop = .25 * Math.sin(Math.PI * clamp(hopT / HOP));
        for (let k = 0; k < h; k++) list.push(Q(h * 8 + k, t, x, .88 - k * .095 - hop, .085, v, .002)); });
      return list; } }; }

// 9. Fourier: 36 squares draw the sum of 1, 2, 3, 4 harmonics of a square wave, the wave runs and the squares bounce.
S.fourier = { title: 'Волна Фурье',
  text: 'Квадратики бегут синусоидой, к ней добавляются гармоники — волна становится «ступенькой» и снова растворяется в синусоиду.',
  sq(t) { const ph = (t % 4500) / 4500, terms = 1 + (ph < .5 ? ph * 2 : (1 - ph) * 2) * 3, list = [];
    for (let i = 0; i < 36; i++) { const x = i / 35, a = x * Math.PI * 4 + t / 260; let y = 0;
      for (let k = 0; k < 4; k++) y += clamp(terms - k) * Math.sin((2 * k + 1) * a) / (2 * k + 1);
      list.push(Q(i, t, .07 + x * .86, .5 - y * .32, .042 * (1 + .25 * Math.abs(Math.sin(a * 2))), .3 + .7 * Math.abs(y), .002)); }
    return list; } };

// --- the regroup card: the squares fly between the app's figures and the new states, as PixelSpinner.kt regroups them.
// Moods of the flight: the app's Burst, Swirl, Rain, Snake and its small explosion (Mood.* and EXPLODE_SHARE in
// PixelSpinner.kt), plus two new ones the owner named (≈ 17:47: «взрыв, торнадо, дождь, матрица (не уверен, есть ли),
// ветер»): Matrix - columns fall from the top one after another; Wind - a gust blows them off one side, they come in from the other.
const TOUR = ['heart', 'net', 'ring', 'attn', 'tiles', 'sort', 'conv', 'fourier', 'quad'];
const MOODS = [
  { name: 'взрыв', boom: true, ctrl: (a, b) => [(a.x + b.x) / 2, (a.y + b.y) / 2], delay: j => hash(j, 3) * .1 },
  { name: 'вихрь', ctrl: (a, b, j) => { const mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2, an = Math.atan2(my - .5, mx - .5) + 2.2; return [.5 + Math.cos(an) * .55, .5 + Math.sin(an) * .55]; }, delay: j => hash(j, 4) * .25 },
  { name: 'дождь', ctrl: (a, b, j) => [b.x + (hash(j, 6) - .5) * .2, -.45], delay: j => hash(j, 7) * .45 },
  { name: 'матрица', matrix: true, delay: (j, b) => clamp(b.x) * .55 + hash(j, 8) * .08 },
  { name: 'змейка', ctrl: (a, b, j) => [(a.x + b.x) / 2 + (hash(j, 1) - .5) * .5, (a.y + b.y) / 2 + (hash(j, 2) - .5) * .5], delay: j => (j % 36) / 36 * .45 },
  { name: 'ветер', ctrl: (a, b, j) => [1.35 + hash(j, 9) * .3, (a.y + b.y) / 2 + (hash(j, 10) - .5) * .3], delay: (j, b) => (1 - clamp(b.x)) * .3 + hash(j, 11) * .1, wind: true },
  { name: 'разлёт', ctrl: (a, b) => [.5 + (a.x - .5) * 3, .5 + (a.y - .5) * 3], delay: j => hash(j, 12) * .15 },
];
const HOLD = 4200, FLY = 1300, BOOM = .22;
const span = k => (S[k].dur ? Math.max(HOLD, S[k].dur) : HOLD);
const TOUR_T = TOUR.reduce((s, k) => s + span(k), 0);
const where = t => { const lap = Math.floor(t / TOUR_T); let tt = t % TOUR_T, k = 0; while (tt >= span(TOUR[k])) { tt -= span(TOUR[k]); k++; }
  return { k, tt, mood: MOODS[(lap * TOUR.length + k) % MOODS.length] }; };
S.tour = { title: 'Как в приложении: перестроения', wide: true,
  text: 'Сердце → нейросеть → кольцо → внимание → квадрат → сортировка → свёртка → Фурье → квадродерево. Каждый переход со своим настроением: взрыв, вихрь, дождь, матрица, змейка, ветер, разлёт; лишние квадратики сливаются, недостающие делятся.',
  sq(t) { const { k, tt, mood } = where(t), B = S[TOUR[k]].sq(tt); if (tt >= FLY) return B;
    const pk = TOUR[(k + TOUR.length - 1) % TOUR.length], A = S[pk].sq(span(pk) - 1), n = Math.max(A.length, B.length), list = [];
    for (let j = 0; j < n; j++) { const a = A[j % A.length], b = B[j % B.length], merge = j >= B.length;
      let u = tt / FLY, ox = a.x, oy = a.y, lift = 0;
      if (mood.boom) {                                           // the small explosion first, then straight into the flight
        const an = Math.atan2(a.y - .5, a.x - .5) + (hash(j, 13) - .5) * 1.1, kick = .1 + hash(j, 14) * .1, bb = 1 - (1 - clamp(u / BOOM)) ** 3;
        ox = a.x + Math.cos(an) * kick * bb; oy = a.y + Math.sin(an) * kick * bb; lift = Math.sin(clamp(u / BOOM) * Math.PI) * .55;
        if (u < BOOM) { list.push({ x: ox, y: oy, s: a.s * (1 - lift * .3), c: mix(a.c, LIGHT, lift) }); continue; }
        u = (u - BOOM) / (1 - BOOM); }
      const d = mood.delay(j, b), e = ease(clamp((u - d) / (1 - .5)));
      let x, y;
      if (mood.matrix) {                                         // fall: rise out of sight upward, then drop down the column
        const up = clamp(e * 2), down = clamp(e * 2 - 1);
        x = up < 1 ? ox : b.x; y = up < 1 ? oy + (-.15 - oy) * ease(up) : -.15 + (b.y + .15) * (down * down);
      } else {
        const [cx, cy] = mood.ctrl({ x: ox, y: oy }, b, j);
        x = (1 - e) ** 2 * ox + 2 * (1 - e) * e * cx + e * e * b.x; y = (1 - e) ** 2 * oy + 2 * (1 - e) * e * cy + e * e * b.y;
        if (mood.wind && x > 1.05) x -= 1.25;                     // blown off the right edge, it comes back in from the left
      }
      lift = Math.max(lift * (1 - e), Math.sin(e * Math.PI) * .35);
      list.push({ x, y, s: (a.s + (b.s - a.s) * e) * (merge ? 1 - e : 1) * (1 - lift * .3), c: mix(mix(a.c, b.c, e), LIGHT, lift) }); }
    return list; },
  extra(g, t) { const { k, tt, mood } = where(t);
    if (tt >= FLY && S[TOUR[k]].extra) { g.ctx.globalAlpha = clamp((tt - FLY) / 500); S[TOUR[k]].extra(g, tt); g.ctx.globalAlpha = 1; }
    label(g, tt < FLY ? `${S[TOUR[k]].title} · переход: ${mood.name}` : S[TOUR[k]].title); } };

/** Draws the tour (the squares and the current scene's links / kernel frame) into a square canvas, without the caption. */
function drawSpinner(cv, t, pad = 0) {
  const r = cv.getBoundingClientRect(), W = Math.round(r.width * devicePixelRatio);
  if (W <= 0) return;
  if (cv.width !== W) { cv.width = W; cv.height = W; }
  const ctx = cv.getContext('2d'), g = { ctx, W: W * (1 - 2 * pad) }; ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.clearRect(0, 0, W, W);
  ctx.translate(W * pad, W * pad);
  const { k, tt } = where(t), sc = S[TOUR[k]];
  if (tt >= FLY && sc.extra) { ctx.globalAlpha = clamp((tt - FLY) / 500); sc.extra(g, tt); ctx.globalAlpha = 1; }
  for (const q of S.tour.sq(t)) { ctx.fillStyle = rgb(q.c); ctx.fillRect((q.x - q.s / 2) * g.W, (q.y - q.s / 2) * g.W, q.s * g.W, q.s * g.W); }
}
