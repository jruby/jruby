require 'benchmark'

def bench_dsymbol(bm)
  bm.report("100k x10 :\"abcd\#{a += 1}ijkl\"") do
    100_000.times do
      a = 0
      :"abcd#{a += 1}ijkl"; :"abcd#{a += 1}ijkl"
      :"abcd#{a += 1}ijkl"; :"abcd#{a += 1}ijkl"
      :"abcd#{a += 1}ijkl"; :"abcd#{a += 1}ijkl"
      :"abcd#{a += 1}ijkl"; :"abcd#{a += 1}ijkl"
      :"abcd#{a += 1}ijkl"; :"abcd#{a += 1}ijkl"
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_dsymbol(bm)} }
end
