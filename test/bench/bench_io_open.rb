require 'benchmark'

MAX  = 10000
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   1000.times{ |n|
      fh.puts "This is line: #{n}"
   }
}

Benchmark.bmbm do |x|
 10.times {
   x.report('File.open(file, "r+")'){
      MAX.times{ File.open(FILE, "r+") {} }
   }
 }
end
File.delete(FILE) if File.exists?(FILE)

