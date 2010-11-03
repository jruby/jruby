require 'benchmark'

MAX = 100
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   30000.times{ |n|
      fh.puts "This is line: #{n}"
   }
}
stat = File.stat(FILE)
f = File.new(FILE)        
Benchmark.bmbm(30) do |x|
  read_iter = stat.size / 2
  x.report("#{MAX}.times { #{read_iter}.times { f.read(1) }}") {
    MAX.times {
      f.seek(0)
      read_iter.times { f.read(1) }
    }
  }
end
f.close
File.delete(FILE) if File.exists?(FILE)

