# frozen_string_literal: true

# Kestówv 0.5.1 — bench_init.rb
#
# Full-kernel benchmark: loads every subsystem via init.rb,
# runs the boot sequence, then benchmarks each subsystem.
#
# Run:  ruby boot/bench_init.rb
#       jruby boot/bench_init.rb

require 'benchmark'

BASE = File.expand_path('..', __FILE__)

# ============================================================
# PHASE 0 — measure load time
# ============================================================

puts "=" * 60
puts "  Kestówv 0.5.1 — Kernel Benchmark"
puts "  Ruby: #{RUBY_ENGINE} #{RUBY_VERSION}"
puts "=" * 60
puts

load_time = Benchmark.realtime do
  require_relative 'init'
end

puts format("  [LOAD]  All subsystems loaded in %.4fs\n\n", load_time)

# Silence subsequent boot output
Boot.config.quiet = true

# ============================================================
# PHASE 1 — boot sequence
# ============================================================

puts "--- Phase 1: Boot Sequence ---"

boot_time = Benchmark.realtime do
  Kestowv::Init.boot
end

puts format("  [BOOT]  Full kernel boot in %.4fs", boot_time)
puts "  [BOOT]  Features active: #{Boot.enabled_features.size}"
puts "  [BOOT]  Modules registered: #{Kestowv::Config::Modules.stats[:total]}"
puts

# ============================================================
# PHASE 2 — subsystem benchmarks
# ============================================================

N = 10_000

puts "--- Phase 2: Subsystem Benchmarks (N=#{N}) ---\n\n"

Benchmark.bm(36) do |bm|

  # Boot bit vector
  bm.report("Boot bit vector set/test/clear") do
    N.times do |i|
      sym = :"bench_bit_#{i % 64}"
      Boot.set_bit(sym)
      Boot.bit_set?(sym)
      Boot.clear_bit(sym)
    end
  end

  # Frame allocator
  bm.report("MM frame alloc + free") do
    pages = []
    (N / 10).times { pages << Kestowv::Mm::FrameAllocator.allocate }
    pages.compact.each { |p| Kestowv::Mm::FrameAllocator.free(p) }
  end

  # Page table map/lookup/unmap
  bm.report("MM page table map/lookup/unmap") do
    pt = Kestowv::Mm::PageTable.new
    (N / 10).times do |i|
      pg = Kestowv::Mm::FrameAllocator.allocate
      next unless pg
      vpn = i + 0x1000
      pt.map(vpn, pg, flags: Kestowv::Mm::PageTable::Flags::PRESENT | Kestowv::Mm::PageTable::Flags::WRITABLE)
      pt.lookup(vpn)
      pt.unmap(vpn)
      Kestowv::Mm::FrameAllocator.free(pg)
    end
  end

  # VmRegion creation
  bm.report("MM VmRegion create + destroy") do
    (N / 10).times do |i|
      r = Kestowv::Mm::VmRegion.new(
        start_vpn: i * 16,
        num_pages: 4,
        flags:     Kestowv::Mm::Protection::USER_RW,
        name:      :"bench_region_#{i}",
        backing:   :anonymous
      )
      r.release
    end
  end

  # PID allocation + release
  bm.report("Proc PID alloc + release") do
    pids = []
    (N / 10).times { pids << Kestowv::Proc::Pid.allocate(task: nil) }
    pids.compact.each { |p| Kestowv::Proc::Pid.release(p) }
  end

  # Task create + transition + destroy
  bm.report("Proc Task create + transition") do
    (N / 100).times do
      t = Kestowv::Proc::Task.new(name: :bench_task)
      t.transition(:running)
      t.transition(:zombie)
      t.release
    end
  end

  # Credentials create
  bm.report("Proc Credentials.create") do
    N.times { Kestowv::Proc::Credentials.user(uid: 1000, gid: 1000) }
  end

  # Signal mask operations
  bm.report("Signal mask/bit ops") do
    N.times do |i|
      sig  = (i % 30) + 1
      next unless Kestowv::Signal.valid?(sig)
      Kestowv::Signal.bit(sig)
      Kestowv::Signal.name(sig)
      Kestowv::Signal.default_action(sig)
    end
  end

  # TmpFS write/read/stat
  bm.report("FS TmpFs write/read/stat") do
    fs = Kestowv::Fs::TmpFs.new(name: "bench_fs", max_bytes: 64 * 1024 * 1024)
    (N / 100).times do |i|
      path = "/bench_#{i}.txt"
      fs.write(path, "hello world #{i}")
      fs.read(path)
      fs.stat(path)
      fs.delete(path)
    end
  end

  # VFS mount/resolve/unmount
  bm.report("FS VFS mount/resolve/unmount") do
    (N / 100).times do |i|
      pt  = "/bench_mount_#{i}"
      be  = Kestowv::Fs::TmpFs.new(name: "vfs_bench_#{i}", max_bytes: 1024)
      Kestowv::Fs::Vfs.mount(pt, be, ns_id: nil, flags: 0)
      Kestowv::Fs::Vfs.resolve(pt, ns_id: nil)
      Kestowv::Fs::Vfs.unmount(pt)
    end
  end

  # Boot byte_dispatch classification
  bm.report("Boot byte_dispatch (existing file)") do
    path = __FILE__
    N.times { Boot.byte_dispatch(path) }
  end

  # Klog ring buffer
  bm.report("Core Klog log entries") do
    N.times { |i| Kestowv::Core::Klog.debug("bench msg #{i}", ctx: i) }
  end

  # Config Modules registration lookup
  bm.report("Config Modules registered? lookup") do
    N.times { Kestowv::Config::Modules.registered?(:hal_cpu) }
  end

  # RunQueue enqueue/dequeue
  bm.report("Core RunQueue enqueue/dequeue") do
    rq = Kestowv::Core::RunQueue.new(99)
    tasks = Array.new(10) { Kestowv::Proc::Task.new(name: :rq_bench) }
    (N / 10).times do
      tasks.each { |t| rq.enqueue(t) }
      tasks.size.times { rq.dequeue }
    end
    tasks.each(&:release)
  end

  # BinaryClassifier — classify a Ruby source file (flat binary path)
  bm.report("Core BinaryClassifier classify") do
    N.times { Kestowv::Core::BinaryClassifier.classify(__FILE__) }
  end

  # BinaryClassifier — classify with synthetic ELF64 header (no file I/O)
  elf64_header = (
    "\x7fELF"           +   # magic
    "\x02"              +   # EI_CLASS  = 64-bit
    "\x01"              +   # EI_DATA   = little-endian
    "\x01\x00"          +   # EI_VERSION, EI_OSABI
    "\x00" * 8          +   # EI_ABIVERSION + padding
    "\x02\x00"          +   # e_type = ET_EXEC
    "\x3e\x00"          +   # e_machine = x86_64
    "\x01\x00\x00\x00"  +   # e_version
    "\x00" * 8          +   # e_entry (64-bit)
    "\x00" * 8          +   # e_phoff
    "\x00" * 8          +   # e_shoff
    "\x00" * 4          +   # e_flags
    "\x40\x00"          +   # e_ehsize
    "\x38\x00"          +   # e_phentsize
    "\x03\x00"            # e_phnum = 3 segments
  ).b
  bm.report("Core BinaryClassifier classify ELF64") do
    N.times { Kestowv::Core::BinaryClassifier.classify("/dev/null", elf64_header) }
  end

  # Core::Wave scheduler stats
  bm.report("Core::Wave.stats") do
    N.times { Kestowv::Core::Wave.stats }
  end
end

# ============================================================
# PHASE 3 — stats summary
# ============================================================

puts "\n--- Phase 3: Subsystem Stats ---\n\n"

puts "  MM FrameAllocator:"
fa = Kestowv::Mm::FrameAllocator.stats
puts "    total=#{fa[:total]}  free=#{fa[:free]}  allocated=#{fa[:allocated]}  util=#{(fa[:utilization] * 100).round(1)}%"

puts "  Proc::Pid:"
ps = Kestowv::Proc::Pid.stats
puts "    allocated=#{ps[:allocated]}  free_list=#{ps[:free_list]}"

puts "  FS:"
fss = Kestowv::Fs.stats rescue {}
puts "    #{fss}"

puts "  Boot bit vector:"
bs = Boot.bit_stats
puts "    registered=#{bs[:registered_features]}  set=#{bs[:bits_set_in_current_thread]}"

puts "  Modules:"
ms = Kestowv::Config::Modules.stats
puts "    total=#{ms[:total]}  loaded=#{ms[:loaded]}  failed=#{ms[:failed]}"

puts "\n  Init stats:"
puts "    booted=#{Kestowv::Init.booted?}"
t = Kestowv::Init.init_task
puts "    pid1 tid=#{t&.tid || 'n/a'}  state=#{t&.state || 'n/a'}"

puts "\n  BinaryClassifier:"
puts "    registered=#{Boot.bit_set?(:core_binary_classifier)}  " \
     "feature=:binary_classifier"
puts "    kinds supported: #{Kestowv::Core::BinaryClassifier::MAGIC.size} magic + script + flat"

puts "\n  Core::Wave (CPU Scheduler):"
ws = Kestowv::Core::Wave.stats
puts "    registered=#{Boot.bit_set?(:core_wave)}  running=#{ws[:running]}"
puts "    threads=#{ws[:threads]}  alive=#{ws[:threads_alive]}"

puts "\n" + "=" * 60
puts "  Benchmark complete."
puts "=" * 60
