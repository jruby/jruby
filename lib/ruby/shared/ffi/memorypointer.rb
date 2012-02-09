require 'ffi/pointer'
require 'ffi/types'

module FFI
  class MemoryPointer

    def self.from_string(s)
      ptr = self.new(1, s.bytesize + 1, false)
      ptr.put_string(0, s)
      ptr
    end
  end
end
