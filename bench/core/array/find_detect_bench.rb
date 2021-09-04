require 'benchmark'

def bench_array(bm)
  arr = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]

  bm.report("2m array.each, 20 Fixnum array") do
    i = 0
    while i<2_000_000
      arr.each {|e| e}
      i+=1
    end
  end

  bm.report("2m array.find(21), 20 Fixnum array") do
    i = 0
    while i<2_000_000
      arr.find {|e| e == 21}
      i+=1
    end
  end

  arr3 = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]

  bm.report("2m array.find(), 200 Fixnum array") do
    i = 0
    while i<200_000
      arr3.find {|e| e == 1000}
      i+=1
    end
  end

end

if $0 == __FILE__
  if ARGV[0]
    ARGV[0].to_i.times {
      Benchmark.bm(40) {|bm| bench_array(bm)}
 class Array; alias_method :old_each, :each; def each(*args, &block); old_each(*args, &block); end;  end

      puts "With overriden Array#each"
      Benchmark.bm(40) {|bm| bench_array(bm)}
    }
  else
    Benchmark.bmbm {|bm| bench_array(bm)}
 class Array; alias_method :old_each, :each; def each(*args, &block); old_each(*args, &block); end;  end

    puts "With overriden Array#each"
    Benchmark.bmbm {|bm| bench_array(bm)}
  end
end
