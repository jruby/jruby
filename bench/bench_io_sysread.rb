require 'benchmark'

MAX  = 100
IOREPS = 100000
BLOCKSIZE = 16 * 1024
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   300000.times{ |n|
      fh.puts "This is line: #{n}"
   }
}
stat = File.stat(FILE)
buf_sizes = (0..16).map { |i| 1 << i }
(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |x|
    # Class Methods
    buf_sizes.each { |size|
      read_iter = BLOCKSIZE / size
      read_iter = read_iter > 4096 ? 4096 : read_iter < 16 ? 16 : read_iter
      f = File.new(FILE)        
      x.report("#{read_iter}.times { f.sysread(#{size}) }") {
        MAX.times{ 
          f.seek(0)
          read_iter.times { f.sysread(size) }
        }
      }
      f.close
    }
  end
end
File.delete(FILE) if File.exists?(FILE)

