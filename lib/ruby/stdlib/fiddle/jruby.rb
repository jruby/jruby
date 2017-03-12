require 'ffi'
require 'ffi/libc'

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

  TYPE_SIZE_T       = -5
  TYPE_SSIZE_T      = 5
  TYPE_PTRDIFF_T    = 5
  TYPE_INTPTR_T     = 5
  TYPE_UINTPTR_T    = -5

  WINDOWS = FFI::Platform.windows?

  LibC = FFI::LibC
  RUBY_FREE = LibC::FREE.address

  BUILD_RUBY_PLATFORM = RUBY_PLATFORM

  def self.malloc(size)
    LibC.malloc(size)
  end

  def self.free(ptr)
    LibC.free(Pointer.to_native(ptr, nil))
  end

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

    def initialize(ptr, args, return_type, abi = DEFAULT, kwargs = nil)
      if kwargs.nil?
        if abi.kind_of? Hash
          kwargs = abi
          abi = DEFAULT
        end
      end
      @ptr, @args, @return_type, @abi = ptr, args, return_type, abi
      raise TypeError.new "invalid return type" unless return_type.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)

      @function = FFI::Function.new(
        Fiddle::JRuby::__ffi_type__(@return_type),
        @args.map { |t| Fiddle::JRuby.__ffi_type__(t) },
        FFI::Pointer.new(ptr.to_i),
        :convention => @abi
      )
      @function.attach(self, "__ffi_call__")
    end

    def call(*args)
      native_args = args.zip(@args).map{ |arg,type| make_native(arg, type) }
      ret = self.__ffi_call__(*native_args)
      make_native(ret, @return_type)
    end

    private

    def make_native(arg, type)
      return arg if type != TYPE_VOIDP
      Pointer[arg]
    end
  end

  class Closure
    def initialize(ret, args, abi = Function::DEFAULT)
      @ctype, @args = ret, args
      raise TypeError.new "invalid return type" unless ret.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)

      @function = FFI::Function.new(
        Fiddle::JRuby::__ffi_type__(@ctype),
        @args.map { |t| Fiddle::JRuby.__ffi_type__(t) },
        self,
        :convention => abi
      )
    end

    def to_i
      @function.to_i
    end
  end

  class DLError < StandardError; end

  class Pointer
    attr_reader :ffi_ptr
    extend FFI::DataConverter
    native_type FFI::Type::Builtin::POINTER

    def self.to_native(value, ctx)
      if value.is_a?(Pointer)
        value.ffi_ptr

      elsif value.is_a?(Integer)
        FFI::Pointer.new(value)

      elsif value.is_a?(String)
        value
      end
    end

    def self.from_native(value, ctx)
      self.new(value)
    end

    def self.to_ptr(value)
      if value.is_a?(String)
        cptr = Pointer.malloc(value.bytesize + 1)
        size = value.bytesize + 1
        cptr.ffi_ptr.put_string(0, value)
        cptr

      elsif value.is_a?(FFI::Pointer)
        Pointer.new(value)

      elsif value.respond_to?(:to_ptr)
        ptr = value.to_ptr
        if ptr.is_a?(Pointer)
          ptr
        elsif ptr.is_a?(FFI::Pointer)
          Pointer.new(ptr)
        else
          p value
          p ptr
          raise DLError.new('to_ptr should return a Fiddle::Pointer object')
        end

      else
        Pointer.new(value)
      end
    end

    class << self
      alias [] to_ptr
    end

    def initialize(addr, size = 0, freefunc = nil)
      ptr = if addr.is_a?(FFI::Pointer)
              addr
            else
              FFI::Pointer.new(Integer(addr))
            end

      @size = size
      self.free = freefunc
      @ffi_ptr = freefunc.nil? ? ptr : FFI::AutoPointer.new(ptr, ->(x){@__freefunc__.call(x)})
    end

    def self.__freefunc__(free)
      if free.is_a?(FFI::Function)
        free

      elsif free.is_a?(FFI::Pointer)
        free.null? ? Proc.new { |ptr| } : FFI::Function.new(:void, [ :pointer ], free)

      elsif free.is_a?(Integer)
        free == 0 ? Proc.new { |ptr| } : FFI::Function.new(:void, [ :pointer ], FFI::Pointer.new(free))

      elsif free.respond_to?(:call)
        free

      else
        raise ArgumentError.new("invalid free func")
      end
    end

    def self.malloc(size, free = nil)
      self.new(Fiddle.malloc(size), size, free)
    end

    def null?
      @ffi_ptr.null?
    end

    def to_ptr
      @ffi_ptr
    end

    def size
      defined?(@layout) ? @layout.size : @size
    end

    def size=(size)
      @size = size
    end

    def free
      return nil if @free.zero?
      Function.new(@free, [TYPE_VOIDP], TYPE_VOID)
    end

    def free=(freefunc)
      if freefunc.nil?
        @free = 0
        @__freefunc__ = Proc.new { |ptr| }
      else
        @free = Integer(freefunc)
        @__freefunc__ = self.class.__freefunc__(freefunc)
      end
    end

    def [](index, length = nil)
      if length
        ffi_ptr.get_bytes(index, length)
      else
        ffi_ptr.get_int8(index)
      end
    rescue FFI::NullPointerError
      raise DLError.new('NULL pointer dereference')
    end

    def []=(index, length = nil, value)
      if length
        if value.is_a?(Integer)
          value_str = Pointer.new(value).to_s
        else
          value_str = value.to_str
        end
        ffi_ptr.put_bytes(index, value_str, 0, [length, value_str.bytesize].min)
      else
        ffi_ptr.put_int8(index, value)
      end
    rescue FFI::NullPointerError
      raise DLError.new('NULL pointer dereference')
    end

    def to_i
      ffi_ptr.to_i
    end
    alias to_int to_i

    def to_s(len = nil)
      if len
        ffi_ptr.get_bytes(0, len)
      else
        ffi_ptr.get_string(0)
      end
    end

    def to_str(len = size)
      ffi_ptr.get_bytes(0, len)
    end

    def inspect
      "#<#{self.class.name} ptr=#{ffi_ptr.address.to_s(16)} size=#{@size} free=#{@free.to_s(16)}>"
    end

    def +(delta)
      self.class.new(ffi_ptr.address + delta, @size - delta)
    end

    def -(delta)
      self.class.new(ffi_ptr.address - delta, @size + delta)
    end

    def ==(value)
      return false unless value.is_a?(Pointer)
      self.ffi_ptr.address == value.ffi_ptr.address
    end
    alias eql? ==

    def <=>(value)
      return nil unless value.is_a?(Pointer)
      self.ffi_ptr.address <=> value.ffi_ptr.address
    end

    def ptr
      Pointer.new(ffi_ptr.get_pointer(0))
    end

    def ref
      cptr = Pointer.malloc(FFI::Type::POINTER.size)
      cptr.ffi_ptr.put_pointer(0, ffi_ptr)
      cptr
    end
  end

  NULL = Pointer.new(0, 0, 0)

  class Handle
    RTLD_GLOBAL = FFI::DynamicLibrary::RTLD_GLOBAL
    RTLD_LAZY = FFI::DynamicLibrary::RTLD_LAZY
    RTLD_NOW = FFI::DynamicLibrary::RTLD_NOW

    def initialize(libname = nil, flags = RTLD_LAZY | RTLD_GLOBAL)
      @lib = FFI::DynamicLibrary.open(libname, flags)
      raise RuntimeError, "Could not open #{libname}" unless @lib

      @open = true

      begin
        yield(self)
      ensure
        self.close
      end if block_given?
    end

    def close
      raise DLError.new("closed handle") unless @open
      @open = false
      0
    end

    def self.sym(func)
      DEFAULT.sym(func)
    end

    def sym(func)
      raise TypeError.new("invalid function name") unless func.is_a?(String)
      raise DLError.new("closed handle") unless @open
      address = @lib.find_function(func)
      raise DLError.new("unknown symbol #{func}") if address.nil? || address.null?
      address.to_i
    end

    def self.[](func)
      self.sym(func)
    end

    def [](func)
      sym(func)
    end

    def enable_close
      @enable_close = true
    end

    def close_enabled?
      @enable_close
    end

    def disable_close
      @enable_close = false
    end
  end

  ALIGN_VOIDP       = Fiddle::JRuby::FFITypes[TYPE_VOIDP].alignment
  ALIGN_CHAR        = Fiddle::JRuby::FFITypes[TYPE_CHAR].alignment
  ALIGN_SHORT       = Fiddle::JRuby::FFITypes[TYPE_SHORT].alignment
  ALIGN_INT         = Fiddle::JRuby::FFITypes[TYPE_INT].alignment
  ALIGN_LONG        = Fiddle::JRuby::FFITypes[TYPE_LONG].alignment
  ALIGN_LONG_LONG   = Fiddle::JRuby::FFITypes[TYPE_LONG_LONG].alignment
  ALIGN_FLOAT       = Fiddle::JRuby::FFITypes[TYPE_FLOAT].alignment
  ALIGN_DOUBLE      = Fiddle::JRuby::FFITypes[TYPE_DOUBLE].alignment

  SIZEOF_VOIDP       = Fiddle::JRuby::FFITypes[TYPE_VOIDP].size
  SIZEOF_CHAR        = Fiddle::JRuby::FFITypes[TYPE_CHAR].size
  SIZEOF_SHORT       = Fiddle::JRuby::FFITypes[TYPE_SHORT].size
  SIZEOF_INT         = Fiddle::JRuby::FFITypes[TYPE_INT].size
  SIZEOF_LONG        = Fiddle::JRuby::FFITypes[TYPE_LONG].size
  SIZEOF_LONG_LONG   = Fiddle::JRuby::FFITypes[TYPE_LONG_LONG].size
  SIZEOF_FLOAT       = Fiddle::JRuby::FFITypes[TYPE_FLOAT].size
  SIZEOF_DOUBLE      = Fiddle::JRuby::FFITypes[TYPE_DOUBLE].size

  ALIGN_SIZE_T       = ALIGN_VOIDP
  ALIGN_SSIZE_T      = ALIGN_VOIDP
  ALIGN_PTRDIFF_T    = ALIGN_VOIDP
  ALIGN_INTPTR_T     = ALIGN_VOIDP
  ALIGN_UINTPTR_T    = ALIGN_VOIDP

  SIZEOF_SIZE_T      = SIZEOF_VOIDP
  SIZEOF_SSIZE_T     = SIZEOF_VOIDP
  SIZEOF_PTRDIFF_T   = SIZEOF_VOIDP
  SIZEOF_INTPTR_T    = SIZEOF_VOIDP
  SIZEOF_UINTPTR_T   = SIZEOF_VOIDP
end
