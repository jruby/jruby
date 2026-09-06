# Stress Dashboard

Two front-ends, same kernel and same 30 stress threads:

| Front-end | Files | Requirement |
|---|---|---|
| **TUI** (recommended) | `boot/tui_stress.rb` + `tui/stress_app.rb` | MRI Ruby, `msgpack` gem, any ANSI terminal |
| Tk window | `boot/tk_stress.rb` + `tk/stress_app.rb` | MRI Ruby, `tk` gem, gnome-terminal |

---

## Overview

The live stress dashboard is the primary integration test for Kestówv. It simultaneously:

- Boots the full kernel
- Registers an external process as a kernel task via `Proc::Exec`
- Runs 30 stress threads across every active subsystem
- Streams live metrics over a Unix domain socket to a CRuby dashboard
- Exercises the Wave governor under real CPU load

It is also a demonstration of two Ruby runtimes cooperating through kernel-managed IPC.

---

## Requirements

| Component | Requirement |
|---|---|
| Kernel process | JRuby (9.4+ recommended) |
| Dashboard process | MRI Ruby + `msgpack` gem |
| Terminal emulator | Any ANSI terminal (TUI); `gnome-terminal` for Tk variant |
| OS | Linux (reads `/proc/stat`, `/proc/meminfo`, `/sys/class/thermal`) |

---

## Running

### TUI dashboard (recommended)

```bash
jruby boot/tui_stress.rb
```

No Tk required. The kernel auto-detects a terminal emulator (gnome-terminal → xterm → kitty → alacritty) and opens the ANSI dashboard in it. If none is found it prints the manual run command instead:

```
ruby tui/stress_app.rb /tmp/kestowv_tui_<pid>.sock
```

Output:

```
[Init] ✓ Kernel: load subsystems
...
[Init] Boot complete — 2 PIDs active

  [tui_stress] Kernel booted
  [tui_stress] Wave:   5 daemon threads
  [tui_stress] Exec:   CRuby task registered (kernel pid slot)
  [tui_stress] UDS:    /tmp/kestowv_tui_<pid>.sock
  [tui_stress] Spawned gnome-terminal (OS pid=<pid>)
  [tui_stress] Waiting for CRuby to connect (up to 20s)...
  [tui_stress] CRuby connected — streaming metrics

  [tui_stress] Running 30 stress threads — streaming to TUI
  [tui_stress] Close the terminal or press q to stop
```

Press `q` in the TUI window or close it to stop cleanly.

### Tk dashboard

```bash
jruby boot/tk_stress.rb
```

Requires the `tk` gem and gnome-terminal. Output is identical in structure with `[tk_stress]` prefix.

---

## TUI Layout

```
  Kestówv 0.5.1  Live Stress Dashboard                        t=42.0s
  ──────────────────────────────────────────────────────────────────────
  CPU   c0 ████████░░░░ 67%  c1 █████████░░░ 73%  c2 ██████░░░░░░ 50%
  Freq  cpu0=3200MHz  cpu1=3200MHz  cpu2=3100MHz  cpu3=3200MHz
  Temp  52.0°C  53.5°C
  RAM   ██████████░░ used=8142MB  free=7101MB  total=15243MB
  JVM   ████████░░░░ 1840MB / 2048MB  (max 4096MB)
  Wave  threads=5  gov=████████░░ 87%  cap=80%
  Hub   sockets=22  backend=hash
  ──────────────────────────────────────────────────────────────────────
  SUBSYSTEM                OPS/SEC      TOTAL OPS  ERRORS
  ────────────────────────────────────────────────────────────────────
  core/binclass              94211       2826330       0
  core/klog                  52018       1560540       0
  ipc/pipe                   16066        481980       0
  mm/vmregion                41333       1239990       0
  proc/creds                 38200       1146000       0
  ...
  ────────────────────────────────────────────────────────────────────
  TOTAL                     328066       9842000       0
  ──────────────────────────────────────────────────────────────────────
  9,842,000 ops — ALL CLEAN                                  [q] quit
```

Block bars use `█`/`░`. Colour thresholds: green < 70%, yellow < 90%, red ≥ 90% for CPU/RAM/JVM; inverted for Wave governor (red < 40%). Status bar turns red on any error. `⚡ GC-yield` tag appears on the Wave line when `gc_headroom` is true (JVM heap > 70%).

---

## Architecture

```
jruby boot/tui_stress.rb (JRuby — Kestówv kernel)
│
├── Kestowv::Init.boot
│     └── [full kernel boot sequence]
│
├── KProc::Task.new(name: :tui_stress_app)         — CRuby as kernel task
├── KProc::Exec.execve(TUI_SCRIPT, [...], {}, ...)  — exec recorded
├── UNIXServer.new(SOCK_PATH)                       — OS-level UDS server
├── Net::UnixHub.register(hub_sock)                 — registered in kernel hub
│
├── Process.spawn(<terminal>, "--", "ruby", "tui/stress_app.rb", SOCK_PATH)
│     └── [CRuby process starts, connects to SOCK_PATH]
│
├── 30 stress threads (see below)
│
└── Metrics ticker (main thread, 500ms loop)
      └── MessagePack.pack(metrics) → client.write → UDS → CRuby

ruby tui/stress_app.rb SOCK_PATH (CRuby/MRI)
│
├── Socket reader thread
│     └── UNIXSocket → readpartial → Unpacker.feed_each → $data
│
├── Keyboard thread
│     └── $stdin.raw { getc } — q/Q/Ctrl-C sets $stop
│
└── Main loop (500ms)
      └── render(d) → ANSI buffer → $stdout.write
```

---

## Stress Threads

30 threads run concurrently. Each reports ops and ops/sec to the shared `results` hash every 200 ops. The dashboard displays per-subsystem throughput live.

| # | Name | What it stresses |
|---|---|---|
| 1 | `mm/frames` | `FrameAllocator.allocate` / `.free` |
| 2 | `mm/pagetable` | `PageTable.map` / `.lookup` / `.unmap` |
| 3 | `mm/vmregion` | `VmRegion.slab_acquire` / `.slab_release` |
| 4 | `mm/heap` | `Heap.allocate` / `.free` |
| 5 | `proc/pid` | `Pid.allocate` / `.release` |
| 6 | `proc/task` | `Task.slab_acquire` / `.transition` / `.slab_release` |
| 7 | `proc/creds` | `Credentials.slab_acquire` / `.slab_release` |
| 8 | `proc/cgroup` | `Cgroup.create` / `.destroy` |
| 9 | `fs/tmpfs` | `TmpFs.write` / `.read` / `.stat` / `.delete` |
| 10 | `fs/vfs` | `Vfs.mount` / `.resolve` / `.unmount` |
| 11 | `ipc/pipe` | `Pipe.create` / `.write` / `.read` / `.close` |
| 12 | `ipc/queue` | `Queue.enqueue` / `.dequeue` |
| 13 | `ipc/sem` | `Sem.post` / `.wait` |
| 14 | `ipc/shm` | `Shm.create` / `.attach` / `.destroy` |
| 15 | `core/klog` | `Klog.debug` (ring buffer reuse) |
| 16 | `boot/bitvec` | `Boot.set_bit` / `.bit_set?` / `.clear_bit` |
| 17 | `core/runqueue` | `RunQueue.enqueue` / `.dequeue` |
| 18 | `core/binclass` | `BinaryClassifier.classify` |
| 19 | `core/wave` | `Wave.stats` polling (governor health check) |
| 20 | `uds/lifecycle` | `UnixSocket.new` / `.connect` / `.close` |
| 21 | `uds/socketpair` | `UnixSocket.pair` / `send_io` / `recv_io` |
| 22 | `uds/hub-churn` | `UnixHub.register` / `.unregister` |
| 23 | `uds/hub-lookup` | `UnixHub.get` (16 pre-registered paths) |
| 24 | `uds/hub-stats` | `UnixHub.list` / `.stats` / `.active_sockets` |
| 25 | `uds/send-io` | `UnixSocket.send_io` / `.recv_io` (pre-alloc payload) |
| 26 | `uds/msgpack` | `MessagePack.pack` / `.unpack` |
| 27 | `uds/writer-0` | `Hub.get(path).send_io` (shared socket, pre-alloc payload) |
| 28 | `uds/writer-1` | same |
| 29 | `uds/writer-2` | same |
| 30 | `uds/writer-3` | same |

---

## Metrics Frame

The kernel packs one MessagePack frame per 500ms tick. The decoded structure:

```json
{
  "t":             42.0,
  "cpu":           [23.1, 31.4, 18.7, 29.2],
  "freqs_mhz":     [3200, 3200, 3100, 3200],
  "temps":         [52.0, 53.5],
  "mem":           { "used_mb": 8142, "free_mb": 7101, "cache_mb": 2048, "total_mb": 15243 },
  "jvm":           { "used_mb": 1840, "total_mb": 2048, "max_mb": 4096 },
  "wave":          {
    "running": true, "threads": 5, "threads_alive": 5,
    "governor_factor": 0.87, "cpu_cap": 0.8, "gc_headroom": false
  },
  "hub":           { "active_sockets": 22, "backend": "hash" },
  "subsystems":    {
    "ipc/pipe":    { "ops": 482000, "ops_sec": 16066, "errors": 0 },
    "mm/vmregion": { "ops": 1240000, "ops_sec": 41333, "errors": 0 },
    ...
  },
  "total_ops":     9842000,
  "total_ops_sec": 328066,
  "total_errs":    0
}
```

---

## Tk Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ Kestówv 0.5.1 — Live Stress Dashboard              t=42.0s     │
├─────────────────────────────────────────────────────────────────┤
│ CPU:   core0=23%  core1=31%  core2=18%  core3=29%              │
│ Freq:  cpu0=3200MHz  cpu1=3200MHz  cpu2=3100MHz  cpu3=3200MHz  │
│ Temp:  52.0°C  53.5°C                                          │
│ RAM:   used=8142MB  free=7101MB  cache=2048MB  total=15243MB   │
│ JVM:   heap 1840MB / 2048MB  (max 4096MB)                      │
│ Wave:  running=true  threads=5  gov=87%  cap=80%               │
│ Hub:   active_sockets=22  backend=hash                         │
├─────────────────────────────────────────────────────────────────┤
│ SUBSYSTEM              OPS/SEC      TOTAL OPS  ERRORS          │
│ ────────────────────────────────────────────────────────────── │
│ core/binclass            94211       2826330       0           │
│ core/klog                52018       1560540       0           │
│ ipc/pipe                 16066        481980       0           │
│ mm/vmregion              41333       1239990       0           │
│ proc/creds               38200       1146000       0           │
│ ...                                                            │
│ ────────────────────────────────────────────────────────────── │
│ TOTAL                   328066       9842000       0           │
├─────────────────────────────────────────────────────────────────┤
│ 9,842,000 ops — ALL CLEAN                          [ Quit ]    │
└─────────────────────────────────────────────────────────────────┘
```

Colours follow the Catppuccin Mocha palette. The status bar turns red if any errors are detected. The `⚡GC-yield` tag appears next to the Wave line when `gc_headroom` is true (JVM heap > 70%).

---

## Allocation Strategy

Several threads in earlier versions created new Ruby objects on every iteration, producing sustained GC pressure that caused the JVM heap to fill monotonically past 300 seconds. The 0.5.1 release resolves all identified hot paths:

| Thread | Previous | 0.5.1 |
|---|---|---|
| `proc/creds` | `Credentials.user(...)` — new Struct each iter | `slab_acquire` / `slab_release` — pool of 64 |
| `uds/send-io` | `{ seq: i, payload: "...", ts: Time.now.to_f }` — new Hash each iter | Pre-allocated Hash, mutates `:seq` and `:ts` in place |
| `uds/socketpair` | `{ msg: "ping" }` — new Hash each iter | Pre-allocated `ping = { msg: "ping" }`, reused |
| `uds/writer-*` | `{ writer: wid, seq: i }` — new Hash each iter | Pre-allocated per-thread, mutates `:seq` in place |
| `core/klog` | `{ time:, level:, message:, context: }` — new Hash each log call | Pre-allocated `Entry` Struct ring, mutated in place |
