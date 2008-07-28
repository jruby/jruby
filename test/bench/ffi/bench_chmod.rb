require 'benchmark'
require 'ffi'

iter = 100000
file = "README"

module POSIX
#  attach_function 'chmod', :_chmod [ :string, :int ], :int
  attach_foreign :int, 'chmod', [ :string, :int ], { :from => "c", :as => '_rbxchmod' }
  jffi_attach :int, 'chmod', [ :string, :int ], { :from => "c", :as => '_jchmod' }
  def self.rbxchmod(mode, path)
    if _rbxchmod(path, mode) != 0
    end
  end
  def self.jchmod(mode, path)
    if _jchmod(path, mode) != 0
    end
  end
end


puts "Benchmark FFI chmod (rubinius api) performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { POSIX.rbxchmod(0622, file) }
  }
}
puts "Benchmark FFI chmod (jruby api) performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { POSIX.jchmod(0622, file) }
  }
}

puts "Benchmark JRuby File.chmod performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { File.chmod(0622, file) }
  }
}
