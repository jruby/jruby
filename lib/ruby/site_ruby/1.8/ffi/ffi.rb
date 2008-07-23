#
# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2008 JRuby project
#
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.
#
# Copyright (c) 2007, 2008 Evan Phoenix
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without 
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of the Evan Phoenix nor the names of its contributors 
#   may be used to endorse or promote products derived from this software 
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

include Java

require 'rbconfig'
require 'ffi/platform'

module JFFI
  
  import org.jruby.ext.ffi.NativeType
  FFIProvider = Java::org.jruby.ext.ffi.FFIProvider.instance
  FFIProvider.setup(self)
  #  Specialised error classes
  class TypeError < RuntimeError; end
  
  class SignatureError < RuntimeError; end
  
  class NotFoundError < RuntimeError
    def initialize(function, library)
      super("Function '#{function}' not found! (Looking in '#{library} or this process)")
    end
  end
  TypeDefs = Hash.new
  
  class << self

    def add_typedef(current, add)
      if current.kind_of? NativeType
        code = current
      else
        code = JFFI::TypeDefs[current]
        raise TypeError, "Unable to resolve type '#{current}'" unless code
      end

      JFFI::TypeDefs[add] = code
    end

    def find_type(name)
      code = JFFI::TypeDefs[name]
      raise TypeError, "Unable to resolve type '#{name}'" unless code
      return code
    end

    def create_invoker(lib, name, args, ret)
      # Ugly hack to simulate the effect of dlopen(NULL, x) - not quite correct
      lib = 'c' unless lib
      # jna & jffi need just the last part of the library name
      lib = lib[3..-1] if lib =~ /^lib/
      # Current artificial limitation based on JFFI limit
      raise JFFI::SignatureError, 'FFI functions may take max 32 arguments!' if args.size > 32

      cargs = args.map { |e| find_type(e) }
      invoker = FFIProvider.createInvoker(lib, name, find_type(ret), 
        cargs.to_java(NativeType))
      raise NotFoundError.new(name, lib) unless invoker
      return invoker
    end
    
    def create_function(lib, name, args, ret)
      invoker = create_invoker(lib, name, args, ret)
      proc { |*args| invoker.invoke(args) }
    end

  end

  # Converts a Rubinius Object
#  add_typedef TYPE_OBJECT,  :object

  # Converts a char
  add_typedef(NativeType::INT8, :char)

  # Converts an unsigned char
  add_typedef(NativeType::UINT8, :uchar)
  
  # Converts an 8 bit int
  add_typedef(NativeType::INT8, :int8)

  # Converts an unsigned char
  add_typedef(NativeType::UINT8, :uint8)

  # Converts a short
  add_typedef(NativeType::INT16, :short)

  # Converts an unsigned short
  add_typedef(NativeType::UINT16, :ushort)
  
  # Converts a 16bit int
  add_typedef(NativeType::INT16, :int16)

  # Converts an unsigned 16 bit int
  add_typedef(NativeType::UINT16, :uint16)

  # Converts an int
  add_typedef(NativeType::INT32, :int)

  # Converts an unsigned int
  add_typedef(NativeType::UINT32, :uint)
  
  # Converts a 32 bit int
  add_typedef(NativeType::INT32, :int32)

  # Converts an unsigned 16 bit int
  add_typedef(NativeType::UINT32, :uint32)

  # Converts a long
  add_typedef(NativeType::LONG, :long)

  # Converts an unsigned long
  add_typedef(NativeType::ULONG, :ulong)
  
  # Converts a 64 bit int
  add_typedef(NativeType::INT64, :int64)

  # Converts an unsigned 64 bit int
  add_typedef(NativeType::UINT64, :uint64)

  # Converts a long long
  add_typedef(NativeType::INT64, :long_long)

  # Converts an unsigned long long
  add_typedef(NativeType::UINT64, :ulong_long)

  # Converts a float
  add_typedef(NativeType::FLOAT32, :float)

  # Converts a double
  add_typedef(NativeType::FLOAT64, :double)

  # Converts a pointer to opaque data
  add_typedef(NativeType::POINTER, :pointer)

  # For when a function has no return value
  add_typedef(NativeType::VOID, :void)

  # Converts NUL-terminated C strings
  add_typedef(NativeType::STRING, :string)
  add_typedef(NativeType::STRING, :jstring)
  
  # Converts byte-arrays
  add_typedef(NativeType::BUFFER, :buffer)
  
  # Data gets copied out of native memory to ruby buffer
  add_typedef(NativeType::BUFFER_OUT, :buffer_out)
  
  # Data gets copied in to native memory from ruby buffer
  add_typedef(NativeType::BUFFER_IN, :buffer_in)
  
  # Pointer to ruby array is passed to native memory.
  # NOTE: This can only be used for functions which cannot block
  # e.g. memchr() is ok, fread() is not
  #
  add_typedef(NativeType::BUFFER_PINNED, :buffer_pinned)

  # Use for a C struct with a char [] embedded inside.
  add_typedef(NativeType::CHAR_ARRAY, :char_array)

  # Load all the platform dependent types/consts/struct members
  class Config
    CONFIG = Hash.new
    begin
      File.open(File.join(Platform::CONF_DIR, 'platform.conf'), "r") do |f|
        typedef = "rbx.platform.typedef."
        f.each_line { |line|
          if line.index(typedef) == 0
            new_type, orig_type = line.chomp.slice(typedef.length..-1).split(/\s*=\s*/)
            JFFI.add_typedef(orig_type.to_sym, new_type.to_sym)
          else
            key, value = line.chomp.split(/\s*=\s*/)
            puts "key=#{key} value=#{value}"
            CONFIG[key] = value
          end
        }
      end
    rescue Errno::ENOENT
    end
    def self.[](name)

    end
  end
  # Load all the platform dependent types
  begin
    File.open(File.join(Platform::CONF_DIR, 'types.conf'), "r") do |f|
      prefix = "rbx.platform.typedef."
      f.each_line { |line|
        if line.index(prefix) == 0
          new_type, orig_type = line.chomp.slice(prefix.length..-1).split(/\s*=\s*/)
#          puts "new type=#{new_type} orig_type=#{orig_type}"
          add_typedef(orig_type.to_sym, new_type.to_sym)
        end
      }
    end
  rescue Errno::ENOENT
  end
  
  TypeSizes = {
    1 => :char,
    2 => :short,
    4 => :int,
    8 => :long_long,
  }
  SizeTypes = {
    NativeType::INT8 => 1,
    NativeType::UINT8 => 1,
    NativeType::INT16 => 2,
    NativeType::UINT16 => 2,
    NativeType::INT32 => 4,
    NativeType::UINT32 => 4,
    NativeType::INT64 => 8,
    NativeType::UINT64 => 8,
    NativeType::FLOAT32 => 4,
    NativeType::FLOAT64 => 8,
    NativeType::LONG => JFFI::Platform::LONG_SIZE / 8,
    NativeType::ULONG => JFFI::Platform::LONG_SIZE / 8,
  }
  
  def self.size_to_type(size)
    if sz = TypeSizes[size]
      return sz
    end

    # Be like C, use int as the default type size.
    return :int
  end
  def self.type_size(type)
    if sz = SizeTypes[find_type(type)]
      return sz
    end
    raise ArgumentError, "Unknown native type"
  end
  def self.errno
    FFIProvider.getLastError
  end
  def self.set_errno(error)
    FFIProvider.setLastError(error)
  end
end
# Define MemoryPointer globally for rubinius FFI backward compatibility
MemoryPointer = JFFI::MemoryPointer

module FFI
  TypeDefs = {
    :string => JFFI::NativeType::RBXSTRING,
    :jstring => JFFI::NativeType::STRING
  }
  
  def self.find_type(type)
    FFI::TypeDefs[type] || JFFI.find_type(type)
  end
  def self.add_typedef(current, add)
    JFFI.add_typedef(current, add)
  end
  def self.create_invoker(lib, name, args, ret)
    # Ugly hack to simulate the effect of dlopen(NULL, x) - not quite correct
    lib = 'c' unless lib
    # jffi needs just the last part of the library name
    lib = lib[3..-1] if lib =~ /^lib/
    
    # Current artificial limitation based on JFFI limit
    raise JFFI::SignatureError, 'FFI functions may take max 32 arguments!' if args.size > 32

    cargs = args.map { |e| find_type(e) }
    invoker = JFFI::FFIProvider.createInvoker(lib, name, find_type(ret),
      cargs.to_java(JFFI::NativeType))
    raise NotFoundError.new(name, lib) unless invoker
    return invoker
  end
  
end

class Module

  # TODO: Rubinius does *names here and saves the array. Multiple libs?
  def set_ffi_lib(name)
    @ffi_lib = name
  end
  ##
  # Attach C function +name+ to this module.
  #
  # If you want to provide an alternate name for the module function, supply
  # it after the +name+, otherwise the C function name will be used.#
  #
  # After the +name+, the C function argument types are provided as an Array.
  #
  # The C function return type is provided last.

  def attach_function(name, a3, a4, a5=nil)
    if a5
      mname = a3
      args = a4
      ret = a5
    else
      mname = name.to_sym
      args = a3
      ret = a4
    end
    attach_foreign(ret, name, args, { :as => mname, :from => @ffi_lib })
  end
  # Create a wrapper to a function in a C-linked library that
  # exists somewhere in the system. If a specific library is
  # not given, the function is assumed to exist in the running
  # process, the Rubinius executable. The process contains many
  # linked libraries in addition to Rubinius' codebase, libc of
  # course the most prominent on the system side. The wrapper
  # method is added to the Module as a singleton method or a
  # "class method."
  #
  # The function is specified like a declaration: the first
  # argument is the type symbol for the return type (see FFI
  # documentation for types), the second argument is the name
  # of the function and the third argument is an Array of the
  # types of the function's arguments. Currently at most 6
  # arguments can be given.
  #
  #   # If you want to wrap this function:
  #   int foobar(double arg_one, const char* some_string);
  #
  #   # The arguments to #attach_foreign look like this:
  #   :int, 'foobar', [:double, :string]
  #
  # If the function is from an external library such as, say,
  # libpcre, libcurl etc. you can give the name or path of
  # the library. The fourth argument is an option hash and
  # the library name should be given in the +:from+ key of
  # the hash. The name may (and for portable code, should)
  # omit the file extension. If the extension is present,
  # it must be the correct one for the runtime platform.
  # The library is searched for in the system library paths
  # but if necessary, the full absolute or relative path can
  # be given.
  #
  # By default, the new method's name is the same as the
  # function it wraps but in some cases it is desirable to
  # change this. You can specify the method name in the +:as+
  # key of the option hash.
  def attach_foreign(ret_type, name, arg_types, opts = {})
    lib = opts[:from]

#    if lib and !lib.chomp! ".#{Rubinius::LIBSUFFIX}"
#      lib.chomp! ".#{Rubinius::ALT_LIBSUFFIX}" rescue nil     # .defined? is broken anyway
#    end

    invoker = FFI.create_invoker lib, name.to_s, arg_types, ret_type
    raise ArgumentError, "Unable to find function '#{name}' to bind to #{self.name}.#{(opts[:as] || name)}" unless invoker
    invoker.attach(self, (opts[:as] || name).to_s)
    # Return a callable version of the invoker
    return proc { |*args| invoker.invoke(args) }
  end
  def jffi_attach(ret_type, name, arg_types, opts = {})
    lib = opts[:from]

#    if lib and !lib.chomp! ".#{Rubinius::LIBSUFFIX}"
#      lib.chomp! ".#{Rubinius::ALT_LIBSUFFIX}" rescue nil     # .defined? is broken anyway
#    end

    invoker = JFFI.create_invoker lib, name.to_s, arg_types, ret_type
    raise ArgumentError, "Unable to find function '#{name}' to bind to #{self.name}.#{(opts[:as] || name)}" unless invoker
    invoker.attach(self, (opts[:as] || name).to_s)
    # Return a callable version of the invoker
    return proc { |*args| invoker.invoke(args) }
  end
end

class JFFI::MemoryPointer
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
      size = JFFI.type_size(type)
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
class JFFI::BaseStruct
  attr_reader :pointer
  
  def initialize(pointer = nil, *spec)
    if pointer then
      @pointer = pointer
    else
      @pointer = JFFI::MemoryPointer.allocateDirect size
    end

    @cspec = self.class.layout(*spec)
  end
  def self.allocate(*spec)
    self.new(JFFI::MemoryPointer.allocate(@size), *spec)
  end
  def self.allocate_direct(*spec)
    self.new(JFFI::MemoryPointer.allocateDirect(@size), *spec)
  end
  def self.size
    @size
  end
  def self.members
    @layout.members
  end
  def size
    self.class.size
  end
  def [](field)
    @cspec.get(@pointer, field)
  end
  def []=(field, val)
    @cspec.put(@pointer, field, val)
  end
  def members
    @cspec.members
  end
  def values
    @cspec.members.map { |m| self[m] }
  end
end
class JFFI::Struct < JFFI::BaseStruct
  
  def self.layout(*spec)
    
    return @layout if spec.size == 0
    spec = spec[0]
    builder = JFFI::StructLayoutBuilder.new
    spec.each do |name,type|
      builder.add_field(name, JFFI.find_type(type))
    end
    cspec = builder.build
    @layout = cspec unless self == JFFI::Struct
    @size = cspec.size
    return cspec
  end
end

class FFI::Struct < JFFI::BaseStruct
  def self.layout(*spec)
    return @layout if spec.size == 0

    builder = JFFI::StructLayoutBuilder.new
    i = 0
    while i < spec.size
      name, type, offset = spec[i, 3]
      
      code = FFI.find_type(type)
      builder.add_field(name, code, offset)
      i += 3
    end
    cspec = builder.build
    @layout = cspec unless self == FFI::Struct
    @size = cspec.size
    return cspec
  end

  def self.config(base, *fields)
    config = JFFI::Config::CONFIG
    @size = config["#{base}.sizeof"]
    
    builder = JFFI::StructLayoutBuilder.new
    
    fields.each do |field|
      offset = config["#{base}.#{field}.offset"]
      size   = config["#{base}.#{field}.size"]
      type   = config["#{base}.#{field}.type"]
      type   = type ? type.to_sym : JFFI.size_to_type(size)

      code = FFI.find_type type
      if (code == NativeType::CHAR_ARRAY)
        builder.add_char_array(field.to_s, size, offset)
      else
        builder.add_field(field.to_s, code, offset)
      end
    end
    cspec = builder.build
    
    @layout = cspec
    @size = cspec.size if @size < cspec.size
    
    return cspec
  end
end
