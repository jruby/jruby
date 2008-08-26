require 'ffi'
module JRuby
  module FFI
    class Buffer
      def self.__calc_size(type, count = nil)
        size = if type.kind_of? Fixnum
          type
        elsif type.kind_of? Symbol
          JRuby::FFI.type_size(type)
        elsif type.kind_of? JRuby::FFI::BaseStruct
          type.size
        else
          raise ArgumentError, "Invalid size type"
        end
        size * (count ? count : 1)
      end
      def self.new(size, count=nil, clear=true)
        self.__alloc_inout(self.__calc_size(size, count), clear)
      end
      def self.alloc_in(size, count=nil, clear=true)
        self.__alloc_in(self.__calc_size(size, count), clear)
      end
      def self.alloc_out(size, count=nil, clear=true)
        self.__alloc_out(self.__calc_size(size, count), clear)
      end
      def self.alloc_inout(size, count=nil, clear=true)
        self.__alloc_inout(self.__calc_size(size, count), clear)
      end
    end
  end
end

