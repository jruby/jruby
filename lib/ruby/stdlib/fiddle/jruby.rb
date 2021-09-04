# This is part of JRuby's FFI-based fiddle implementation.
# It should be maintained as part of the JRuby repository, as it has no
# equivalent file in CRuby.

require 'ffi'

module Fiddle
  def self.malloc(size)
    Fiddle::Pointer.malloc(size)
  end

  def self.free(ptr)
    Fiddle::Pointer.__freefunc__(ptr)
    nil
  end

  def self.dlwrap(val)
    Pointer.to_ptr(val)
  end

  TYPE_VOID         = 0
  TYPE_VOIDP        = 1
  TYPE_CHAR         = 2
  TYPE_UCHAR        = -2
  TYPE_SHORT        = 3
  TYPE_USHORT       = -3
  TYPE_INT          = 4
  TYPE_UINT         = -4
  TYPE_LONG         = 5
  TYPE_ULONG        = -5
  TYPE_LONG_LONG    = 6
  TYPE_ULONG_LONG   = -6
  TYPE_FLOAT        = 7
  TYPE_DOUBLE       = 8
  TYPE_SSIZE_T      = 9
  TYPE_SIZE_T       = 10
  TYPE_PTRDIFF_T    = 11
  TYPE_INTPTR_T     = 12
  TYPE_UINTPTR_T    = 13

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
        TYPE_UCHAR => FFI::Type::Builtin::UCHAR,
        TYPE_SHORT => FFI::Type::Builtin::SHORT,
        TYPE_USHORT => FFI::Type::Builtin::USHORT,
        TYPE_INT => FFI::Type::Builtin::INT,
        TYPE_UINT => FFI::Type::Builtin::UINT,
        TYPE_LONG => FFI::Type::Builtin::LONG,
        TYPE_ULONG => FFI::Type::Builtin::ULONG,
        TYPE_LONG_LONG => FFI::Type::Builtin::LONG_LONG,
        TYPE_ULONG_LONG => FFI::Type::Builtin::ULONG_LONG,
        TYPE_FLOAT => FFI::Type::Builtin::FLOAT,
        TYPE_DOUBLE => FFI::Type::Builtin::DOUBLE,

        # FIXME: platform specific values?
        TYPE_SIZE_T => FFI::Type::Builtin::LONG_LONG,
        TYPE_PTRDIFF_T => FFI::Type::Builtin::LONG_LONG,
        TYPE_INTPTR_T => FFI::Type::Builtin::LONG_LONG,
        TYPE_UINTPTR_T => FFI::Type::Builtin::LONG_LONG,
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
      @name = kwargs[:name] if kwargs.kind_of? Hash
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
        Fiddle::JRuby.__ffi_type__(@ctype),
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
        cptr = Pointer.malloc(value.bytesize)
        size = value.bytesize
        cptr.ffi_ptr.put_string(0, value)
        cptr

      elsif value.is_a?(Array)
        raise NotImplementedError, "array ptr"

      elsif value.respond_to?(:to_ptr)
        ptr = value.to_ptr
        ptr.is_a?(Pointer) ? ptr : Pointer.new(ptr)

      else
        Pointer.new(value)
      end
    end

    class << self
      alias [] to_ptr
    end

    def []=(*args, value)
      if args.size == 2
        if value.is_a?(Integer)
          value = self.class.new(value)
        end
        if value.is_a?(Fiddle::Pointer)
          value = value.to_str(args[1])
        end

        @ffi_ptr.put_bytes(args[0], value, 0, args[1])
      elsif args.size == 1
        if value.is_a?(Fiddle::Pointer)
          value = value.to_str(args[0] + 1)
        else
          value = value.chr
        end

        @ffi_ptr.put_bytes(args[0], value, 0, 1)
      end
    rescue FFI::NullPointerError
      raise DLError.new("NULL pointer access")
    end

    def initialize(addr, size = nil, free = nil)
      ptr = if addr.is_a?(FFI::Pointer)
              addr

            elsif addr.is_a?(Integer)
              FFI::Pointer.new(addr)

            elsif addr.respond_to?(:to_ptr)
              fiddle_ptr = addr.to_ptr
              if fiddle_ptr.is_a?(Pointer)
                fiddle_ptr.ffi_ptr
              elsif fiddle_ptr.is_a?(FFI::AutoPointer)
                addr.ffi_ptr
              elsif fiddle_ptr.is_a?(FFI::Pointer)
                fiddle_ptr
              else
                raise DLError.new("to_ptr should return a Fiddle::Pointer object, was #{fiddle_ptr.class}")
              end
            elsif addr.is_a?(IO)
              raise NotImplementedError, "IO ptr isn't supported"
            end

      @size = size ? size : ptr.size
      @free = free || 0
      @ffi_ptr = free.nil? ? ptr : FFI::AutoPointer.new(ptr, self.class.__freefunc__(free))
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

    module LibC
      extend FFI::Library
      ffi_lib FFI::Library::LIBC
      MALLOC = attach_function :malloc, [ :size_t ], :pointer
      REALLOC = attach_function :realloc, [ :pointer, :size_t ], :pointer
      FREE = attach_function :free, [ :pointer ], :void
    end

    def self.malloc(size, free = nil)
      self.new(LibC.malloc(size), size, free ? free : LibC::FREE)
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

    def free
      @free == LibC::FREE ? nil : @free
    end

    def free=(free)
      raise NotImplementedError, "free= isn't supported"
    end

    def size=(size)
      @size = size
    end

    def [](index, length = nil)
      if length
        ffi_ptr.get_string(index, length)
      else
        ffi_ptr.get_string(index, index + 1).ord
      end
    rescue FFI::NullPointerError
      raise DLError.new("NULL pointer dereference")
    end

    def to_i
      ffi_ptr.to_i
    end
    alias to_int to_i

    # without \0
    def to_s(len = nil)
      if len
        ffi_ptr.get_string(0, len)
      else
        ffi_ptr.get_string(0)
      end
    rescue FFI::NullPointerError
      raise DLError.new("NULL pointer access")
    end

    def to_str(len = nil)
      if len
        ffi_ptr.read_string(len)
      else
        ffi_ptr.read_string(@size)
      end
    rescue FFI::NullPointerError
      raise DLError.new("NULL pointer access")
    end

    def to_value
      raise NotImplementedError, "to_value isn't supported"
    end

    def inspect
      "#<#{self.class.name} ptr=#{to_i.to_s(16)} size=#{@size} free=#{@free.inspect}>"
    end

    def +(delta)
      self.class.new(to_i + delta, @size - delta)
    end

    def -(delta)
      self.class.new(to_i - delta, @size + delta)
    end

    def <=>(other)
      return unless other.is_a?(Pointer)
      diff = self.to_i - other.to_i
      return 0 if diff == 0
      diff > 0 ? 1 : -1
    end

    def eql?(other)
      return unless other.is_a?(Pointer)
      self.to_i == other.to_i
    end

    def ==(other)
      eql?(other)
    end

    def ptr
      Pointer.new(ffi_ptr.get_pointer(0))
    end
    
    def +@
      ptr
    end

    def -@
      ref
    end

    def ref
      cptr = Pointer.malloc(FFI::Type::POINTER.size)
      cptr.ffi_ptr.put_pointer(0, ffi_ptr)
      cptr
    end
  end

  class Handle
    RTLD_GLOBAL = FFI::DynamicLibrary::RTLD_GLOBAL
    RTLD_LAZY = FFI::DynamicLibrary::RTLD_LAZY
    RTLD_NOW = FFI::DynamicLibrary::RTLD_NOW

    def initialize(libname = nil, flags = RTLD_LAZY | RTLD_GLOBAL)
      @lib = FFI::DynamicLibrary.open(libname, flags) rescue LoadError
      raise DLError.new("Could not open #{libname}") unless @lib

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

  RUBY_FREE = Fiddle::Pointer::LibC::FREE.address
  NULL = Fiddle::Pointer.new(0)

  ALIGN_VOIDP       = Fiddle::JRuby::FFITypes[TYPE_VOIDP].alignment
  ALIGN_CHAR        = Fiddle::JRuby::FFITypes[TYPE_CHAR].alignment
  ALIGN_SHORT       = Fiddle::JRuby::FFITypes[TYPE_SHORT].alignment
  ALIGN_INT         = Fiddle::JRuby::FFITypes[TYPE_INT].alignment
  ALIGN_LONG        = Fiddle::JRuby::FFITypes[TYPE_LONG].alignment
  ALIGN_LONG_LONG   = Fiddle::JRuby::FFITypes[TYPE_LONG_LONG].alignment
  ALIGN_FLOAT       = Fiddle::JRuby::FFITypes[TYPE_FLOAT].alignment
  ALIGN_DOUBLE      = Fiddle::JRuby::FFITypes[TYPE_DOUBLE].alignment
  ALIGN_SIZE_T      = Fiddle::JRuby::FFITypes[TYPE_SIZE_T].alignment
  ALIGN_SSIZE_T     = ALIGN_SIZE_T
  ALIGN_PTRDIFF_T   = Fiddle::JRuby::FFITypes[TYPE_PTRDIFF_T].alignment
  ALIGN_INTPTR_T    = Fiddle::JRuby::FFITypes[TYPE_INTPTR_T].alignment
  ALIGN_UINTPTR_T   = Fiddle::JRuby::FFITypes[TYPE_UINTPTR_T].alignment

  SIZEOF_VOIDP       = Fiddle::JRuby::FFITypes[TYPE_VOIDP].size
  SIZEOF_CHAR        = Fiddle::JRuby::FFITypes[TYPE_CHAR].size
  SIZEOF_SHORT       = Fiddle::JRuby::FFITypes[TYPE_SHORT].size
  SIZEOF_INT         = Fiddle::JRuby::FFITypes[TYPE_INT].size
  SIZEOF_LONG        = Fiddle::JRuby::FFITypes[TYPE_LONG].size
  SIZEOF_LONG_LONG   = Fiddle::JRuby::FFITypes[TYPE_LONG_LONG].size
  SIZEOF_FLOAT       = Fiddle::JRuby::FFITypes[TYPE_FLOAT].size
  SIZEOF_DOUBLE      = Fiddle::JRuby::FFITypes[TYPE_DOUBLE].size
  SIZEOF_SIZE_T      = Fiddle::JRuby::FFITypes[TYPE_SIZE_T].size
  SIZEOF_SSIZE_T     = SIZEOF_SIZE_T
  SIZEOF_PTRDIFF_T   = Fiddle::JRuby::FFITypes[TYPE_PTRDIFF_T].size
  SIZEOF_INTPTR_T    = Fiddle::JRuby::FFITypes[TYPE_INTPTR_T].size
  SIZEOF_UINTPTR_T   = Fiddle::JRuby::FFITypes[TYPE_UINTPTR_T].size
end
