# frozen_string_literal: true

# Performance guards: benchmarks that fail CI when performance regresses.
#
# Each benchmark compares a candidate snippet against a baseline snippet
# that does the same work by a path known to be fast, and fails when the
# candidate is slower than the baseline by more than max_ratio. Comparing
# two snippets in the same JVM, instead of comparing against stored timings,
# makes the result independent of how fast or busy the machine is.
#
# See bench/guard/README.md for the file format and how failures are decided.
#
#   bin/jruby bench/guard/runner.rb [options] [FILE.yml ...]

require 'java'
require 'jruby'
require 'json'
require 'optparse'
require 'rbconfig'
require 'yaml'

module PerfGuard
  CASES_DIR = File.expand_path('cases', __dir__)

  THREADS = java.lang.management.ManagementFactory.getThreadMXBean
  THREADS.setThreadCpuTimeEnabled(true)

  # Group-level settings that a YAML file may override.
  GROUP_DEFAULTS = {
    'max_ratio' => 1.5,       # fail when candidate time / baseline time exceeds this
    'max_alloc_ratio' => nil, # optionally, the same for bytes allocated
    'batch' => 100,           # times each snippet runs per method call
  }.freeze

  Options = Struct.new(:warmup, :sample, :min_rounds, :max_rounds, :confidence,
                       :recheck, :confirm, :seed, :shard, :only, :json, :verbose, keyword_init: true)

  Benchmark = Struct.new(:name, :baseline, :candidate, :max_ratio, :max_alloc_ratio, keyword_init: true)
  Group = Struct.new(:file, :name, :prelude, :batch, :benchmarks, keyword_init: true)

  # The outcome for one benchmark.
  #   status: :pass, :inconclusive or :regressed
  Result = Struct.new(:file, :group, :name, :status, :ratio, :ci, :alloc_ratio, :rounds,
                      :max_ratio, :max_alloc_ratio, :reason, :confirmed, keyword_init: true) do
    def to_h_json = to_h.transform_values { |v| v.is_a?(Symbol) ? v.to_s : v }
  end

  module_function

  def cpu_ns = THREADS.getCurrentThreadCpuTime
  def allocated_bytes = THREADS.getCurrentThreadAllocatedBytes

  # -- Loading --------------------------------------------------------------

  def load_group(path)
    spec = YAML.safe_load_file(path)
    settings = GROUP_DEFAULTS.merge(spec.slice(*GROUP_DEFAULTS.keys))
    shared_baseline = spec['baseline']

    benchmarks = spec.fetch('benchmarks').map do |name, bench|
      bench = { 'candidate' => bench } if bench.is_a?(String)
      bench = settings.merge(bench)
      Benchmark.new(
        name: name,
        baseline: bench['baseline'] || shared_baseline || raise(ArgumentError, "#{path}: #{name} has no baseline"),
        candidate: bench.fetch('candidate'),
        max_ratio: Float(bench['max_ratio']),
        max_alloc_ratio: bench['max_alloc_ratio']&.then { Float(_1) }
      )
    end

    Group.new(file: path, name: File.basename(path, '.yml'), prelude: spec['prelude'],
              batch: Integer(settings['batch']), benchmarks: benchmarks)
  end

  # Compiles every distinct snippet into its own method on a fresh class, so
  # that groups can't see each other's prelude. The method runs the snippet
  # in a loop, so the loop and the snippet are JIT-compiled together, as they
  # would be in application code. Returns { snippet => Method }.
  def compile(group, snippets)
    klass = Class.new
    klass.class_eval(group.prelude, group.file, 1) if group.prelude
    obj = klass.new

    snippets.each_with_index.to_h do |code, i|
      klass.class_eval(<<~RUBY, group.file)
        def __perf_guard_#{i}
          __perf_guard_i = 0
          while __perf_guard_i < #{group.batch}
            #{code}
            __perf_guard_i += 1
          end
          nil
        end
      RUBY
      [code, obj.method(:"__perf_guard_#{i}")]
    end
  end

  # -- Measuring ------------------------------------------------------------

  # Runs a snippet method `calls` times and returns [cpu_ns, allocated_bytes]
  # for the current thread. Thread CPU time isn't inflated by other processes
  # competing for the machine, which wall-clock time is.
  def measure(meth, calls)
    bytes = allocated_bytes
    time = cpu_ns
    n = calls
    while n > 0
      meth.call
      n -= 1
    end
    [cpu_ns - time, allocated_bytes - bytes]
  end

  # Runs every snippet, interleaved, until the JIT has had a chance to compile
  # them and the warmup budget is spent.
  def warm_up(methods, opts)
    deadline = cpu_ns + (opts.warmup * 1e9)
    calls = 0
    until calls >= 200 && cpu_ns >= deadline
      methods.each { |m| measure(m, 10) }
      calls += 10
    end
  end

  # Picks a call count so that one sample of the fastest snippet takes about
  # opts.sample seconds of CPU time. Every snippet uses the same count, so
  # per-call time and allocation compare directly.
  def calibrate(methods, opts)
    fastest = methods.map { |m| measure(m, 20).first / 20.0 }.min
    [(opts.sample * 1e9 / [fastest, 1].max).ceil, 1].max
  end

  # A distribution-free confidence interval for the median of `values`: the
  # pair of order statistics [x(k), x(n-k+1)] that covers the median with at
  # least the given confidence. Returns nil when there are too few values.
  def median_ci(values, confidence)
    sorted = values.sort
    n = sorted.size
    tail = (1 - confidence) / 2
    k = 0
    cumulative = 0r
    while k < n
      p = Rational(binomial(n, k), 2**n)
      break if cumulative + p > tail
      cumulative += p
      k += 1
    end
    return nil if k.zero?

    [sorted[k - 1], sorted[n - k]]
  end

  def binomial(n, k) = (1..k).reduce(1) { |acc, i| acc * (n - k + i) / i }

  def median(values)
    sorted = values.sort
    mid = sorted.size / 2
    sorted.size.odd? ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2.0
  end

  # Measures a group in rounds. Each round runs every snippet still needed
  # once, in a random order, and records candidate time / baseline time for
  # each benchmark. A benchmark stops early as soon as its confidence
  # interval lies entirely below max_ratio (pass) or entirely above it
  # (regressed); one that is still undecided after max_rounds is
  # inconclusive, which doesn't fail the run.
  def run_group(group, opts, rng, only: opts.only)
    benchmarks = group.benchmarks
    benchmarks = benchmarks.select { |b| only.include?("#{group.name}: #{b.name}") } if only
    return [] if benchmarks.empty?

    snippets = benchmarks.flat_map { |b| [b.baseline, b.candidate] }.uniq
    methods = compile(group, snippets)
    warm_up(methods.values, opts)
    calls = calibrate(methods.values, opts)

    ratios = Hash.new { |h, k| h[k] = [] }
    bytes = Hash.new(0)
    decided = {}
    rounds = 0

    while rounds < opts.max_rounds && decided.size < benchmarks.size
      rounds += 1
      pending = benchmarks.reject { |b| decided.key?(b) }
      sample = {}
      pending.flat_map { |b| [b.baseline, b.candidate] }.uniq.shuffle(random: rng).each do |code|
        sample[code] = measure(methods[code], calls)
      end

      pending.each do |b|
        base_time, base_bytes = sample[b.baseline]
        time, alloc = sample[b.candidate]
        ratios[b] << time.fdiv([base_time, 1].max)
        bytes[[b, :baseline]] += base_bytes
        bytes[[b, :candidate]] += alloc

        next if rounds < opts.min_rounds

        lo, hi = median_ci(ratios[b], opts.confidence)
        next unless lo

        alloc_ratio = alloc_ratio(bytes, b)
        if b.max_alloc_ratio && alloc_ratio > b.max_alloc_ratio
          decided[b] = [:regressed, "allocates #{format('%.2f', alloc_ratio)}x the baseline"]
        elsif lo > b.max_ratio
          decided[b] = [:regressed, nil]
        elsif hi < b.max_ratio
          decided[b] = [:pass, nil]
        end
      end

      log(opts, "  #{group.name}: round #{rounds}, #{decided.size}/#{benchmarks.size} decided")
    end

    benchmarks.map do |b|
      status, reason = decided.fetch(b, [:inconclusive, nil])
      Result.new(file: group.file, group: group.name, name: b.name, status: status, ratio: median(ratios[b]),
                 ci: median_ci(ratios[b], opts.confidence), alloc_ratio: alloc_ratio(bytes, b),
                 rounds: ratios[b].size, max_ratio: b.max_ratio, max_alloc_ratio: b.max_alloc_ratio,
                 reason: reason)
    end
  end

  def alloc_ratio(bytes, bench)
    bytes[[bench, :candidate]].fdiv([bytes[[bench, :baseline]], 1].max)
  end

  # -- Confirming -----------------------------------------------------------
  #
  # A first measurement that looks like a regression is only a suspect. The
  # JIT makes different decisions in every process, and early in a process
  # the shared core code is still being compiled and recompiled, so one group
  # can read 2-3x slow for a few seconds and then recover. A regression only
  # counts when it shows up again later in the same JVM and then twice more
  # in a fresh one. Suspects are rare, so this costs almost nothing when
  # nothing has regressed.

  # Re-measures suspects in the same JVM, after every other group has run and
  # the JIT has settled, with freshly compiled snippets.
  def recheck(results, opts, rng)
    suspects(results).each do |(file, group_name), rs|
      log(opts, "  rechecking #{rs.size} suspected regression(s) in #{group_name}")
      again = run_group(load_group(file), opts, rng, only: rs.map { |r| "#{group_name}: #{r.name}" })
      again = again.to_h { |r| [r.name, r] }
      rs.each do |r|
        next if again[r.name].status == :regressed

        r.status = :inconclusive
        r.reason = format('did not reproduce later in the same JVM (%.2fx)', again[r.name].ratio)
      end
    end
  end

  # Re-measures suspects in a fresh JVM, which rechecks them itself.
  def confirm(results, opts)
    suspects(results).each do |(file, group_name), rs|
      cmd = [RbConfig.ruby, __FILE__, '--json', '--recheck', '--no-confirm', "--seed=#{opts.seed + 1}",
             "--warmup=#{opts.warmup}", "--sample=#{opts.sample}", "--min-rounds=#{opts.min_rounds}",
             "--max-rounds=#{opts.max_rounds}", "--confidence=#{opts.confidence}",
             *rs.map { |r| "--only=#{group_name}: #{r.name}" }, file]
      log(opts, "  confirming #{rs.size} suspected regression(s) in #{group_name} in a fresh JVM")
      output = IO.popen(cmd, err: [:child, :out], &:read)
      json = output.lines.reverse.find { |l| l.start_with?('{') }

      # A crash is reported as a failure, since the snippets ran fine here.
      unless json
        rs.each { |r| r.reason = "confirmation run crashed:\n#{output}" }
        next
      end

      rerun = JSON.parse(json).fetch('results').to_h { |r| [r['name'], r] }
      rs.each do |r|
        again = rerun[r.name]
        r.confirmed = again&.fetch('status') == 'regressed'
        next if r.confirmed

        r.status = :inconclusive
        note = again ? [format('%.2fx', again['ratio']), again['reason']].compact.join('; ') : 'no result'
        r.reason = "not confirmed in a fresh JVM (#{note})"
      end
    end
  end

  def suspects(results)
    results.select { |r| r.status == :regressed }.group_by { |r| [r.file, r.group] }
  end

  # -- Reporting ------------------------------------------------------------

  def describe(r)
    ci = r.ci ? format('%.2f-%.2f', *r.ci) : 'n/a'
    line = format('%-12s %s: %s  %.2fx (CI %s, limit %.2fx, %d rounds, alloc %.2fx)',
                  r.status.to_s.upcase, r.group, r.name, r.ratio, ci, r.max_ratio, r.rounds, r.alloc_ratio)
    line += ', confirmed in a fresh JVM' if r.confirmed
    r.reason ? "#{line}\n             #{r.reason}" : line
  end

  def write_step_summary(results)
    path = ENV['GITHUB_STEP_SUMMARY'] or return
    flagged = results.reject { |r| r.status == :pass }
    File.open(path, 'a') do |f|
      f.puts "### Performance guards: #{results.count { |r| r.status == :pass }}/#{results.size} passed"
      next if flagged.empty?

      f.puts '', '| status | benchmark | ratio | limit | note |', '|---|---|--:|--:|---|'
      flagged.each do |r|
        f.puts "| #{r.status} | #{r.group}: #{r.name} | #{format('%.2fx', r.ratio)} | " \
               "#{format('%.2fx', r.max_ratio)} | #{r.reason&.lines&.first&.strip} |"
      end
    end
  end

  def log(opts, message)
    warn message if opts.verbose
  end

  # -- Main -----------------------------------------------------------------

  def parse_options(argv)
    opts = Options.new(warmup: 0.3, sample: 0.005, min_rounds: 8, max_rounds: 40, confidence: 0.99,
                       recheck: true, confirm: true, seed: Integer(ENV['PERF_GUARD_SEED'] || rand(1 << 31)),
                       shard: ENV['PERF_GUARD_SHARD'], json: false, verbose: false)
    OptionParser.new do |o|
      o.banner = 'Usage: bin/jruby bench/guard/runner.rb [options] [FILE.yml ...]'
      o.on('--warmup=SECONDS', Float, "CPU time to warm up each group (#{opts.warmup})") { opts.warmup = _1 }
      o.on('--sample=SECONDS', Float, "CPU time per sample (#{opts.sample})") { opts.sample = _1 }
      o.on('--min-rounds=N', Integer, "rounds before deciding (#{opts.min_rounds})") { opts.min_rounds = _1 }
      o.on('--max-rounds=N', Integer, "rounds before giving up (#{opts.max_rounds})") { opts.max_rounds = _1 }
      o.on('--confidence=P', Float, "confidence needed to decide (#{opts.confidence})") { opts.confidence = _1 }
      o.on('--[no-]recheck', 'recheck regressions later in the same JVM (default: yes)') { opts.recheck = _1 }
      o.on('--[no-]confirm', 'confirm regressions in a fresh JVM (default: yes)') { opts.confirm = _1 }
      o.on('--seed=N', Integer, 'seed for the order snippets run in') { opts.seed = _1 }
      o.on('--shard=I/N', 'run only the I-th of N slices of the files (1-based)') { opts.shard = _1 }
      o.on('--only=GROUP: NAME', 'run only this benchmark (repeatable)') { (opts.only ||= []) << _1 }
      o.on('--json', 'print results as JSON') { opts.json = true }
      o.on('-v', '--verbose', 'print progress') { opts.verbose = true }
    end.parse!(argv)
    opts
  end

  def select_files(argv, opts)
    files = argv.empty? ? Dir[File.join(CASES_DIR, '*.yml')].sort : argv
    return files unless opts.shard

    index, count = opts.shard.split('/').map { Integer(_1) }
    files.select.with_index { |_, i| i % count == index - 1 }
  end

  def main(argv)
    opts = parse_options(argv)
    if JRuby.runtime.instance_config.compile_mode.to_s == 'OFF'
      abort 'perf guard: the JIT is off (--dev or -X-C), so JIT regressions would go unnoticed'
    end
    files = select_files(argv, opts)
    rng = Random.new(opts.seed)
    log(opts, "perf guard: #{files.size} file(s), seed #{opts.seed}")
    started = cpu_ns

    results = files.flat_map do |file|
      run_group(load_group(file), opts, rng).each { |r| log(opts, describe(r)) }
    end

    recheck(results, opts, rng) if opts.recheck
    confirm(results, opts) if opts.confirm
    failed = results.select { |r| r.status == :regressed }

    if opts.json
      puts JSON.generate(results: results.map(&:to_h_json))
    else
      # Passing benchmarks are only listed with --verbose.
      results.reject { |r| r.status == :pass }.each { |r| puts describe(r) }
      puts format("\n%d benchmark(s): %d passed, %d inconclusive, %d regressed (%.1fs CPU, seed %d)",
                  results.size, results.count { |r| r.status == :pass },
                  results.count { |r| r.status == :inconclusive }, failed.size,
                  (cpu_ns - started) / 1e9, opts.seed)
      write_step_summary(results)
    end

    failed.empty?
  end
end

exit(PerfGuard.main(ARGV.dup)) if $PROGRAM_NAME == __FILE__
