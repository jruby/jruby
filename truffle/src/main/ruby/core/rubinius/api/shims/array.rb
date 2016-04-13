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

# Modifications are subject to:
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  class Mirror
    class Array

      def self.reflect(object)
        if Rubinius::Type.object_kind_of? object, ::Array
          Array.new(object)
        elsif ary = Rubinius::Type.try_convert(object, ::Array, :to_ary)
          Array.new(ary)
        else
          message = "expected Array, given #{Rubinius::Type.object_class(object)}"
          raise TypeError, message
        end
      end

      def initialize(array)
        @array = array
      end

      def total
        @array.size
      end

      def tuple
        @array
      end

      def start
        0
      end

    end
  end
end

class Array

  def new_range(start, count)
    ret = Array.new(count)

    self[start..-1].each_with_index { |x, index| ret[index] = x }

    ret
  end

  def new_reserved(count)
    # TODO CS 6-Feb-15 do we want to reserve space or allow the runtime to optimise for us?
    self.class.new(0 , nil)
  end

  # We must override the definition of `reverse!` because our Array isn't backed by a Tuple.  Rubinius expects
  # modifications to the Tuple to update the backing store and to do that, we treat the Array itself as its own Tuple.
  # However, Rubinius::Tuple#reverse! has a different, conflicting signature from Array#reverse!.  This override avoids
  # all of those complications.
  def reverse!
    Rubinius.check_frozen
    return self unless @total > 1

    i = 0
    while i < self.length / 2
      temp = self[i]
      self[i] = self[self.length - i - 1]
      self[self.length - i - 1] = temp
      i += 1
    end

    return self
  end

  # Rubinius expects to be able to resize the array and adjust pointers by modifying `@total` and `@start`, respectively.
  # We might be able to handle such changes by special handling in the body translator, however simply resizing could
  # delete elements from either side and we're not able to tell which without additional context.
  def slice!(start, length=undefined)
    Rubinius.check_frozen

    if undefined.equal? length
      if start.kind_of? Range
        range = start
        out = self[range]

        range_start = Rubinius::Type.coerce_to_collection_index range.begin
        if range_start < 0
          range_start = range_start + @total
        end

        range_end = Rubinius::Type.coerce_to_collection_index range.end
        if range_end < 0
          range_end = range_end + @total
        elsif range_end >= @total
          range_end = @total - 1
          range_end += 1 if range.exclude_end?
        end

        range_length = range_end - range_start
        range_length += 1 unless range.exclude_end?
        range_end    -= 1 if     range.exclude_end?

        if range_start < @total && range_start >= 0 && range_end < @total && range_end >= 0 && range_length > 0
          delete_range(range_start, range_length)
        end
      else
        # make sure that negative values are not passed through to the
        # []= assignment
        start = Rubinius::Type.coerce_to_collection_index start
        start = start + @total if start < 0

        # This is to match the MRI behaviour of not extending the array
        # with nil when specifying an index greater than the length
        # of the array.
        return out unless start >= 0 and start < @total

        out = @tuple.at start + @start

        # Check for shift style.
        if start == 0
          @tuple.put @start, nil
          self.shift
          @start += 1
        else
          delete_range(start, 1)
        end
      end
    else
      start = Rubinius::Type.coerce_to_collection_index start
      length = Rubinius::Type.coerce_to_collection_length length
      return nil if length < 0

      out = self[start, length]

      if start < 0
        start = @total + start
      end
      if start + length > @total
        length = @total - start
      end

      if start < @total && start >= 0
        delete_range(start, length)
      end
    end

    out
  end

  # Rubinius expects to modify the backing store via updates to `@tuple` and we don't support that.  As such, we must
  # provide our own modifying implementation here.
  def delete_range(index, del_length)
    # optimize for fast removal..
    reg_start = index + del_length
    reg_length = @total - reg_start
    if reg_start <= @total
      # If we're removing from the front, also reset @start to better
      # use the Tuple
      if index == 0
        # Use a shift start optimization if we're only removing one
        # element and the shift started isn't already huge.
        if del_length == 1
          @start += 1
        else
          @tuple.copy_from @tuple, reg_start + @start, reg_length, 0
          @start = 0
        end
      else
        @tuple.copy_from @tuple, reg_start + @start, reg_length,
                         @start + index
      end

      # TODO we leave the old references in the Tuple, we should
      # probably clear them out though.
      del_length.times do
        self.pop
      end

    end
  end

  # Rubinius expects to modify the backing store via updates to `@tuple` and we don't support that.  As such, we must
  # provide our own modifying implementation here.
  def uniq!(&block)
    Rubinius.check_frozen

    if block_given?
      im = Rubinius::IdentityMap.from(self, &block)
    else
      im = Rubinius::IdentityMap.from(self)
    end
    return if im.size == size

    m = Rubinius::Mirror::Array.reflect im.to_array
    @tuple = m.tuple
    @start = m.start
    @total = m.total

    copy_from(m.tuple, 0, m.total, 0)
    delete_range(m.total, self.size - m.total)
    self
  end

end
