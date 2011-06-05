require 'benchmark'

def foo
  self
end

def control
  i = 0;
  while i < 10_000_000
    i += 1;
  end
end

def invoking
  i = 0;
  while i < 10_000_000
    foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
    i += 1;
  end
end

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(40) do |bm|
    bm.report "10M loop" do
      control
    end

    bm.report "10M loop calling self's foo 10 times" do
      invoking
    end
  end
end
