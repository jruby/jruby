#########################################################################
# bench_file_stat.rb
#
# Benchmark for (some of) the File::Stat instance methods.
#########################################################################
require 'benchmark'

MAX = 800000

Benchmark.bm(20) do |bench|
   bench.report("File::Stat.new"){
      MAX.times{ File::Stat.new(__FILE__) }
   }

   bench.report("File::Stat#atime"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.atime }
   }

   bench.report("File::Stat#blksize"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.blksize }
   }

   bench.report("File::Stat#blockdev?"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.blockdev? }
   }

   bench.report("File::Stat#blocks"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.blocks }
   }

   bench.report("File::Stat#chardev?"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.chardev? }
   }

   bench.report("File::Stat#ctime"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.ctime }
   }

   bench.report("File::Stat#dev"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.dev }
   }

   bench.report("File::Stat#directory?"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.directory? }
   }

   bench.report("File::Stat#gid"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.gid }
   }

   bench.report("File::Stat#ino"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.ino }
   }

   bench.report("File::Stat#nlink"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.nlink }
   }
   
   bench.report("File::Stat#rdev"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.rdev }
   }

   bench.report("File::Stat#size"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.size }
   }

   bench.report("File::Stat#uid"){
      s = File::Stat.new(__FILE__)
      MAX.times{ s.uid }
   }
end
