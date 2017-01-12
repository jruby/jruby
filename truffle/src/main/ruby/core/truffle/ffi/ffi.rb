# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
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
# * Neither the name of Rubinius nor the names of its contributors
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

##
# A Foreign Function Interface used to bind C libraries to ruby.

module Rubinius
module FFI

  #  Specialised error classes
  class TypeError < RuntimeError; end

  class NotFoundError < RuntimeError; end

  # Shorthand for the current process, i.e. all code that
  # the process image itself contains. In addition to the
  # Rubinius codebase, this also includes libc etc.
  #
  # Use this constant instead of nil directly.
  #
  CURRENT_PROCESS = nil

  TypeDefs = {}

  class << self
    def add_typedef(current, add)
      if current.kind_of? Fixnum
        code = current
      else
        code = FFI::TypeDefs[current]
        raise TypeError, "Unable to resolve type '#{current}'" unless code
      end

      FFI::TypeDefs[add] = code
    end

    def find_type(name)
      code = FFI::TypeDefs[name]
      raise TypeError, "Unable to resolve type '#{name}'" unless code
      return code
    end

    ##
    # Given a +type+ as a number, indicate how many bytes that type
    # takes up on this platform.

    def type_size(type)
      Truffle.primitive :nativefunction_type_size

      case type
      when Symbol
        return type_size(find_type(type))
      when Rubinius::NativeFunction
        return type_size(TYPE_PTR)
      when FFI::Enum
        return type_size(TYPE_ENUM)
      end

      raise PrimitiveFailure, "FFI.type_size primitive failed: #{type}"
    end

    def size_to_type(size)
      if sz = TypeSizes[size]
        return sz
      end

      # Be like C, use int as the default type size.
      return :int
    end

    def config(name)
      Rubinius::Config["rbx.platform.#{name}"]
    end

    def config_hash(name)
      vals = {}
      section = "rbx.platform.#{name}."
      Rubinius::Config.section section do |key, value|
        vals[key.substring(section.size, key.length)] = value
      end
      vals
    end

    def errno
      Truffle::POSIX.errno
    end

  end

  # Converts a char
  add_typedef TYPE_CHAR,    :char

  # Converts an unsigned char
  add_typedef TYPE_UCHAR,   :uchar

  # The C++ boolean type
  add_typedef TYPE_BOOL,    :bool

  # Converts a short
  add_typedef TYPE_SHORT,   :short

  # Converts an unsigned short
  add_typedef TYPE_USHORT,  :ushort

  # Converts an int
  add_typedef TYPE_INT,     :int

  # Converts an unsigned int
  add_typedef TYPE_UINT,    :uint

  # Converts a long
  add_typedef TYPE_LONG,    :long

  # Converts an unsigned long
  add_typedef TYPE_ULONG,   :ulong

  # Converts a size_t
  add_typedef TYPE_ULONG,   :size_t

  # Converts a long long
  add_typedef TYPE_LL,      :long_long

  # Converts an unsigned long long
  add_typedef TYPE_ULL,     :ulong_long

  # Converts a float
  add_typedef TYPE_FLOAT,   :float

  # Converts a double
  add_typedef TYPE_DOUBLE,  :double

  # Converts a pointer to opaque data
  add_typedef TYPE_PTR,     :pointer

  # For when a function has no return value
  add_typedef TYPE_VOID,    :void

  # Converts NULL-terminated C strings
  add_typedef TYPE_STRING,  :string

  # Use strptr when you need to free the result of some operation.
  add_typedef TYPE_STRPTR,  :strptr
  add_typedef TYPE_STRPTR,  :string_and_pointer

  # Use for a C struct with a char [] embedded inside.
  add_typedef TYPE_CHARARR, :char_array

  # A set of unambigious integer types
  add_typedef TYPE_CHAR,   :int8
  add_typedef TYPE_UCHAR,  :uint8
  add_typedef TYPE_SHORT,  :int16
  add_typedef TYPE_USHORT, :uint16
  add_typedef TYPE_INT,    :int32
  add_typedef TYPE_UINT,   :uint32

  # Converts a varargs argument
  add_typedef TYPE_VARARGS, :varargs

  if Rubinius::L64
    add_typedef TYPE_LONG,  :int64
    add_typedef TYPE_ULONG, :uint64
  else
    add_typedef TYPE_LL,    :int64
    add_typedef TYPE_ULL,   :uint64
  end

  TypeSizes = {}
  TypeSizes[1] = :char
  TypeSizes[2] = :short
  TypeSizes[4] = :int
  TypeSizes[8] = Rubinius::L64 ? :long : :long_long

  # Load all the platform dependent types

  Rubinius::Config.section("rbx.platform.typedef.") do |key, value|
    add_typedef(find_type(value.to_sym), key.substring(21, key.length).to_sym)
  end

  # It's a class to be compat with the ffi gem.
  class Type
    class Array
    end

    class StructByValue
    end

    Struct = StructByValue

    CHAR    = TYPE_CHAR
    UCHAR   = TYPE_UCHAR
    BOOL    = TYPE_BOOL
    SHORT   = TYPE_SHORT
    USHORT  = TYPE_USHORT
    INT     = TYPE_INT
    UINT    = TYPE_UINT
    LONG    = TYPE_LONG
    ULONG   = TYPE_ULONG
    LL      = TYPE_LL
    ULL     = TYPE_ULL
    FLOAT   = TYPE_FLOAT
    DOUBLE  = TYPE_DOUBLE
    PTR     = TYPE_PTR
    VOID    = TYPE_VOID
    STRING  = TYPE_STRING
    STRPTR  = TYPE_STRPTR
    CHARARR = TYPE_CHARARR
    ENUM    = TYPE_ENUM
    VARARGS = TYPE_VARARGS
  end
end

##
# Namespace for holding platform-specific C constants.

module FFI::Platform
  case
  when Rubinius.windows?
    LIBSUFFIX = "dll"
    IS_WINDOWS = true
    OS = 'windows'
  when Rubinius.darwin?
    LIBSUFFIX = "dylib"
    IS_WINDOWS = false
    OS = 'darwin'
  else
    LIBSUFFIX = "so"
    IS_WINDOWS = false
    OS = 'linux'
  end

  ARCH = Rubinius::CPU

  # ruby-ffi compatible
  LONG_SIZE = Rubinius::SIZEOF_LONG * 8
  ADDRESS_SIZE = Rubinius::WORDSIZE
end
end
