#######################################################################
# bench_dir.rb
#
# A series of benchmarks for the Dir class.
#######################################################################
require "benchmark"

MAX = 40000
PWD = Dir.pwd
Dir.rmdir('test_dir') if File.exists?('test_dir')

Benchmark.bm(30) do |x|
   x.report("Dir.chdir"){
      MAX.times{ Dir.chdir(PWD) }
   }

   x.report("Dir.chdir{}"){
      MAX.times{ Dir.chdir(PWD){} }
   }

   x.report("Dir.mkdir & Dir.delete"){
      MAX.times{ |n|
         Dir.mkdir('test_dir')
         Dir.delete('test_dir')
      }
   }

   x.report("Dir.entries"){
      MAX.times{ Dir.entries(PWD) }
   }

   x.report("Dir.foreach"){
      MAX.times{ Dir.foreach(PWD){} }
   }

   x.report("Dir.getwd"){
      MAX.times{ Dir.getwd }
   }

   x.report("Dir.glob('*')"){
      MAX.times{ Dir["*"] } 
   }
   
   x.report("Dir.glob('**/*')"){
      MAX.times{ Dir["**/*"] } 
   }   

   x.report("Dir.new + Dir#close"){
      MAX.times{
         Dir.new(PWD).close
      }
   }

   x.report("Dir.open"){
      MAX.times{ Dir.open(PWD){} }
   }
   
   x.report("Dir#each"){
      dir = Dir.new(PWD)
      MAX.times{ dir.each{} }
      dir.close
   }
   
   x.report("Dir#path"){
      dir = Dir.new(PWD)
      MAX.times{ dir.path }
      dir.close
   }
   
   x.report("Dir#read + Dir#pos"){
      dir = Dir.new(PWD)
      MAX.times{
         dir.read
         dir.pos
      }
      dir.close
   }
            
   x.report("Dir#read + Dir#pos="){
      dir = Dir.new(PWD)
      MAX.times{
         5.times{ dir.read }
         dir.pos = 2
      }
      dir.close
   }
         
   x.report("Dir#read + Dir#rewind"){
      dir = Dir.new(PWD)
      MAX.times{      
         5.times{ dir.read }
         dir.rewind
      }
      dir.close
   } 
end
