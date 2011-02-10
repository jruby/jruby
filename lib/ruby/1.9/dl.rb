warn "DL: This is only a partial implementation, and it's likely broken" if $VERBOSE

require 'ffi'

class String
  def dl_ptr
    if !defined?(@dl_ptr) || @dl_ptr.ffi_ptr.read_string != self
      @dl_ptr = DL::CPtr.new(FFI::MemoryPointer.from_string(self))
    end
  end
end

module DL
  puts "Loading DL module"
  class CPtr
    attr_reader :ffi_ptr
    extend FFI::DataConverter
    native_type FFI::Type::Builtin::POINTER

    NULL = CPtr.new(FFI::Pointer::NULL, 0, 0)

    def self.to_native(value, ctx)
      if value.is_a?(CPtr)
        value.ffi_ptr

      elsif value.is_a?(Integer)
        FFI::Pointer.new(value)

      elsif value.is_a?(::String)
        value
      end
    end

    def self.from_native(value, ctx)
      self.new(value)
    end

    def self.to_ptr(value)
      if value.is_a?(String)
        CPtr.new(FFI::MemoryPointer.from_string(value))

      elsif value.respond_to?(:to_ptr)
        ptr = value.to_ptr
        ptr.is_a?(CPtr) ? ptr : CPtr.new(ptr)

      else
        CPtr.new(value)
      end
    end

    class << self
      alias [] to_ptr
    end

    def initialize(addr, size = nil, free = nil)
      

      if addr.is_a?(FFI::Pointer)
        @ffi_ptr = addr

      elsif addr.is_a?(Integer)
        @ffi_ptr = FFI::Pointer.new(addr)
      end
      @size = size ? size : @ffi_ptr.size
      @free = free
    end

    def self.malloc(size, free = nil)
      self.new(FFI::MemoryPointer.new(size))
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

    def [](index, length = nil)
      if length
        ffi_ptr.get_string(index, length)
      else
        ffi_ptr.get_int(index)
      end
    end

    def to_i
      ffi_ptr.to_i
    end
    alias to_int to_i

    def to_str(len = nil)
      if len
        ffi_ptr.get_string(0, len)
      else
        ffi_ptr.get_string(0)
      end
    end
    alias to_s to_str

    def inspect
      "#<#{self.class.name} ptr=:#{ffi_ptr.address.to_s(16)} size=#{@size} free=#{free_func.address}>"
    end

    def +(delta)
      self.class.new(ffi_ptr + delta, @size - delta)
    end

    def -(delta)
      self.class.new(ffi_ptr - delta, @size + delta)
    end

    def ptr
      CPtr.new(ffi_ptr.get_pointer(0))
    end

    def ref
      mp = FFI::MemoryPointer.new(FFI::Type::POINTER, 1)
      mp.put_pointer(0, ffi_ptr)
      CPtr.new(mp)
    end
  end

  NULL = CPtr.new(FFI::Pointer::NULL, 0, 0)

  TYPE_VOID         = FFI::Type::Builtin::VOID
  TYPE_VOIDP        = FFI::Type::Mapped.new(CPtr)
  TYPE_CHAR         = FFI::Type::Builtin::CHAR
  TYPE_SHORT        = FFI::Type::Builtin::SHORT
  TYPE_INT          = FFI::Type::Builtin::INT
  TYPE_LONG         = FFI::Type::Builtin::LONG
  TYPE_LONG_LONG    = FFI::Type::Builtin::LONG_LONG
  TYPE_FLOAT        = FFI::Type::Builtin::FLOAT
  TYPE_DOUBLE       = FFI::Type::Builtin::DOUBLE

  ALIGN_VOIDP       = FFI::Type::Builtin::POINTER.alignment
  ALIGN_CHAR        = FFI::Type::Builtin::CHAR.alignment
  ALIGN_SHORT       = FFI::Type::Builtin::SHORT.alignment
  ALIGN_INT         = FFI::Type::Builtin::INT.alignment
  ALIGN_LONG        = FFI::Type::Builtin::LONG.alignment
  ALIGN_LONG_LONG   = FFI::Type::Builtin::LONG_LONG.alignment
  ALIGN_FLOAT       = FFI::Type::Builtin::FLOAT.alignment
  ALIGN_DOUBLE      = FFI::Type::Builtin::DOUBLE.alignment

  SIZEOF_VOIDP       = FFI::Type::Builtin::POINTER.size
  SIZEOF_CHAR        = FFI::Type::Builtin::CHAR.size
  SIZEOF_SHORT       = FFI::Type::Builtin::SHORT.size
  SIZEOF_INT         = FFI::Type::Builtin::INT.size
  SIZEOF_LONG        = FFI::Type::Builtin::LONG.size
  SIZEOF_LONG_LONG   = FFI::Type::Builtin::LONG_LONG.size
  SIZEOF_FLOAT       = FFI::Type::Builtin::FLOAT.size
  SIZEOF_DOUBLE      = FFI::Type::Builtin::DOUBLE.size

  TypeMap = {
    '0' => TYPE_VOID,
    'C' => TYPE_CHAR,
    'H' => TYPE_SHORT,
    'I' => TYPE_INT,
    'L' => TYPE_LONG,
    'F' => TYPE_FLOAT,
    'D' => TYPE_DOUBLE,
    'S' => FFI::Type::Builtin::STRING,
    's' => TYPE_VOIDP,
    'p' => TYPE_VOIDP,
    'P' => TYPE_VOIDP,
    'c' => TYPE_VOIDP,
    'h' => TYPE_VOIDP,
    'i' => TYPE_VOIDP,
    'l' => TYPE_VOIDP,
    'f' => TYPE_VOIDP,
    'd' => TYPE_VOIDP,
  }
  
  Char2TypeName = {
    '0' => 'void',
    'C' => 'char',
    'H' => 'short',
    'I' => 'int',
    'L' => 'long',
    'F' => 'float',
    'D' => 'double',
    'S' => 'const char *',
    's' => 'char *',
    'p' => 'void *',
    'P' => 'void *',
    'c' => 'char *',
    'h' => 'short *',
    'i' => 'int *',
    'l' => 'long *',
    'f' => 'float *',
    'd' => 'double *',
    'A' => '[]',
    'a' => '[]',
  }

  FFITypes = {
    'c' => FFI::Type::INT8,
    'h' => FFI::Type::INT16,
    'i' => FFI::Type::INT32,
    'l' => FFI::Type::LONG,
    'f' => FFI::Type::FLOAT32,
    'd' => FFI::Type::FLOAT64,
    'p' => FFI::Type::Mapped.new(CPtr),
    's' => FFI::Type::STRING,
  }

  RTLD_LAZY = FFI::DynamicLibrary::RTLD_LAZY
  RTLD_GLOBAL = FFI::DynamicLibrary::RTLD_GLOBAL
  RTLD_NOW = FFI::DynamicLibrary::RTLD_NOW

  class DLError < StandardError

  end

  class DLTypeError < DLError

  end

  def self.find_type(type)
    ffi_type = TypeMap[type]
    raise DLTypeError.new("Unknown type '#{type}'") unless ffi_type
    ffi_type
  end

  def self.align(offset, align)
    mask = align - 1;
    off = offset;
    ((off & mask) != 0) ? (off & ~mask) + align : off
  end

  def self.sizeof(type)
    type = type.split(//)
    i = 0
    size = 0
    while i < type.length
      t = type[i]
      i += 1
      count = String.new
      while i < type.length && type[i] =~ /[0123456789]/
        count << type[i]
        i += 1
      end
      n = count.empty? ? 1 : count.to_i
      ffi_type = FFITypes[t.downcase]
      raise DLTypeError.new("unexpected type '#{t}'") unless ffi_type
      if t.upcase == t
        size = align(size, ffi_type.alignment) + n * ffi_type.size
      else
        size += n * ffi_type.size
      end
    end
    size
  end

  class Handle

    def initialize(libname, flags = RTLD_LAZY | RTLD_GLOBAL)
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
      raise "Closing #{self} not allowed" unless @enable_close
      @open = false
    end

    def sym(func, prototype = "0")
      raise "Closed handle" unless @open
      address = @lib.find_function(func)
      Symbol.new(address, prototype, func) if address && !address.null?
    end

    def [](func, ty = nil)
      sym(func, ty || "0")
    end

    def enable_close
      @enable_close = true
    end

    def disable_close
      @enable_close = false
    end
  end

  def self.find_return_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported return type '#{type}'" unless type =~ /[0CHILFDPS]/
    DL.find_type(type)
  end

  def self.find_param_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported parameter type '#{type}'" unless type =~ /[CHILFDPS]/
    DL.find_type(type)
  end

  class Symbol

    attr_reader :name, :proto

    def initialize(address, type = nil, name = nil)
      @address = address
      @name = name
      @proto = type
      
      rt = DL.find_return_type(type[0].chr)
      arg_types = []
      type[1..-1].each_byte { |t| arg_types << DL.find_param_type(t.chr) } if type.length > 1

      @invoker = FFI::Invoker.new(address, arg_types, rt, "default")
      
      if rt == FFI::NativeType::POINTER
        def self.call(*args)
          [ PtrData.new(@invoker.call(*args)), args ]
        end
      end
    end

    def call(*args)
      [ @invoker.call(*args), args ]
    end

    def cproto
      cproto = @proto[1..-1].split(//).map { |t| Symbol.char2type(t) }.join(', ')
      "#{Symbol.char2type(@proto[0].chr)} #{@name}(#{cproto})"
    end

    def inspect
      "#<DL::Symbol func=0x#{@address.address.to_s(16)} '#{cproto}'>"
    end

    def to_s
      cproto
    end

    def to_i
      @address.address.to_i
    end

    def self.char2type(ch)
      Char2TypeName[ch]
    end

  end


  def self.dlopen(libname)
    Handle.new(libname)
  end

  def dlopen(libname)
    DL.dlopen libname
  end

  module LibC
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    attach_function :malloc, [ :size_t ], :pointer
    attach_function :free, [ :pointer ], :void
  end

  def self.malloc(size, free = nil)
    ptr = LibC.malloc(size)
    CPtr.new(ptr, size, free)
  end
end
