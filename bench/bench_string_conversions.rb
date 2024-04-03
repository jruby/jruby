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

SYM_FSTRING = "string"
SYM_NORMAL = +SYM_FSTRING
INT_FSTRING = "1000"
INT_NORMAL = +INT_FSTRING
FLO_FSTRING = "1000.0"
FLO_NORMAL = +FLO_FSTRING

Benchmark.ips do |bm|
  bm.report("intern normal") do |i|
    while i > 0
      intern(SYM_NORMAL)
      i-=1
    end
  end

  bm.report("intern fstring") do |i|
    while i > 0
      intern(SYM_FSTRING)
      i-=1
    end
  end
  
  bm.report("to_i normal") do |i|
    while i > 0
      to_i(INT_NORMAL)
      i-=1
    end
  end

  bm.report("to_i fstring") do |i|
    while i > 0
      to_i(INT_FSTRING)
      i-=1
    end
  end

  bm.report("to_f normal") do |i|
    while i > 0
      to_f(FLO_NORMAL)
      i-=1
    end
  end

  bm.report("to_f fstring") do |i|
    while i > 0
      to_f(FLO_FSTRING)
      i-=1
    end
  end
end