# Architecture

---

## The Machine Layer

Kestówv does not emulate hardware. The JVM provides the following hardware-equivalent guarantees that the kernel is built against directly:

| Hardware concept | JVM equivalent |
|---|---|
| Physical memory | JVM heap (`Runtime.total_memory`, `max_memory`) |
| Memory pressure | `heap_used / max_memory` ratio |
| High-resolution timer | `System.nanoTime()` — CPU TSC, nanosecond resolution |
| Native threads | JRuby 1:1 Ruby→Java→OS thread mapping, no GIL |
| Atomic 64-bit reads | JMM §17.7 — `double` field reads are atomic |
| Hardware GC | `System.gc()` — JVM GC hint |
| I/O | Java NIO / standard JRuby socket layer |
| Host filesystem | `/host` mount via `Fs::HostFs` |

Everything above the JVM is pure Ruby. There is no Java or C in the kernel.

---

## Module Namespace Layout

```
Kestowv
├── Boot                      — sequencer, feature flags, file dispatch
├── Core
│   ├── KObject               — base lifecycle (refcount, mutex, destroy)
│   ├── Wave                  — polyphase CPU scheduler + governor
│   ├── Klog                  — ring-buffer kernel log
│   ├── RunQueue              — priority run queue
│   ├── BinaryClassifier      — file type detection
│   ├── Scheduler             — scheduler interface stub
│   ├── Signals               — signal table
│   └── WorkQueue             — deferred work queue
├── Hal
│   ├── Cpu                   — core count, topology, clock
│   └── Memory                — physical memory inventory
├── Mm
│   ├── MemoryManager         — top-level init, VmSpace factory, pressure relay
│   ├── FrameAllocator        — physical page pool
│   ├── PageTable             — VPN→PFN mapping
│   ├── VmRegion              — virtual memory region (slab-pooled)
│   ├── VmSpace               — per-process virtual address space
│   ├── Heap                  — kernel heap allocator
│   ├── Slab                  — generic object pool
│   ├── Pressure              — heap pressure signal
│   ├── Protection            — page protection flags
│   ├── CoW                   — copy-on-write tracking
│   ├── Fault                 — page fault handler
│   ├── Numa                  — NUMA topology
│   ├── Oom                   — OOM killer
│   ├── Page                  — physical page struct
│   ├── Physical              — physical memory model
│   └── Virtual               — virtual memory utilities
├── Proc
│   ├── Task                  — process task struct (slab-pooled)
│   ├── Pid                   — PID allocator and namespace
│   ├── Cgroup                — control group hierarchy
│   ├── Namespace             — 8-type namespace set
│   ├── Session               — session and process group
│   ├── Credentials           — UID/GID/capability sets (slab-pooled)
│   ├── Limits                — per-process resource limits
│   ├── Exec                  — exec registration
│   ├── SignalDelivery        — signal delivery mechanism
│   ├── PTrace                — ptrace stub
│   ├── Wait                  — waitpid stub
│   └── Env                   — environment block
├── Fs
│   ├── Vfs                   — VFS switch (mount/unmount/resolve)
│   ├── TmpFs                 — in-memory filesystem
│   ├── HostFs                — read-only Linux host bind mount
│   ├── ProcFs                — /proc stub
│   ├── DevFs                 — /dev stub
│   ├── SysFs                 — /sys stub
│   ├── MemFs                 — memory-backed FS base
│   ├── Dentry                — directory entry cache
│   ├── Mount                 — mount table
│   ├── Path                  — path resolution
│   ├── Stat                  — file stat struct
│   ├── Attr                  — extended attributes
│   ├── Buffer                — block buffer cache
│   ├── Dir                   — directory operations
│   ├── Watch                 — inotify-style watch
│   └── HostDetector          — OS/container detection at boot
├── Ipc
│   ├── Pipe                  — kernel pipe
│   ├── Queue                 — named message queue
│   ├── Sem                   — semaphore
│   ├── Shm                   — shared memory
│   ├── Bus                   — event bus
│   ├── Channel               — typed channel
│   ├── Msg                   — message struct
│   ├── Namespace             — IPC namespace
│   ├── RPC                   — remote procedure call
│   ├── Transport             — transport abstraction
│   ├── Discovery             — service discovery
│   ├── Client                — IPC client helper
│   └── Server                — IPC server helper
├── Net
│   ├── UnixHub               — UDS socket registry
│   ├── UnixSocket            — Unix domain socket
│   ├── TcpSocket             — TCP stub
│   ├── UdpSocket             — UDP stub
│   ├── Socket                — socket base
│   ├── Interface             — network interface
│   ├── Ip                    — IP layer
│   ├── Arp                   — ARP table
│   ├── Dns                   — DNS resolver stub
│   ├── Route                 — routing table
│   ├── Filter                — packet filter
│   ├── Icmp                  — ICMP stub
│   ├── Namespace             — network namespace
│   ├── Neighbor              — neighbor discovery
│   ├── Device                — network device
│   ├── RawSocket             — raw socket
│   └── Pool                  — socket pool
├── Config
│   ├── Modules               — module registry with dependency tracking
│   ├── Defaults              — default configuration values
│   ├── Loader                — configuration file loader
│   ├── Params                — parameter definitions
│   └── Tune                  — runtime tuning
├── Runtime
│   ├── Detector              — JRuby/MRI/container detection
│   └── Router                — subsystem routing
└── Debug
    ├── Console               — kernel console
    ├── Log                   — log inspector
    └── Trace                 — execution tracer
```

---

## Boot Load Order

The kernel loads subsystems in strict dependency order via `Boot.load_directory`. Load order is enforced by the directory sequence in `Init#load_kernel_subsystems`:

```
config/modules.rb   ← must be first; every other file calls
                      Config::Modules.register at load time

core/               ← KObject is the base class for Page, VmRegion, Task
hal/
runtime/
mm/                 ← depends on Core::KObject
config/             ← remaining config files
debug/
fs/                 ← depends on Mm
ipc/
proc/               ← depends on Mm, Core
net/
```

After loading, `Config::Modules.sync_loaded_state` reconciles the loaded state for files that bypassed `load_module` via `Boot.safe_require`.

The boot sequence itself is documented in detail in [`BOOT_SEQUENCE.md`](BOOT_SEQUENCE.md).

---

## Feature Flags

`Boot` maintains a bit-vector feature flag system. Every subsystem registers a symbol and sets its bit when it finishes initialising:

```ruby
Boot.register(:core_wave)
Boot.set_bit(:core_wave)
Boot.bit_set?(:core_wave)   # => true
```

The bit vector lives in thread-local storage (`Thread.current[:boot_bit_vector]`) for the wave scheduler's inner loop, which XORs bit positions as its CPU work unit. Module-level feature state lives in `Boot`'s class-level integer bitmask.

---

## KObject — Base Lifecycle

`core/kobject.rb` — `Kestowv::Core::KObject`

Every kernel object that participates in reference counting and the slab pool inherits from `KObject`:

```ruby
class Mm::VmRegion < Core::KObject
class Proc::Task   < Core::KObject
```

`Proc::Credentials::Cred` is a `Struct` (for the keyword-init pattern and hash-like access) and implements the slab interface methods (`slab_reclaim!`, `slab_reuse!`, `slab_release`) directly without inheriting from `KObject`.

KObject provides:

```ruby
retain    # refcount += 1
release   # refcount -= 1; destroy if 0
destroy   # marks @destroyed = true; subclass hooks
alive?
destroyed?
to_h      # base introspection

slab_reclaim!  # reset lifecycle fields (called by Slab::Pool#acquire)
```

---

## Module Registry

`config/modules.rb` — `Config::Modules`

Every kernel file registers itself at load time:

```ruby
Kestowv::Config::Modules.register(
  :ipc_pipe,
  __FILE__,
  feature:    :ipc_pipe,
  depends_on: [:ipc]
)
```

The registry tracks loaded state, dependencies, and the file path for each module. `Config::Modules.loaded?(:ipc_pipe)` returns true after the file has been `require`d. `sync_loaded_state` reconciles entries that were loaded via `Boot.safe_require` (which bypasses the `load_module` path).

---

## ByteClass Dispatch

`Boot.byte_dispatch(path)` classifies a file before loading it. `ByteClass` reads the first few bytes and returns a dispatch object with a `loadable?` predicate. Files that are not loadable Ruby source (binaries, data files, non-Ruby scripts) are skipped by `Boot.load_directory`. This is the `BinaryClassifier` mechanism applied at the boot level.

---

## Two-Runtime Architecture

The live stress dashboard introduces a deliberate two-runtime design:

```
JRuby process (Kestówv kernel)
  │
  ├── Proc::Task(:tk_stress_app)     — CRuby registered as kernel task
  ├── Proc::Exec(:tk/stress_app.rb)  — exec recorded in task table
  ├── Net::UnixHub                   — UDS socket registered in kernel hub
  │
  └── UDS /tmp/kestowv_tk_<pid>.sock ─────────────────────────────────────┐
                                                                            │
CRuby/MRI process (tk/stress_app.rb)                                       │
  ├── Socket reader thread ── UNIXSocket.new(SOCK_PATH).gets loop ─────────┘
  └── Tk event loop ── TkRoot, TkLabel, TkText, Tk.after(500ms) refresh
```

The kernel streams MessagePack over the socket every 500ms. The CRuby process feeds chunks into a `MessagePack::Unpacker` in a background thread, stores decoded frames in a mutex-protected `$data` hash, and the Tk event loop reads it in the `Tk.after` callback. The two runtimes share no memory and have no Ruby-level coupling beyond the MessagePack wire format.

The kernel treats the CRuby process as a first-class task: it has a `Proc::Task` with a `VmSpace`, `Credentials`, and a registered `Proc::Exec` entry. When the Tk window closes, the kernel detects the `EPIPE` on the socket and shuts down cleanly.
