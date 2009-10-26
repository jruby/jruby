require 'benchmark'

MAX  = 1000
BLOCKSIZE = 16 * 1024
LINE_SIZE = 10000
LINES = 10
FILE = 'io_test_bench_file.txt'

File.open(FILE, 'w'){ |fh|
  LINES.times{ |n|
    LINE_SIZE.times { |t|
      fh.print "This is time: #{t} "
    }
    fh.puts
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

