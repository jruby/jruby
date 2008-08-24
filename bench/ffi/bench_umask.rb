require 'benchmark'
require 'ffi'

iter = 100000
module Posix
  extend FFI::Library
  attach_function 'umask', [ :int ], :int
end
module NativeFile
  extend FFI::Library
  # Attaching the function to this module is about 10% faster than calling Posix.umask
  if JRuby::FFI::Platform::IS_WINDOWS
    attach_function '_umask', :_umask, [ :int ], :int
  else
    attach_function 'umask', :_umask, [ :int ], :int
  end
  def self.umask(mask = nil)
    if mask
      _umask(mask)
    else
      old = _umask(0)
      _umask(old)
      old
    end
  end
end
puts "FFI umask=#{NativeFile.umask} File.umask=#{File.umask}"
puts "Benchmark File.umask performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { File.umask(0777) }
  }
}
puts "Benchmark FFI File.umask performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { NativeFile.umask(0777) }
  }
}
puts "Benchmark FFI Posix umask performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.umask(0777) }
  }
}
