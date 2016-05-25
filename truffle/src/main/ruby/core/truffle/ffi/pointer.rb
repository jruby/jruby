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

module Rubinius
module FFI

  ##
  # Pointer is Rubinius's "fat" pointer class. It represents an actual
  # pointer, in C language terms, to an address in memory. They're called
  # fat pointers because the Pointer object is an wrapper around
  # the actual pointer, the Rubinius runtime doesn't have direct access
  # to the raw address.
  #
  # This class is used extensively in FFI usage to interface with various
  # parts of the underlying system. It provides a number of operations
  # for operating on the memory that is pointed to. These operations effectively
  # give Rubinius the cast/read capabilities available in C, but using
  # high level methods.
  #
  # MemoryPointer objects can be put in autorelease mode. In this mode,
  # when the GC cleans up a MemoryPointer object, the memory it points
  # to is passed to free(3), releasing the memory back to the OS.
  #
  # NOTE: MemoryPointer exposes direct, unmanaged operations on any
  # memory. It therefore MUST be used carefully. Reading or writing to
  # invalid address will cause bus errors and segmentation faults.
  #
  class Pointer
    def initialize(a1, a2=undefined)
      if undefined.equal? a2
        self.address = a1
      else
        @type = a1
        self.address = a2
      end
    end

    def inspect
      # Don't have this print the data at the location. It can crash everything.
      addr = address()

      if addr < 0
        sign = "-"
        addr = -addr
      else
        sign = ""
      end

      "#<#{self.class.name} address=#{sign}0x#{addr.to_s(16)}>"
    end

    # Return the address pointed to as an Integer
    def address
      Truffle.primitive :pointer_address
      raise PrimitiveFailure, "FFI::Pointer#address primitive failed"
    end

    alias_method :to_i, :address

    # Set the address pointed to from an Integer
    def address=(address)
      Truffle.primitive :pointer_set_address
      raise PrimitiveFailure, "FFI::Pointer#address= primitive failed"
    end

    def null?
      address == 0x0
    end

    # Add +value+ to the address pointed to and return a new Pointer
    def +(value)
      Truffle.primitive :pointer_add
      raise PrimitiveFailure, "FFI::Pointer#+ primitive failed"
    end

    # Indicates if +self+ and +other+ point to the same address
    def ==(other)
      return false unless other.kind_of? Pointer
      return address == other.address
    end

    def network_order(start, size)
      Truffle.primitive :pointer_network_order
      raise PrimitiveFailure, "FFI::Pointer#network_order primitive failed"
    end

    # Read +len+ bytes from the memory pointed to and return them as
    # a String
    def read_string_length(len)
      Truffle.primitive :pointer_read_string
      raise PrimitiveFailure, "FFI::Pointer#read_string_length primitive failed"
    end

    # Read bytes from the memory pointed to until a NULL is seen, return
    # the bytes as a String
    def read_string_to_null
      Truffle.primitive :pointer_read_string_to_null
      raise PrimitiveFailure, "FFI::Pointer#read_string_to_null primitive failed"
    end

    # Read bytes as a String from the memory pointed to
    def read_string(len=nil)
      if len
        read_string_length(len)
      else
        read_string_to_null
      end
    end

    # FFI compat methods
    def get_bytes(offset, length)
      (self + offset).read_string_length(length)
    end

    # Write String +str+ as bytes into the memory pointed to. Only
    # write up to +len+ bytes.
    def write_string_length(str, len)
      Truffle.primitive :pointer_write_string
      raise PrimitiveFailure, "FFI::Pointer#write_string_length primitive failed"
    end

    # Write a String +str+ as bytes to the memory pointed to.
    def write_string(str, len=nil)
      len = str.bytesize unless len

      write_string_length(str, len);
    end

    # Read a sequence of types +type+, length +length+, using method +reader+
    def read_array_of_type(type, reader, length, signed=nil)
      # If signed is not nil and is actually a boolean,
      # then use that as an argument to the reader, which
      # is then assumed to support signed reading.
      args = []
      args = [signed] if !signed.nil?

      # Build up the array
      ary = []
      size = FFI.type_size(FFI.find_type type)
      tmp = self
      length.times {
        ary << tmp.send(reader, *args)
        tmp += size
      }
      ary
    end

    # Write a sequence of types +type+  using method +reader+ from +ary+
    def write_array_of_type(type, writer, ary)
      size = FFI.type_size(FFI.find_type type)
      tmp = self
      ary.each do |i|
        tmp.send(writer, i)
        tmp += size
      end
      self
    end

    # Read bytes from +offset+ from the memory pointed to as type +type+
    def get_at_offset(offset, type)
      Truffle.primitive :pointer_get_at_offset
      raise PrimitiveFailure, "FFI::Pointer#get_at_offset primitive failed"
    end

    # Write +val+ as type +type+ to bytes from +offset+
    def set_at_offset(offset, type, val)
      Truffle.primitive :pointer_set_at_offset
      raise PrimitiveFailure, "FFI::Pointer#set_at_offset primitive failed"
    end

    # Number of bytes taken up by a pointer.
    def self.size
      Rubinius::WORDSIZE / 8
    end

    # Primitive methods
    def primitive_read_char(signed)
      Truffle.primitive :pointer_read_char
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_char primitive failed"
    end

    def primitive_write_char(obj)
      Truffle.primitive :pointer_write_char
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_char primitive failed"
    end

    def primitive_read_short(signed)
      Truffle.primitive :pointer_read_short
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_short primitive failed"
    end

    def primitive_write_short(obj)
      Truffle.primitive :pointer_write_short
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_short primitive failed"
    end

    def primitive_read_int(signed)
      Truffle.primitive :pointer_read_int
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_int primitive failed"
    end

    def primitive_write_int(obj)
      Truffle.primitive :pointer_write_int
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_int primitive failed"
    end

    def primitive_read_long(signed)
      Truffle.primitive :pointer_read_long
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_long primitive failed"
    end

    def primitive_write_long(obj)
      Truffle.primitive :pointer_write_long
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_long primitive failed"
    end

    def primitive_read_long_long(signed)
      Truffle.primitive :pointer_read_long_long
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_long_long primitive failed"
    end

    def primitive_write_long_long(obj)
      Truffle.primitive :pointer_write_long_long
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_long_long primitive failed"
    end

    def primitive_read_float
      Truffle.primitive :pointer_read_float
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_float primitive failed"
    end

    def primitive_write_float(obj)
      Truffle.primitive :pointer_write_float
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_float primitive failed"
    end

    def primitive_read_double
      Truffle.primitive :pointer_read_double
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_double primitive failed"
    end

    def primitive_write_double(obj)
      Truffle.primitive :pointer_write_double
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_double primitive failed"
    end

    def primitive_read_pointer
      Truffle.primitive :pointer_read_pointer
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_pointer primitive failed"
    end

    def primitive_write_pointer(obj)
      Truffle.primitive :pointer_write_pointer
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_pointer primitive failed"
    end

    ##
    # If +val+ is true, this Pointer object will call
    # free() on it's address when it is garbage collected.
    def autorelease=(val)
      Truffle.primitive :pointer_set_autorelease
      raise PrimitiveFailure, "FFI::Pointer#autorelease= primitive failed"
    end

    ##
    # Returns true if autorelease is enabled, otherwise false.
    def autorelease?
      Truffle.primitive :pointer_autorelease_p
      raise PrimitiveFailure, "FFI::Pointer#pointer_autorelease_p primitive failed"
    end

    if Truffle::Safe.memory_safe?
      NULL = Pointer.new(0x0)
    else
      NULL = nil
    end


    #
    # Name: int8
    # Type: char
    # Signed: true
    #

    def read_int8(signed=true)
      primitive_read_char(signed)
    end

    def write_int8(obj)
      primitive_write_char(obj)
    end

    def get_int8(offset)
      (self + offset).read_int8
    end

    def put_int8(offset, obj)
      (self + offset).write_int8(obj)
    end

    def read_array_of_int8(length, signed=true)
      read_array_of_type(:char, :read_char, length, signed)
    end

    def write_array_of_int8(ary)
      write_array_of_type(:char, :write_char, ary)
    end

    def get_array_of_int8(offset, length, signed=true)
      (self + offset).read_array_of_int8(length, signed)
    end

    def put_array_of_int8(offset, ary)
      (self + offset).write_array_of_int8(ary)
    end

    def read_uint8
      self.read_int8(false)
    end

    def write_uint8(obj)
      self.write_int8(obj)
    end

    def get_uint8(offset)
      (self + offset).read_uint8
    end

    def put_uint8(offset, obj)
      (self + offset).write_uint8(obj)
    end

    def read_array_of_uint8(length)
      self.read_array_of_int8(length, false)
    end

    def write_array_of_uint8(ary)
      self.write_array_of_int8(ary)
    end

    def get_array_of_uint8(offset, length)
      (self + offset).read_array_of_uint8(length)
    end

    def put_array_of_uint8(offset, ary)
      (self + offset).write_array_of_uint8(ary)
    end

    #
    # Name: int16
    # Type: short
    # Signed: true
    #

    def read_int16(signed=true)
      primitive_read_short(signed)
    end

    def write_int16(obj)
      primitive_write_short(obj)
    end

    def get_int16(offset)
      (self + offset).read_int16
    end

    def put_int16(offset, obj)
      (self + offset).write_int16(obj)
    end

    def read_array_of_int16(length, signed=true)
      read_array_of_type(:short, :read_short, length, signed)
    end

    def write_array_of_int16(ary)
      write_array_of_type(:short, :write_short, ary)
    end

    def get_array_of_int16(offset, length, signed=true)
      (self + offset).read_array_of_int16(length, signed)
    end

    def put_array_of_int16(offset, ary)
      (self + offset).write_array_of_int16(ary)
    end

    def read_uint16
      self.read_int16(false)
    end

    def write_uint16(obj)
      self.write_int16(obj)
    end

    def get_uint16(offset)
      (self + offset).read_uint16
    end

    def put_uint16(offset, obj)
      (self + offset).write_uint16(obj)
    end

    def read_array_of_uint16(length)
      self.read_array_of_int16(length, false)
    end

    def write_array_of_uint16(ary)
      self.write_array_of_int16(ary)
    end

    def get_array_of_uint16(offset, length)
      (self + offset).read_array_of_uint16(length)
    end

    def put_array_of_uint16(offset, ary)
      (self + offset).write_array_of_uint16(ary)
    end

    #
    # Name: int32
    # Type: int
    # Signed: true
    #

    def read_int32(signed=true)
      primitive_read_int(signed)
    end

    def write_int32(obj)
      primitive_write_int(obj)
    end

    def get_int32(offset)
      (self + offset).read_int32
    end

    def put_int32(offset, obj)
      (self + offset).write_int32(obj)
    end

    def read_array_of_int32(length, signed=true)
      read_array_of_type(:int, :read_int, length, signed)
    end

    def write_array_of_int32(ary)
      write_array_of_type(:int, :write_int, ary)
    end

    def get_array_of_int32(offset, length, signed=true)
      (self + offset).read_array_of_int32(length, signed)
    end

    def put_array_of_int32(offset, ary)
      (self + offset).write_array_of_int32(ary)
    end

    def read_uint32
      self.read_int32(false)
    end

    def write_uint32(obj)
      self.write_int32(obj)
    end

    def get_uint32(offset)
      (self + offset).read_uint32
    end

    def put_uint32(offset, obj)
      (self + offset).write_uint32(obj)
    end

    def read_array_of_uint32(length)
      self.read_array_of_int32(length, false)
    end

    def write_array_of_uint32(ary)
      self.write_array_of_int32(ary)
    end

    def get_array_of_uint32(offset, length)
      (self + offset).read_array_of_uint32(length)
    end

    def put_array_of_uint32(offset, ary)
      (self + offset).write_array_of_uint32(ary)
    end

    #
    # Name: int64
    # Type: long_long
    # Signed: true
    #

    def read_int64(signed=true)
      primitive_read_long_long(signed)
    end

    def write_int64(obj)
      primitive_write_long_long(obj)
    end

    def get_int64(offset)
      (self + offset).read_int64
    end

    def put_int64(offset, obj)
      (self + offset).write_int64(obj)
    end

    def read_array_of_int64(length, signed=true)
      read_array_of_type(:long_long, :read_long_long, length, signed)
    end

    def write_array_of_int64(ary)
      write_array_of_type(:long_long, :write_long_long, ary)
    end

    def get_array_of_int64(offset, length, signed=true)
      (self + offset).read_array_of_int64(length, signed)
    end

    def put_array_of_int64(offset, ary)
      (self + offset).write_array_of_int64(ary)
    end

    def read_uint64
      self.read_int64(false)
    end

    def write_uint64(obj)
      self.write_int64(obj)
    end

    def get_uint64(offset)
      (self + offset).read_uint64
    end

    def put_uint64(offset, obj)
      (self + offset).write_uint64(obj)
    end

    def read_array_of_uint64(length)
      self.read_array_of_int64(length, false)
    end

    def write_array_of_uint64(ary)
      self.write_array_of_int64(ary)
    end

    def get_array_of_uint64(offset, length)
      (self + offset).read_array_of_uint64(length)
    end

    def put_array_of_uint64(offset, ary)
      (self + offset).write_array_of_uint64(ary)
    end

    #
    # Name: long
    # Type: long
    # Signed: true
    #

    def read_long(signed=true)
      primitive_read_long(signed)
    end

    def write_long(obj)
      primitive_write_long(obj)
    end

    def get_long(offset)
      (self + offset).read_long
    end

    def put_long(offset, obj)
      (self + offset).write_long(obj)
    end

    def read_array_of_long(length, signed=true)
      read_array_of_type(:long, :read_long, length, signed)
    end

    def write_array_of_long(ary)
      write_array_of_type(:long, :write_long, ary)
    end

    def get_array_of_long(offset, length, signed=true)
      (self + offset).read_array_of_long(length, signed)
    end

    def put_array_of_long(offset, ary)
      (self + offset).write_array_of_long(ary)
    end

    def read_ulong
      self.read_long(false)
    end

    def write_ulong(obj)
      self.write_long(obj)
    end

    def get_ulong(offset)
      (self + offset).read_ulong
    end

    def put_ulong(offset, obj)
      (self + offset).write_ulong(obj)
    end

    def read_array_of_ulong(length)
      self.read_array_of_long(length, false)
    end

    def write_array_of_ulong(ary)
      self.write_array_of_long(ary)
    end

    def get_array_of_ulong(offset, length)
      (self + offset).read_array_of_ulong(length)
    end

    def put_array_of_ulong(offset, ary)
      (self + offset).write_array_of_ulong(ary)
    end

    #
    # Name: float32
    # Type: float
    # Signed: false
    #

    def read_float32
      primitive_read_float
    end

    def write_float32(obj)
      primitive_write_float(obj)
    end

    def get_float32(offset)
      (self + offset).read_float32
    end

    def put_float32(offset, obj)
      (self + offset).write_float32(obj)
    end

    def read_array_of_float32(length)
      read_array_of_type(:float, :read_float, length)
    end

    def write_array_of_float32(ary)
      write_array_of_type(:float, :write_float, ary)
    end

    def get_array_of_float32(offset, length)
      (self + offset).read_array_of_float32(length)
    end

    def put_array_of_float32(offset, ary)
      (self + offset).write_array_of_float32(ary)
    end

    #
    # Name: float64
    # Type: double
    # Signed: false
    #

    def read_float64
      primitive_read_double
    end

    def write_float64(obj)
      primitive_write_double(obj)
    end

    def get_float64(offset)
      (self + offset).read_float64
    end

    def put_float64(offset, obj)
      (self + offset).write_float64(obj)
    end

    def read_array_of_float64(length)
      read_array_of_type(:double, :read_double, length)
    end

    def write_array_of_float64(ary)
      write_array_of_type(:double, :write_double, ary)
    end

    def get_array_of_float64(offset, length)
      (self + offset).read_array_of_float64(length)
    end

    def put_array_of_float64(offset, ary)
      (self + offset).write_array_of_float64(ary)
    end

    #
    # Name: float
    # Type: float
    # Signed: false
    #

    def read_float
      primitive_read_float
    end

    def write_float(obj)
      primitive_write_float(obj)
    end

    def get_float(offset)
      (self + offset).read_float
    end

    def put_float(offset, obj)
      (self + offset).write_float(obj)
    end

    def read_array_of_float(length)
      read_array_of_type(:float, :read_float, length)
    end

    def write_array_of_float(ary)
      write_array_of_type(:float, :write_float, ary)
    end

    def get_array_of_float(offset, length)
      (self + offset).read_array_of_float(length)
    end

    def put_array_of_float(offset, ary)
      (self + offset).write_array_of_float(ary)
    end

    #
    # Name: double
    # Type: double
    # Signed: false
    #

    def read_double
      primitive_read_double
    end

    def write_double(obj)
      primitive_write_double(obj)
    end

    def get_double(offset)
      (self + offset).read_double
    end

    def put_double(offset, obj)
      (self + offset).write_double(obj)
    end

    def read_array_of_double(length)
      read_array_of_type(:double, :read_double, length)
    end

    def write_array_of_double(ary)
      write_array_of_type(:double, :write_double, ary)
    end

    def get_array_of_double(offset, length)
      (self + offset).read_array_of_double(length)
    end

    def put_array_of_double(offset, ary)
      (self + offset).write_array_of_double(ary)
    end

    #
    # Name: char
    # Type: char
    # Signed: true
    #

    def read_char(signed=true)
      primitive_read_char(signed)
    end

    def write_char(obj)
      primitive_write_char(obj)
    end

    def get_char(offset)
      (self + offset).read_char
    end

    def put_char(offset, obj)
      (self + offset).write_char(obj)
    end

    def read_array_of_char(length, signed=true)
      read_array_of_type(:char, :read_char, length, signed)
    end

    def write_array_of_char(ary)
      write_array_of_type(:char, :write_char, ary)
    end

    def get_array_of_char(offset, length, signed=true)
      (self + offset).read_array_of_char(length, signed)
    end

    def put_array_of_char(offset, ary)
      (self + offset).write_array_of_char(ary)
    end

    def read_uchar
      self.read_char(false)
    end

    def write_uchar(obj)
      self.write_char(obj)
    end

    def get_uchar(offset)
      (self + offset).read_uchar
    end

    def put_uchar(offset, obj)
      (self + offset).write_uchar(obj)
    end

    def read_array_of_uchar(length)
      self.read_array_of_char(length, false)
    end

    def write_array_of_uchar(ary)
      self.write_array_of_char(ary)
    end

    def get_array_of_uchar(offset, length)
      (self + offset).read_array_of_uchar(length)
    end

    def put_array_of_uchar(offset, ary)
      (self + offset).write_array_of_uchar(ary)
    end

    #
    # Name: short
    # Type: short
    # Signed: true
    #

    def read_short(signed=true)
      primitive_read_short(signed)
    end

    def write_short(obj)
      primitive_write_short(obj)
    end

    def get_short(offset)
      (self + offset).read_short
    end

    def put_short(offset, obj)
      (self + offset).write_short(obj)
    end

    def read_array_of_short(length, signed=true)
      read_array_of_type(:short, :read_short, length, signed)
    end

    def write_array_of_short(ary)
      write_array_of_type(:short, :write_short, ary)
    end

    def get_array_of_short(offset, length, signed=true)
      (self + offset).read_array_of_short(length, signed)
    end

    def put_array_of_short(offset, ary)
      (self + offset).write_array_of_short(ary)
    end

    def read_ushort
      self.read_short(false)
    end

    def write_ushort(obj)
      self.write_short(obj)
    end

    def get_ushort(offset)
      (self + offset).read_ushort
    end

    def put_ushort(offset, obj)
      (self + offset).write_ushort(obj)
    end

    def read_array_of_ushort(length)
      self.read_array_of_short(length, false)
    end

    def write_array_of_ushort(ary)
      self.write_array_of_short(ary)
    end

    def get_array_of_ushort(offset, length)
      (self + offset).read_array_of_ushort(length)
    end

    def put_array_of_ushort(offset, ary)
      (self + offset).write_array_of_ushort(ary)
    end

    #
    # Name: int
    # Type: int
    # Signed: true
    #

    def read_int(signed=true)
      primitive_read_int(signed)
    end

    def write_int(obj)
      primitive_write_int(obj)
    end

    def get_int(offset)
      (self + offset).read_int
    end

    def put_int(offset, obj)
      (self + offset).write_int(obj)
    end

    def read_array_of_int(length, signed=true)
      read_array_of_type(:int, :read_int, length, signed)
    end

    def write_array_of_int(ary)
      write_array_of_type(:int, :write_int, ary)
    end

    def get_array_of_int(offset, length, signed=true)
      (self + offset).read_array_of_int(length, signed)
    end

    def put_array_of_int(offset, ary)
      (self + offset).write_array_of_int(ary)
    end

    def read_uint
      self.read_int(false)
    end

    def write_uint(obj)
      self.write_int(obj)
    end

    def get_uint(offset)
      (self + offset).read_uint
    end

    def put_uint(offset, obj)
      (self + offset).write_uint(obj)
    end

    def read_array_of_uint(length)
      self.read_array_of_int(length, false)
    end

    def write_array_of_uint(ary)
      self.write_array_of_int(ary)
    end

    def get_array_of_uint(offset, length)
      (self + offset).read_array_of_uint(length)
    end

    def put_array_of_uint(offset, ary)
      (self + offset).write_array_of_uint(ary)
    end

    #
    # Name: long_long
    # Type: long_long
    # Signed: true
    #

    def read_long_long(signed=true)
      primitive_read_long_long(signed)
    end

    def write_long_long(obj)
      primitive_write_long_long(obj)
    end

    def get_long_long(offset)
      (self + offset).read_long_long
    end

    def put_long_long(offset, obj)
      (self + offset).write_long_long(obj)
    end

    def read_array_of_long_long(length, signed=true)
      read_array_of_type(:long_long, :read_long_long, length, signed)
    end

    def write_array_of_long_long(ary)
      write_array_of_type(:long_long, :write_long_long, ary)
    end

    def get_array_of_long_long(offset, length, signed=true)
      (self + offset).read_array_of_long_long(length, signed)
    end

    def put_array_of_long_long(offset, ary)
      (self + offset).write_array_of_long_long(ary)
    end

    def read_ulong_long
      self.read_long_long(false)
    end

    def write_ulong_long(obj)
      self.write_long_long(obj)
    end

    def get_ulong_long(offset)
      (self + offset).read_ulong_long
    end

    def put_ulong_long(offset, obj)
      (self + offset).write_ulong_long(obj)
    end

    def read_array_of_ulong_long(length)
      self.read_array_of_long_long(length, false)
    end

    def write_array_of_ulong_long(ary)
      self.write_array_of_long_long(ary)
    end

    def get_array_of_ulong_long(offset, length)
      (self + offset).read_array_of_ulong_long(length)
    end

    def put_array_of_ulong_long(offset, ary)
      (self + offset).write_array_of_ulong_long(ary)
    end

    #
    # Name: pointer
    # Type: pointer
    # Signed: false
    #

    def read_pointer
      primitive_read_pointer
    end

    def write_pointer(obj)
      primitive_write_pointer(obj)
    end

    def get_pointer(offset)
      (self + offset).read_pointer
    end

    def put_pointer(offset, obj)
      (self + offset).write_pointer(obj)
    end

    def read_array_of_pointer(length)
      read_array_of_type(:pointer, :read_pointer, length)
    end

    def write_array_of_pointer(ary)
      write_array_of_type(:pointer, :write_pointer, ary)
    end

    def get_array_of_pointer(offset, length)
      (self + offset).read_array_of_pointer(length)
    end

    def put_array_of_pointer(offset, ary)
      (self + offset).write_array_of_pointer(ary)
    end
  end

  class MemoryPointer < Pointer

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
    #
    def self.new(type, count=nil, clear=true)
      if type.kind_of? Fixnum
        size = type
      elsif type.kind_of? Symbol
        type = FFI.find_type type
        size = FFI.type_size(type)
      else
        size = type.size
      end

      if count
        total = size * count
      else
        total = size
      end

      ptr = malloc total
      ptr.total = total
      ptr.type_size = size
      Truffle::POSIX.memset ptr, 0, total if clear

      if block_given?
        begin
          value = yield ptr
        ensure
          ptr.free
        end

        return value
      else
        ptr.autorelease = true
        ptr
      end
    end

    def self.malloc(total)
      Truffle.primitive :pointer_malloc
      raise PrimitiveFailure, "FFI::MemoryPointer.malloc primitive failed"
    end

    def self.from_string(str)
      ptr = new str.bytesize + 1
      ptr.write_string str + "\0"

      ptr
    end

    def copy
      other = malloc total
      other.total = total
      other.type_size = type_size
      Truffle::POSIX.memcpy other, self, total

      Truffle.privately do
        other.initialize_copy self
      end

      other
    end

    # Indicates how many bytes the chunk of memory that is pointed to takes up.
    attr_accessor :total

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

    # Release the memory pointed to back to the OS.
    def free
      Truffle.primitive :pointer_free
      raise PrimitiveFailure, "FFI::MemoryPointer#free primitive failed"
    end
  end

  class DynamicLibrary::Symbol < Pointer
    def initialize(library, ptr, name)
      @library = library
      @name = name
      self.address = ptr.address
    end

    def inspect
      "#<FFI::Library::Symbol name=#{@name} address=#{address.to_s(16)}>"
    end
  end

  class Function < Pointer
    def initialize(ret_type, arg_types, val=nil, options=nil, &block)
      if block
        if val or options
          raise ArgumentError, "specify a block or a proc/address, not both"
        end

        val = block
      end

      args = arg_types.map { |x| FFI.find_type(x) }
      ret =  FFI.find_type(ret_type)

      if val.kind_of? Pointer
        @function = FFI.generate_function(val, :func, args, ret)
        self.address = val.address
      elsif val.respond_to? :call
        @function, ptr = FFI.generate_trampoline(val, :func, args, ret)
        self.address = ptr.address
      else
        raise ArgumentError, "value wasn't a FFI::Pointer and didn't respond to call"
      end

      # Hook the created function into the method_table so that #call goes
      # straight to it.
      sc = Rubinius::Type.object_singleton_class(self)
      sc.method_table.store :call, @function, :public
    end

    attr_reader :function

    # Hook this Function up to be an instance/class method +name+ on +mod+
    def attach(mod, name)
      unless mod.kind_of?(Module)
        raise TypeError, "mod must be a Module"
      end

      name = name.to_sym

      # Make it available as a method callable directly..
      sc = Rubinius::Type.object_singleton_class(mod)
      sc.method_table.store name, @function, :public

      # and expose it as a private method for people who
      # want to include this module.
      mod.method_table.store name, @function, :public
    end
  end
end
end
