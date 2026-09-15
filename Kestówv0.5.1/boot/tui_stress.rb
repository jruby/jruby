# frozen_string_literal: true

# Kestówv 0.5.1 — boot/tui_stress.rb
#
# Kernel-controlled CRuby ANSI TUI stress dashboard.
# No Tk required — pure terminal, stdlib + msgpack only.
#
# Architecture:
#   JRuby (Kestowv kernel)
#     ├── Proc::Exec   — records the CRuby exec in the kernel task table
#     ├── Net::UnixHub — tracks the UDS connection in the kernel socket registry
#     ├── 30 stress threads — all subsystems under continuous load
#     └── metrics ticker   — serialises live state as MessagePack → CRuby every 500ms
#
#   CRuby (tui/stress_app.rb)
#     └── ANSI TUI — live dashboard reading MessagePack from the UDS
#
# Terminal emulator priority: gnome-terminal → xterm → kitty → manual instructions
#
# Run: jruby boot/tui_stress.rb

require 'socket'
require 'msgpack'
require_relative 'init'

Boot.config.quiet = true
Kestowv::Init.boot

include Kestowv
KProc   = Kestowv::Proc
Hub     = Net::UnixHub
USocket = Net::UnixSocket

SOCK_PATH  = "/tmp/kestowv_tui_#{Process.pid}.sock"
TUI_SCRIPT = File.expand_path("../tui/stress_app.rb", __dir__)
RUBY_BIN   = RbConfig::CONFIG['bindir'] + '/' + RbConfig::CONFIG['ruby_install_name'] rescue '/usr/bin/ruby'

# ── Register TUI child with kernel Proc::Exec ─────────────────────────────────
tui_task = KProc::Task.new(name: :tui_stress_app)
tui_task.assign_credentials(KProc::Credentials.root)
KProc::Exec.execve(TUI_SCRIPT, [RUBY_BIN, TUI_SCRIPT, SOCK_PATH], {}, task: tui_task)
tui_task.transition(:running)

# ── OS-level UDS server ────────────────────────────────────────────────────────
File.delete(SOCK_PATH) if File.exist?(SOCK_PATH)
server   = UNIXServer.new(SOCK_PATH)
hub_sock = Net::UnixSocket::Socket.new(SOCK_PATH, :stream)
Hub.register(hub_sock)

# ── Spawn CRuby TUI in a terminal window ──────────────────────────────────────
def terminal_available?(cmd)
  system("which #{cmd} > /dev/null 2>&1")
end

spawn_pid = nil
spawner   = nil

if terminal_available?('gnome-terminal')
  spawner   = 'gnome-terminal'
  spawn_pid = Process.spawn(
    'gnome-terminal',
    '--title', 'Kestówv 0.5.1 Stress',
    '--',
    RUBY_BIN, TUI_SCRIPT, SOCK_PATH
  )
elsif terminal_available?('xterm')
  spawner   = 'xterm'
  spawn_pid = Process.spawn(
    'xterm',
    '-title', 'Kestówv 0.5.1 Stress',
    '-fa', 'Monospace', '-fs', '10',
    '-e', "#{RUBY_BIN} #{TUI_SCRIPT} #{SOCK_PATH}"
  )
elsif terminal_available?('kitty')
  spawner   = 'kitty'
  spawn_pid = Process.spawn(
    'kitty', '--title', 'Kestówv 0.5.1 Stress',
    RUBY_BIN, TUI_SCRIPT, SOCK_PATH
  )
elsif terminal_available?('alacritty')
  spawner   = 'alacritty'
  spawn_pid = Process.spawn(
    'alacritty', '--title', 'Kestówv 0.5.1 Stress',
    '-e', RUBY_BIN, TUI_SCRIPT, SOCK_PATH
  )
end

puts ""
puts "  [tui_stress] Kernel booted"
puts "  [tui_stress] Wave:   #{Core::Wave.stats[:threads]} daemon threads"
puts "  [tui_stress] Exec:   CRuby task registered (kernel pid slot)"
puts "  [tui_stress] UDS:    #{SOCK_PATH}"

if spawn_pid
  puts "  [tui_stress] Spawned #{spawner} (OS pid=#{spawn_pid})"
else
  puts "  [tui_stress] No terminal found — run manually in another terminal:"
  puts "  [tui_stress]   #{RUBY_BIN} #{TUI_SCRIPT} #{SOCK_PATH}"
end

puts "  [tui_stress] Waiting for CRuby to connect (up to 20s)..."

# ── Accept CRuby connection ────────────────────────────────────────────────────
client   = nil
deadline = Time.now + 20
loop do
  begin
    client = server.accept_nonblock
    break
  rescue IO::WaitReadable
    raise "TUI app did not connect within 20s" if Time.now > deadline
    sleep 0.1
  end
end

puts "  [tui_stress] CRuby connected — streaming metrics"
puts ""

# ── SysMonitor ────────────────────────────────────────────────────────────────
module SysMonitor
  def self.cpu_stat
    File.readlines("/proc/stat").select { |l| l =~ /^cpu\d/ }.map do |l|
      f = l.split.drop(1).map(&:to_i)
      [f[3] + f[4], f.sum]
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
    File.foreach("/proc/meminfo") { |l| k, v = l.split(":"); info[k.strip] = v.strip.to_i if v }
    { "used_mb"  => ((info["MemTotal"] - info["MemAvailable"]) / 1024.0).round.to_i,
      "free_mb"  => (info["MemAvailable"] / 1024.0).round.to_i,
      "cache_mb" => ((info["Buffers"].to_i + info["Cached"].to_i) / 1024.0).round.to_i,
      "total_mb" => (info["MemTotal"] / 1024.0).round.to_i }
  end

  def self.jvm_heap
    rt = Java::JavaLang::Runtime.get_runtime
    { "used_mb"  => ((rt.total_memory - rt.free_memory) / 1_048_576.0).round.to_i,
      "total_mb" => (rt.total_memory  / 1_048_576.0).round.to_i,
      "max_mb"   => (rt.max_memory    / 1_048_576.0).round.to_i }
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

# ── Stress thread harness ─────────────────────────────────────────────────────
stop_flag = false
results   = {}
mutex     = Mutex.new

def stress_thread(name, results, mutex, stopped_ref)
  ops = 0; errors = 0; t0 = Time.now
  ok = proc do
    ops += 1
    if ops % 200 == 0
      el = Time.now - t0
      mutex.synchronize do
        results[name] = { "ops" => ops, "errors" => errors,
                          "ops_sec" => el > 0 ? (ops / el).round : 0 }
      end
    end
  end
  begin
    yield(->{stopped_ref.call}, ok, ->(_e){errors+=1})
  rescue => e
    errors += 1
    warn "  [stress/#{name}] #{e.message}"
  ensure
    el = Time.now - t0
    mutex.synchronize do
      results[name] = { "ops" => ops, "errors" => errors,
                        "ops_sec" => el > 0 ? (ops / el).round : 0 }
    end
  end
end

threads = []

# ── 1–4: Memory management ────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("mm/frames", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      pg = Mm::FrameAllocator.allocate
      if pg; Mm::FrameAllocator.free(pg); ok.call; end
    end
  end
end

threads << Thread.new do
  stress_thread("mm/pagetable", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    pt = Mm::PageTable.new; i = 0
    until stopped.call
      pg = Mm::FrameAllocator.allocate; next unless pg
      vpn = 0x2000 + i
      pt.map(vpn, pg, flags: Mm::PageTable::Flags::PRESENT | Mm::PageTable::Flags::WRITABLE)
      pt.lookup(vpn); pt.unmap(vpn); Mm::FrameAllocator.free(pg)
      i = (i + 1) % 0x1000; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("mm/vmregion", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      r = Mm::VmRegion.slab_acquire(start_vpn: 0x8000 + i * 16, num_pages: 4,
            flags: Mm::Protection::USER_RW, name: :"tui_vma_#{i}", backing: :anonymous)
      r.slab_release; i = (i + 1) % 0x1000; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("mm/heap", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      Mm::Heap.allocate(4096); Mm::Heap.free(4096); ok.call
    end
  end
end

# ── 5–8: Process management ───────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("proc/pid", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      pid = KProc::Pid.allocate(task: nil)
      if pid; KProc::Pid.release(pid); ok.call; end
    end
  end
end

threads << Thread.new do
  stress_thread("proc/task", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      t = KProc::Task.slab_acquire(name: :tui_task)
      t.transition(:running); t.transition(:zombie); t.slab_release; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("proc/creds", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    uid = 1000
    until stopped.call
      cred = KProc::Credentials.slab_acquire(uid: uid, gid: uid)
      cred.slab_release
      uid = (uid + 1) % 60_000 + 1000; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("proc/cgroup", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      cg = KProc::Cgroup.create("tui_cg_#{i}", controllers: [:cpu, :pids])
      KProc::Cgroup.destroy(cg.id); i += 1; ok.call
    end
  end
end

# ── 9–10: Filesystem ──────────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("fs/tmpfs", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    fs = Fs::TmpFs.new(name: "tui_fs", max_bytes: 32 * 1024 * 1024); i = 0
    until stopped.call
      path = "/tui_#{i % 256}.dat"
      fs.write(path, "kestowv tui stress #{i}")
      fs.read(path); fs.stat(path); fs.delete(path)
      i += 1; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("fs/vfs", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      pt = "/tui_mnt_#{i}"
      be = Fs::TmpFs.new(name: "tui_vfs_#{i}", max_bytes: 4096)
      Fs::Vfs.mount(pt, be, ns_id: nil, flags: 0)
      Fs::Vfs.resolve(pt, ns_id: nil); Fs::Vfs.unmount(pt)
      i += 1; ok.call
    end
  end
end

# ── 11–14: IPC ────────────────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("ipc/pipe", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      id = Ipc::Pipe.create
      Ipc::Pipe.write(id, "tui payload")
      Ipc::Pipe.read(id); Ipc::Pipe.close(id); ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("ipc/queue", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    key = :"tui_q_#{Thread.current.object_id}"
    Ipc::Queue.create(key)
    until stopped.call
      Ipc::Queue.enqueue(key, "msg"); Ipc::Queue.dequeue(key); ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("ipc/sem", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    key = :"tui_sem_#{Thread.current.object_id}"
    Ipc::Sem.create(key, 100)
    until stopped.call
      Ipc::Sem.post(key); Ipc::Sem.wait(key); ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("ipc/shm", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      key = :"tui_shm_#{i % 64}"
      Ipc::Shm.create(key, 4096); Ipc::Shm.attach(key)
      Ipc::Shm.detach(key) rescue nil; Ipc::Shm.destroy(key) rescue nil
      i += 1; ok.call
    end
  end
end

# ── 15–18: Core ───────────────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/klog", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      Core::Klog.debug("tui stress #{i}", subsystem: :tui); i += 1; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("boot/bitvec", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    i = 0
    until stopped.call
      sym = :"tui_bit_#{i % 64}"
      Boot.set_bit(sym); Boot.bit_set?(sym); Boot.clear_bit(sym)
      i += 1; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("core/runqueue", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    rq   = Core::RunQueue.new(99)
    pool = Array.new(8) { KProc::Task.new(name: :tui_rq) }
    i    = 0
    until stopped.call
      rq.enqueue(pool[i % pool.size]); rq.dequeue; i += 1; ok.call
    end
    pool.each(&:release)
  end
end

threads << Thread.new do
  stress_thread("core/binclass", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      Core::BinaryClassifier.classify(__FILE__); ok.call
    end
  end
end

# ── 19: Wave health ───────────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("core/wave", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      s = Core::Wave.stats
      raise "wave died" unless s[:running]
      ok.call; sleep 0.01
    end
  end
end

# ── 20–28: UDS ────────────────────────────────────────────────────────────────
threads << Thread.new do
  stress_thread("uds/lifecycle", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    tid = Thread.current.object_id; i = 0
    until stopped.call
      path = "/tmp/kestowv_tui_uds_#{tid}_#{i}"
      sock = USocket.new(path); sock.connect; sock.close
      USocket.close(path); i += 1; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("uds/socketpair", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    ping = { msg: "ping" }
    until stopped.call
      s1, s2 = USocket.pair
      s1.connect; s2.connect
      s1.send_io(nil, ping); s2.recv_io
      s1.close; s2.close
      USocket.close(s1.path); USocket.close(s2.path)
      ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("uds/hub-churn", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    tid = Thread.current.object_id; i = 0
    until stopped.call
      path = "/tmp/kestowv_tui_hub_#{tid}_#{i}"
      s = Net::UnixSocket::Socket.new(path, :stream)
      Hub.register(s); Hub.unregister(path)
      i += 1; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("uds/hub-lookup", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    paths = 16.times.map { |i|
      path = "/tmp/kestowv_tui_lookup_#{i}"
      Hub.register(Net::UnixSocket::Socket.new(path, :stream)); path
    }
    until stopped.call
      paths.each { |p| Hub.get(p) }; ok.call
    end
    paths.each { |p| Hub.unregister(p) }
  end
end

threads << Thread.new do
  stress_thread("uds/hub-stats", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    until stopped.call
      Hub.list; Hub.stats; Hub.active_sockets; ok.call
    end
  end
end

threads << Thread.new do
  stress_thread("uds/send-io", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    path    = "/tmp/kestowv_tui_sendio_#{Thread.current.object_id}"
    sock    = USocket.new(path); sock.connect; i = 0
    payload = { seq: 0, payload: "tui uds stress", ts: 0.0 }
    until stopped.call
      payload[:seq] = i
      payload[:ts]  = Time.now.to_f
      sock.send_io(nil, payload); sock.recv_io; i += 1; ok.call
    end
    sock.close; USocket.close(path)
  end
end

threads << Thread.new do
  stress_thread("uds/msgpack", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
    payload = { cmd: "write", path: "/tmp/sock", size: 4096 }
    until stopped.call
      packed = MessagePack.pack(payload); MessagePack.unpack(packed); ok.call
    end
  end
end

shared_path = "/tmp/kestowv_tui_shared_writer"
shared_sock = Net::UnixSocket::Socket.new(shared_path, :stream)
Hub.register(shared_sock)

4.times do |w|
  threads << Thread.new(w) do |wid|
    stress_thread("uds/writer-#{wid}", results, mutex, ->{ stop_flag }) do |stopped, ok, _|
      i = 0
      payload = { writer: wid, seq: 0 }
      until stopped.call
        s = Hub.get(shared_path)
        if s
          payload[:seq] = i
          s.send_io(nil, payload)
        end
        i += 1; ok.call
      end
    end
  end
end

# ── Metrics ticker → CRuby ────────────────────────────────────────────────────
start_time = Time.now
cpu_before = SysMonitor.cpu_stat

puts "  [tui_stress] Running #{threads.size} stress threads — streaming to TUI"
puts "  [tui_stress] Close the terminal or press q to stop"
puts ""

loop do
  sleep 0.5
  break if stop_flag

  elapsed    = (Time.now - start_time).round(1)
  cpu_after  = SysMonitor.cpu_stat
  cpu_pcts   = SysMonitor.cpu_pct(cpu_before, cpu_after)
  cpu_before = cpu_after

  snap   = mutex.synchronize { results.dup }
  t_ops  = snap.values.sum { |r| r["ops"] }
  t_errs = snap.values.sum { |r| r["errors"] }
  t_rate = elapsed > 0 ? (t_ops / elapsed).round : 0

  ws = Core::Wave.stats
  hs = Hub.stats

  metrics = {
    "t"             => elapsed,
    "cpu"           => cpu_pcts,
    "freqs_mhz"     => SysMonitor.freqs_mhz,
    "temps"         => SysMonitor.temps,
    "mem"           => SysMonitor.mem,
    "jvm"           => SysMonitor.jvm_heap,
    "wave"          => { "running"         => ws[:running],
                         "threads"         => ws[:threads],
                         "threads_alive"   => ws[:threads_alive],
                         "governor_factor" => ws[:governor_factor],
                         "cpu_cap"         => ws[:cpu_cap],
                         "gc_headroom"     => ws[:gc_headroom] },
    "hub"           => { "active_sockets" => hs[:active_sockets],
                         "backend"        => hs[:backend].to_s },
    "subsystems"    => snap,
    "total_ops"     => t_ops,
    "total_ops_sec" => t_rate,
    "total_errs"    => t_errs
  }

  begin
    client.write(MessagePack.pack(metrics))
    client.flush
  rescue Errno::EPIPE, IOError
    puts "  [tui_stress] TUI closed — shutting down"
    stop_flag = true
    break
  end
end

stop_flag = true
threads.each { |t| t.join(5) }
Hub.unregister(shared_path)
Hub.unregister(SOCK_PATH)
client.close rescue nil
server.close rescue nil
File.delete(SOCK_PATH) rescue nil

snap  = mutex.synchronize { results.dup }
total = snap.values.sum { |r| r["ops"] }
errs  = snap.values.sum { |r| r["errors"] }
puts ""
puts "  [tui_stress] Done — #{total} ops / #{errs} errors"
