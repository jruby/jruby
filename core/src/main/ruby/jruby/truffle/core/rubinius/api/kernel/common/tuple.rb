# Copyright (c) 2011, Evan Phoenix
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

# Modified to use Array for Truffle.

##
# The tuple data type.
# A simple storage class. Created to contain a fixed number of elements.
#
# Not designed to be subclassed, as it does not call initialize
# on new instances.

module Rubinius
  class Tuple

    def initialize (size, fill=nil)
        if size.kind_of? Array
            @array = size
        else
            @array = Array.new(size, fill)
        end
    end

    attr_accessor :array
    protected :array, :array=

    include Enumerable

    def self.[](*args)
        new(args)
    end

    def self.pattern (size, fill)
        new(size, fill)
    end

    def bounds_exceeded_error (method_name, i)
        ObjectBoundsExceededError.new("Tuple::#{method_name}: index #{i} out of bounds for size #{size}")
    end

    def check_bounds (method_name, i)
        if i < 0 or i >= size
            raise bounds_exceeded_error(method_name, i)
        end
    end

    def size
        @array.size
    end

    def put (i, v)
        check_bounds(__method__, i)
        @array[i] = v
        v
    end
    alias_method :[]=, :put

    def insert_at_index (i, obj)
        if i < 0 or i > size
            raise bounds_exceeded_error(__method__, i)
        end
        Tuple.new(@array.dup.insert(i, obj))
    end

    def delete_at_index (i)
        check_bounds(__method__, i)
        a = @array.dup
        a.delete_at(i)
        Tuple.new(a)
    end

    def at (i)
        check_bounds(__method__, i)
        @array[i]
    end
    alias_method :[], :at

    def to_s
        "#<#{self.class}:0x#{object_id.to_s(16)} #{size} elements>"
    end

    def each (&b)
        @array.each &b
        self
    end

    def ==(tup)
        return super unless tup.kind_of?(Tuple)
        return self.array == tup.array
    end

    def copy_from (other, src, len, dst)
        if src < 0 or src > other.size
            raise bounds_exceeded_error(__method__, src)
        end
        if dst < 0 or dst > size
            raise bounds_exceeded_error(__method__, dst)
        end
        if len < 0
            raise bounds_exceeded_error(__method__, len)
        end
        if (src + len) > other.size
            raise bounds_exceeded_error(__method__, src + len)
        end
        if len > (size - dst)
            raise bounds_exceeded_error(__method__, len)
        end
        if other.equal? self
            if src == dst
                self
            end
            if src < dst
                (len-1).downto(0).each do |i|
                    @array[dst + i] = @array[src + i];
                end
            else
                (0..(len-1)).each do |i|
                    @array[dst + i] = @array[src + i];
                end
            end
        else
            (0..(len-1)).each do |i|
                @array[dst + i] = other.array[src + i];
            end
        end
        self
    end

    def +(o)
        Tuple.new(@array + o)
    end

    def inspect
      str = "#<#{self.class}"
      if size == 0
        str << " empty>"
      else
        str << ": #{join(", ", :inspect)}>"
      end
      return str
    end

    def join(sep, meth=:to_s)
      join_upto(sep, size, meth)
    end

    def join_upto(sep, count, meth=:to_s)
      return "" if count == 0 or empty?
      count = size if count > size
      return @array[0, count].map(&meth).join(sep)
    end

    def ===(other)
      Tuple === other and @array === other.array
    end

    def to_a
        @array.dup
    end

    def shift
      return self unless size > 0
      Tuple.new(@array.dup.shift)
    end

    # Swap elements of the two indexes.
    def swap(a, b)
      temp = at(a)
      put a, at(b)
      put b, temp
    end

    alias_method :length, :size
    alias_method :fields, :size

    def empty?
      size == 0
    end

    def first
      at(0)
    end

    def last
      at(size - 1)
    end

    # Marshal support - _dump / _load are deprecated so eventually we should figure
    # out a better way.
    def _dump(depth)
      # TODO use depth
      Marshal.dump to_a
    end

    def self._load(str)
      ary = Marshal.load(str)
      t = new(ary.size)
      ary.each_with_index { |obj, idx| t[idx] = obj }
      return t
    end

    private :bounds_exceeded_error, :check_bounds
  end
end
