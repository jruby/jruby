# frozen_string_literal: true

# Kestówv 0.5.1 — tk/stress_app.rb
#
# CRuby/Tk live stress dashboard.
# Launched by boot/tk_stress.rb — receives MessagePack metrics from the
# Kestowv kernel (JRuby) over a Unix Domain Socket and renders
# them live in a Tk window.
#
# Do not run directly. Invoked as:
#   ruby tk/stress_app.rb <socket_path>

require 'tk'
require 'socket'
require 'msgpack'

SOCK_PATH = ARGV[0] || abort("Usage: stress_app.rb <socket_path>")

# ── Shared state (updated by socket thread, read by Tk.after) ─────────────────
$data  = {}
$dmx   = Mutex.new

# ── Fonts & colours ───────────────────────────────────────────────────────────
BG    = "#1e1e2e"
BG2   = "#181825"
BG3   = "#11111b"
FG    = "#cdd6f4"
TITLE = "#cba6f7"
GREEN = "#a6e3a1"
BLUE  = "#89b4fa"
YELL  = "#f9e2af"
RED   = "#f38ba8"
GRAY  = "#585b70"

MONO   = TkFont.new(family: "Monospace", size: 10)
MONO_B = TkFont.new(family: "Monospace", size: 10, weight: "bold")
LARGE  = TkFont.new(family: "Monospace", size: 13, weight: "bold")

# ── Root window ───────────────────────────────────────────────────────────────
root = TkRoot.new do
  title  "Kestówv 0.5.1 — Live Stress Dashboard"
  background BG
end
root.geometry("860x700")
root.resizable(true, true)

# ── Header ────────────────────────────────────────────────────────────────────
hdr = TkFrame.new(root, background: BG2, padx: 10, pady: 6)
hdr.pack(fill: "x")

TkLabel.new(hdr,
  text: "Kestówv 0.5.1 — Live Stress Dashboard",
  font: LARGE, foreground: TITLE, background: BG2, anchor: "w"
).pack(side: "left")

time_lbl = TkLabel.new(hdr,
  text: "connecting...",
  font: MONO, foreground: GRAY, background: BG2
)
time_lbl.pack(side: "right")

# ── System metrics ────────────────────────────────────────────────────────────
sys_f = TkFrame.new(root, background: BG, padx: 10, pady: 4)
sys_f.pack(fill: "x")

def met(parent, color)
  TkLabel.new(parent, text: "─", font: MONO, foreground: color,
    background: BG, anchor: "w").tap { |l| l.pack(fill: "x") }
end

cpu_lbl  = met(sys_f, YELL)
freq_lbl = met(sys_f, BLUE)
temp_lbl = met(sys_f, RED)
mem_lbl  = met(sys_f, FG)
jvm_lbl  = met(sys_f, FG)
wave_lbl = met(sys_f, GREEN)
hub_lbl  = met(sys_f, BLUE)

# ── Divider ───────────────────────────────────────────────────────────────────
TkFrame.new(root, background: GRAY, height: 1).pack(fill: "x", pady: 2)

# ── Subsystem table ───────────────────────────────────────────────────────────
tbl_f = TkFrame.new(root, background: BG3)
tbl_f.pack(fill: "both", expand: true, padx: 10, pady: 0)

ysb = TkScrollbar.new(tbl_f, orient: "vertical")
ysb.pack(side: "right", fill: "y")

tbl = TkText.new(tbl_f,
  font: MONO, background: BG3, foreground: FG,
  state: "disabled", cursor: "arrow", wrap: "none",
  yscrollcommand: proc { |*a| ysb.set(*a) }
)
tbl.pack(side: "left", fill: "both", expand: true)
ysb.command(proc { |*a| tbl.yview(*a) })

tbl.tag_configure("hdr",   foreground: BLUE,  font: MONO_B)
tbl.tag_configure("name",  foreground: FG)
tbl.tag_configure("rate",  foreground: GREEN)
tbl.tag_configure("ops",   foreground: YELL)
tbl.tag_configure("ok",    foreground: GRAY)
tbl.tag_configure("err",   foreground: RED)
tbl.tag_configure("sep",   foreground: GRAY)
tbl.tag_configure("total", foreground: BLUE,  font: MONO_B)

# ── Status bar ────────────────────────────────────────────────────────────────
TkFrame.new(root, background: GRAY, height: 1).pack(fill: "x")

bot = TkFrame.new(root, background: BG2, padx: 10, pady: 4)
bot.pack(fill: "x")

status_lbl = TkLabel.new(bot,
  text: "Connecting to Kestówv kernel...",
  font: MONO, foreground: GRAY, background: BG2, anchor: "w"
)
status_lbl.pack(side: "left", fill: "x", expand: true)

TkButton.new(bot,
  text: " Quit ",
  font: MONO, foreground: RED, background: BG2,
  activeforeground: BG, activebackground: RED,
  relief: "flat", cursor: "hand2",
  command: proc { exit(0) }
).pack(side: "right")

# ── Socket reader thread ──────────────────────────────────────────────────────
Thread.new do
  tries = 0
  begin
    sock     = UNIXSocket.new(SOCK_PATH)
    unpacker = MessagePack::Unpacker.new
    while (chunk = sock.readpartial(65536))
      unpacker.feed_each(chunk) do |msg|
        $dmx.synchronize { $data = msg }
      end
    end
  rescue Errno::ENOENT, Errno::ECONNREFUSED
    tries += 1
    sleep 0.2
    retry if tries < 75    # wait up to 15s
    $dmx.synchronize { $data = { "_error" => "Cannot connect to #{SOCK_PATH}" } }
  rescue EOFError, Errno::EPIPE, IOError
    $dmx.synchronize { $data["_gone"] = true }
  rescue => e
    $dmx.synchronize { $data["_error"] = e.message }
  end
end

# ── UI refresh (runs in Tk event loop) ───────────────────────────────────────
refresh = nil
refresh = proc do
  d = $dmx.synchronize { $data.dup }

  if (msg = d["_error"])
    status_lbl.configure(text: "ERROR: #{msg}", foreground: RED)

  elsif d["_gone"]
    status_lbl.configure(text: "Kernel disconnected.", foreground: GRAY)

  elsif d.empty?
    status_lbl.configure(text: "Waiting for kernel...", foreground: GRAY)

  else
    # ── time
    time_lbl.configure(text: "t=#{d['t']}s")

    # ── CPU
    cpu = Array(d["cpu"])
    cpu_lbl.configure(
      text: "CPU:   " + cpu.each_with_index.map { |p, i| "core#{i}=#{p}%" }.join("  ")
    )

    # ── Freq
    fr = Array(d["freqs_mhz"])
    freq_lbl.configure(
      text: fr.empty? ? "Freq:  n/a" :
        "Freq:  " + fr.each_with_index.map { |f, i| "cpu#{i}=#{f}MHz" }.join("  ")
    )

    # ── Temp
    tm = Array(d["temps"])
    temp_lbl.configure(
      text: tm.empty? ? "Temp:  n/a" :
        "Temp:  " + tm.map { |t| "#{t}°C" }.join("  ")
    )

    # ── RAM
    m = d["mem"] || {}
    mem_lbl.configure(
      text: "RAM:   used=#{m['used_mb']}MB  free=#{m['free_mb']}MB  " \
            "cache=#{m['cache_mb']}MB  total=#{m['total_mb']}MB"
    )

    # ── JVM
    j = d["jvm"] || {}
    jvm_lbl.configure(
      text: "JVM:   heap #{j['used_mb']}MB / #{j['total_mb']}MB  (max #{j['max_mb']}MB)"
    )

    # ── Wave
    w = d["wave"] || {}
    gc_tag = w['gc_headroom'] ? "  ⚡GC-yield" : ""
    wave_lbl.configure(
      text: "Wave:  running=#{w['running']}  threads=#{w['threads']}  " \
            "gov=#{(w['governor_factor'].to_f * 100).round}%  " \
            "cap=#{(w['cpu_cap'].to_f * 100).round}%#{gc_tag}"
    )

    # ── Hub
    h = d["hub"] || {}
    hub_lbl.configure(
      text: "Hub:   active_sockets=#{h['active_sockets']}  backend=#{h['backend']}"
    )

    # ── Table
    subs   = d["subsystems"] || {}
    t_ops  = d["total_ops"]     || 0
    t_rate = d["total_ops_sec"] || 0
    t_err  = d["total_errs"]    || 0

    tbl.configure(state: "normal")
    tbl.delete("1.0", "end")

    hdr_line = "%-22s  %10s  %13s  %6s\n" % %w[SUBSYSTEM OPS/SEC TOTAL\ OPS ERRORS]
    tbl.insert("end", hdr_line, "hdr")
    tbl.insert("end", "─" * 60 + "\n", "sep")

    subs.sort.each do |name, r|
      ops_sec = r["ops_sec"] || 0
      ops     = r["ops"]     || 0
      errors  = r["errors"]  || 0
      etag    = errors > 0 ? "err" : "ok"
      tbl.insert("end", "%-22s" % name,           "name")
      tbl.insert("end", "  %10s" % ops_sec.to_s,  "rate")
      tbl.insert("end", "  %13s" % ops.to_s,      "ops")
      tbl.insert("end", "  %6s\n" % errors.to_s,  etag)
    end

    tbl.insert("end", "─" * 60 + "\n", "sep")
    tbl.insert("end",
      "%-22s  %10s  %13s  %6s\n" % ["TOTAL", t_rate.to_s, t_ops.to_s, t_err.to_s],
      "total"
    )
    tbl.configure(state: "disabled")

    # ── Status bar
    if t_err == 0
      status_lbl.configure(
        text: "#{t_ops.to_s.reverse.gsub(/(\d{3})(?=\d)/, '\\1,').reverse} ops — ALL CLEAN",
        foreground: GREEN
      )
    else
      status_lbl.configure(
        text: "#{t_ops} ops — #{t_err} ERRORS",
        foreground: RED
      )
    end
  end

  Tk.after(500) { refresh.call }
end

Tk.after(500) { refresh.call }
Tk.mainloop
