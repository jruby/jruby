module FFI
  class Pointer

      def write_string(str, len=nil)
        len = str.size unless len
        # Write the string data without NUL termination
        put_bytes(0, str)
      end
      def read_array_of_type(type, reader, length)
        ary = []
        size = FFI.type_size(type)
        tmp = self
        (length - 1).times {
          ary << tmp.send(reader)
          tmp += size
        }
        ary << tmp.send(reader)
        ary
      end

      def write_array_of_type(type, writer, ary)
        size = FFI.type_size(type)
        tmp = self
        (ary.length - 1).times do |i|
          tmp.send(writer, ary[i])
          tmp += size
        end
        tmp.send(writer, ary.last)
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

      def read_array_of_pointer(length)
        get_array_of_pointer(0, length)
      end
      def write_array_of_pointer(ary)
        put_array_of_pointer(0, ary)
      end
  end
end
