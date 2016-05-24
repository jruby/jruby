# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Some of the code in this class is transliterated from C++ code in Rubinius.
#
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

  class Tuple < Array

    def self.pattern(num, val)
       Tuple.new(num, val)
    end

    def self.create(*args)
      ret = Tuple.new(args.size)

      args.each_with_index do |arg, index|
        ret[index] = arg
      end

      ret
    end

    # Taken from Rubinius.
    def reverse!(start, total)
      if total <= 0 || start < 0 || start >= size
        return self
      end

      _end = start + total - 1
      if _end > size
        _end = size - 1
      end

      head_ptr = start
      tail_ptr = _end

      while head_ptr < tail_ptr
        tmp = self[head_ptr]
        self[head_ptr] = self[tail_ptr]
        self[tail_ptr] = tmp

        head_ptr += 1
        tail_ptr -= 1
      end

      self
    end

  end

end


class Array

  def copy_from(other, start, length, dest)
    Truffle.primitive :tuple_copy_from

    unless other.kind_of? Array # Rubinius::Tuple # We use Array
      raise TypeError, "Tuple#copy_from was expecting an Array, not a #{other.class}"
    end
    start = Rubinius::Type.coerce_to start, Fixnum, :to_i
    length = Rubinius::Type.coerce_to length, Fixnum, :to_i
    dest = Rubinius::Type.coerce_to dest, Fixnum, :to_i

    if start < 0 || start > other.size # other.fields
      raise IndexError, "Start %d is out of bounds %d" % [start, other.size]# other.fields]
    end

    if dest < 0 || dest > self.size # self.fields
      raise IndexError, "Destination %d is out of bounds %d" % [dest, self.size] # self.fields]
    end

    if length < 0
      raise IndexError, "length %d must be positive" % [length]
    end

    if (start + length) > other.size # other.fields
      raise IndexError, "end index %d can not exceed size of source %d" % [start+length, other.size]# other.fields]
    end

    if length > ( self.size - dest )#self.fields - dest)
      raise IndexError, "length %d can not exceed space %d in destination" % [length, self.size - dest]#self.fields - dest]
    end

    src = start
    dst = dest
    while src < (start + length)
      put dst, other.at(src)
      src += 1
      dst += 1
    end

    self
  end


  def swap(a, b)
    temp = at(a)
    self[a] = at(b)
    self[b] = temp
  end

  alias_method :put, :[]=

end
