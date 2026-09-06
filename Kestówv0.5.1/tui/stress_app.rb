# frozen_string_literal: true

# Kestówv 0.5.1 — tui/stress_app.rb
#
# CRuby ANSI TUI live stress dashboard.
# Launched by boot/tui_stress.rb — receives MessagePack metrics from the
# Kestowv kernel (JRuby) over a Unix Domain Socket and renders them live
# in any ANSI terminal. No gems beyond msgpack and stdlib.
#
# Do not run directly. Invoked as:
#   ruby tui/stress_app.rb <socket_path>

require 'socket'
require 'msgpack'
require 'io/console'

SOCK_PATH = ARGV[0] || abort("Usage: stress_app.rb <socket_path>")

$data = {}
$dmx  = Mutex.new
$stop = false

# ── ANSI primitives ───────────────────────────────────────────────────────────
module A
  R     = "\e[0m"
  B     = "\e[1m"
  DIM   = "\e[2m"
  HOME  = "\e[H"
  CLR   = "\e[2J"
  EOL   = "\e[K"
  HCUR  = "\e[?25l"
  SCUR  = "\e[?25h"

  TITLE = "\e[95m"
  GREEN = "\e[92m"
  BLUE  = "\e[94m"
  YELL  = "\e[93m"
  RED   = "\e[91m"
  GRAY  = "\e[90m"
  FG    = "\e[37m"
  CYAN  = "\e[96m"

  def self.strip(s)  = s.gsub(/\e\[[0-9;]*[A-Za-z]/, '')
  def self.len(s)    = strip(s).length
  def self.pad(s, w) = s + ' ' * [w - len(s), 0].max
end

# ── Block bar ─────────────────────────────────────────────────────────────────
def bar(pct, width: 12, color: A::GREEN)
  filled = (pct.to_f / 100.0 * width).round.clamp(0, width)
  "#{color}#{'█' * filled}#{A::GRAY}#{'░' * (width - filled)}#{A::R}"
end

def comma(n) = n.to_s.reverse.gsub(/(\d{3})(?=\d)/, '\1,').reverse

# ── Socket reader thread ──────────────────────────────────────────────────────
Thread.new do
  tries = 0
  begin
    sock     = UNIXSocket.new(SOCK_PATH)
    unpacker = MessagePack::Unpacker.new
    while (chunk = sock.readpartial(65536))
      unpacker.feed_each(chunk) { |msg| $dmx.synchronize { $data = msg } }
    end
  rescue Errno::ENOENT, Errno::ECONNREFUSED
    tries += 1
    sleep 0.2
    retry if tries < 75
    $dmx.synchronize { $data = { '_error' => "Cannot connect to #{SOCK_PATH}" } }
  rescue EOFError, Errno::EPIPE, IOError
    $dmx.synchronize { $data['_gone'] = true }
  rescue => e
    $dmx.synchronize { $data['_error'] = e.message }
  end
end

# ── Keyboard input thread ─────────────────────────────────────────────────────
Thread.new do
  $stdin.raw do
    loop do
      ch = $stdin.getc rescue nil
      if ch == 'q' || ch == 'Q' || ch == "\x03" || ch == "\x04"
        $stop = true
        break
      end
    end
  end
rescue
end

# ── Render ────────────────────────────────────────────────────────────────────
def render(d)
  rows, cols = ($stdout.winsize rescue [24, 80])
  cols = cols.clamp(60, 240)

  buf = +''
  buf << A::HOME

  # ── Header
  left  = "  #{A::TITLE}#{A::B}Kestówv 0.5.1#{A::R}  #{A::GRAY}Live Stress Dashboard#{A::R}"
  right = "#{A::GRAY}t=#{d['t']}s#{A::R}  "
  gap   = cols - A.len(left) - A.len(right)
  buf << left << (' ' * [gap, 0].max) << right << "\n"
  buf << "#{A::GRAY}#{'─' * cols}#{A::R}\n"

  # ── CPU with per-core bars
  cpu = Array(d['cpu'])
  if cpu.any?
    cpu_parts = cpu.each_with_index.map do |p, i|
      pct = p.to_f
      col = pct > 90 ? A::RED : pct > 70 ? A::YELL : A::GREEN
      "#{A::DIM}c#{i}#{A::R} #{bar(pct, width: 8, color: col)}#{col}#{pct.round}%#{A::R}"
    end
    buf << "  #{A::BLUE}CPU #{A::R}  #{cpu_parts.join('  ')}#{A::EOL}\n"
  end

  # ── Freq
  fr = Array(d['freqs_mhz'])
  if fr.any?
    buf << "  #{A::BLUE}Freq#{A::R}  #{A::GRAY}#{fr.each_with_index.map { |f, i| "cpu#{i}=#{f}MHz" }.join('  ')}#{A::R}#{A::EOL}\n"
  end

  # ── Temp
  tm = Array(d['temps'])
  if tm.any?
    temp_parts = tm.map { |t| col = t.to_f > 85 ? A::RED : t.to_f > 70 ? A::YELL : A::CYAN; "#{col}#{t}°C#{A::R}" }
    buf << "  #{A::BLUE}Temp#{A::R}  #{temp_parts.join('  ')}#{A::EOL}\n"
  end

  # ── RAM
  m = d['mem'] || {}
  total = m['total_mb'].to_f
  used  = m['used_mb'].to_f
  ram_pct  = total > 0 ? used / total * 100 : 0
  ram_col  = ram_pct > 90 ? A::RED : ram_pct > 70 ? A::YELL : A::GREEN
  buf << "  #{A::BLUE}RAM #{A::R}  #{bar(ram_pct, width: 10, color: ram_col)} " \
         "used=#{ram_col}#{m['used_mb']}MB#{A::R}  free=#{A::GREEN}#{m['free_mb']}MB#{A::R}" \
         "  total=#{m['total_mb']}MB#{A::EOL}\n"

  # ── JVM heap
  j = d['jvm'] || {}
  max_mb  = j['max_mb'].to_f
  used_mb = j['used_mb'].to_f
  heap_pct = max_mb > 0 ? used_mb / max_mb * 100 : 0
  jvm_col  = heap_pct > 85 ? A::RED : heap_pct > 70 ? A::YELL : A::GREEN
  buf << "  #{A::BLUE}JVM #{A::R}  #{bar(heap_pct, width: 10, color: jvm_col)} " \
         "#{jvm_col}#{j['used_mb']}MB#{A::R} / #{j['total_mb']}MB  " \
         "(max #{j['max_mb']}MB)#{A::EOL}\n"

  # ── Wave governor
  w  = d['wave'] || {}
  gf = (w['governor_factor'].to_f * 100).round
  gov_col = gf < 40 ? A::RED : gf < 70 ? A::YELL : A::GREEN
  gc_tag  = w['gc_headroom'] ? "  #{A::YELL}⚡ GC-yield#{A::R}" : ''
  buf << "  #{A::BLUE}Wave#{A::R}  threads=#{w['threads']}  " \
         "gov=#{bar(gf, width: 8, color: gov_col)}#{gov_col}#{gf}%#{A::R}  " \
         "cap=#{(w['cpu_cap'].to_f * 100).round}%#{gc_tag}#{A::EOL}\n"

  # ── Hub
  h = d['hub'] || {}
  buf << "  #{A::BLUE}Hub #{A::R}  sockets=#{h['active_sockets']}  backend=#{h['backend']}#{A::EOL}\n"
  buf << "#{A::GRAY}#{'─' * cols}#{A::R}\n"

  # ── Table
  subs   = d['subsystems'] || {}
  t_ops  = d['total_ops']     || 0
  t_rate = d['total_ops_sec'] || 0
  t_err  = d['total_errs']    || 0

  hdr = "  #{A::B}#{A::BLUE}#{'SUBSYSTEM'.ljust(22)}  #{'OPS/SEC'.rjust(10)}  #{'TOTAL OPS'.rjust(13)}  #{'ERRORS'.rjust(6)}#{A::R}"
  buf << hdr << A::EOL << "\n"
  buf << "  #{A::GRAY}#{'─' * [cols - 4, 56].max}#{A::R}#{A::EOL}\n"

  fixed_lines = 16   # header + metrics + table hdr + footer rows
  avail = [rows - fixed_lines, 1].max

  subs.sort.first(avail).each do |name, r|
    ops_sec = r['ops_sec'] || 0
    ops     = r['ops']     || 0
    errors  = r['errors']  || 0
    err_col = errors > 0 ? A::RED : A::GRAY
    buf << "  #{A::FG}#{name.ljust(22)}#{A::R}" \
           "  #{A::GREEN}#{ops_sec.to_s.rjust(10)}#{A::R}" \
           "  #{A::YELL}#{ops.to_s.rjust(13)}#{A::R}" \
           "  #{err_col}#{errors.to_s.rjust(6)}#{A::R}#{A::EOL}\n"
  end

  buf << "  #{A::GRAY}#{'─' * [cols - 4, 56].max}#{A::R}#{A::EOL}\n"
  buf << "  #{A::B}#{'TOTAL'.ljust(22)}#{A::R}" \
         "  #{A::GREEN}#{A::B}#{t_rate.to_s.rjust(10)}#{A::R}" \
         "  #{A::YELL}#{A::B}#{t_ops.to_s.rjust(13)}#{A::R}" \
         "  #{t_err > 0 ? A::RED : A::GRAY}#{A::B}#{t_err.to_s.rjust(6)}#{A::R}#{A::EOL}\n"
  buf << "#{A::GRAY}#{'─' * cols}#{A::R}\n"

  # ── Status bar
  ops_fmt   = comma(t_ops)
  if t_err == 0
    status = "  #{A::GREEN}#{A::B}#{ops_fmt} ops — ALL CLEAN#{A::R}"
  else
    status = "  #{A::RED}#{A::B}#{ops_fmt} ops — #{comma(t_err)} ERRORS#{A::R}"
  end
  hint    = "#{A::GRAY}[q] quit#{A::R}  "
  gap     = cols - A.len(status) - A.len(hint)
  buf << status << (' ' * [gap, 0].max) << hint << A::EOL

  $stdout.write(buf.gsub("\n", "\r\n"))
  $stdout.flush
end

# ── Main loop ─────────────────────────────────────────────────────────────────
begin
  $stdout.write("#{A::CLR}#{A::HOME}#{A::HCUR}")
  $stdout.flush

  loop do
    break if $stop

    d = $dmx.synchronize { $data.dup }

    if (msg = d['_error'])
      $stdout.write("#{A::HOME}#{A::RED}ERROR: #{msg}#{A::R}#{A::EOL}")
      $stdout.flush
    elsif d['_gone']
      $stdout.write("#{A::HOME}#{A::GRAY}Kernel disconnected. Press q to exit.#{A::R}#{A::EOL}")
      $stdout.flush
    elsif d.empty?
      $stdout.write("#{A::HOME}#{A::GRAY}Waiting for kernel...#{A::R}#{A::EOL}")
      $stdout.flush
    else
      render(d)
    end

    sleep 0.5
  end
ensure
  $stdout.write("#{A::SCUR}#{A::CLR}#{A::HOME}")
  $stdout.flush
end
