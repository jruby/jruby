# Boot Sequence

`boot/init.rb` — `Kestowv::Init`

---

## Overview

The kernel boots in a single call to `Kestowv::Init.boot`. Each step is wrapped in `step(label) { ... }` which prints a checkmark on success or a failure message and re-raises on error. The boot is not resumable — if any step fails, the process exits.

Boot is idempotent-guarded: `Init.boot` raises if called twice without `Init.reset!` in between.

---

## Step-by-Step

### Step 0 — Load all kernel subsystems

```ruby
step("Kernel: load subsystems") do
  load_kernel_subsystems
end
```

`load_kernel_subsystems` calls `Boot.safe_require` for `config/modules.rb` first (the module registry must exist before any other file's auto-register call runs), then `Boot.load_directory` for each subdirectory in dependency order:

```
core → hal → runtime → mm → config → debug → fs → ipc → proc → net
```

`Boot.load_directory` applies `Boot.byte_dispatch` to each file before loading — non-Ruby files are skipped. All files are `require`d exactly once; `Boot.safe_require` is idempotent.

After loading, `Config::Modules.sync_loaded_state` marks any file that was loaded via `safe_require` as loaded in the module registry (those files bypass `load_module` and would otherwise appear as unloaded).

### Step 1 — HAL

```ruby
step("HAL: CPU")    { Hal::Cpu.register_with_boot }
step("HAL: Memory") { Hal::Memory.register_with_boot }
```

`Hal::Cpu` reads `/proc/cpuinfo` to discover core count, topology, and clock speed. `Hal::Memory` reads `/proc/meminfo` to inventory physical memory. Both register their feature bits with `Boot`.

### Step 2 — Memory Manager

```ruby
step("MM: MemoryManager") { Mm::MemoryManager.init(1024) }
```

Initialises the memory manager with 1024 MB of managed address space. Creates the zone table and sets up the frame allocator pool.

```ruby
step("MM: Slab pools") do
  Mm::VmRegion.init_pool(capacity: 256)
  Proc::Task.init_pool(capacity: 128)
  Proc::Credentials.init_pool(capacity: 64)
end
```

Pre-allocates 448 kernel objects across three slab pools. This must happen after `Mm::MemoryManager.init` (the slab pools use `Mm::Slab` which is managed by the MM) and before the process layer loads (Tasks and Credentials are needed by `create_init_task`).

### Step 3 — Filesystem

```ruby
step("FS: init") { Fs.init(ns_id: nil, mount_host: true) }
```

Mounts a `TmpFs` at `/` (the kernel's root). If `mount_host: true` and the kernel is running on Linux, mounts the host filesystem read-only at `/host` via `HostFs`. The `HostDetector` determines the host OS and whether the process is containerised.

### Step 3a — Binary Classifier

```ruby
step("Core: BinaryClassifier") { Core::BinaryClassifier.register_with_boot }
```

Registers the classifier feature bit. The classifier itself is loaded in Step 0 as part of the `core/` directory; this step simply marks it active.

### Step 3b — Wave Scheduler

```ruby
step("Core: Wave scheduler") do
  Core::Wave.start(n_banks: 2, phases_per_bank: 2, period_ms: 2000, slice_ms: 10)
  Core::Wave.register_with_boot
end
```

Starts 4 wave threads (2 banks × 2 phases) plus the governor thread — 5 daemon threads total. These threads run for the kernel lifetime. The `start` call returns immediately after spawning all 5 threads; the threads enter their loops independently.

This step is a no-op on MRI Ruby — `defined?(JRUBY_VERSION)` guards the thread creation.

### Step 4 — IPC

```ruby
step("IPC: register") { Ipc.register_with_boot }
```

Registers the IPC subsystem feature bit. Individual IPC primitives (Pipe, Queue, Sem, Shm) are auto-registered when their files are loaded in Step 0.

### Step 5 — Process Layer

```ruby
step("Proc: Pid")         { Proc::Pid.register_with_boot }
step("Proc: Cgroup")      { Proc::Cgroup.register_with_boot }
step("Proc: Namespace")   { Proc::Namespace.register_with_boot }
step("Proc: Session")     { Proc::Session.register_with_boot }
step("Proc: Credentials") { Proc::Credentials.register_with_boot }
step("Proc: Limits")      { Proc::Limits.register_with_boot }
```

Each subsystem registers its feature bit. They depend on the MM slab pools being ready (Steps 2 and 2a) because `Task` and `Credentials` are slab-allocated.

### Step 6 — PID 1

```ruby
step("Init: PID 1") { task = create_init_task }
```

Creates the init task — the process from which all other processes descend. Full wiring:

1. `Mm::MemoryManager.create_vm_space(name: :init)` — address space
2. `Proc::Namespace.create_set` — 8-type namespace set (mount, pid, net, ipc, uts, user, cgroup, time)
3. `Proc::Credentials.root` — root credential with all capabilities (CHOWN, DAC_OVERRIDE, KILL, SETUID, SETGID, NET_ADMIN, SYS_ADMIN, SYS_PTRACE)
4. `Proc::Limits.default` — default resource limits
5. `Proc::Task.new(name: :init)` — task struct with all of the above assigned
6. `Proc::Pid.bind(1, task)` — bind PID 1
7. `Proc::Session.create_session(1)` — SID 1
8. `Proc::Cgroup.assign(1, root_cg.id)` — root cgroup assignment
9. `Fs.mount("/proc", ...)` — procfs visible in init's mount namespace

### Step 7 — Scheduler Handoff

```ruby
step("Init: scheduler handoff") { task.transition(:running) }
```

Transitions PID 1 from `:ready` to `:running`. From this point the kernel is live.

---

## Boot Log

```
[Init] ✓ Kernel: load subsystems
[Init] ✓ HAL: CPU
[Init] ✓ HAL: Memory
[Init] ✓ MM: MemoryManager
[Init] ✓ MM: Slab pools
[Fs] Detected: #<HostDetector::Detection os=linux jvm=Linux containerized=false>
[Fs] Mounted tmpfs at /
[Fs] Mounted hostfs at /host (linux, read-only)
[Init] ✓ FS: init
[Init] ✓ Core: BinaryClassifier
[Init] ✓ Core: Wave scheduler
[Init] ✓ IPC: register
[Init] ✓ Proc: Pid
[Init] ✓ Proc: Cgroup
[Init] ✓ Proc: Namespace
[Init] ✓ Proc: Session
[Init] ✓ Proc: Credentials
[Init] ✓ Proc: Limits
[Init] ✓ Init: PID 1
[Init] ✓ Init: scheduler handoff
[Init] Boot complete — 2 PIDs active
```

---

## After Boot

```ruby
Kestowv::Init.booted?    # => true
Kestowv::Init.init_task  # => #<Proc::Task name=:init state=:running>
Kestowv::Init.stats      # => { booted:, init_task:, mm:, fs:, proc_pid: }
```

`Init.reset!` tears down boot state (sets `@booted = false`, clears `@init_task`) for testing or re-boot scenarios. It does not stop Wave threads or unmount filesystems — callers are responsible for clean teardown of subsystems they care about.
