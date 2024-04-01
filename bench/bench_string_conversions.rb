# frozen-string-literal: true

require 'benchmark/ips'

def intern(a)
  a.intern
end

def to_i(a)
  a.to_i
end

def to_f(a)
  a.to_f
end

FSTRING = "string"
NORMAL = +FSTRING

Benchmark.ips do |bm|
  bm.report("intern normal") do |i|
    while i > 0
      intern(NORMAL)
      i-=1
    end
  end

  bm.report("intern fstring") do |i|
    while i > 0
      intern(FSTRING)
      i-=1
    end
  end
  
  bm.report("to_i normal") do |i|
    while i > 0
      to_i(NORMAL)
      i-=1
    end
  end

  bm.report("to_i fstring") do |i|
    while i > 0
      to_i(FSTRING)
      i-=1
    end
  end

  bm.report("to_f normal") do |i|
    while i > 0
      to_f(NORMAL)
      i-=1
    end
  end

  bm.report("to_f fstring") do |i|
    while i > 0
      to_f(FSTRING)
      i-=1
    end
  end
end