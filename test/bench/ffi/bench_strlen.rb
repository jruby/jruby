require 'benchmark'
require 'ffi'
iter = 100000
str = "test"

module LibC
  attach_foreign(:int, :strlen, [ :string ], :as => :rbxstrlen)
  jffi_attach(:int, :strlen, [ :string ], :as => :jstrlen)
end
if LibC.rbxstrlen("test") != 4
  raise ArgumentError, "FFI.rbxstrlen returned incorrect value"
end
if LibC.jstrlen("test") != 4
  raise ArgumentError, "FFI.jstrlen returned incorrect value"
end
puts "Benchmark rubinius FFI api strlen(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.rbxstrlen(str) }
  }
}

puts "Benchmark jruby FFI api strlen(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.jstrlen(str) }
  }
}
