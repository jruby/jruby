# Wave Scheduler

`core/wave.rb` — `Kestowv::Core::Wave`

---

## Concept

The Wave scheduler distributes CPU load across cores using phase-offset sine waves. The goal is to prevent cores from peaking simultaneously — eliminating thermal throttling spikes and preventing the JVM from bunching work onto the same core at the same moment.

The voltage analogy is intentional:

```
  0% CPU  =  -240V   (negative peak)
 50% CPU  =     0V   (neutral — AC ground reference)
100% CPU  =  +240V   (positive peak)
```

Each scheduler thread traces one phase of a sine wave through time. Multiple threads form a polyphase system where the phases sum to a constant — the same property that makes three-phase AC power deliver smooth torque rather than pulsed torque.

---

## Transformer Bank Mode

Default configuration: 2 banks × 2 phases = 4 wave threads.

```
Bank A:  T0 = 0°    T1 = 180°   →  A0 + A1 = constant 100% CPU
Bank B:  T2 = 90°   T3 = 270°   →  B0 + B1 = constant 100% CPU
```

Each bank's two phases are always in opposition — when one is at peak load the other is at minimum. The sum across a bank is constant. This is the transformer bank property: no thermal spikes, no idle waste, no cores fighting the scheduler for time slices.

---

## Sine Formula

```
cpu(t) = 50% + 50% × sin(2π·t/T + φₖ)
```

Where:
- `T` = period in milliseconds (default 2000ms)
- `φₖ` = phase offset for thread k
- Each slice: work for `target%` of `slice_ms`, sleep for the remainder

The wave threads do not simulate work. They perform real CPU operations (bit vector XOR across 60-bit positions) at the rate the sine formula dictates for their current position in the wave. The JVM JIT promotes the inner loop after the first few hundred slices.

---

## JVM Timing

```ruby
SYSTEM = defined?(JRUBY_VERSION) ? Java::JavaLang::System : nil

now               = SYSTEM.nano_time
elapsed_in_period = (now - wave_start_ns) % period_ns
theta             = TWO_PI * elapsed_in_period.to_f / period_ns + phase_rad
```

`System.nanoTime()` returns CPU TSC time. On Linux with `clock_gettime(CLOCK_MONOTONIC)` as the backing implementation, resolution is typically 1–10ns. This is what makes sub-millisecond slice accounting possible. `Time.now` is not sufficient — its resolution is implementation-defined and typically in the microsecond range at best.

The wave scheduler is guarded by `defined?(JRUBY_VERSION)`. It is a no-op on MRI.

---

## Governor

The governor runs as a 5th daemon thread alongside the 4 wave threads. It samples CPU utilisation and JVM heap every 500ms and adjusts `@governor_factor` — a scalar in [0.10, 1.00] that multiplies wave amplitude.

```ruby
target = @governor_factor * (0.5 + 0.5 * Math.sin(theta))
```

At `@governor_factor = 1.0` the wave runs at full amplitude. At `0.10` the threads are doing 10% of their normal work. The governor drives this value based on two signals:

### CPU signal

```ruby
avg_cpu > @cpu_cap (0.80)  →  factor -= GOV_STEP (0.05)
avg_cpu < @cpu_cap - GOV_HYSTERESIS (0.10)  →  factor += GOV_STEP
```

The hysteresis band (10%) prevents rapid oscillation around the cap. The governor does not respond to momentary spikes — it responds to the average across all cores over the 500ms sample window.

### JVM heap signal

```ruby
heap_ratio = heap_used.to_f / rt.max_memory

heap_ratio > 0.85  →  :critical
heap_ratio > 0.70  →  :high
heap_ratio > 0.50  →  :medium
else               →  :low
```

When heap pressure is high, two things happen simultaneously:

1. The effective CPU cap is reduced: `gc_cap = cpu_cap - 0.20` when `heap_ratio > 0.70`. This backs the wave threads off an additional 20% of CPU, giving the GC more time to run without competing for cores.

2. A pressure floor is applied: `:critical → 0.30`, `:high → 0.50`, `:medium → 0.65`. The governor cannot raise `@governor_factor` above the floor regardless of what the CPU utilisation signal says.

### JMM and the lock-free factor read

`@governor_factor` is a Ruby `Float`. On JRuby, Ruby `Float` maps to a JVM `double`. The JVM memory model (JLS §17.7) guarantees that reads and writes of `double` on conforming JVMs are atomic when the field is accessed on a 64-bit platform. The wave thread inner loops read `@governor_factor` without acquiring the governor mutex:

```ruby
gf     = @governor_factor   # atomic 64-bit read — JMM guarantee
target = gf * (0.5 + 0.5 * Math.sin(theta))
```

The governor writes it inside the mutex (for the read-modify-write sequence). The readers do not need the mutex for the read itself. This is not a data race. It is a deliberate application of the JMM atomicity guarantee to eliminate lock contention on the hot path of every wave thread.

### GC nudge

```ruby
if heap_ratio > 0.85 && gc_nudge_tick % 10 == 0
  GC.start
  SYSTEM&.gc
end
gc_nudge_tick += 1
```

When the heap exceeds 85%, the governor issues an explicit GC request once every 10 ticks (5 seconds). `GC.start` hints the Ruby GC. `System.gc()` hints the JVM GC. Neither call guarantees a collection cycle — the JVM may decline — but in practice both calls reliably trigger a partial or full collection when heap pressure is high. The 5-second throttle prevents stacking GC pauses.

---

## Immediate Pressure Signal

The Wave governor also receives out-of-band signals from the memory manager:

```ruby
# In Mm::MemoryManager:
def signal_gc_pressure(level)
  Mm::Pressure.set_level(level)
  Core::Wave.signal_gc_pressure(level) if Core::Wave.running?
end

# In Core::Wave:
def signal_gc_pressure(level)
  delta = case level
          when :critical then -0.20
          when :high     then -0.10
          when :medium   then -0.05
          when :low      then  +GOV_STEP
          end
  @mutex.synchronize do
    @governor_factor = [[@governor_factor + delta, GOV_MIN].max, 1.0].min
  end
end
```

This nudges the governor immediately rather than waiting for the next 500ms sample. The allocator calls this when it detects that a slab pool miss occurred under high pressure, or that a large allocation succeeded with very little headroom remaining.

---

## Stats

```ruby
Core::Wave.stats
# => {
#   running:         true,
#   threads:         5,
#   threads_alive:   5,
#   governor_factor: 0.87,   # current amplitude (1.0 = uncapped)
#   cpu_cap:         0.80,
#   gc_headroom:     false    # true when JVM heap > 70%
# }
```

---

## Constants

| Constant | Value | Purpose |
|---|---|---|
| `CPU_CAP` | 0.80 | Target CPU ceiling per core |
| `GOV_STEP` | 0.05 | Amplitude adjustment per feedback tick |
| `GOV_MIN` | 0.10 | Minimum governor factor (never below 10%) |
| `GOV_HYSTERESIS` | 0.10 | Dead-band before ramping back up |
| `GOV_INTERVAL` | 0.5s | Feedback sample period |

---

## Entry Points

```ruby
# Start as daemon threads for kernel lifetime (called by Init.boot)
Core::Wave.start(n_banks: 2, phases_per_bank: 2, period_ms: 2000, slice_ms: 10)

# Timed run with banner output (benchmarks)
Core::Wave.run(n_banks: 2, phases_per_bank: 2, period_ms: 2000, seconds: 60)

# Graceful shutdown
Core::Wave.stop

Core::Wave.running?   # => true/false
Core::Wave.stats      # => Hash
```
