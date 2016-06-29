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

    # Primitive methods

    def primitive_read_int(signed)
      Truffle.primitive :pointer_read_int
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_int primitive failed"
    end

    def primitive_write_int(obj)
      Truffle.primitive :pointer_write_int
      raise PrimitiveFailure, "FFI::Pointer#primitive_write_int primitive failed"
    end

    def primitive_read_pointer
      Truffle.primitive :pointer_read_pointer
      raise PrimitiveFailure, "FFI::Pointer#primitive_read_pointer primitive failed"
    end

    ##
    # If +val+ is true, this Pointer object will call
    # free() on it's address when it is garbage collected.
    def autorelease=(val)
      Truffle.primitive :pointer_set_autorelease
      raise PrimitiveFailure, "FFI::Pointer#autorelease= primitive failed"
    end

    if Truffle::Safe.memory_safe?
      NULL = Pointer.new(0x0)
    else
      NULL = nil
    end

    #
    # Name: short
    # Type: short
    # Signed: true
    #

    def put_short(offset, obj)
      (self + offset).write_short(obj)
    end

    def read_array_of_short(length, signed=true)
      read_array_of_type(:short, :read_short, length, signed)
    end

    def write_array_of_short(ary)
      write_array_of_type(:short, :write_short, ary)
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

    def read_array_of_int(length, signed=true)
      read_array_of_type(:int, :read_int, length, signed)
    end

    #
    # Name: pointer
    # Type: pointer
    # Signed: false
    #

    def read_pointer
      primitive_read_pointer
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
end
end
