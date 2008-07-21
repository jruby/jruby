# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'

module Foo
  attach_function('umask', :_umask, [ :int ], :int)
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
puts "FFI umask=#{Foo.umask} File.umask=#{File.umask}"
puts "Benchmark File.umask performance, 10000x"
10.times {
  puts Benchmark.measure {
    10000.times { File.umask(0777) }
  }
}
puts "Benchmark FFI umask(2) performance, 10000x"

20.times {
  puts Benchmark.measure {
    10000.times { Foo.umask(0777) }
  }
}
