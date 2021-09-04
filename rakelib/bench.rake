namespace :bench do
  desc "Run all benchmarks in bench/language several times (runs a long time)"
  task :language do
    ['client', 'server'].each do |jvm_type|
      jruby(:output => "bench.language.#{jvm_type}.interpreted.txt") do
        jvmarg :line => "-#{jvm_type}"
        arg :line => "-X-C bench/language/bench_all.rb 5"
      end
      jruby(:output => "bench.language.#{jvm_type}.jitted.txt") do
        sysproperty :key => "jruby.jit.threshold", :value => "5"
        jvmarg :line => "-#{jvm_type}"
        arg :line => "bench/language/bench_all.rb 5"
      end
      jruby(:output => "bench.language.#{jvm_type}.precompiled.txt") do
        jvmarg :line => "-#{jvm_type}"
        arg :line => "-X+C bench/language/bench_all.rb 5"
      end
    end
  end
end
