require 'benchmark'
require 'ffi'
require 'ffi/platform'

iter = 100_000
file = "README"

module BasePosix
  def chmod(mode, path)
    if _chmod(path, mode) != 0
    end
  end

end
module Chmod
  if JRuby::FFI::Platform::IS_WINDOWS
    attach_function :_chmod, :_chmod, [ :string, :int ], :int
  else
    attach_function :chmod, :_chmod, [ :string, :int ], :int
  end
end
module RbxPosix
  extend FFI::Library
  extend BasePosix
  extend Chmod
end
module JPosix
  extend JRuby::FFI::Library
  extend BasePosix
  extend Chmod
end

puts "Benchmark FFI chmod (rubinius api) performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { RbxPosix.chmod(0622, file) }
  }
}
puts "Benchmark FFI chmod (jruby api) performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { JPosix.chmod(0622, file) }
  }
}

puts "Benchmark JRuby File.chmod performance, #{iter}x changing mode"
10.times {
  puts Benchmark.measure {
    iter.times { File.chmod(0622, file) }
  }
}
