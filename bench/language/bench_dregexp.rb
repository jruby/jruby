require 'benchmark'

def bench_dregexp(bm)
  bm.report('1m x10 /abcd#{foo}ijkl/') do
    1_000_000.times do
      /abcd#{foo}ijkl/; /abcd#{foo}ijkl/
      /abcd#{foo}ijkl/; /abcd#{foo}ijkl/
      /abcd#{foo}ijkl/; /abcd#{foo}ijkl/
      /abcd#{foo}ijkl/; /abcd#{foo}ijkl/
      /abcd#{foo}ijkl/; /abcd#{foo}ijkl/
    end
  end
  bm.report('1m x10 /abcd#{foo}ijkl/u') do
    1_000_000.times do
      /abcd#{foo}ijkl/u; /abcd#{foo}ijkl/u
      /abcd#{foo}ijkl/u; /abcd#{foo}ijkl/u
      /abcd#{foo}ijkl/u; /abcd#{foo}ijkl/u
      /abcd#{foo}ijkl/u; /abcd#{foo}ijkl/u
      /abcd#{foo}ijkl/u; /abcd#{foo}ijkl/u
    end
  end
  bm.report('1m x10 /abcd#{foo}ijkl/o') do
    1_000_000.times do
      /abcd#{foo}ijkl/o; /abcd#{foo}ijkl/o
      /abcd#{foo}ijkl/o; /abcd#{foo}ijkl/o
      /abcd#{foo}ijkl/o; /abcd#{foo}ijkl/o
      /abcd#{foo}ijkl/o; /abcd#{foo}ijkl/o
      /abcd#{foo}ijkl/o; /abcd#{foo}ijkl/o
    end
  end
end

def foo; 'efgh'; end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_dregexp(bm)} }
end
