#!/usr/bin/env ruby
# frozen_string_literal: true

# monitor_wave.rb — Bank-aware CPU monitor for dual 2-phase transformer bank wave
# Kestówv 0.5.1
#
# Run this in a second terminal WHILE bench_wave.rb is running:
#   jruby boot/monitor_wave.rb
#
# Shows per-core % CPU at 1-second intervals, plus pairwise bank sums.
# Bank A = core 0 + core 1  (should be ~constant 100%)
# Bank B = core 2 + core 3  (should be ~constant 100%)
#
# Reads /proc/stat — Linux only.

INTERVAL  = 0.5   # sample interval in seconds
N_CORES   = 4     # expected cores (T0..T3 = one per wave phase)

# Bank pairing matches the wave layout: [0°,180°,90°,270°]
#   Bank A: T0=0°, T1=180°  →  cores 0 and 1
#   Bank B: T2=90°, T3=270° →  cores 2 and 3
BANKS = [
  { name: "Bank A (0°+180°)",  cores: [0, 1] },
  { name: "Bank B (90°+270°)", cores: [2, 3] }
].freeze

def read_stat
  File.readlines("/proc/stat").each_with_object({}) do |line, h|
    m = line.match(/\Acpu(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/)
    next unless m
    n = m[1].to_i
    user, nice, sys, idle, iowait, irq, softirq =
      m[2].to_i, m[3].to_i, m[4].to_i, m[5].to_i, m[6].to_i, m[7].to_i, m[8].to_i
    total  = user + nice + sys + idle + iowait + irq + softirq
    active = total - idle - iowait
    h[n] = { active: active, total: total }
  end
end

def cpu_pct(prev, curr, core)
  p = prev[core]; c = curr[core]
  return nil unless p && c
  dt = c[:total] - p[:total]
  return 0.0 if dt == 0
  da = c[:active] - p[:active]
  (da.to_f / dt * 100).round(1)
end

prev = read_stat
sleep(INTERVAL)

puts ""
puts "┌─────────────────────────────────────────────────────────────────────┐"
puts "│  monitor_wave.rb — Transformer Bank CPU Monitor                     │"
puts "│  Bank A (T0+T1): 0°+180° → constant ~100%                          │"
puts "│  Bank B (T2+T3): 90°+270° → constant ~100%                         │"
puts "│  Total: BankA + BankB → constant ~200%                              │"
puts "└─────────────────────────────────────────────────────────────────────┘"
puts ""
printf "%-6s  %6s  %6s  %6s  %6s  │  %-20s  %-20s  %s\n",
       "Time", "T0(0°)", "T1(180°)", "T2(90°)", "T3(270°)",
       "BankA(T0+T1)", "BankB(T2+T3)", "Total"
puts "-" * 105

t0 = Time.now

loop do
  curr   = read_stat
  pcts   = N_CORES.times.map { |c| cpu_pct(prev, curr, c) }
  elapsed = (Time.now - t0).round(0).to_i
  ts     = format("%02d:%02d", elapsed / 60, elapsed % 60)

  core_str = pcts.map { |p| p ? format("%5.1f%%", p) : "  N/A " }

  bank_sums = BANKS.map do |bank|
    vals = bank[:cores].map { |c| pcts[c] }.compact
    vals.size == bank[:cores].size ? vals.sum.round(1) : nil
  end

  bank_strs = bank_sums.map { |s| s ? format("%5.1f%%", s) : "  N/A " }
  total     = bank_sums.all? ? bank_sums.sum.round(1) : nil
  total_str = total ? format("%5.1f%%", total) : "  N/A "

  printf "%-6s  %6s  %6s  %6s  %6s  │  %-20s  %-20s  %s\n",
         ts, *core_str, bank_strs[0], bank_strs[1], total_str

  prev = curr
  sleep(INTERVAL)
end
