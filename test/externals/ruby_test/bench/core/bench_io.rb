########################################################################
# bench_io.rb
#
# A series of benchmarks for the IO class.
########################################################################
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

   # Instance Methods
   x.report('IO#<<'){
      io = File.open('append_bench.txt', 'a')
      MAX.times{ io << "hello" << "world" << "\n" }
      io.close
      File.delete('append_bench.txt')
   }

   x.report('IO#each'){
      io = File.open(FILE)
      MAX.times{ io.each{} }
      io.close
   }

   x.report('IO#each_byte'){
      io = File.open(FILE)
      MAX.times{ io.each{} }
      io.close
   }

   x.report('IO#fileno'){
      io = File.open(FILE)
      MAX.times{ io.fileno }
      io.close
   }

   x.report('IO#gets'){
      io = File.open(FILE)
      MAX.times{ io.gets }
      io.close
   }

   x.report('IO#isatty'){
      io = File.open(FILE)
      MAX.times{ io.isatty }
      io.close
   }

   x.report('IO#lineno'){
      io = File.open(FILE)
      MAX.times{ io.lineno }
      io.close
   }

   x.report('IO#lineno='){
      io = File.open(FILE)
      MAX.times{ io.lineno = 99 }
      io.close
   }

   x.report('IO#pos'){
      io = File.open(FILE)
      MAX.times{ io.pos }
      io.close
   }

   x.report('IO#pos='){
      io = File.open(FILE)
      MAX.times{ io.pos = 99 }
      io.close
   }

   x.report('IO#print'){
      file = 'print_bench_test.txt'
      io = File.open(file, 'w')
      MAX.times{ io.print "hello\n" }
      io.close
      File.delete(file)
   }

   x.report('IO#puts'){
      file = 'puts_bench_test.txt'
      io = File.open(file, 'w')
      MAX.times{ io.print "hello\n" }
      io.close
      File.delete(file)
   }

   x.report('IO#read'){
      io = File.open(FILE)
      MAX.times{ io.read }
      io.close
   }

   x.report('IO#readlines'){
      io = File.open(FILE)
      MAX.times{ io.readlines }
      io.close
   }

   x.report('IO#rewind'){
      io = File.open(FILE)
      MAX.times{ io.rewind }
      io.close
   }

   x.report('IO#seek'){
      io = File.open(FILE)
      MAX.times{ io.seek(-13, IO::SEEK_END) }
      io.close
   }

   x.report('IO#stat'){
      io = File.open(FILE)
      MAX.times{ io.stat }
      io.close
   }

   x.report('IO#sync'){
      io = File.open(FILE)
      MAX.times{ io.sync }
      io.close
   }

   x.report('IO#sysread(1000) + rewind'){
      io = File.open(FILE)
      MAX.times{
         io.sysread(1000)
         io.rewind
      }
      io.close
   }

   x.report('IO#syswrite'){
      file = 'syswrite_bench_test.txt'
      io = File.open(file, 'w')
      MAX.times{ io.syswrite "hello\n" }
      io.close
      File.delete(file)
   }

   x.report('IO#write'){
      file = 'write_bench_test.txt'
      io = File.open(file, 'w')
      MAX.times{ io.write "hello\n" }
      io.close
      File.delete(file)
   }
end

File.delete(FILE) if File.exists?(FILE)
