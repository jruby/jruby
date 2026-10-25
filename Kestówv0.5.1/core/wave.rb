# frozen_string_literal: true

# Kestówv 0.5.1 — core/wave.rb
#
# Polyphase AC Sine Wave CPU Scheduler.
# Distributes CPU load across cores using phase-offset sine waves so that
# cores never peak simultaneously — eliminating thermal throttling and
# preventing the JVM from bunching work onto the same core.
#
# VOLTAGE ANALOGY
#   0% CPU  = -240V   (negative peak)
#  50% CPU  =    0V   (neutral — AC ground reference)
# 100% CPU  = +240V   (positive peak)
#
# TRANSFORMER BANK MODE (default: n_banks=2, phases_per_bank=2)
#   Bank A: T0=0°  T1=180°  → A0+A1 = 100% CPU at all times
#   Bank B: T2=90° T3=270°  → B0+B1 = 100% CPU at all times
#   Property: each bank's phases sum to a constant — no thermal spikes,
#   no idle waste, no core fighting each other for scheduler time.
#
# Entry points:
#   Wave.start(n_banks:, phases_per_bank:, period_ms:, slice_ms:)
#     — starts the scheduler as daemon threads for the kernel lifetime
#   Wave.run(n_banks:, phases_per_bank:, period_ms:, seconds:, slice_ms:)
#     — timed run with full banner (benchmark / manual observation)
#   Wave.stop — signal scheduler threads to exit cleanly

module Kestowv
  module Core
    module Wave

      TWO_PI        = 2 * Math::PI
      BIT_POSITIONS = (0..59).to_a.freeze
      SYSTEM        = defined?(JRUBY_VERSION) ? Java::JavaLang::System : nil

      PHASE_LABELS = {
        2 => %w[0° 180°],
        3 => %w[0° 120° 240°],
        4 => %w[0° 90° 180° 270°]
      }.freeze

      # ── Governor constants ────────────────────────────────────────────────
      CPU_CAP        = 0.80   # target ceiling (80 % per core average)
      GOV_STEP       = 0.05   # amplitude adjustment per feedback tick
      GOV_MIN        = 0.10   # never drop below 10 % amplitude
      GOV_HYSTERESIS = 0.10   # dead-band before ramping back up
      GOV_INTERVAL   = 0.5    # feedback sample period (seconds)

      @daemon_threads  = []
      @running         = false
      @mutex           = Mutex.new
      @governor_factor = 1.0   # current amplitude scale (1.0 = uncapped)
      @cpu_cap         = CPU_CAP
      @gc_headroom     = false  # true when JVM heap pressure is high

      class << self

        def register_with_boot
          Boot.register(:core_wave)
          Boot.set_bit(:core_wave)
        end

        # Start the polyphase scheduler as background daemon threads.
        # Called once by Init.boot — runs for the kernel lifetime.
        # No warmup here: the JIT promotes the hot loop within the first
        # few hundred slices naturally.
        def start(n_banks: 2, phases_per_bank: 2, period_ms: 2000, slice_ms: 10)
          return false unless defined?(JRUBY_VERSION)
          @mutex.synchronize do
            return false if @running
            @running         = true
            @governor_factor = 1.0
            @gc_headroom     = false
          end

          phase_offsets, labels, n_total = resolve_bank_params(n_banks, phases_per_bank)
          period_ns     = period_ms * 1_000_000
          slice_ns      = slice_ms  * 1_000_000
          wave_start_ns = SYSTEM.nano_time + 200_000_000

          @mutex.synchronize do
            @daemon_threads = n_total.times.map do |k|
              phi_k = phase_offsets[k]
              Thread.new(phi_k) do |phase_rad|
                bc = BIT_POSITIONS.size
                i  = 0
                Thread.current[:boot_bit_vector] = 0
                Thread.current.name = "kestowv-wave-T#{k}"

                loop do
                  break unless @running
                  now = SYSTEM.nano_time

                  elapsed_in_period = (now - wave_start_ns) % period_ns
                  theta = TWO_PI * elapsed_in_period.to_f / period_ns + phase_rad

                  # Governor scales amplitude — read without full lock.
                  # On JVM 64-bit, double reads are atomic (JMM guarantee).
                  gf     = @governor_factor
                  target = gf * (0.5 + 0.5 * Math.sin(theta))

                  slice_end = now + slice_ns
                  work_end  = now + (slice_ns * target).to_i

                  tc = Thread.current
                  while SYSTEM.nano_time < work_end
                    tc[:boot_bit_vector] = tc[:boot_bit_vector] ^ (1 << BIT_POSITIONS[i])
                    i = (i + 1) % bc
                  end

                  remaining_ns = slice_end - SYSTEM.nano_time
                  sleep(remaining_ns / 1e9) if remaining_ns > 1_500_000
                end
              end
            end

            # ── Governor thread ──────────────────────────────────────────────
            # Samples /proc/stat (CPU %) and JVM heap every GOV_INTERVAL
            # seconds.  Adjusts @governor_factor to keep cores at or below
            # @cpu_cap.  Also signals Mm::Pressure when JVM heap is high so
            # GC gets CPU headroom.
            @daemon_threads << Thread.new do
              Thread.current.name = "kestowv-wave-governor"
              cpu_before    = gov_cpu_stat
              gc_nudge_tick = 0

              loop do
                sleep GOV_INTERVAL
                break unless @running

                # ── CPU utilisation ───────────────────────────────────────
                cpu_after  = gov_cpu_stat
                pcts       = gov_cpu_pct(cpu_before, cpu_after)
                cpu_before = cpu_after
                avg_cpu    = pcts.empty? ? 0.0 :
                               pcts.sum / pcts.size.to_f / 100.0

                # ── JVM heap ratio ────────────────────────────────────────
                rt         = Java::JavaLang::Runtime.get_runtime
                heap_used  = rt.total_memory - rt.free_memory
                heap_ratio = rt.max_memory > 0 ?
                               heap_used.to_f / rt.max_memory : 0.0

                # ── Tell Mm::Pressure about JVM heap state ────────────────
                # This is the MM→CPU load-balance signal the allocator uses
                # to let the GC breathe.
                heap_level = if heap_ratio > 0.85 then :critical
                             elsif heap_ratio > 0.70 then :high
                             elsif heap_ratio > 0.50 then :medium
                             else :low
                             end
                begin
                  Mm::Pressure.set_level(heap_level)
                rescue; end

                # Pressure floor: if MM says heap is stressed, floor the
                # governor so GC always has headroom.
                pressure_floor = case heap_level
                                 when :critical then 0.30
                                 when :high     then 0.50
                                 when :medium   then 0.65
                                 end

                # Effective cap is lowered when heap pressure is high —
                # gives GC extra CPU by backing wave threads off sooner.
                gc_cap = heap_ratio > 0.70 ?
                           [(@cpu_cap - 0.20), 0.40].max : @cpu_cap

                @mutex.synchronize do
                  if avg_cpu > gc_cap || heap_ratio > 0.80
                    @governor_factor =
                      [@governor_factor - GOV_STEP, GOV_MIN].max
                  elsif avg_cpu < gc_cap - GOV_HYSTERESIS && heap_ratio < 0.60
                    @governor_factor =
                      [@governor_factor + GOV_STEP, 1.0].min
                  end
                  @governor_factor =
                    [pressure_floor, @governor_factor].min if pressure_floor
                  @gc_headroom = heap_ratio > 0.70
                end

                # Nudge JVM GC when heap is critically high — once every 5s
                # (10 × 0.5s ticks) to avoid stacking GC pauses.
                gc_nudge_tick += 1
                if heap_ratio > 0.85 && gc_nudge_tick % 10 == 0
                  GC.start
                  SYSTEM&.gc
                end
              end
            end
          end

          true
        end

        def stop
          @mutex.synchronize { @running = false }
          threads = @mutex.synchronize { @daemon_threads.dup }
          threads.each { |t| t.join(5) }
          @mutex.synchronize { @daemon_threads.clear }
        end

        def running?
          @mutex.synchronize { @running }
        end

        def stats
          threads = @mutex.synchronize { @daemon_threads.dup }
          gf, cap, gc = @mutex.synchronize do
            [@governor_factor, @cpu_cap, @gc_headroom]
          end
          {
            feature:         :core_wave,
            running:         running?,
            threads:         threads.size,
            threads_alive:   threads.count(&:alive?),
            governor_factor: gf.round(2),
            cpu_cap:         cap,
            gc_headroom:     gc
          }
        end

        # Immediate pressure signal from Mm — nudges the governor without
        # waiting for the next GOV_INTERVAL sample.
        def signal_gc_pressure(level)
          delta = case level
                  when :critical then -0.20
                  when :high     then -0.10
                  when :medium   then -0.05
                  when :low      then  +GOV_STEP
                  else 0
                  end
          @mutex.synchronize do
            @governor_factor =
              [[@governor_factor + delta, GOV_MIN].max, 1.0].min
          end
        end

        # Timed run — warmup, calibrate, print banner, run for `seconds`.
        # Used by bench_wave.rb and manual invocation.
        def run(n_phases: nil, n_banks: nil, phases_per_bank: nil,
                period_ms: 2000, seconds: 60, slice_ms: 10)
          unless defined?(JRUBY_VERSION)
            warn "[Wave] ERROR: requires JRuby (detected: #{RUBY_ENGINE} #{RUBY_VERSION})"
            return false
          end

          bank_mode = !n_banks.nil? || !phases_per_bank.nil?
          if bank_mode
            n_banks         ||= 1
            phases_per_bank ||= 2
            phase_offsets, labels, n_total = resolve_bank_params(n_banks, phases_per_bank)
          else
            n_phases        ||= 4
            n_total          = n_phases
            n_banks          = 1
            phases_per_bank  = n_phases
            phase_offsets    = n_phases.times.map { |k| TWO_PI * k / n_phases }
            labels           = PHASE_LABELS.fetch(n_phases) do
              n_phases.times.map { |k| "#{(360.0 * k / n_phases).round}°" }
            end
          end

          period_ns = period_ms * 1_000_000
          slice_ns  = slice_ms  * 1_000_000

          warmup(n_total)
          cal_ops, ns_per_op = calibrate(slice_ns)

          SYSTEM.gc; GC.start; sleep(0.3)

          wave_start_ns = SYSTEM.nano_time + 500_000_000
          wave_end_ns   = wave_start_ns + (seconds * 1_000_000_000)

          print_banner(n_total, n_banks, phases_per_bank, bank_mode,
                       labels, period_ms, slice_ms, seconds, cal_ops, ns_per_op)

          puts "[Wave] Running — #{seconds}s, #{n_total}-thread " \
               "#{bank_mode ? "#{n_banks}×#{phases_per_bank} bank" : "#{n_total}-phase"}, " \
               "#{period_ms}ms period"
          puts "[Wave] #{labels.each_with_index.map { |p, i| "T#{i}=#{p}" }.join("  ")}"
          if bank_mode && n_banks > 1
            n_banks.times { |b| puts "[Wave] Bank#{b}: #{phases_per_bank} phases sum to constant 100% CPU" }
          end
          puts "[Wave] Each core traces one phase of the sine — watch CPU monitor"
          puts ""

          threads = n_total.times.map do |k|
            phi_k = phase_offsets[k]
            Thread.new(phi_k) do |phase_rad|
              bc = BIT_POSITIONS.size
              i  = 0
              Thread.current[:boot_bit_vector] = 0

              while SYSTEM.nano_time < wave_end_ns
                now = SYSTEM.nano_time
                break if now >= wave_end_ns

                elapsed_in_period = (now - wave_start_ns) % period_ns
                theta  = TWO_PI * elapsed_in_period.to_f / period_ns + phase_rad
                target = 0.5 + 0.5 * Math.sin(theta)

                slice_end = [now + slice_ns, wave_end_ns].min
                work_end  = now + (slice_ns * target).to_i

                tc = Thread.current
                while SYSTEM.nano_time < work_end
                  tc[:boot_bit_vector] = tc[:boot_bit_vector] ^ (1 << BIT_POSITIONS[i])
                  i = (i + 1) % bc
                end

                remaining_ns = slice_end - SYSTEM.nano_time
                sleep(remaining_ns / 1e9) if remaining_ns > 1_500_000
              end
            end
          end

          threads.each(&:join)

          puts ""
          puts "[Wave] Complete — " \
               "#{bank_mode ? "#{n_banks}×#{phases_per_bank} transformer bank" : "#{n_total}-phase"}" \
               " wave finished."
          true
        end

        private

        def gov_cpu_stat
          File.readlines("/proc/stat").select { |l| l =~ /^cpu\d/ }.map do |l|
            f = l.split.drop(1).map(&:to_i)
            [f[3] + f[4], f.sum]
          end
        rescue
          []
        end

        def gov_cpu_pct(before, after)
          before.zip(after).map do |(i0, t0), (i1, t1)|
            dt = t1 - t0
            dt == 0 ? 0.0 : (100.0 * (1.0 - (i1 - i0).to_f / dt)).round(1)
          end
        rescue
          []
        end

        def resolve_bank_params(n_banks, phases_per_bank)
          n_total    = n_banks * phases_per_bank
          phase_step = TWO_PI / phases_per_bank
          bank_step  = TWO_PI / n_total
          offsets = n_banks.times.flat_map do |b|
            phases_per_bank.times.map { |p| b * bank_step + p * phase_step }
          end
          labels = n_banks.times.flat_map do |b|
            phases_per_bank.times.map do |p|
              deg = ((b * bank_step + p * phase_step) * 180.0 / Math::PI).round
              "B#{b}T#{p}=#{deg}°"
            end
          end
          [offsets, labels, n_total]
        end

        def warmup(n_total)
          puts "[Wave] JIT warmup (#{n_total} threads × 500k iters)..."
          bc = BIT_POSITIONS.size
          threads = n_total.times.map do
            Thread.new do
              Thread.current[:boot_bit_vector] = 0
              500_000.times do |i|
                tc = Thread.current
                tc[:boot_bit_vector] = tc[:boot_bit_vector] ^ (1 << BIT_POSITIONS[i % bc])
              end
            end
          end
          threads.each(&:join)
          sleep(1.0)
          SYSTEM.gc; GC.start
        end

        def calibrate(slice_ns)
          puts "[Wave] Calibrating ops/slice..."
          bc       = BIT_POSITIONS.size
          Thread.current[:boot_bit_vector] = 0
          ops      = 0
          deadline = SYSTEM.nano_time + slice_ns
          while SYSTEM.nano_time < deadline
            tc = Thread.current
            tc[:boot_bit_vector] = tc[:boot_bit_vector] ^ (1 << BIT_POSITIONS[ops % bc])
            ops += 1
          end
          ns_per_op = (slice_ns.to_f / ops).round(1)
          [ops, ns_per_op]
        end

        def print_banner(n_total, n_banks, phases_per_bank, bank_mode,
                         labels, period_ms, slice_ms, seconds, cal_ops, ns_per_op)
          config_str = bank_mode ?
            "#{n_banks} banks × #{phases_per_bank} phases = #{n_total} threads" :
            "#{n_total}-phase uniform spacing"
          puts ""
          puts "┌─────────────────────────────────────────────────────────────┐"
          puts "│  core/wave.rb — Polyphase AC Sine Wave CPU Scheduler        │"
          puts "│  Kestówv 0.5.1 / #{JRUBY_VERSION.ljust(40)}│"
          puts "├─────────────────────────────────────────────────────────────┤"
          puts "│  CONFIG        = #{config_str.ljust(42)}│"
          puts "│  PHASES        = #{labels.join(" / ").ljust(42)}│"
          puts "│  PERIOD_MS     = #{period_ms.to_s.ljust(42)}│"
          puts "│  SLICE_MS      = #{slice_ms.to_s.ljust(42)}│"
          puts "│  WAVE_SECONDS  = #{seconds.to_s.ljust(42)}│"
          puts "│  Ops/slice     = #{("#{cal_ops}  (#{ns_per_op} ns/op)").ljust(42)}│"
          puts "│  Timing        = #{"System.nanoTime() — CPU TSC (RDTSC)".ljust(42)}│"
          puts "├─────────────────────────────────────────────────────────────┤"
          puts "│  Sine formula  = cpu(t) = 50% + 50%×sin(2π·t/T + φ_k)     │"
          puts "│    0% CPU = -240V    50% CPU = 0V    100% CPU = +240V       │"
          if bank_mode
          puts "│  Bank property = each bank's phases sum to constant 100%    │"
          end
          puts "│  Slice control = work(target%) then sleep(1-target%)        │"
          puts "└─────────────────────────────────────────────────────────────┘"
          puts ""
        end

      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_wave,
  __FILE__,
  feature:    :core_wave,
  depends_on: [:core_klog]
)
