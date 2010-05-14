#
# Copyright (C) 2008-2010 Wayne Meissner
#
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
  class StructLayoutBuilder
    attr_reader :size, :alignment
    
    def initialize
      @size = 0
      @alignment = 1
      @min_alignment = 1
      @packed = false
      @union = false
      @fields = Array.new
    end

    def size=(size)
      @size = size if size > @size
    end

    def alignment=(align)
      @alignment = align if align > @alignment
      @min_alignment = align
    end

    def union=(is_union)
      @union = is_union
    end

    def union?
      @union
    end

    def packed=(packed)
      if packed.is_a?(Fixnum)
        @alignment = packed
        @packed = packed
      else
        @packed = packed ? 1 : 0
      end
    end


    NUMBER_TYPES = [
      FFI::Type::INT8,
      FFI::Type::UINT8,
      FFI::Type::INT16,
      FFI::Type::UINT16,
      FFI::Type::INT32,
      FFI::Type::UINT32,
      FFI::Type::LONG,
      FFI::Type::ULONG,
      FFI::Type::INT64,
      FFI::Type::UINT64,
      FFI::Type::FLOAT32,
      FFI::Type::FLOAT64,
    ]

    def add(name, type, offset = nil)

      if offset.nil? || offset == -1
        offset = @union ? 0 : align(@size, @packed ? [ @packed, type.alignment ].min : [ @min_alignment, type.alignment ].max)
      end

      #
      # If a FFI::Type type was passed in as the field arg, try and convert to a StructLayout::Field instance
      #
      field = if !type.is_a?(StructLayout::Field)

        field_class = case
          when type.is_a?(FFI::Type::Function)
            StructLayout::Function

          when type.is_a?(FFI::Type::Struct)
            StructLayout::InnerStruct

          when type.is_a?(FFI::Type::Array)
            StructLayout::Array

          when type.is_a?(FFI::Enum)
            StructLayout::Enum

          when NUMBER_TYPES.include?(type)
            StructLayout::Number

          when type == FFI::Type::POINTER
            StructLayout::Pointer

          when type == FFI::Type::STRING
            StructLayout::String

          when type.is_a?(Class) && type < FFI::StructLayout::Field
            type

          else
            raise TypeError, "invalid struct field type #{type.inspect}"
          end

        field_class.new(name, offset, type)

      else
        type
      end

      @fields << field
      @alignment = [ @alignment, field.alignment ].max unless @packed
      @size = [ @size, field.size + (@union ? 0 : field.offset) ].max

      return self
    end

    def add_field(name, type, offset = nil)
      add(name, type, offset)
    end
    
    def add_struct(name, type, offset = nil)
      add(name, FFI::Type::Struct.new(type), offset)
    end

    def add_array(name, type, count, offset = nil)
      add(name, FFI::Type::Array.new(type, count), offset)
    end

    def build
      # Add tail padding if the struct is not packed
      size = @packed ? @size : align(@size, @alignment)
      
      FFI::StructLayout.new(@fields, size, @alignment)
    end

    private
    
    def align(offset, align)
      align + ((offset - 1) & ~(align - 1));
    end

  end

end