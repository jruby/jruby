module JRuby
  module FFI
    class MemoryPointer
      # call-seq:
      #   MemoryPointer.new(num) => MemoryPointer instance of <i>num</i> bytes
      #   MemoryPointer.new(sym) => MemoryPointer instance with number
      #                             of bytes need by FFI type <i>sym</i>
      #   MemoryPointer.new(obj) => MemoryPointer instance with number
      #                             of <i>obj.size</i> bytes
      #   MemoryPointer.new(sym, count) => MemoryPointer instance with number
      #                             of bytes need by length-<i>count</i> array
      #                             of FFI type <i>sym</i>
      #   MemoryPointer.new(obj, count) => MemoryPointer instance with number
      #                             of bytes need by length-<i>count</i> array
      #                             of <i>obj.size</i> bytes
      #   MemoryPointer.new(arg) { |p| ... }
      #
      # Both forms create a MemoryPointer instance. The number of bytes to
      # allocate is either specified directly or by passing an FFI type, which
      # specifies the number of bytes needed for that type.
      #
      # The form without a block returns the MemoryPointer instance. The form
      # with a block yields the MemoryPointer instance and frees the memory
      # when the block returns. The value returned is the value of the block.

      def self.new(type, count=nil, clear=true)
        if type.kind_of? Fixnum
          size = type
        elsif type.kind_of? Symbol
          size = JRuby::FFI.type_size(type)
        else
          size = type.size
        end
        total = count ? size * count : size
        ptr = self.allocateDirect(total, clear)
        ptr.type_size = size
        if block_given?
          yield ptr
        else
          ptr
        end
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
      # Write +obj+ as a C int at the memory pointed to.
      def write_int(obj)
        put_int32(0, obj)
      end

      # Read a C int from the memory pointed to.
      def read_int
        get_int32(0)
      end

      # Write +obj+ as a C long at the memory pointed to.
      def write_long(obj)
        put_long(0, obj)
      end

      # Read a C long from the memory pointed to.
      def read_long
        get_long(0)
      end
      def read_string(len=nil)
        if len
          get_buffer(0, len)
        else
          get_string(0)
        end
      end

      def write_string(str, len=nil)
        len = str.size unless len
        # Write the string data without NUL termination
        put_buffer(0, str, len)
      end
      def read_array_of_type(type, reader, length)
        ary = []
        size = FFI.type_size(type)
        tmp = self
        length.times {
          ary << tmp.send(reader)
          tmp += size
        }
        ary
      end

      def write_array_of_type(type, writer, ary)
        size = FFI.type_size(type)
        tmp = self
        ary.each {|i|
          tmp.send(writer, i)
          tmp += size
        }
        self
      end
      def read_array_of_int(length)
        get_array_of_int32(0, length)
      end

      def write_array_of_int(ary)
        put_array_of_int32(0, ary)
      end

      def read_array_of_long(length)
        get_array_of_long(0, length)
      end

      def write_array_of_long(ary)
        put_array_of_long(0, ary)
      end
    end
  end
end
