#
# Copyright (C) 2008, 2009 Wayne Meissner
# Copyright (C) 2009 Luc Heinrich
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

module FFI
  TypeDefs = Hash.new

  def self.add_typedef(current, add)
    TypeDefs[add] = self.find_type(current)
  end
  
  
  def self.find_type(name, type_map = nil)
    type = if name.is_a?(FFI::Type)
      name

    elsif name.is_a?(DataConverter)
      (type_map || TypeDefs)[name] = Type::Mapped.new(name)

    elsif name.respond_to?("native_type") && name.respond_to?("to_native") && name.respond_to?("from_native")
      FFI::Type::Mapped.new(name)

    elsif type_map
      type_map[name]
    
    end || TypeDefs[name]

    raise TypeError, "Unable to resolve type '#{name}'" unless type

    return type
  end

  # Converts a char
  add_typedef(Type::CHAR, :char)

  # Converts an unsigned char
  add_typedef(Type::UCHAR, :uchar)

  # Converts an 8 bit int
  add_typedef(Type::INT8, :int8)

  # Converts an unsigned char
  add_typedef(Type::UINT8, :uint8)

  # Converts a short
  add_typedef(Type::SHORT, :short)

  # Converts an unsigned short
  add_typedef(Type::USHORT, :ushort)

  # Converts a 16bit int
  add_typedef(Type::INT16, :int16)

  # Converts an unsigned 16 bit int
  add_typedef(Type::UINT16, :uint16)

  # Converts an int
  add_typedef(Type::INT, :int)

  # Converts an unsigned int
  add_typedef(Type::UINT, :uint)

  # Converts a 32 bit int
  add_typedef(Type::INT32, :int32)

  # Converts an unsigned 16 bit int
  add_typedef(Type::UINT32, :uint32)

  # Converts a long
  add_typedef(Type::LONG, :long)

  # Converts an unsigned long
  add_typedef(Type::ULONG, :ulong)

  # Converts a 64 bit int
  add_typedef(Type::INT64, :int64)

  # Converts an unsigned 64 bit int
  add_typedef(Type::UINT64, :uint64)

  # Converts a long long
  add_typedef(Type::LONG_LONG, :long_long)

  # Converts an unsigned long long
  add_typedef(Type::ULONG_LONG, :ulong_long)

  # Converts a float
  add_typedef(Type::FLOAT, :float)

  # Converts a double
  add_typedef(Type::DOUBLE, :double)

  # Converts a pointer to opaque data
  add_typedef(Type::POINTER, :pointer)

  # For when a function has no return value
  add_typedef(Type::VOID, :void)

  # Native boolean type
  add_typedef(Type::BOOL, :bool)

  # Converts NUL-terminated C strings
  add_typedef(Type::STRING, :string)

  # Converts FFI::Buffer objects
  add_typedef(Type::BUFFER_IN, :buffer_in)
  add_typedef(Type::BUFFER_OUT, :buffer_out)
  add_typedef(Type::BUFFER_INOUT, :buffer_inout)

  add_typedef(Type::VARARGS, :varargs)

  add_typedef(Type::BOOL, :bool)

  # Returns a [ String, Pointer ] tuple so the C memory for the string can be freed
  class StrPtrConverter
    extend DataConverter
    native_type Type::POINTER

    def self.from_native(val, ctx)
      [ val.null? ? Qnil : val.get_string(0), val ]
    end

  end

  add_typedef(StrPtrConverter, :strptr)
  
  TypeSizes = {
    1 => :char,
    2 => :short,
    4 => :int,
    8 => :long_long,
  }

  def self.size_to_type(size)
    if sz = TypeSizes[size]
      return sz
    end

    # Be like C, use int as the default type size.
    return :int
  end

  def self.type_size(type)
    find_type(type).size
    end

  # Load all the platform dependent types
  begin
    File.open(File.join(FFI::Platform::CONF_DIR, 'types.conf'), "r") do |f|
      prefix = "rbx.platform.typedef."
      f.each_line { |line|
        if line.index(prefix) == 0
          new_type, orig_type = line.chomp.slice(prefix.length..-1).split(/\s*=\s*/)
          add_typedef(orig_type.to_sym, new_type.to_sym)
        end
      }
    end
  rescue Errno::ENOENT
  end
end