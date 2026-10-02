# frozen_string_literal: true

# bench_jruby.rb — JRuby-only scaling benchmark
#
# Starts at N=10_000_000 and scales up until heap cap or OOM.
# Uses Benchmark.realtime per benchmark so each result prints immediately.
# Finite-pool subsystems batch-cycle to avoid draining the pool.
#
# Run: jruby boot/bench_jruby.rb

abort "JRuby only — got #{RUBY_ENGINE} #{RUBY_VERSION}" unless RUBY_ENGINE == "jruby"

require 'benchmark'
require_relative 'init'

Boot.config.quiet = true
Kestowv::Init.boot

$stdout.sync = true

RT = Java::JavaLang::Runtime.runtime

def heap_mb
  used = (RT.totalMemory - RT.freeMemory) / 1_048_576
  max  = RT.maxMemory / 1_048_576
  "#{used}MB / #{max}MB"
end

def fmt_n(n)
  n.to_s.reverse.gsub(/(\d{3})(?=\d)/, '\1_').reverse
end

COL_LABEL = 42
COL_REAL  = 10

def bench(label, &blk)
  print "  #{label.ljust(COL_LABEL)}"
  oom = false
  t = begin
    Benchmark.realtime(&blk)
  rescue java.lang.OutOfMemoryError => e
    puts "  [OOM] #{e.message}"
    oom = true
    nil
  rescue => e
    puts "  [ERR] #{e.class}: #{e.message}"
    oom = true
    nil
  end
  if t
    puts format("%#{COL_REAL}.4fs", t)
  end
  oom
end

# Pre-register 64 bit-vector symbols so the hot loop uses raw ops:
# set_bit_raw / test_bit_raw / clear_bit_raw — pure Thread.current bit writes,
# no mutex acquisition, no mark_array / invalidate_array linear scan.
BIT_POSITIONS = Array.new(64) { |i| Boot.register(:"bvec#{i}") }.freeze

FRAME_BATCH = 256       # FrameAllocator pool = 1024
PID_BATCH   = 8_192     # PID pool = 32768

puts "=" * 68
puts "  Kestówv 0.5.1 — JRuby Scaling Benchmark"
puts "  JRuby #{JRUBY_VERSION}  /  Ruby #{RUBY_VERSION}"
puts "  Cores: #{java.lang.Runtime.runtime.availableProcessors}  |  Heap max: #{RT.maxMemory / 1_048_576}MB"
puts "=" * 68

[10_000_000, 100_000_000, 1_000_000_000].each do |n|
  puts "\n" + "=" * 68
  puts "  N = #{fmt_n(n)}"
  puts "=" * 68
  puts format("  %-#{COL_LABEL}s %#{COL_REAL}s", "benchmark", "real(s)")
  puts "  " + "-" * (COL_LABEL + COL_REAL + 1)

  Java::JavaLang::System.gc
  oom_hit = false

  # ── Boot bit vector (raw: no mutex, no array scan) ─────────────────────
  oom_hit |= bench("Boot  bit vector set/test/clear") do
    n.times do |i|
      pos = BIT_POSITIONS[i % 64]
      Boot.set_bit_raw(pos)
      Boot.test_bit_raw(pos)
      Boot.clear_bit_raw(pos)
    end
  end
  break if oom_hit

  # ── MM frame alloc (batch 256/cycle) ───────────────────────────────────
  oom_hit |= bench("MM    frame alloc+free (b256)") do
    (n / FRAME_BATCH).times do
      pages = Array.new(FRAME_BATCH) { Kestowv::Mm::FrameAllocator.allocate }
      pages.compact.each { |p| Kestowv::Mm::FrameAllocator.free(p) }
    end
  end
  break if oom_hit

  # ── MM page table (N/10 single-frame cycles) ───────────────────────────
  oom_hit |= bench("MM    page table map/lookup/unmap") do
    pt = Kestowv::Mm::PageTable.new
    (n / 10).times do |i|
      pg = Kestowv::Mm::FrameAllocator.allocate
      next unless pg
      vpn = (i % 0x8000) + 0x1000
      pt.map(vpn, pg,
        flags: Kestowv::Mm::PageTable::Flags::PRESENT |
               Kestowv::Mm::PageTable::Flags::WRITABLE)
      pt.lookup(vpn)
      pt.unmap(vpn)
      Kestowv::Mm::FrameAllocator.free(pg)
    end
  end
  break if oom_hit

  # ── MM VmRegion create+destroy (N/10) ──────────────────────────────────
  oom_hit |= bench("MM    VmRegion create+destroy") do
    (n / 10).times do |i|
      r = Kestowv::Mm::VmRegion.new(
        start_vpn: (i % 0x80000) * 16,
        num_pages: 4,
        flags:     Kestowv::Mm::Protection::USER_RW,
        name:      :"r#{i % 1000}",
        backing:   :anonymous
      )
      r.release
    end
  end
  break if oom_hit

  # ── Proc PID alloc (batch 8192/cycle) ──────────────────────────────────
  oom_hit |= bench("Proc  PID alloc+release (b8192)") do
    (n / PID_BATCH).times do
      pids = Array.new(PID_BATCH) { Kestowv::Proc::Pid.allocate(task: nil) }
      pids.compact.each { |p| Kestowv::Proc::Pid.release(p) }
    end
  end
  break if oom_hit

  # ── Proc Task create+transition (N/100) ────────────────────────────────
  oom_hit |= bench("Proc  Task create+transition") do
    (n / 100).times do
      t = Kestowv::Proc::Task.new(name: :bench_task)
      t.transition(:running)
      t.transition(:zombie)
      t.release
    end
  end
  break if oom_hit

  # ── Proc Credentials ───────────────────────────────────────────────────
  oom_hit |= bench("Proc  Credentials.create") do
    n.times { Kestowv::Proc::Credentials.user(uid: 1000, gid: 1000) }
  end
  break if oom_hit

  # ── Signal bit ops ─────────────────────────────────────────────────────
  oom_hit |= bench("Signal mask/bit ops") do
    n.times do |i|
      sig = (i % 30) + 1
      next unless Kestowv::Signal.valid?(sig)
      Kestowv::Signal.bit(sig)
      Kestowv::Signal.name(sig)
      Kestowv::Signal.default_action(sig)
    end
  end
  break if oom_hit

  # ── FS TmpFs write/read/stat (N/100, 1000-path pool) ───────────────────
  oom_hit |= bench("FS    TmpFs write/read/stat") do
    fs = Kestowv::Fs::TmpFs.new(name: "bfs_#{n}", max_bytes: 64 * 1_048_576)
    (n / 100).times do |i|
      path = "/b#{i % 1000}.txt"
      fs.write(path, "x#{i}")
      fs.read(path)
      fs.stat(path)
    end
  end
  break if oom_hit

  # ── FS VFS mount/resolve/unmount (N/100, 50-backend pool) ──────────────
  oom_hit |= bench("FS    VFS mount/resolve/unmount") do
    backends = Array.new(50) { |i|
      Kestowv::Fs::TmpFs.new(name: "vb#{i}", max_bytes: 1024)
    }
    (n / 100).times do |i|
      slot = i % 50
      pt   = "/bm#{slot}"
      Kestowv::Fs::Vfs.mount(pt, backends[slot], ns_id: nil, flags: 0)
      Kestowv::Fs::Vfs.resolve(pt, ns_id: nil)
      Kestowv::Fs::Vfs.unmount(pt)
    end
  end
  break if oom_hit

  # ── Boot byte_dispatch ─────────────────────────────────────────────────
  oom_hit |= bench("Boot  byte_dispatch (self)") do
    path = __FILE__
    n.times { Boot.byte_dispatch(path) }
  end
  break if oom_hit

  # ── Core Klog (bounded key pool) ───────────────────────────────────────
  oom_hit |= bench("Core  Klog log entries") do
    n.times { |i| Kestowv::Core::Klog.debug("b#{i % 10_000}", ctx: i % 10_000) }
  end
  break if oom_hit

  # ── Config Modules lookup ──────────────────────────────────────────────
  oom_hit |= bench("Config Modules registered? lookup") do
    n.times { Kestowv::Config::Modules.registered?(:hal_cpu) }
  end
  break if oom_hit

  # ── Core RunQueue (N/10 cycles, 10 tasks) ──────────────────────────────
  oom_hit |= bench("Core  RunQueue enqueue/dequeue") do
    rq    = Kestowv::Core::RunQueue.new(99)
    tasks = Array.new(10) { Kestowv::Proc::Task.new(name: :rq_bench) }
    (n / 10).times do
      tasks.each { |t| rq.enqueue(t) }
      tasks.size.times { rq.dequeue }
    end
    tasks.each(&:release)
  end
  break if oom_hit

  Java::JavaLang::System.gc
  puts "\n  Heap: #{heap_mb}"
end

puts "\n" + "=" * 68
puts "  Scaling complete."
puts "=" * 68
