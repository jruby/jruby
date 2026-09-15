# Performance

All figures from `boot/tui_stress.rb` / `boot/tk_stress.rb` — 30 concurrent threads across all active subsystems.

**Platform:** Linux, JRuby 9.4+, Java 21, OpenJDK, x86-64.

---

## Throughput

| Run | Duration | Ops | Errors | Avg ops/sec |
|---|---|---|---|---|
| Integration test | ~10 min | 1,036,251,523 | 0 | ~1.7M/s |
| 30-minute soak | 30 min | 1,146,605,615+ | 0 | ~640k/s |

The soak run's lower average reflects JIT warmup cost amortised over a longer window and governor throttling under sustained load. Peak throughput (post-warmup, governor at 1.0) routinely exceeds 1M ops/sec.

---

## JVM Heap Profile — 0.5.1

30 seconds per sample, 30-thread run.

```
t=  61s   1243 MB   (JIT still warming up)
t=  91s   1808 MB
t= 121s   1396 MB   ← first GC cycle (early collection)
t= 151s   1884 MB
t= 181s   2056 MB
t= 211s   2160 MB
t= 241s   2219 MB
t= 271s   2257 MB
t= 302s   2286 MB   ← 0.5.0 cliff point — STABLE in 0.5.1
t= 332s   2309 MB
t= 362s   2327 MB
t= 392s   2341 MB
t= 422s   2348 MB
t= 452s   2353 MB
t= 482s   2358 MB
t= 512s   2370 MB
t= 542s   2248 MB   ← GC reclaimed 122 MB
t= 573s   2347 MB
t= 603s   2394 MB
t= 633s   2414 MB
t= 663s   2425 MB
```

Heap plateaus near 2.4 GB and cycles via GC rather than climbing to OOM. The 300-second stability boundary is the primary engineering achievement of 0.5.1.

---

## Memory Leaks Fixed in 0.5.1

Four categories of memory growth were identified and resolved:

### 1. Ipc::Pipe — primary leak

`Ipc::Pipe` had no `close(id)` method. The stress thread called:
```ruby
Ipc::Pipe.close(id) rescue nil
```

The `rescue nil` silently swallowed a `NoMethodError`. Every `create()` call added entries to `@pipes` (Hash: id → Array) and `@heads` (Hash: id → Integer) that were never removed.

At ~1,600 creates/second × 300 seconds = **480,000 permanently retained entries** per run.

**Fix:** Added `Ipc::Pipe.close(id)` — deletes from both `@pipes` and `@heads`.

### 2. Ipc::Shm — secondary leak

Same pattern: `Ipc::Shm.destroy(key) rescue nil` swallowed `NoMethodError`. `@segments` grew without bound.

**Fix:** Added `Ipc::Shm.destroy(key)` — deletes from `@segments`.

### 3. Klog — ring buffer allocation pressure

Each `log()` call created a new `Hash` + `Time` object:
```ruby
entry = { time: ::Time.now, level: lvl, message: message, context: context }
```

At ~34,000 log calls/second across the `core/klog` stress thread, this produced ~34k short-lived Hash allocations/second — sustained GC pressure.

**Fix:** Pre-allocated 1024 `Entry = Struct.new(:time, :level, :message, :context)` objects at module load. The hot `log()` path mutates the struct at `@ring_pos` in place. Zero Hash allocations per log call. `Time.now` is unavoidable.

### 4. Credentials — Struct allocation pressure

`Credentials.user(uid:, gid:)` called `create()` which called `Cred.new(...)` on every invocation. At ~10,000 calls/second from the `proc/creds` stress thread, this produced 10k new Struct allocations/second.

**Fix:** Added a slab pool of 64 `Cred` structs. `Credentials.slab_acquire` pulls from the pool; `cred.slab_release` returns to it. The stress thread uses `slab_acquire`/`slab_release`. `slab_reclaim!` and `slab_reuse!` are defined directly on the Struct to reset fields without instantiating a new object.

### 5. UDS payload allocation pressure

Three families of stress threads created new Hash objects on every iteration:
- `uds/send-io`: `{ seq: i, payload: "...", ts: Time.now.to_f }` — ~15k/s
- `uds/socketpair`: `{ msg: "ping" }` — ~10k/s
- `uds/writer-0..3`: `{ writer: wid, seq: i }` — ~15k/s × 4 = 60k/s

Combined: ~85k new Hash objects/second.

**Fix:** Pre-allocate the payload Hash once per thread; mutate `:seq` and `:ts` in place on each iteration. The Hash identity is stable; only the values change.

---

## GC Cooperation

The Wave governor implements a three-tier response to JVM heap pressure:

| Heap ratio | Response |
|---|---|
| > 50% | Pressure level `:medium` — governor floor 0.65 |
| > 70% | Pressure level `:high` — effective CPU cap drops to 60%, floor 0.50, `gc_headroom = true` |
| > 80% | Governor backs off an additional step regardless of CPU% |
| > 85% | Pressure level `:critical` — floor 0.30; `GC.start` + `System.gc()` issued once per 5s |

The effect is visible in the heap profile: when heap ratio exceeds 85%, the governor simultaneously backs the wave threads off to ~30% amplitude and issues the GC hint. The result is a rapid 100–240 MB collection (as seen at t=121s, t=542s in the profile above) followed by the heap resuming its normal growth trajectory.

---

## Per-Subsystem Throughput

Approximate single-thread ops/sec for each subsystem. Actual throughput varies with concurrent load.

| Subsystem | Approx ops/sec |
|---|---|
| `core/binclass` | 90,000–100,000 |
| `core/runqueue` | 80,000–90,000 |
| `mm/frames` | 60,000–70,000 |
| `proc/creds` (slab) | 35,000–45,000 |
| `mm/vmregion` (slab) | 35,000–45,000 |
| `proc/task` (slab) | 30,000–40,000 |
| `core/klog` (reuse) | 45,000–55,000 |
| `mm/heap` | 55,000–65,000 |
| `ipc/queue` | 55,000–65,000 |
| `ipc/sem` | 50,000–60,000 |
| `mm/pagetable` | 25,000–35,000 |
| `ipc/pipe` | 15,000–20,000 |
| `uds/hub-lookup` | 40,000–50,000 |
| `uds/send-io` | 12,000–18,000 |
| `fs/tmpfs` | 15,000–25,000 |
| `boot/bitvec` | 70,000–85,000 |

---

## IPC Wire Format — JSON vs MessagePack

The JRuby kernel serialises one metrics frame per 500ms tick over the UDS connection to the CRuby dashboard. Prior to 0.5.1 this used JSON; 0.5.1 uses MessagePack throughout.

Benchmark: 100,000 iterations of a full 30-subsystem metrics frame (representative of a live tick). MRI Ruby, x86-64 Linux.

| Operation | JSON | MessagePack | Speedup |
|---|---|---|---|
| Serialise (pack/generate) | 26.5 µs | 6.3 µs | **4.2×** faster |
| Deserialise (unpack/parse) | 45.4 µs | 31.5 µs | **1.4×** faster |
| Payload size | 2,189 bytes | 1,658 bytes | **1.32×** smaller |

At the 500ms tick interval the latency difference per tick is negligible in absolute terms (~20 µs). The significance is cumulative CPU cost over long soak runs — MessagePack's 4.2× serialisation advantage removes a consistent source of overhead on every tick — and payload size on the UDS socket, which reduces kernel buffer pressure during high-throughput bursts.

MessagePack also avoids the text-encoding round-trip (UTF-8 escaping, float-to-string, number parsing) entirely, making it the correct default for any structured binary IPC between Ruby processes.

---

## Wave Scheduler Overhead

The Wave scheduler daemon threads consume CPU in a controlled sine wave pattern, deliberatly up to the 80% cap. This is not overhead — it is the scheduler demonstrating its primary function: preventing thermal throttling by distributing CPU work in phase-offset waves rather than simultaneous peaks.

The governor's effect on observed throughput:

| Governor factor | Wave CPU draw | Headroom for stress threads |
|---|---|---|
| 1.00 | ~80% (capped) | ~20% |
| 0.87 | ~70% | ~30% |
| 0.50 | ~40% | ~60% |
| 0.30 (critical) | ~24% | ~76% |

At critical heap pressure, the governor backs off to 0.30 — the stress threads see more CPU and the GC runs without competing for cores. This is the intended behaviour.
