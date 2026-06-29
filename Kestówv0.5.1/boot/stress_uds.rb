# frozen_string_literal: true

# Kestówv 0.5.1 — boot/stress_uds.rb
#
# Unix Domain Socket (UDS) IPC stress test.
# Boots the kernel (wave scheduler starts), then fires concurrent
# stress threads against every UDS code path simultaneously.
#
# Run: jruby boot/stress_uds.rb

require_relative 'init'

STRESS_DURATION = 30
TICK_INTERVAL   =  5

Boot.config.quiet = true
Kestowv::Init.boot

puts ""
puts "=" * 66
puts "  Kestówv 0.5.1 — UDS IPC Stress Test"
puts "  Ruby: #{RUBY_ENGINE} #{RUBY_VERSION}  |  Duration: #{STRESS_DURATION}s"
puts "=" * 66
puts "  Wave scheduler: #{Kestowv::Core::Wave.stats[:threads]} daemon threads running"
puts ""

include Kestowv
Hub    = Net::UnixHub
USocket = Net::UnixSocket

# ── System monitor (identical to stress.rb) ───────────────────────────────────
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
    File.readlines("/proc/stat").select { |l| l =~ /^cpu\d/ }.map do |l|
      f = l.split.drop(1).map(&:to_i)
      [f[3] + f[4], f.sum]
    end
  end

  def self.cpu_pct(before, after)
    before.zip(after).map do |(i0,t0),(i1,t1)|
      dt = t1 - t0; dt == 0 ? 0.0 : (100.0*(1.0-(i1-i0).to_f/dt)).round(1)
    end
  end

  def self.mem
    info = {}
    File.foreach("/proc/meminfo") { |l| k,v = l.split(":"); info[k.strip] = v.strip.to_i if v }
    { used_mb:  ((info["MemTotal"]  - info["MemAvailable"]) / 1024.0).round.to_i,
      free_mb:   (info["MemAvailable"] / 1024.0).round.to_i,
      cache_mb: ((info["Buffers"].to_i + info["Cached"].to_i) / 1024.0).round.to_i,
      total_mb:  (info["MemTotal"] / 1024.0).round.to_i }
  end

  def self.jvm_heap
    rt = Java::JavaLang::Runtime.get_runtime
    { used_mb:  ((rt.total_memory - rt.free_memory) / 1024.0 / 1024.0).round.to_i,
      total_mb:  (rt.total_memory  / 1024.0 / 1024.0).round.to_i,
      max_mb:    (rt.max_memory    / 1024.0 / 1024.0).round.to_i }
  end

  def self.temps
    Dir["/sys/class/thermal/thermal_zone*/temp"].map { |f|
      (File.read(f).strip.to_i / 1000.0).round(1) rescue nil }.compact
  end

  def self.freqs_mhz
    Dir["/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_avg_freq"].sort.map { |f|
      (File.read(f).strip.to_i / 1000.0).round.to_i rescue nil }.compact
  end
end

# ── Stress harness ─────────────────────────────────────────────────────────────
stop_flag = false
results   = {}
mutex     = Mutex.new

def stress_thread(name, results, mutex, stopped_ref)
  ops = 0; errors = 0; t0 = Time.now
  begin
    yield(->{stopped_ref.call}, ->{ops+=1}, ->(e){errors+=1})
  rescue => e
    errors += 1
    warn "  [stress/#{name}] fatal: #{e.message}\n  #{e.backtrace.first}"
  ensure
    el = Time.now - t0
    mutex.synchronize do
      results[name] = { ops: ops, errors: errors, elapsed: el.round(2),
                        ops_sec: el > 0 ? (ops/el).round : 0 }
    end
  end
end

threads = []

# ── 1. Socket create → connect → close (full lifecycle) ───────────────────────
threads << Thread.new do
  stress_thread("uds/lifecycle", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
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

# ── 2. Socketpair create → send_io → close ────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/socketpair", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
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

# ── 3. Hub register/unregister (routing table churn) ─────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-churn", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
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

# ── 4. Hub lookup — get() read-heavy ──────────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-lookup", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
    # seed a set of sockets to look up
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

# ── 5. Hub list/stats — introspection under load ──────────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-stats", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
    until stopped.call
      Hub.list
      Hub.stats
      Hub.active_sockets
      ok.call
    end
  end
end

# ── 6. Hub cleanup — sweep closed sockets ─────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/hub-cleanup", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
    tid = Thread.current.object_id
    i   = 0
    until stopped.call
      # register and immediately close so cleanup finds them
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

# ── 7. send_io with MessagePack payloads ──────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/send-io", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
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

# ── 8. Raw MessagePack serialize/deserialize throughput ───────────────────────
threads << Thread.new do
  stress_thread("uds/msgpack", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
    payload = { cmd: "write", path: "/tmp/sock", size: 4096, flags: [:nonblock] }
    until stopped.call
      packed   = MessagePack.pack(payload)
      MessagePack.unpack(packed)
      ok.call
    end
  end
end

# ── 9. Concurrent writers to same hub path ────────────────────────────────────
shared_path = "/tmp/kestowv_shared_writer"
shared_sock = Net::UnixSocket::Socket.new(shared_path, :stream)
Hub.register(shared_sock)

4.times do |w|
  threads << Thread.new(w) do |writer_id|
    stress_thread("uds/writer-#{writer_id}", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
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

# ── 10. Wave health check ─────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/wave", results, mutex, ->{ stop_flag }) do |stopped, ok, err|
    until stopped.call
      s = Core::Wave.stats
      raise "wave died" unless s[:running]
      ok.call
      sleep 0.01
    end
  end
end

# ── Progress ticker + system monitor ─────────────────────────────────────────
start_time  = Time.now
sys_samples = []
cpu_before  = SysMonitor.cpu_stat

ticker = Thread.new do
  until stop_flag
    sleep TICK_INTERVAL
    elapsed    = (Time.now - start_time).round(1)
    cpu_after  = SysMonitor.cpu_stat
    cpu_pcts   = SysMonitor.cpu_pct(cpu_before, cpu_after)
    cpu_before = cpu_after
    mem        = SysMonitor.mem
    jvm        = SysMonitor.jvm_heap
    temps      = SysMonitor.temps
    freqs      = SysMonitor.freqs_mhz
    sys_samples << { t: elapsed, cpu: cpu_pcts, mem: mem, jvm: jvm,
                     temps: temps, freqs: freqs }

    snapshot = mutex.synchronize { results.dup }
    hub_st   = Hub.stats

    puts "┌─ [#{elapsed}s] ─────────────────────────────────────────────────────"
    puts "│  CPU:  #{cpu_pcts.each_with_index.map { |p,i| "core#{i}=#{p}%" }.join("  ")}"
    puts "│  Freq: #{freqs.each_with_index.map { |f,i| "cpu#{i}=#{f}MHz" }.join("  ")}" unless freqs.empty?
    puts "│  Temp: #{temps.map { |t| "#{t}°C" }.join("  ")}" unless temps.empty?
    puts "│  RAM:  used=#{mem[:used_mb]}MB  free=#{mem[:free_mb]}MB  " \
         "fs-cache=#{mem[:cache_mb]}MB  total=#{mem[:total_mb]}MB"
    puts "│  JVM:  heap #{jvm[:used_mb]}MB / #{jvm[:total_mb]}MB  (max #{jvm[:max_mb]}MB)"
    puts "│  Hub:  active_sockets=#{hub_st[:active_sockets]}  backend=#{hub_st[:backend]}"
    puts "│  Wave: running=#{Core::Wave.stats[:running]}  alive=#{Core::Wave.stats[:threads_alive]}"
    puts "├── UDS throughput ─────────────────────────────────────────────────"
    snapshot.sort.each do |name, r|
      rate    = r[:elapsed] > 0 ? (r[:ops] / r[:elapsed]).round : 0
      err_str = r[:errors] > 0 ? " !! ERRORS=#{r[:errors]}" : ""
      puts "│  %-22s  %9d ops  %8d/s%s" % [name, r[:ops], rate, err_str]
    end
    puts "└────────────────────────────────────────────────────────────────────"
    puts ""
  end
end

puts "  Running #{threads.size} UDS stress threads + wave daemon..."
puts "  Progress every #{TICK_INTERVAL}s\n\n"
sleep STRESS_DURATION
stop_flag = true
threads.each { |t| t.join(10) }
ticker.kill

Hub.unregister(shared_path)

# ── Final results ─────────────────────────────────────────────────────────────
snapshot   = mutex.synchronize { results.dup }
total_ops  = snapshot.values.sum { |r| r[:ops] }
total_errs = snapshot.values.sum { |r| r[:errors] }

puts ""
puts "=" * 66
puts "  UDS STRESS RESULTS — #{STRESS_DURATION}s  |  #{threads.size} threads"
puts "=" * 66
puts "  %-22s  %10s  %10s  %8s" % ["SUBSYSTEM", "OPS", "OPS/SEC", "ERRORS"]
puts "  " + "-" * 58
snapshot.sort.each do |name, r|
  puts "  %-22s  %10d  %10d  %8d" % [name, r[:ops], r[:ops_sec], r[:errors]]
end
puts "  " + "-" * 58
puts "  %-22s  %10d  %10d  %8d" % ["TOTAL", total_ops, (total_ops/STRESS_DURATION).round, total_errs]
puts "=" * 66
puts ""
ws = Core::Wave.stats
puts "  Wave:    running=#{ws[:running]}  threads=#{ws[:threads]}  alive=#{ws[:threads_alive]}"
puts "  Hub:     active_sockets=#{Hub.stats[:active_sockets]}"
ms = Config::Modules.stats
puts "  Modules: total=#{ms[:total]}  loaded=#{ms[:loaded]}  failed=#{ms[:failed]}"

unless sys_samples.empty?
  puts ""
  puts "=" * 66
  puts "  SYSTEM METRICS OVER #{STRESS_DURATION}s"
  puts "=" * 66

  puts "\n  Cache topology (cpu0):"
  SysMonitor::CACHE.each { |l, s| puts "    #{l}: #{s}" }

  puts "\n  CPU utilization per core:"
  puts "  #{"t(s)".ljust(8)}" + SysMonitor::N_CORES.times.map { |i| "core#{i}%".ljust(10) }.join + "avg%"
  sys_samples.each do |s|
    avg = s[:cpu].empty? ? 0 : (s[:cpu].sum / s[:cpu].size).round(1)
    puts "  #{s[:t].to_s.ljust(8)}" + s[:cpu].map { |v| v.to_s.ljust(10) }.join + avg.to_s
  end

  unless sys_samples.first[:freqs].empty?
    puts "\n  CPU frequency (MHz):"
    puts "  #{"t(s)".ljust(8)}" + SysMonitor::N_CORES.times.map { |i| "cpu#{i}".ljust(10) }.join
    sys_samples.each do |s|
      puts "  #{s[:t].to_s.ljust(8)}" + s[:freqs].map { |v| v.to_s.ljust(10) }.join
    end
  end

  unless sys_samples.first[:temps].empty?
    puts "\n  Thermal zones (°C):"
    puts "  #{"t(s)".ljust(8)}" + sys_samples.first[:temps].each_index.map { |i| "zone#{i}".ljust(10) }.join
    sys_samples.each do |s|
      puts "  #{s[:t].to_s.ljust(8)}" + s[:temps].map { |v| v.to_s.ljust(10) }.join
    end
  end

  puts "\n  Memory over time (MB):"
  puts "  #{"t(s)".ljust(8)} #{"used".ljust(10)} #{"free".ljust(10)} #{"fs-cache".ljust(12)} #{"jvm-heap".ljust(10)}"
  sys_samples.each do |s|
    puts "  #{s[:t].to_s.ljust(8)} #{s[:mem][:used_mb].to_s.ljust(10)} " \
         "#{s[:mem][:free_mb].to_s.ljust(10)} #{s[:mem][:cache_mb].to_s.ljust(12)} " \
         "#{s[:jvm][:used_mb].to_s.ljust(10)}"
  end

  cpu_all  = sys_samples.flat_map { |s| s[:cpu] }
  puts "\n  Peak CPU:  #{cpu_all.max}%  (avg #{cpu_all.empty? ? 0 : (cpu_all.sum/cpu_all.size).round(1)}%)"
  puts "  Peak RAM:  #{sys_samples.map { |s| s[:mem][:used_mb] }.max}MB"
  puts "  Peak JVM:  #{sys_samples.map { |s| s[:jvm][:used_mb] }.max}MB heap"
  t = sys_samples.flat_map { |s| s[:temps] }.max
  puts "  Peak temp: #{t}°C" if t
end

puts ""
puts total_errs == 0 ? "  ALL CLEAN — 0 errors across #{total_ops} operations." \
                     : "  #{total_errs} ERRORS — see output above."
puts ""
