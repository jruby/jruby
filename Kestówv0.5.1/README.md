# Kestówv 0.5.1

**A JRuby-native operating system kernel.**

The JVM is not emulated hardware. It *is* the hardware.

---

## What This Is

Kestówv is a kernel written entirely in Ruby, running on JRuby. It is not a toy OS simulator, not a teaching exercise in hardware emulation. It is a structurally complete kernel — process management, memory management, IPC, filesystem, networking, scheduling — implemented against the JVM as its machine layer.

The central thesis: the JVM provides everything a kernel needs from hardware. Real threads mapped 1:1 to OS threads. Sub-microsecond timing via `System.nanoTime()`. A live heap with introspectable pressure. Atomic memory model guarantees. Native I/O. JRuby exposes all of it through Ruby. Kestówv builds a kernel on top of that exposure, without a line of Java or C.

The secondary thesis: Ruby is expressive enough to make kernel internals readable. `proc/task.rb` looks like what it is. `mm/vm_region.rb` is not opaque. A kernel does not have to be written in a language that requires ten years of C expertise to navigate. The concepts transfer; the notation does not have to.

---

## Why JRuby Specifically

Several kernel behaviours require JRuby directly and would not work on MRI:

**Native thread scheduling.** JRuby maps Ruby threads 1:1 to Java threads, which map 1:1 to OS threads. The Wave scheduler's daemon threads are real threads on real cores. The OS scheduler sees them. There is no GIL.

**Sub-microsecond timing.** `Java::JavaLang::System.nanoTime()` returns CPU TSC time in nanoseconds. The Wave scheduler uses this to compute sine wave phase positions with nanosecond precision. `Time.now` is not sufficient for slice-level CPU work accounting.

**Atomic double reads.** The JVM memory model guarantees that 64-bit `double` reads and writes are atomic on all conforming JVMs (JLS §17.7). The Wave governor writes `@governor_factor` (a Ruby Float, stored as a JVM `double`) from one thread and all wave threads read it without a lock. This is not a race condition. It is a deliberate use of the JMM guarantee.

**Heap introspection.** `Java::JavaLang::Runtime.get_runtime` exposes `total_memory`, `free_memory`, and `max_memory` live. The Wave governor samples these every 500ms to issue backpressure signals to the allocator and reduce wave amplitude when the GC needs CPU headroom.

**GC cooperation.** When JVM heap usage exceeds 85%, the governor calls both `GC.start` (Ruby GC) and `Java::JavaLang::System.gc()` (JVM GC hint), throttled to once per five seconds. This triggers the GC cycle rather than waiting for the JVM to decide on its own.

The CRuby/Tk dashboard (`tk/stress_app.rb`) is the deliberate exception: it runs on standard MRI Ruby specifically because the `tk` gem and `UNIXSocket` are available there. The JRuby kernel spawns it as a child process and streams MessagePack metrics over a Unix domain socket. Two Ruby runtimes, two different implementations of the language, communicating over a kernel-managed socket. The kernel treats the CRuby process as a first-class task registered via `Proc::Exec`.

---

## Ruby as Kernel Notation

A kernel written in Ruby differs from a kernel written in C in one important structural way: the module system does the work that `#include` and linker scripts do in C. `Kestowv::Proc::Task`, `Kestowv::Mm::VmRegion`, `Kestowv::Ipc::Pipe` are not just namespaces — they are the actual kernel objects, with their state, their lifecycle, and their invariants living in one place, expressible in the same language as everything that calls them.

Several Ruby features are used as kernel primitives:

**`Struct` for credentials.** `Proc::Credentials::Cred` is a `Struct` with fields mirroring Linux's `task_struct->cred`: real/effective/saved UID and GID, supplementary groups, capability bitmask. The struct is treated as immutable by convention — `derive(**overrides)` returns `Cred.new(...)` rather than mutating in place. This is the same copy-on-write pattern Linux uses for credential sharing between parent and child tasks. When performance requires it, a slab pool of 64 pre-allocated `Cred` structs is managed explicitly; `slab_reclaim!` and `slab_release` are defined directly on the Struct, extending it without subclassing.

**`frozen_string_literal: true` throughout.** Every kernel file declares this. Kernel paths, state names, and labels are frozen strings. This is not style — it eliminates an entire class of accidental mutation of shared kernel state from hot paths.

**Module hierarchies as namespace hierarchies.** The kernel has eight namespace types (`mount`, `pid`, `net`, `ipc`, `uts`, `user`, `cgroup`, `time`). They are implemented as Ruby modules in `proc/namespace.rb`. Namespace isolation is Ruby constant isolation. The conceptual mapping is clean enough that reading the code teaches you how Linux namespaces work.

**`Mutex` everywhere it matters, absent where the JMM provides the guarantee.** The codebase does not use locks defensively. It uses them where state must be consistent across a read-modify-write sequence, and relies on JVM atomicity where it holds. The Wave governor's read of `@governor_factor` in each wave thread's tight inner loop is unlocked by design.

---

## Architecture

```
Kestówv 0.5.1
│
├── boot/          Boot sequencer, feature-flag bit vector, safe_require,
│                  ByteClass file dispatch, stress harnesses, Tk launcher
│
├── core/          KObject (base lifecycle), Wave scheduler, RunQueue,
│                  Klog (ring buffer), BinaryClassifier, Signals
│
├── hal/           CPU topology, memory inventory
│                  (reads /proc/cpuinfo, /proc/meminfo at boot)
│
├── mm/            MemoryManager, FrameAllocator, PageTable, VmRegion,
│                  VmSpace, Heap, Slab object pool, Pressure signal
│
├── proc/          Task, Pid, Cgroup, Namespace (8 types), Session,
│                  Credentials (cap-aware), Limits, Exec, Signal delivery
│
├── fs/            VFS switch, TmpFs, HostFs (read-only Linux bind),
│                  ProcFs, DevFs, SysFs stubs, Dentry, Watch
│
├── ipc/           Pipe, Queue, Sem, Shm, Bus, Channel, RPC, Transport
│
├── net/           UnixHub, UnixSocket, TCP/UDP stubs, IP, ARP, DNS,
│                  Route, Filter, Interface, Namespace, Pool
│
├── config/        Module registry with dependency tracking, boot params
│
├── runtime/       Host detector (Linux/macOS/container), router
│
├── debug/         Console, log inspector, trace
│
└── tk/            CRuby/Tk live stress dashboard (runs on MRI)
```

---

## Subsystems

| Subsystem | File | Status |
|---|---|---|
| Boot sequencer | `boot/boot.rb` | active |
| Feature flags | `boot/boot.rb` (`Boot::BitVector`) | active |
| KObject lifecycle | `core/kobject.rb` | active |
| Wave CPU scheduler | `core/wave.rb` | active |
| Wave governor | `core/wave.rb` | active |
| Kernel log | `core/klog.rb` | active |
| Run queue | `core/run_queue.rb` | active |
| Binary classifier | `core/binary_classifier.rb` | active |
| HAL: CPU | `hal/cpu.rb` | active |
| HAL: Memory | `hal/memory.rb` | active |
| Memory manager | `mm/mm.rb` | active |
| Frame allocator | `mm/frame_allocator.rb` | active |
| Page table | `mm/page_table.rb` | active |
| Virtual memory region | `mm/vm_region.rb` | active |
| VM address space | `mm/vm_space.rb` | active |
| Slab object pool | `mm/slab.rb` | active |
| Heap allocator | `mm/heap.rb` | active |
| Memory pressure | `mm/pressure.rb` | active |
| VFS switch | `fs/vfs.rb` | active |
| TmpFs | `fs/tmpfs.rb` | active |
| HostFs | `fs/hostfs.rb` | active |
| Process task | `proc/task.rb` | active |
| PID allocator | `proc/pid.rb` | active |
| Cgroup | `proc/cgroup.rb` | active |
| Namespaces | `proc/namespace.rb` | active |
| Session | `proc/session.rb` | active |
| Credentials | `proc/credentials.rb` | active |
| Resource limits | `proc/limits.rb` | active |
| Exec registration | `proc/exec.rb` | active |
| IPC pipe | `ipc/pipe.rb` | active |
| IPC queue | `ipc/queue.rb` | active |
| IPC semaphore | `ipc/sem.rb` | active |
| IPC shared memory | `ipc/shm.rb` | active |
| Unix socket hub | `net/unix_hub.rb` | active |
| Unix domain socket | `net/unix_socket.rb` | active |
| Module registry | `config/modules.rb` | active |
| Tk dashboard | `tk/stress_app.rb` | active (CRuby/MRI) |

---

## Quick Start

**Requirements:** JRuby (tested on 9.4+), MRI Ruby with `tk` gem for the dashboard.

```bash
# Boot the kernel — runs full subsystem init, prints boot log
jruby boot/init.rb

# Kernel stress test — 30 threads across all subsystems, ~1B ops/run
jruby boot/stress.rb

# Live Tk dashboard — spawns CRuby Tk window, streams metrics over UDS
jruby boot/tk_stress.rb

# Wave scheduler benchmark
jruby boot/bench_wave.rb
```

The `tk_stress.rb` entry point boots the kernel, registers the Tk child process as a kernel task via `Proc::Exec`, opens a Unix domain socket via `Net::UnixHub`, spawns a `gnome-terminal` running `tk/stress_app.rb` under MRI Ruby, and begins streaming MessagePack metrics every 500ms. Close the Tk window to shut down cleanly.

---

## Performance — 0.5.1 test-stable

All figures from a 30-thread run across all active subsystems simultaneously.

| Metric | Value |
|---|---|
| Sustained throughput | ~100M ops/min |
| 10-minute run | 1,036,251,523 ops / 0 errors |
| 30-minute run | 1,146,605,615+ ops / 0 errors |
| Heap behaviour | Sawtooth — plateaus ~2.4 GB, GC cycles ~150–250 MB |
| Heap at t=300s | 2,286 MB (stable — no cliff) |
| GC reclaim observed | 238 MB in a single cycle (t=512s→542s) |

The 300-second stability boundary was the primary engineering target for this release. Earlier builds accumulated orphaned IPC pipe entries at ~1,600/second (no `close` method — `rescue nil` silently swallowed the `NoMethodError`). That single leak produced ~480,000 retained entries per run. Combined with unbounded shared-memory segments, 34k Hash allocations/sec from the log ring buffer, and 10k Struct allocations/sec from credential creation, the JVM heap filled monotonically past 300s. All four sources are resolved in 0.5.1.

---

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — full subsystem reference and load order
- [`docs/BOOT_SEQUENCE.md`](docs/BOOT_SEQUENCE.md) — step-by-step kernel boot walkthrough
- [`docs/WAVE_SCHEDULER.md`](docs/WAVE_SCHEDULER.md) — polyphase sine wave scheduler and governor
- [`docs/MEMORY.md`](docs/MEMORY.md) — MM subsystem, slab allocator, GC pressure signals
- [`docs/IPC.md`](docs/IPC.md) — IPC primitives reference
- [`docs/STRESS_DASHBOARD.md`](docs/STRESS_DASHBOARD.md) — running the live Tk dashboard
- [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) — full benchmark data and heap profiles

---

## Status

`0.5.1 test-stable` — all active subsystems boot cleanly, pass 30+ minute continuous stress under full load with zero errors. Heap is stable. This is the first tagged stable release.

Active subsystems are feature-complete for this version. Several stubs exist (`net/tcp_socket.rb`, `net/udp_socket.rb`, `fs/procfs.rb`, `fs/devfs.rb`) and are noted as such in their respective source files. Binary execution (`Proc::Exec`) registers tasks in the kernel task table but does not yet load and execute foreign binaries — that is the primary target for 0.6.0.
