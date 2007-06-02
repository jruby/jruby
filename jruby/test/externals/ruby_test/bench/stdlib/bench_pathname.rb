##################################################
# bench_pathname.rb
#
# Benchmark suite for the Pathname class.
##################################################
require "benchmark"
require "pathname"

MAX = 100000

Benchmark.bm(20) do |x|
   x.report("Pathname#+"){
      path1 = Pathname.new("/usr/local")
      path2 = Pathname.new("bin")
      MAX.times{ path1 + path2 }
   }
   
   x.report("Pathname#cleanpath"){
      path = Pathname.new("/foo/../bar/./baz")
      MAX.times{ path.cleanpath }
   }
end