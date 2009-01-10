require 'ffi/pointer'
require 'ffi/types'

module FFI
  class MemoryPointer

    def self.from_string(s)
      ptr = self.new(s.length + 1, 1, false)
      ptr.put_string(0, s)
      ptr
    end
    # Indicates how many bytes the type that the pointer is cast as uses.
    attr_accessor :type_size

    # Access the MemoryPointer like a C array, accessing the +which+ number
    # element in memory. The position of the element is calculate from
    # +@type_size+ and +which+. A new MemoryPointer object is returned, which
    # points to the address of the element.
    #
    # Example:
    #   ptr = MemoryPointer.new(:int, 20)
    #   new_ptr = ptr[9]
    #
    # c-equiv:
    #   int *ptr = (int*)malloc(sizeof(int) * 20);
    #   int *new_ptr;
    #   new_ptr = &ptr[9];
    #
    def [](which)
      raise ArgumentError, "unknown type size" unless @type_size
      self + (which * @type_size)
    end
  end
end
