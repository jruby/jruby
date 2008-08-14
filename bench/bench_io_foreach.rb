require 'benchmark'

MAX  = 1000
BLOCKSIZE = 16 * 1024
LINES = 10000
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   LINES.times{ |n|
      fh.puts "This is line: #{n}"
   }
}
stat = File.stat(FILE)
(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |x|
    x.report('IO.foreach(file)'){
      MAX.times{ IO.foreach(FILE){} }
    }

  end
end
File.delete(FILE) if File.exists?(FILE)

