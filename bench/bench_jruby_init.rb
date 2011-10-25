require 'java'
require 'benchmark'

Ruby = org.jruby.Ruby
Cfg = org.jruby.RubyInstanceConfig
System = java.lang.System

args = ARGV[1]

(ARGV[0] || 5).to_i.times do
  Benchmark.bm(20) { |bench|
    bench.report("in-process `jruby #{args}`") {
      cfg = Cfg.new
      if args
        cfg.process_arguments args.split.to_java(:string)
      end
      Ruby.newInstance(cfg)
    }
  }
end
