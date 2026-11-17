require 'benchmark/ips'

def foo_method
  __method__
end

def foo_callee
  __callee__
end

alias bar_method foo_method
alias bar_callee foo_callee

Benchmark.ips do |bm|
  bm.report("__method__ same") do |i|
    while i > 0
      i-=1
      foo_method;foo_method;foo_method;foo_method;foo_method;foo_method;foo_method;foo_method;foo_method;foo_method
    end
  end

  bm.report("__method__ different") do |i|
    while i > 0
      i-=1
      bar_method;bar_method;bar_method;bar_method;bar_method;bar_method;bar_method;bar_method;bar_method;bar_method
    end
  end


  bm.report("__callee__ same") do |i|
    while i > 0
      i-=1
      foo_callee;foo_callee;foo_callee;foo_callee;foo_callee;foo_callee;foo_callee;foo_callee;foo_callee;foo_callee
    end
  end

  bm.report("__callee__ different") do |i|
    while i > 0
      i-=1
      bar_callee;bar_callee;bar_callee;bar_callee;bar_callee;bar_callee;bar_callee;bar_callee;bar_callee;bar_callee
    end
  end
end