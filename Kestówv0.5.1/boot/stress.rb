# frozen_string_literal: true

# Kestówv 0.5.1 — boot/stress.rb
#
# Full-kernel + UDS combined stress test.
# Boots the kernel (wave scheduler starts as core daemon),
# then fires concurrent stress threads against every subsystem
# AND all UDS IPC paths simultaneously.
#
# Run: jruby boot/stress.rb

require_relative 'init'

STRESS_DURATION = 30   # seconds
TICK_INTERVAL   =  5   # progress report interval

Boot.config.quiet = true
Kestowv::Init.boot

puts ""
puts "=" * 66
puts "  Kestówv 0.5.1 — Full Kernel + UDS Combined Stress Test"
puts "  Ruby: #{RUBY_ENGINE} #{RUBY_VERSION}  |  Duration: #{STRESS_DURATION}s"
puts "=" * 66
puts "  Wave scheduler: #{Kestowv::Core::Wave.stats[:threads]} daemon threads running"
puts ""

include Kestowv
KProc   = Kestowv::Proc
Hub     = Net::UnixHub
USocket = Net::UnixSocket

# ── System monitor ────────────────────────────────────────────────────────────
module SysMonitor
  N_CORES = `nproc`.strip.to_i

  CACHE = begin
    sizes  = Dir["/sys/devices/system/cpu/cpu0/cache/index*/size"].sort
    levels = Dir["/sys/devices/system/cpu/cpu0/cache/index*/level"].sort
    levels.zip(sizes).map { |lf, sf|
      ["L#{File.read(lf).strip}", File.read(sf).strip]
    }.uniq.to_h
  rescue {}
  end

  def self.cpu_stat
    lines = File.readlines("/proc/stat").select { |l| l =~ /^cpu\d/ }
    lines.map do |l|
      fields = l.split.drop(1).map(&:to_i)
      idle   = fields[3] + fields[4]   # idle + iowait
      total  = fields.sum
      [idle, total]
    end
  end

  def self.cpu_pct(before, after)
    before.zip(after).map do |(i0, t0), (i1, t1)|
      dt = t1 - t0
      dt == 0 ? 0.0 : (100.0 * (1.0 - (i1 - i0).to_f / dt)).round(1)
    end
  end

  def self.mem
    info = {}
    File.foreach("/proc/meminfo") do |l|
      k, v = l.split(":")
      info[k.strip] = v.strip.to_i if v
    end
    used_mb  = ((info["MemTotal"] - info["MemAvailable"]) / 1024.0).round(0).to_i
    free_mb  = (info["MemAvailable"] / 1024.0).round(0).to_i
    cache_mb = ((info["Buffers"].to_i + info["Cached"].to_i) / 1024.0).round(0).to_i
    total_mb = (info["MemTotal"] / 1024.0).round(0).to_i
    { used_mb: used_mb, free_mb: free_mb, cache_mb: cache_mb, total_mb: total_mb }
  end

  def self.jvm_heap
    rt        = Java::JavaLang::Runtime.get_runtime
    used_mb   = ((rt.total_memory - rt.free_memory) / 1024.0 / 1024.0).round(0).to_i
    total_mb  = (rt.total_memory  / 1024.0 / 1024.0).round(0).to_i
    max_mb    = (rt.max_memory    / 1024.0 / 1024.0).round(0).to_i
    { used_mb: used_mb, total_mb: total_mb, max_mb: max_mb }
  end

  def self.temps
    Dir["/sys/class/thermal/thermal_zone*/temp"].map { |f|
      (File.read(f).strip.to_i / 1000.0).round(1) rescue nil
    }.compact
  end

  def self.freqs_mhz
    Dir["/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_avg_freq"].sort.map { |f|
      (File.read(f).strip.to_i / 1000.0).round(0).to_i rescue nil
    }.compact
  end
end

stop_flag = false
results   = {}
mutex     = Mutex.new

def stress_thread(name, results, mutex, stop_flag_ref)
  ops    = 0
  errors = 0
  t0     = Time.now

  begin
    yield(-> { stop_flag_ref.call }, ->(){ ops += 1 }, ->(_e){ errors += 1 })
  rescue => e
    errors += 1
    warn "  [stress/#{name}] fatal: #{e.message}"
  ensure
    elapsed = Time.now - t0
    mutex.synchronize do
      results[name] = {
        ops:     ops,
        errors:  errors,
        elapsed: elapsed.round(2),
        ops_sec: elapsed > 0 ? (ops / elapsed).round(0) : 0
      }
    end
  end
end

# ── each thread gets its own vpn/key space via tid ───────────────────────────
tid_base = -> (k) { k * 0x10_0000 }

threads = []

# ── 1. MM: Frame allocator ───────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("mm/frames", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      pg = Mm::FrameAllocator.allocate
      if pg
        Mm::FrameAllocator.free(pg)
        ok.call
      end
    end
  end
end

# ── 2. MM: Page table map/lookup/unmap ───────────────────────────────────────
threads << Thread.new do
  stress_thread("mm/pagetable", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    pt  = Mm::PageTable.new
    i   = 0
    until stopped.call
      pg = Mm::FrameAllocator.allocate
      next unless pg
      vpn = 0x2000 + i
      pt.map(vpn, pg,
        flags: Mm::PageTable::Flags::PRESENT | Mm::PageTable::Flags::WRITABLE)
      pt.lookup(vpn)
      pt.unmap(vpn)
      Mm::FrameAllocator.free(pg)
      i = (i + 1) % 0x1000
      ok.call
    end
  end
end

# ── 3. MM: VmRegion create/release ───────────────────────────────────────────
threads << Thread.new do
  stress_thread("mm/vmregion", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      r = Mm::VmRegion.slab_acquire(
        start_vpn: 0x8000 + i * 16,
        num_pages: 4,
        flags:     Mm::Protection::USER_RW,
        name:      :"stress_vma_#{i}",
        backing:   :anonymous
      )
      r.slab_release
      i = (i + 1) % 0x1000
      ok.call
    end
  end
end

# ── 4. MM: Heap alloc/free ───────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("mm/heap", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      Mm::Heap.allocate(4096)
      Mm::Heap.free(4096)
      ok.call
    end
  end
end

# ── 5. Proc: PID alloc/release ───────────────────────────────────────────────
threads << Thread.new do
  stress_thread("proc/pid", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      pid = KProc::Pid.allocate(task: nil)
      if pid
        KProc::Pid.release(pid)
        ok.call
      end
    end
  end
end

# ── 6. Proc: Task create/transition/release ───────────────────────────────────
threads << Thread.new do
  stress_thread("proc/task", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      t = KProc::Task.slab_acquire(name: :stress_task)
      t.transition(:running)
      t.transition(:zombie)
      t.slab_release
      ok.call
    end
  end
end

# ── 7. Proc: Credentials ─────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("proc/creds", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    uid = 1000
    until stopped.call
      KProc::Credentials.user(uid: uid, gid: uid)
      uid = (uid + 1) % 60_000 + 1000
      ok.call
    end
  end
end

# ── 8. Proc: Cgroup create/assign/destroy ────────────────────────────────────
threads << Thread.new do
  stress_thread("proc/cgroup", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      cg = KProc::Cgroup.create("stress_cg_#{i}", controllers: [:cpu, :pids])
      KProc::Cgroup.destroy(cg.id)
      i += 1
      ok.call
    end
  end
end

# ── 9. FS: TmpFs write/read/stat/delete ──────────────────────────────────────
threads << Thread.new do
  stress_thread("fs/tmpfs", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    fs = Fs::TmpFs.new(name: "stress_fs", max_bytes: 32 * 1024 * 1024)
    i  = 0
    until stopped.call
      path = "/stress_#{i % 256}.dat"
      fs.write(path, "kestowv stress #{i}")
      fs.read(path)
      fs.stat(path)
      fs.delete(path)
      i += 1
      ok.call
    end
  end
end

# ── 10. FS: VFS mount/resolve/unmount ────────────────────────────────────────
threads << Thread.new do
  stress_thread("fs/vfs", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      pt = "/stress_mnt_#{i}"
      be = Fs::TmpFs.new(name: "stress_vfs_#{i}", max_bytes: 4096)
      Fs::Vfs.mount(pt, be, ns_id: nil, flags: 0)
      Fs::Vfs.resolve(pt, ns_id: nil)
      Fs::Vfs.unmount(pt)
      i += 1
      ok.call
    end
  end
end

# ── 11. IPC: Pipe write/read ─────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("ipc/pipe", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      id = Ipc::Pipe.create
      Ipc::Pipe.write(id, "stress payload")
      Ipc::Pipe.read(id)
      Ipc::Pipe.close(id) rescue nil
      ok.call
    end
  end
end

# ── 12. IPC: Queue enqueue/dequeue ───────────────────────────────────────────
threads << Thread.new do
  stress_thread("ipc/queue", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    key = :"stress_q_#{Thread.current.object_id}"
    Ipc::Queue.create(key)
    until stopped.call
      Ipc::Queue.enqueue(key, "msg")
      Ipc::Queue.dequeue(key)
      ok.call
    end
  end
end

# ── 13. IPC: Semaphore wait/post ─────────────────────────────────────────────
threads << Thread.new do
  stress_thread("ipc/sem", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    key = :"stress_sem_#{Thread.current.object_id}"
    Ipc::Sem.create(key, 100)
    until stopped.call
      Ipc::Sem.post(key)
      Ipc::Sem.wait(key)
      ok.call
    end
  end
end

# ── 14. IPC: Shared memory ───────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("ipc/shm", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      key = :"stress_shm_#{i % 64}"
      Ipc::Shm.create(key, 4096)
      Ipc::Shm.attach(key)
      Ipc::Shm.detach(key) rescue nil
      Ipc::Shm.destroy(key) rescue nil
      i += 1
      ok.call
    end
  end
end

# ── 15. Core: Klog flood ─────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/klog", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      Core::Klog.debug("stress #{i}", subsystem: :stress)
      i += 1
      ok.call
    end
  end
end

# ── 16. Boot: bit vector ops ─────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("boot/bitvec", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    i = 0
    until stopped.call
      sym = :"stress_bit_#{i % 64}"
      Boot.set_bit(sym)
      Boot.bit_set?(sym)
      Boot.clear_bit(sym)
      i += 1
      ok.call
    end
  end
end

# ── 17. Core: RunQueue enqueue/dequeue ───────────────────────────────────────
threads << Thread.new do
  stress_thread("core/runqueue", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    rq   = Core::RunQueue.new(99)
    pool = Array.new(8) { KProc::Task.new(name: :rq_stress) }
    i    = 0
    until stopped.call
      t = pool[i % pool.size]
      rq.enqueue(t)
      rq.dequeue
      i += 1
      ok.call
    end
    pool.each(&:release)
  end
end

# ── 18. BinaryClassifier ─────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/binclass", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      Core::BinaryClassifier.classify(__FILE__)
      ok.call
    end
  end
end

# ── 19. Wave: scheduler stats poll ───────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/wave", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      s = Core::Wave.stats
      raise "wave died" unless s[:running]
      ok.call
      sleep 0.01
    end
  end
end

# ── UDS stress threads ────────────────────────────────────────────────────────

# ── 20. UDS: Socket create → connect → close (full lifecycle) ────────────────
threads << Thread.new do
  stress_thread("uds/lifecycle", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    tid = Thread.current.object_id
    i   = 0
    until stopped.call
      path = "/tmp/kestowv_stress_#{tid}_#{i}"
      sock = USocket.new(path)
      sock.connect
      sock.close
      USocket.close(path)
      i += 1
      ok.call
    end
  end
end

# ── 21. UDS: Socketpair create → send_io → close ─────────────────────────────
threads << Thread.new do
  stress_thread("uds/socketpair", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      s1, s2 = USocket.pair
      s1.connect; s2.connect
      s1.send_io(nil, { msg: "ping", ts: Time.now.to_f })
      s2.recv_io
      s1.close; s2.close
      USocket.close(s1.path); USocket.close(s2.path)
      ok.call
    end
  end
end

# ── 22. UDS: Hub register/unregister (routing table churn) ───────────────────
threads << Thread.new do
  stress_thread("uds/hub-churn", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    tid = Thread.current.object_id
    i   = 0
    until stopped.call
      path = "/tmp/kestowv_hub_#{tid}_#{i}"
      sock = Net::UnixSocket::Socket.new(path, :stream)
      Hub.register(sock)
      Hub.unregister(path)
      i += 1
      ok.call
    end
  end
end

# ── 23. UDS: Hub lookup — get() read-heavy ────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-lookup", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    paths = 16.times.map do |i|
      path = "/tmp/kestowv_lookup_#{i}"
      sock = Net::UnixSocket::Socket.new(path, :stream)
      Hub.register(sock)
      path
    end
    until stopped.call
      paths.each { |p| Hub.get(p) }
      ok.call
    end
    paths.each { |p| Hub.unregister(p) }
  end
end

# ── 24. UDS: Hub list/stats — introspection under load ───────────────────────
threads << Thread.new do
  stress_thread("uds/hub-stats", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    until stopped.call
      Hub.list
      Hub.stats
      Hub.active_sockets
      ok.call
    end
  end
end

# ── 25. UDS: Hub cleanup — sweep closed sockets ──────────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-cleanup", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    tid = Thread.current.object_id
    i   = 0
    until stopped.call
      path = "/tmp/kestowv_cleanup_#{tid}_#{i}"
      sock = Net::UnixSocket::Socket.new(path, :stream)
      Hub.register(sock)
      sock.close
      Hub.cleanup
      i += 1
      ok.call
    end
  end
end

# ── 26. UDS: send_io with MessagePack payloads ───────────────────────────────
threads << Thread.new do
  stress_thread("uds/send-io", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    tid  = Thread.current.object_id
    path = "/tmp/kestowv_sendio_#{tid}"
    sock = USocket.new(path)
    sock.connect
    i = 0
    until stopped.call
      sock.send_io(nil, {
        seq:     i,
        payload: "kestowv uds stress payload #{i}",
        ts:      Time.now.to_f,
        flags:   0x0A
      })
      sock.recv_io
      i += 1
      ok.call
    end
    sock.close
    USocket.close(path)
  end
end

# ── 27. UDS: Raw MessagePack serialize/deserialize throughput ─────────────────
threads << Thread.new do
  stress_thread("uds/msgpack", results, mutex, -> { stop_flag }) do |stopped, ok, err|
    payload = { cmd: "write", path: "/tmp/sock", size: 4096, flags: [:nonblock] }
    until stopped.call
      packed = MessagePack.pack(payload)
      MessagePack.unpack(packed)
      ok.call
    end
  end
end

# ── 28-31. UDS: Concurrent writers to shared hub path ────────────────────────
shared_path = "/tmp/kestowv_shared_writer"
shared_sock = Net::UnixSocket::Socket.new(shared_path, :stream)
Hub.register(shared_sock)

4.times do |w|
  threads << Thread.new(w) do |writer_id|
    stress_thread("uds/writer-#{writer_id}", results, mutex, -> { stop_flag }) do |stopped, ok, err|
      i = 0
      until stopped.call
        s = Hub.get(shared_path)
        s&.send_io(nil, { writer: writer_id, seq: i })
        i += 1
        ok.call
      end
    end
  end
end

# ── Progress ticker + system monitor ─────────────────────────────────────────
start_time   = Time.now
sys_samples  = []    # array of { t:, cpu:[], mem:{}, jvm:{}, temps:[], freqs:[] }
cpu_before   = SysMonitor.cpu_stat

ticker = Thread.new do
  until stop_flag
    sleep TICK_INTERVAL
    elapsed = (Time.now - start_time).round(1)

    # ── system sample ──
    cpu_after  = SysMonitor.cpu_stat
    cpu_pcts   = SysMonitor.cpu_pct(cpu_before, cpu_after)
    cpu_before = cpu_after
    mem        = SysMonitor.mem
    jvm        = SysMonitor.jvm_heap
    temps      = SysMonitor.temps
    freqs      = SysMonitor.freqs_mhz
    sys_samples << { t: elapsed, cpu: cpu_pcts, mem: mem, jvm: jvm,
                     temps: temps, freqs: freqs }

    # ── throughput snapshot ──
    snapshot = mutex.synchronize { results.dup }

    puts "┌─ [#{elapsed}s] ─────────────────────────────────────────────────────"
    puts "│  CPU:  #{cpu_pcts.each_with_index.map { |p, i| "core#{i}=#{p}%" }.join("  ")}"
    puts "│  Freq: #{freqs.each_with_index.map { |f, i| "cpu#{i}=#{f}MHz" }.join("  ")}" unless freqs.empty?
    puts "│  Temp: #{temps.map { |t| "#{t}°C" }.join("  ")}" unless temps.empty?
    puts "│  RAM:  used=#{mem[:used_mb]}MB  free=#{mem[:free_mb]}MB  " \
         "fs-cache=#{mem[:cache_mb]}MB  total=#{mem[:total_mb]}MB"
    puts "│  JVM:  heap #{jvm[:used_mb]}MB / #{jvm[:total_mb]}MB  " \
         "(max #{jvm[:max_mb]}MB)"
    puts "│  Wave: #{Kestowv::Core::Wave.stats.slice(:running, :threads_alive).map { |k,v| "#{k}=#{v}" }.join('  ')}"
    hub_st = Hub.stats
    puts "│  Hub:  active_sockets=#{hub_st[:active_sockets]}  backend=#{hub_st[:backend]}"
    puts "├── Subsystem throughput ───────────────────────────────────────────"
    snapshot.sort.each do |name, r|
      rate    = r[:elapsed] > 0 ? (r[:ops] / r[:elapsed]).round(0) : 0
      err_str = r[:errors] > 0 ? " !! ERRORS=#{r[:errors]}" : ""
      puts "│  %-18s  %9d ops  %8d/s%s" % [name, r[:ops], rate, err_str]
    end
    puts "└────────────────────────────────────────────────────────────────────"
    puts ""
  end
end

# ── Run for STRESS_DURATION seconds ──────────────────────────────────────────
puts "  Running #{threads.size} stress threads + wave daemon..."
puts "  Progress every #{TICK_INTERVAL}s\n\n"
sleep STRESS_DURATION
stop_flag = true
threads.each { |t| t.join(10) }
ticker.kill
Hub.unregister(shared_path)

# ── Final report ─────────────────────────────────────────────────────────────
snapshot   = mutex.synchronize { results.dup }
total_ops  = snapshot.values.sum { |r| r[:ops] }
total_errs = snapshot.values.sum { |r| r[:errors] }

puts ""
puts "=" * 66
puts "  COMBINED STRESS RESULTS — #{STRESS_DURATION}s  |  #{threads.size} threads"
puts "=" * 66
puts "  %-22s  %10s  %10s  %8s" % ["SUBSYSTEM", "OPS", "OPS/SEC", "ERRORS"]
puts "  " + "-" * 58
snapshot.sort.each do |name, r|
  puts "  %-22s  %10d  %10d  %8d" % [name, r[:ops], r[:ops_sec], r[:errors]]
end
puts "  " + "-" * 58
puts "  %-22s  %10d  %10d  %8d" % ["TOTAL", total_ops, (total_ops / STRESS_DURATION).round, total_errs]
puts "=" * 66
puts ""
ws = Core::Wave.stats
puts "  Wave:    running=#{ws[:running]}  threads=#{ws[:threads]}  alive=#{ws[:threads_alive]}"
puts "  Hub:     active_sockets=#{Hub.stats[:active_sockets]}"
ms = Config::Modules.stats
puts "  Modules: total=#{ms[:total]}  loaded=#{ms[:loaded]}  failed=#{ms[:failed]}"
puts ""
puts total_errs == 0 ? "  ALL CLEAN — 0 errors across #{total_ops} operations." \
                     : "  #{total_errs} ERRORS detected — see thread output above."

# ── System metrics summary ────────────────────────────────────────────────────
unless sys_samples.empty?
  puts ""
  puts "=" * 66
  puts "  SYSTEM METRICS OVER #{STRESS_DURATION}s"
  puts "=" * 66

  puts ""
  puts "  Cache topology (cpu0):"
  SysMonitor::CACHE.each { |level, size| puts "    #{level}: #{size}" }

  puts ""
  puts "  CPU utilization per core (sampled every #{TICK_INTERVAL}s):"
  header = ["  t(s)"] + SysMonitor::N_CORES.times.map { |i| "core#{i}%" } + ["avg%"]
  puts "  " + header.map { |h| h.ljust(10) }.join
  sys_samples.each do |s|
    avg = s[:cpu].empty? ? 0 : (s[:cpu].sum / s[:cpu].size).round(1)
    row = [s[:t].to_s] + s[:cpu].map(&:to_s) + [avg.to_s]
    puts "  " + row.map { |v| v.ljust(10) }.join
  end

  unless sys_samples.first[:freqs].empty?
    puts ""
    puts "  CPU frequency per core (MHz):"
    header = ["  t(s)"] + SysMonitor::N_CORES.times.map { |i| "cpu#{i}" }
    puts "  " + header.map { |h| h.ljust(10) }.join
    sys_samples.each do |s|
      row = [s[:t].to_s] + s[:freqs].map(&:to_s)
      puts "  " + row.map { |v| v.ljust(10) }.join
    end
  end

  unless sys_samples.first[:temps].empty?
    puts ""
    puts "  Thermal zones (°C):"
    header = ["  t(s)"] + sys_samples.first[:temps].each_index.map { |i| "zone#{i}" }
    puts "  " + header.map { |h| h.ljust(10) }.join
    sys_samples.each do |s|
      row = [s[:t].to_s] + s[:temps].map(&:to_s)
      puts "  " + row.map { |v| v.ljust(10) }.join
    end
  end

  puts ""
  puts "  Memory over time (MB):"
  puts "  #{"t(s)".ljust(8)} #{"used".ljust(10)} #{"free".ljust(10)} #{"fs-cache".ljust(12)} #{"jvm-heap".ljust(12)}"
  sys_samples.each do |s|
    puts "  #{s[:t].to_s.ljust(8)} #{s[:mem][:used_mb].to_s.ljust(10)} " \
         "#{s[:mem][:free_mb].to_s.ljust(10)} #{s[:mem][:cache_mb].to_s.ljust(12)} " \
         "#{s[:jvm][:used_mb].to_s.ljust(12)}"
  end

  puts ""
  cpu_all   = sys_samples.flat_map { |s| s[:cpu] }
  cpu_peak  = cpu_all.max
  cpu_avg   = cpu_all.empty? ? 0 : (cpu_all.sum / cpu_all.size).round(1)
  mem_peak  = sys_samples.map { |s| s[:mem][:used_mb] }.max
  jvm_peak  = sys_samples.map { |s| s[:jvm][:used_mb] }.max
  temp_peak = sys_samples.flat_map { |s| s[:temps] }.max

  puts "  Peak CPU:    #{cpu_peak}%  (avg #{cpu_avg}% across all cores and samples)"
  puts "  Peak RAM:    #{mem_peak}MB used"
  puts "  Peak JVM:    #{jvm_peak}MB heap"
  puts "  Peak temp:   #{temp_peak}°C" if temp_peak
end

puts ""
