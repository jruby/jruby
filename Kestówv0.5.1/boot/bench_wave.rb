# frozen_string_literal: true

# bench_wave.rb — Dual 2-Phase Transformer Bank CPU Shaper
# Kestówv 0.5.1 / JRuby 10.1 (Ruby 4.0.0)
#
# VOLTAGE ANALOGY
# ===============
#   0% CPU  = -240V   (negative peak)
#  50% CPU  =    0V   (neutral — the AC ground reference)
# 100% CPU  = +240V   (positive peak)
#
# TRANSFORMER BANK MODE (n_banks=2, phases_per_bank=2)
# =====================================================
# Inter-bank offset = 360° / (n_banks × phases_per_bank) = 360°/4 = 90°
# Within-bank spacing = 360° / phases_per_bank = 360°/2 = 180°
#
#   Bank A: T0=  0°  T1=180°  → A0+A1 = 100% CPU at all times
#   Bank B: T2= 90°  T3=270°  → B0+B1 = 100% CPU at all times
#
# Each bank is a 180°-complementary pair:
#   sin(θ) + sin(θ+180°) = sin(θ) - sin(θ) = 0  → balanced, constant sum
#
# Combined: Bank A + Bank B = constant 200% (2 fully loaded cores).
# Individual cores still trace a full sine from 0% to 100%.
#
# TRUE SINE FROM CLOCK CYCLES
# ============================
# Every SLICE_MS milliseconds, each thread k:
#   1. Reads System.nanoTime()  — CPU TSC (RDTSC), nanosecond resolution
#   2. Computes phase position within the current period
#   3. Evaluates:  target = 0.5 + 0.5 × sin(2π·pos/T + φ_k)
#   4. Runs bit-vector ops for (target × SLICE_NS) nanoseconds   → CPU up
#   5. Sleeps for  ((1-target) × SLICE_NS) nanoseconds           → CPU down
#
# BIT POSITIONS 0..59 (FIXNUM SAFE)
# ==================================
# Positions 0..59 keep 1<<pos inside Java long (Fixnum) territory.
# All ops stay zero-allocation → JIT-compilable hot loop.
# Thread.current[:boot_bit_vector] writes still exercise BopTracker.
#
# ─────────────────────────────────────────────────────────────────────────
# TUNING KNOBS — TRANSFORMER BANK MODE
# ─────────────────────────────────────────────────────────────────────────
#
# Dual 2-phase transformer bank (n_banks=2, phases_per_bank=2):
#   Bank A: T0=0°  T1=180°  → A0+A1 = 100% CPU at all times
#   Bank B: T2=90° T3=270°  → B0+B1 = 100% CPU at all times
#   Inter-bank offset = 360°/(2×2) = 90°
#
# Combined load: Bank A + Bank B = constant 200% (2 fully loaded cores)
# Individual cores still trace a full sine [0%..100%].
#
# Switch to uniform mode by setting N_BANKS=1 and PHASES_PER_BANK=4.

N_BANKS        = 2        # Number of transformer banks
PHASES_PER_BANK = 2       # Phases per bank (complementary pairs sum to 100%)
PERIOD_MS      = 2000     # Wave period in milliseconds (0.5 Hz, ~30 cycles/60s)
SLICE_MS       = 10       # Sine control-loop resolution (must be > JVM jitter ~2ms)
WAVE_SECONDS   = 60       # Total waveform duration in seconds

# ─────────────────────────────────────────────────────────────────────────

$stdout.sync = true

unless defined?(JRUBY_VERSION)
  warn "[bench_wave] ERROR: requires JRuby (detected: #{RUBY_ENGINE} #{RUBY_VERSION})"
  exit 1
end

require_relative 'init'

Boot.config.quiet = true
Kestowv::Init.boot

# Stop the kernel scheduler daemon so the timed benchmark
# has clean thread ownership and accurate calibration.
Kestowv::Core::Wave.stop

Kestowv::Core::Wave.run(
  n_banks:         N_BANKS,
  phases_per_bank: PHASES_PER_BANK,
  period_ms:       PERIOD_MS,
  seconds:         WAVE_SECONDS,
  slice_ms:        SLICE_MS
)
