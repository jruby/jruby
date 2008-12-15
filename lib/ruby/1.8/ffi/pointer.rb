module FFI
  class Pointer
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
          __get_buffer(0, len)
        else
          get_string(0)
        end
      end

      def write_string(str, len=nil)
        len = str.size unless len
        # Write the string data without NUL termination
        __put_buffer(0, str, len)
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
      def read_pointer
        get_pointer(0)
      end
  end
end