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

module Rubinius
  class Mirror
    class String < Mirror
      subject = ::String

      def character_to_byte_index(idx, start=0)
        Rubinius.invoke_primitive :string_character_byte_index, @object, idx, start
      end

      def byte_to_character_index(idx, start=0)
        Rubinius.invoke_primitive :string_byte_character_index, @object, idx, start
      end

      def character_index(str, start)
        Rubinius.invoke_primitive :string_character_index, @object, str, start
      end

      def byte_index(value, start=0)
        Rubinius.invoke_primitive :string_byte_index, @object, value, start
      end

      def previous_byte_index(index)
        Rubinius.invoke_primitive :string_previous_byte_index, @object, index
      end

      def copy_from(other, start, size, dest)
        Rubinius.invoke_primitive :string_copy_from, @object, other, start, size, dest
      end

      def resize_capacity(count)
        Rubinius.invoke_primitive :string_resize_capacity, @object, count
      end

      def splice(start, count, replacement)
        str = @object

        # TODO: copy_from unshares
        str.modify!

        bs = str.bytesize
        rbs = replacement.bytesize

        bytes = bs - count + rbs

        s = start + count
        b = bs - s
        d = start + rbs

        # Always resize if the resulting size is different to prevent "leaking"
        # bytes in large ByteArray instances when splicing out chunks.

        if rbs < count
          copy_from str, s, b, d
          resize_capacity bytes
        elsif rbs > count
          resize_capacity bytes
          copy_from str, s, b, d
        end

        copy_from replacement, 0, rbs, start if rbs > 0

        str.num_bytes = bytes

        self
      end
    end
  end
end
