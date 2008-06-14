require 'benchmark'

MAX  = 10000
IOREPS = 100000
BLOCKSIZE = 16 * 1024
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
   30000.times{ |n|
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
      x.report("#{read_iter}.times { f.read(#{size}) }") {
        MAX.times{ 
          f.seek(0)
          read_iter.times { f.read(size) }
        }
      }
      f.close
    }
    x.report('IO.read(file)'){
      MAX.times{ 
        buf = IO.read(FILE) 
        if buf.length < stat.size
          raise "Incorrect size returned by IO.read() #{buf.length} != #{stat.size}"
        end
      }
    }
    
    buf_sizes.each do |size|
      x.report("IO.read(file, #{size})"){
        IOREPS.times{ 
	  buf = IO.read(FILE, size) 
        }
      }
      x.report("IO.read(file, #{size}, 20)"){
        IOREPS.times{ IO.read(FILE, size, 20) }
      }
    end
    
    x.report('IO.foreach(file)'){
      MAX.times{ IO.foreach(FILE){} }
    }
  end
end
File.delete(FILE) if File.exists?(FILE)

