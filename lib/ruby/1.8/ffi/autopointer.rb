module FFI
  class AutoPointer

    # call-seq:
    #   AutoPointer.new(pointer, method)     => the passed Method will be invoked at GC time
    #   AutoPointer.new(pointer, proc)       => the passed Proc will be invoked at GC time (SEE WARNING BELOW!)
    #   AutoPointer.new(pointer) { |p| ... } => the passed block will be invoked at GC time (SEE WARNING BELOW!)
    #   AutoPointer.new(pointer)             => the pointer's release() class method will be invoked at GC time
    #
    # WARNING: passing a proc _may_ cause your pointer to never be GC'd, unless you're careful to avoid trapping a reference to the pointer in the proc. See the test specs for examples.
    # WARNING: passing a block will cause your pointer to never be GC'd. This is bad.
    #
    # Please note that the safest, and therefore preferred, calling
    # idiom is to pass a Method as the second parameter. Example usage:
    #
    #   class PointerHelper
    #     def self.release(pointer)
    #       ...
    #     end
    #   end
    #
    #   p = AutoPointer.new(other_pointer, PointerHelper.method(:release))
    #
    # The above code will cause PointerHelper#release to be invoked at GC time.
    #
    # The last calling idiom (only one parameter) is generally only
    # going to be useful if you subclass AutoPointer, and override
    # release(), which by default does nothing.
    #
  end
end
