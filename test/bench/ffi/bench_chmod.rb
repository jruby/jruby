# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'
module POSIX
  attach_foreign :int, :chmod, [ :jstring, :int ], { :from => "c", :as => :_chmod }
  def self.chmod(mode, path)
    if _chmod(path, mode) != 0

    end
  end

end

iter = 10000
puts "Benchmark FFI chmod performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { POSIX.chmod(0622, "README") }
  }
}
puts "Benchmark JRuby File.chmod performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { File.chmod(0622, "README") }
  }
}
