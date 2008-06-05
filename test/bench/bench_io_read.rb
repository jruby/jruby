require 'benchmark'

MAX  = 10000
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   1000.times{ |n|
      fh.puts "This is line: #{n}"
   }
}

Benchmark.bm(30) do |x|
   # Class Methods
   x.report('IO.foreach(file)'){
      MAX.times{ IO.foreach(FILE){} }
   }

   x.report('IO.read(file)'){
      MAX.times{ IO.read(FILE) }
   }

   x.report('IO.read(file, 100)'){
      MAX.times{ IO.read(FILE, 100) }
   }

   x.report('IO.read(file, 100, 20)'){
      MAX.times{ IO.read(FILE, 100, 20) }
   }
end
File.delete(FILE) if File.exists?(FILE)

