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

class Range
  include Enumerable

  def initialize(first, last, exclude_end = false)
    raise NameError, "`initialize' called twice" if @begin

    unless first.kind_of?(Fixnum) && last.kind_of?(Fixnum)
      begin
        raise ArgumentError, "bad value for range" unless first <=> last
      rescue
        raise ArgumentError, "bad value for range"
      end
    end

    @begin = first
    @end = last
    @excl = exclude_end
  end
  private :initialize

  def ==(other)
    return true if equal? other

    other.kind_of?(Range) and
      self.first == other.first and
      self.last == other.last and
      self.exclude_end? == other.exclude_end?
  end

  alias_method :eql?, :==

  attr_reader_specific :excl, :exclude_end?

  attr_reader :begin
  attr_reader :end

  def bsearch
    return to_enum :bsearch unless block_given?

    unless @begin.kind_of? Numeric and @end.kind_of? Numeric
      raise TypeError, "bsearch is not available for #{@begin.class}"
    end

    min = @begin
    max = @end

    max -= 1 if max.kind_of? Integer and @excl

    start = min = Rubinius::Type.coerce_to min, Integer, :to_int
    total = max = Rubinius::Type.coerce_to max, Integer, :to_int

    last_true = nil

    seeker = Proc.new do |current|
      x = yield current

      return current if x == 0

      case x
      when Numeric
        if x > 0
          min = current + 1
        else
          max = current
        end
      when true
        last_true = current
        max = current
      when false, nil
        min = current + 1
      else
        raise TypeError, "Range#bsearch block must return Numeric or boolean"
      end
    end

    while min < max
      if max < 0 and min < 0
        mid = min + (max - min) / 2
      elsif min < -max
        mid = -((-1 - min - max) / 2 + 1)
      else
        mid = (min + max) / 2
      end      
      
      seeker.call mid
    end
    
    if min == max
      seeker.call min
    end

    if min < max
      return @begin if mid == start
      return @begin.kind_of?(Float) ? mid.to_f : mid
    end

    if last_true
      return @begin if last_true == start
      return @begin.kind_of?(Float) ? last_true.to_f : last_true
    end

    nil
  end

  def each_internal
    return to_enum { size } unless block_given?
    first, last = @begin, @end

    unless first.respond_to?(:succ) && !first.kind_of?(Time)
      raise TypeError, "can't iterate from #{first.class}"
    end

    case first
    when Fixnum
      last -= 1 if @excl

      i = first
      while i <= last
        yield i
        i += 1
      end
    when String
      first.upto(last, @excl) do |str|
        yield str
      end
    when Symbol
      first.to_s.upto(last.to_s, @excl) do |str|
        yield str.to_sym
      end
    else
      current = first
      if @excl
        while (current <=> last) < 0
          yield current
          current = current.succ
        end
      else
        while (c = current <=> last) && c <= 0
          yield current
          break if c == 0
          current = current.succ
        end
      end
    end

    self
  end

  def first(n=undefined)
    return @begin if undefined.equal? n

    super
  end

  def hash
    excl = @excl ? 1 : 0
    hash = excl
    hash ^= @begin.hash << 1
    hash ^= @end.hash << 9
    hash ^= excl << 24;
    # Are we throwing away too much here for a good hash value distribution?
    return hash & Fixnum::MAX
  end

  def include?(value)
    if @begin.respond_to?(:to_int) ||
       @end.respond_to?(:to_int) ||
       @begin.kind_of?(Numeric) ||
       @end.kind_of?(Numeric)
      cover? value
    else
      super
    end
  end

  alias_method :member?, :include?

  def ===(value)
    include?(value)
  end

  def inspect
    "#{@begin.inspect}#{@excl ? "..." : ".."}#{@end.inspect}"
  end

  def last(n=undefined)
    return @end if undefined.equal? n

    to_a.last(n)
  end

  def max
    return super if block_given? || (@excl && !@end.kind_of?(Numeric))
    return nil if @end < @begin || (@excl && @end == @begin)
    return @end unless @excl

    unless @end.kind_of?(Integer)
      raise TypeError, "cannot exclude non Integer end value"
    end

    unless @begin.kind_of?(Integer)
      raise TypeError, "cannot exclude end value with non Integer begin value"
    end

    @end - 1
  end

  def min
    return super if block_given?
    return nil if @end < @begin || (@excl && @end == @begin)

    @begin
  end

  def step_internal(step_size=1) # :yields: object
    return to_enum(:step, step_size) do
      m = Rubinius::Mirror::Range.reflect(self)
      m.step_iterations_size(*m.validate_step_size(@begin, @end, step_size))
    end unless block_given?

    m = Rubinius::Mirror::Range.reflect(self)
    values = m.validate_step_size(@begin, @end, step_size)
    first = values[0]
    last = values[1]
    step_size = values[2]

    case first
    when Float
      iterations = m.step_float_iterations_size(first, last, step_size)

      i = 0
      while i < iterations
        curr = i * step_size + first
        curr = last if last < curr
        yield curr
        i += 1
      end
    when Numeric
      curr = first
      last -= 1 if @excl

      while curr <= last
        yield curr
        curr += step_size
      end
    else
      i = 0
      each do |curr|
        yield curr if i % step_size == 0
        i += 1
      end
    end

    self
  end

  def to_s
    "#{@begin}#{@excl ? "..." : ".."}#{@end}"
  end

  def to_a_internal
    return super unless @begin.kind_of? Fixnum and @end.kind_of? Fixnum

    fin = @end
    fin += 1 unless @excl

    size = fin - @begin
    return [] if size <= 0

    ary = Array.new(size)

    i = 0
    while i < size
      ary[i] = @begin + i
      i += 1
    end

    ary
  end

  def cover?(value)
    # MRI uses <=> to compare, so must we.

    beg_compare = (@begin <=> value)
    return false unless beg_compare

    if Comparable.compare_int(beg_compare) <= 0
      end_compare = (value <=> @end)

      if @excl
        return true if Comparable.compare_int(end_compare) < 0
      else
        return true if Comparable.compare_int(end_compare) <= 0
      end
    end

    false
  end

  def size
    return nil unless @begin.kind_of?(Numeric)

    delta = @end - @begin
    return 0 if delta < 0

    if @begin.kind_of?(Float) || @end.kind_of?(Float)
      return delta if delta == Float::INFINITY

      err = (@begin.abs + @end.abs + delta.abs) * Float::EPSILON
      err = 0.5 if err > 0.5

      (@excl ? delta - err : delta + err).floor + 1
    else
      delta += 1 unless @excl
      delta
    end
  end

  def to_a_internal # MODIFIED called from java to_a
    return to_a_from_enumerable unless @begin.kind_of? Fixnum and @end.kind_of? Fixnum

    fin = @end
    fin += 1 unless @excl

    size = fin - @begin
    return [] if size <= 0

    ary = Array.new(size)

    i = 0
    while i < size
      ary[i] = @begin + i
      i += 1
    end

    ary
  end

  def to_a_from_enumerable(*arg)
    ary = []
    each(*arg) do
      o = Truffle.single_block_arg
      ary << o
      nil
    end
    Rubinius::Type.infect ary, self
    ary
  end
end
