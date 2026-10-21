# Memory Subsystem

`mm/` — `Kestowv::Mm`

---

## Overview

The memory subsystem models the full Linux memory management stack: physical frames, page tables, virtual memory regions, address spaces, a heap allocator, and a slab object pool. The JVM heap is the physical memory. `Java::JavaLang::Runtime.get_runtime` exposes its capacity and pressure state live.

Subsystems in load order:

| File | Module | Purpose |
|---|---|---|
| `mm/protection.rb` | `Mm::Protection` | Page protection flags (READ, WRITE, EXEC, USER) |
| `mm/physical.rb` | `Mm::Physical` | Physical memory inventory from HAL |
| `mm/page.rb` | `Mm::Page` | Page struct (PFN, flags, ref count) |
| `mm/frame_allocator.rb` | `Mm::FrameAllocator` | Physical page pool, allocate/free |
| `mm/page_table.rb` | `Mm::PageTable` | VPN→PFN mapping with flags |
| `mm/vm_region.rb` | `Mm::VmRegion` | Virtual memory region (VMA), slab-pooled |
| `mm/vm_space.rb` | `Mm::VmSpace` | Process virtual address space |
| `mm/heap.rb` | `Mm::Heap` | Kernel heap allocator |
| `mm/slab.rb` | `Mm::Slab` | Generic object pool |
| `mm/pressure.rb` | `Mm::Pressure` | Heap pressure signal (`:low`→`:critical`) |
| `mm/mm.rb` | `Mm::MemoryManager` | Top-level init, VmSpace factory, pressure relay |
| `mm/cow.rb` | `Mm::CoW` | Copy-on-write region tracking |
| `mm/fault.rb` | `Mm::Fault` | Page fault handler |
| `mm/numa.rb` | `Mm::Numa` | NUMA topology stub |
| `mm/oom.rb` | `Mm::Oom` | OOM killer stub |
| `mm/virtual.rb` | `Mm::Virtual` | Virtual memory utilities |

---

## Slab Allocator

`mm/slab.rb` — `Mm::Slab`

The slab allocator eliminates GC pressure for high-churn kernel objects. Instead of allocating and discarding short-lived objects in hot paths, the slab pre-allocates a fixed pool at boot and hands them out via `acquire`/`release`.

### Pool

```ruby
pool = Mm::Slab.create_pool(:vm_region, capacity: 256) do
  Mm::VmRegion.new(start_vpn: 0, num_pages: 1, backing: :anonymous, name: :_slab)
end
```

The factory block is called `capacity` times at creation. All instances live in `@free` until `acquire` is called.

### acquire

```ruby
def acquire
  obj = @mutex.synchronize do
    @total_req += 1
    @free.empty? ? (@misses += 1; nil) : (@acquired += 1; @free.pop)
  end
  obj&.slab_reclaim!   # called OUTSIDE the pool mutex
  obj
end
```

`slab_reclaim!` is called outside the pool mutex. This is deliberate: `KObject#slab_reclaim!` acquires the object's own `@mutex` to reset lifecycle fields. Calling it inside the pool mutex would create lock ordering `pool_mu → obj_mu`. Any other path that holds `obj_mu` and then calls back into the pool would deadlock. Inverting the order — release the pool lock first, then reclaim — eliminates the inversion entirely.

On a miss (`@free` is empty), `acquire` returns `nil` and the caller falls back to normal allocation. The pool degrades gracefully under load rather than blocking.

### release

```ruby
def release(obj)
  @mutex.synchronize do
    @free.push(obj) if @free.size < @capacity
    @acquired -= 1 if @acquired > 0
  end
end
```

If the pool is already at capacity (which should not happen under normal operation but can occur if a caller releases twice), the object is simply discarded — the pool does not grow beyond its initial size.

### slab_reclaim! and reuse!

`KObject#slab_reclaim!` resets the lifecycle fields that the base class tracks:

```ruby
def slab_reclaim!
  @mutex.synchronize do
    @refcount   = 1
    @destroyed  = false
    @created_at = ::Time.now.freeze
  end
  self
end
```

Domain-specific reset is handled by `reuse!` on the concrete class, called by the module-level `slab_acquire` after `acquire` returns:

```ruby
# VmRegion:
def slab_acquire(start_vpn:, num_pages:, flags: ..., name: nil, backing: :anonymous)
  obj = @pool&.acquire            # pool calls slab_reclaim! internally
  obj ? obj.reuse!(start_vpn: start_vpn, ...) : new(...)
end

def reuse!(start_vpn:, num_pages:, flags:, name:, backing:)
  @mutex.synchronize do
    @start_vpn = start_vpn
    @num_pages = num_pages
    @flags     = flags
    @name      = name
    @backing   = backing
    @mapped    = false
  end
  self
end
```

### slab_release

```ruby
def slab_release
  @mutex.synchronize do
    @refcount -= 1
    return self if @refcount > 0
    @destroyed = true
  end
  self.class.slab_pool&.release(self)
  self
end
```

If the refcount is still positive after decrement, the object is still live and is not returned to the pool. Only when refcount reaches zero is it released. This mirrors the kernel reference counting model.

### Active Pools

| Pool | Capacity | Objects |
|---|---|---|
| `:vm_region` | 256 | `Mm::VmRegion` |
| `:task` | 128 | `Proc::Task` |
| `:credentials` | 64 | `Proc::Credentials::Cred` |

All three are initialised during the `MM: Slab pools` boot step, after `Mm::MemoryManager` is ready and before the process layer loads.

### Pool Stats

```ruby
Mm::Slab.pool_stats
# => {
#   vm_region:   { capacity: 256, free: 254, acquired: 2, misses: 0, hit_rate: 1.0 },
#   task:        { capacity: 128, free: 127, acquired: 1, misses: 0, hit_rate: 1.0 },
#   credentials: { capacity: 64,  free: 62,  acquired: 2, misses: 0, hit_rate: 1.0 }
# }
```

---

## Memory Pressure

`mm/pressure.rb` — `Mm::Pressure`

A module-level signal shared between the memory subsystem and the Wave governor. The allocator sets the level; the governor reads it.

```ruby
Mm::Pressure.set_level(:critical)
Mm::Pressure.level   # => :critical
```

Levels: `:low`, `:medium`, `:high`, `:critical`.

The relay path from allocator to scheduler:

```ruby
# Mm::MemoryManager:
def signal_gc_pressure(level)
  Mm::Pressure.set_level(level)
  Core::Wave.signal_gc_pressure(level) if defined?(Core::Wave) && Core::Wave.running?
end
```

This is the MM→CPU load balance signal. When the allocator detects heap stress it tells the Wave governor to back off immediately, before the governor's own 500ms sample tick would catch it. The governor applies a delta to `@governor_factor` (see `docs/WAVE_SCHEDULER.md`) and the wave threads observe the change on their next slice.

---

## Page Table

`mm/page_table.rb` — `Mm::PageTable`

VPN → PFN mapping backed by a Ruby Hash. Flags use integer bitmasks matching Linux `pte_t` conventions:

```ruby
Mm::PageTable::Flags::PRESENT  = 0x01
Mm::PageTable::Flags::WRITABLE = 0x02
Mm::PageTable::Flags::USER     = 0x04
Mm::PageTable::Flags::NX       = 0x08
```

```ruby
pt = Mm::PageTable.new
pt.map(vpn, pfn, flags: Flags::PRESENT | Flags::WRITABLE)
pt.lookup(vpn)    # => { pfn:, flags: }
pt.unmap(vpn)
pt.stats          # => { mappings:, page_table_id: }
```

---

## Protection Flags

`mm/protection.rb` — `Mm::Protection`

```ruby
Mm::Protection::READ    = 0x1
Mm::Protection::WRITE   = 0x2
Mm::Protection::EXEC    = 0x4
Mm::Protection::USER    = 0x8

Mm::Protection::USER_RW  = READ | WRITE | USER
Mm::Protection::KERNEL_R = READ
```

`readable?`, `writable?`, `executable?`, `user_accessible?` predicates are defined as module methods.

---

## Virtual Memory Region

`mm/vm_region.rb` — `Mm::VmRegion`

A single contiguous virtual address range within a process's address space. Models Linux's `vm_area_struct`.

```ruby
r = Mm::VmRegion.slab_acquire(
  start_vpn: 0x8000,
  num_pages: 4,
  flags:     Mm::Protection::USER_RW,
  backing:   :anonymous
)
# ... use r ...
r.slab_release
```

Fields: `start_vpn`, `num_pages`, `flags`, `backing` (`:anonymous`, `:file`, `:device`), `name`, `mapped`.

---

## Address Space

`mm/vm_space.rb` — `Mm::VmSpace`

Container for all `VmRegion` objects belonging to one process. Created by `Mm::MemoryManager.create_vm_space(name:)` and assigned to a `Proc::Task`.

```ruby
vm_space = Mm::MemoryManager.create_vm_space(name: :init)
task.assign_vm_space(vm_space)
```
