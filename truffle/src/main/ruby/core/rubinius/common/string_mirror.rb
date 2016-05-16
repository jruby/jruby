# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2

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
  class Mirror
    class String < Mirror
      self.subject = ::String

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

      def splice(starting_byte_index, byte_count_to_replace, replacement, encoding=nil)
        Rubinius.invoke_primitive :string_splice, @object, replacement, starting_byte_index, byte_count_to_replace, (encoding || @object.encoding)
      end
    end
  end
end
