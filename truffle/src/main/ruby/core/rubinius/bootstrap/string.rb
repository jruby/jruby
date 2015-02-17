# Copyright (c) 2007-2014, Evan Phoenix and contributors
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

# Only part of Rubinius' string.rb

class String

  def self.from_codepoint(code, enc)
    Rubinius.primitive :string_from_codepoint
    raise PrimitiveFailure, "String.from_codepoint primitive failed"
  end

  def find_string(pattern, start)
    Rubinius.primitive :string_index
    raise PrimitiveFailure, "String#find_string primitive failed"
  end

  def byteslice(index_or_range, length=undefined)
    Rubinius.primitive :string_byte_substring

    if index_or_range.kind_of? Range
      index = Rubinius::Type.coerce_to index_or_range.begin, Fixnum, :to_int
      index += @num_bytes if index < 0
      return if index < 0 or index > @num_bytes

      finish = Rubinius::Type.coerce_to index_or_range.end, Fixnum, :to_int
      finish += @num_bytes if finish < 0

      finish += 1 unless index_or_range.exclude_end?
      length = finish - index

      return byteslice 0, 0 if length < 0
    else
      index = Rubinius::Type.coerce_to index_or_range, Fixnum, :to_int
      index += @num_bytes if index < 0

      if undefined.equal?(length)
        return if index == @num_bytes
        length = 1
      else
        length = Rubinius::Type.coerce_to length, Fixnum, :to_int
        return if length < 0
      end

      return if index < 0 or index > @num_bytes
    end

    byteslice index, length
  end

  def find_character(offset)
    Rubinius.primitive :string_find_character
    raise PrimitiveFailure, "String#find_character primitive failed"
  end

end