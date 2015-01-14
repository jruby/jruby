require 'ffi'

module Fiddle
  TYPE_VOID         = 0
  TYPE_VOIDP        = 1
  TYPE_CHAR         = 2
  TYPE_SHORT        = 3
  TYPE_INT          = 4
  TYPE_LONG         = 5
  TYPE_LONG_LONG    = 6
  TYPE_FLOAT        = 7
  TYPE_DOUBLE       = 8

  WINDOWS = FFI::Platform.windows?

  module JRuby
    FFITypes = {
        'c' => FFI::Type::INT8,
        'h' => FFI::Type::INT16,
        'i' => FFI::Type::INT32,
        'l' => FFI::Type::LONG,
        'f' => FFI::Type::FLOAT32,
        'd' => FFI::Type::FLOAT64,
        'p' => FFI::Type::POINTER,
        's' => FFI::Type::STRING,

        TYPE_VOID => FFI::Type::Builtin::VOID,
        TYPE_VOIDP => FFI::Type::Builtin::POINTER,
        TYPE_CHAR => FFI::Type::Builtin::CHAR,
        TYPE_SHORT => FFI::Type::Builtin::SHORT,
        TYPE_INT => FFI::Type::Builtin::INT,
        TYPE_LONG => FFI::Type::Builtin::LONG,
        TYPE_LONG_LONG => FFI::Type::Builtin::LONG_LONG,
        TYPE_FLOAT => FFI::Type::Builtin::FLOAT,
        TYPE_DOUBLE => FFI::Type::Builtin::DOUBLE,
    }

    def self.__ffi_type__(dl_type)
      ffi_type = FFITypes[dl_type]
      ffi_type = FFITypes[-dl_type] if ffi_type.nil? && dl_type.is_a?(Integer) && dl_type < 0
      raise TypeError.new("cannot convert #{dl_type} to ffi") unless ffi_type
      ffi_type
    end
  end

  class Function
    DEFAULT = "default"
    STDCALL = "stdcall"

    def initialize(ptr, args, return_type, abi = DEFAULT)
      @ptr, @args, @return_type, @abi = ptr, args, return_type, abi
      raise TypeError.new "invalid return type" unless return_type.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)
      
      @function = FFI::Function.new(
        Fiddle::JRuby::__ffi_type__(@return_type),
        @args.map { |t| Fiddle::JRuby.__ffi_type__(t) },
        FFI::Pointer.new(ptr.to_i),
        :convention => @abi
      )
      @function.attach(self, "call")
    end

    # stubbed; should be overwritten by initialize's #attach call above
    def call(*args); end
  end

  class Closure
    def initialize(ret, args, abi = Function::DEFAULT)
      @ctype, @args = ret, args
      raise TypeError.new "invalid return type" unless ret.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)

      @function = FFI::Function.new(
        __ffi_type__(@ctype),
        @args.map { |t| Fiddle::JRuby.__ffi_type__(t) },
        self,
        :convention => abi
      )
    end

    def to_i
      @function.to_i
    end
  end
end