# Micro-benchmark for constructing Ruby subclasses of a Java class
# (reified `<init>` split-constructor path)
#
# Exercises each strategy the split-constructor plumbing recognizes:
# - terminal literal super,
# - direct forwarding (no-arg / fixed / rest / zsuper)
# - arbitrary pre `super` work
# - post `super` continuation
# - Ruby < Ruby < Java chain
#
# Run:  bin/jruby bench/bench_java_subclass_ctor.rb [name-filter]
# Compare versions: run under each JRuby and diff the ips columns.

require 'benchmark/ips'
require 'java'

ArrayList = java.util.ArrayList

# --- control: no Ruby subclass ---
# (constructing the plain Java proxy, no split-constructor involved)

# --- subclass with no initialize (default proxy path) ---
class SubNoInit < ArrayList
end

# --- terminal literal super: cached-terminator fast path ---
class SubLiteralSuper < ArrayList
  def initialize
    super(10)
  end
end

# --- direct super forwarding, no args ---
class SubDirectNoArgs < ArrayList
  def initialize
    super()
  end
end

# --- direct forwarding of a single fixed arg ---
class SubFixedForward < ArrayList
  def initialize(n)
    super(n)
  end
end

# --- direct forwarding via rest splat ---
class SubRestForward < ArrayList
  def initialize(*args)
    super(*args)
  end
end

# --- zsuper inside (first, *rest) ---
class SubZSuper < ArrayList
  def initialize(first, *rest)
    super
  end
end

# --- arbitrary pre-super work (falls to the split interpreter) ---
class SubPreSuper < ArrayList
  def initialize(n)
    m = n + 1
    super(m)
  end
end

# --- post-super continuation (Ruby runs after the Java ctor) ---
class SubPostSuper < ArrayList
  def initialize(n)
    super(n)
    @marker = n
  end
end

# --- Ruby < Ruby < Java chain ---
class MidChain < ArrayList
  def initialize(n)
    super(n)
  end
end

class LeafChain < MidChain
  def initialize(n)
    super(n)
  end
end

puts RUBY_DESCRIPTION
puts "engine: #{defined?(JRUBY_VERSION) ? JRUBY_VERSION : 'n/a'}"

filter = ARGV[0]

Benchmark.ips do |x|
  x.config(warmup: Integer(ENV.fetch('BENCH_WARMUP', 3)), time: Integer(ENV.fetch('BENCH_TIME', 5)))

  reports = {
    'baseline ArrayList.new (no subclass)' => -> { ArrayList.new(10) },
    'subclass, no initialize' => -> { SubNoInit.new },
    'terminal literal super(10)' => -> { SubLiteralSuper.new },
    'direct super() no args' => -> { SubDirectNoArgs.new },
    'direct forward fixed super(n)' => -> { SubFixedForward.new(10) },
    'direct forward rest super(*args)' => -> { SubRestForward.new(10) },
    'zsuper (first, *rest)' => -> { SubZSuper.new(10) },
    'arbitrary pre-super work' => -> { SubPreSuper.new(10) },
    'post-super continuation' => -> { SubPostSuper.new(10) },
    'Ruby < Ruby < Java chain' => -> { LeafChain.new(10) }
  }

  reports.each do |name, blk|
    next if filter && !name.include?(filter)

    x.report(name, &blk)
  end

  x.compare!
end
