require 'benchmark'

TIMES = (ARGV[0] || 1).to_i

TIMES.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, 10m times") do
      ell = nil
      10_000_000.times { ell }
    end
    bm.report("10m lambda.call no args") do
      ell = lambda {self}
      10_000_000.times { ell.call }
    end
    bm.report("10m lambda.call one arg") do
      ell = lambda {|a| self}
      10_000_000.times { ell.call(self) }
    end
    bm.report("10m lambda.call two args") do
      ell = lambda {|a,b| self}
      10_000_000.times { ell.call(self, self) }
    end
    bm.report("10m lambda.call three args") do
      ell = lambda {|a,b,c| self}
      10_000_000.times { ell.call(self, self, self) }
    end
    bm.report("10m lambda.call four args") do
      ell = lambda {|a,b,c,d| self}
      10_000_000.times { ell.call(self,self,self,self) }
    end
  end
end
