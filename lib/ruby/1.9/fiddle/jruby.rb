require 'ffi'

module Fiddle
  TYPE_VOID         = FFI::Type::Builtin::VOID
  TYPE_VOIDP        = FFI::Type::Builtin::POINTER
  TYPE_CHAR         = FFI::Type::Builtin::CHAR
  TYPE_SHORT        = FFI::Type::Builtin::SHORT
  TYPE_INT          = FFI::Type::Builtin::INT
  TYPE_LONG         = FFI::Type::Builtin::LONG
  TYPE_LONG_LONG    = FFI::Type::Builtin::LONG_LONG
  TYPE_FLOAT        = FFI::Type::Builtin::FLOAT
  TYPE_DOUBLE       = FFI::Type::Builtin::DOUBLE

  WINDOWS = FFI::Platform.windows?

  class Function
    DEFAULT = "default"
    STDCALL = "stdcall"

    def initialize(ptr, args, return_type, abi = DEFAULT)
      @ptr, @args, @return_type, @abi = ptr, args, return_type, abi

      @function = FFI::Function.new(
        @return_type,
        @args,
        FFI::Pointer.new(@ptr.to_i),
        :convention => @abi
      )
    end

    def call(*args)
      result = @function.call(*args)

      result
    end
  end

  class Closure
    def initialize(ret, args, abi = Function::DEFAULT)
      @ctype, @args = ret, args

      @function = FFI::Function.new(
        @ctype,
        @args,
        self,
        :convention => abi
      )
    end

    def to_i
      @function.to_i
    end
  end
end