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

# Only part of Rubinius' array.rb

# Rubinius uses the instance variable @total to store the size. We replace this
# in the translator with a call to size. We also replace the instance variable
# @tuple to be self, and @start to be 0.

class Array

  def self.[](*args)
    ary = allocate
    ary.replace args
    ary
  end

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, Array, :to_ary
  end

  def &(other)
    other = Rubinius::Type.coerce_to other, Array, :to_ary

    array = []
    im = Rubinius::IdentityMap.from other

    each { |x| array << x if im.delete x }

    array
  end


  def values_at(*args)
    out = []

    args.each do |elem|
      # Cannot use #[] because of subtly different errors
      if elem.kind_of? Range
        finish = Rubinius::Type.coerce_to_collection_index elem.last
        start = Rubinius::Type.coerce_to_collection_index elem.first

        start += @total if start < 0
        next if start < 0

        finish += @total if finish < 0
        finish -= 1 if elem.exclude_end?

        next if finish < start

        start.upto(finish) { |i| out << at(i) }

      else
        i = Rubinius::Type.coerce_to_collection_index elem
        out << at(i)
      end
    end

    out
  end

  def first(n = undefined)
    return at(0) if undefined.equal?(n)

    n = Rubinius::Type.coerce_to_collection_index n
    raise ArgumentError, "Size must be positive" if n < 0

    Array.new self[0, n]
  end

  def hash
    hash_val = size
    mask = Fixnum::MAX >> 1

    # This is duplicated and manually inlined code from Thread for performance
    # reasons. Before refactoring it, please benchmark it and compare your
    # refactoring against the original.

    id = object_id
    objects = Thread.current.recursive_objects

    # If there is already an our version running...
    if objects.key? :__detect_outermost_recursion__

      # If we've seen self, unwind back to the outer version
      if objects.key? id
        raise Thread::InnerRecursionDetected
      end

      # .. or compute the hash value like normal
      begin
        objects[id] = true

        each { |x| hash_val = ((hash_val & mask) << 1) ^ x.hash }
      ensure
        objects.delete id
      end

      return hash_val
    else
      # Otherwise, we're the outermost version of this code..
      begin
        objects[:__detect_outermost_recursion__] = true
        objects[id] = true

        each { |x| hash_val = ((hash_val & mask) << 1) ^ x.hash }

        # An inner version will raise to return back here, indicating that
        # the whole structure is recursive. In which case, abondon most of
        # the work and return a simple hash value.
      rescue Thread::InnerRecursionDetected
        return size
      ensure
        objects.delete :__detect_outermost_recursion__
        objects.delete id
      end
    end

    return hash_val
  end

  def last(n=undefined)
    if undefined.equal?(n)
      return at(-1)
    elsif size < 1
      return []
    end

    n = Rubinius::Type.coerce_to_collection_index n
    return [] if n == 0

    raise ArgumentError, "count must be positive" if n < 0

    n = size if n > size
    Array.new self[-n..-1]
  end

  def permutation(num=undefined, &block)
    return to_enum(:permutation, num) unless block_given?

    if undefined.equal? num
      num = @total
    else
      num = Rubinius::Type.coerce_to_collection_index num
    end

    if num < 0 || @total < num
      # no permutations, yield nothing
    elsif num == 0
      # exactly one permutation: the zero-length array
      yield []
    elsif num == 1
      # this is a special, easy case
      each { |val| yield [val] }
    else
      # this is the general case
      perm = Array.new(num)
      used = Array.new(@total, false)

      if block
        # offensive (both definitions) copy.
        offensive = dup
        Rubinius.privately do
          offensive.__permute__(num, perm, 0, used, &block)
        end
      else
        __permute__(num, perm, 0, used, &block)
      end
    end

    self
  end

  def __permute__(num, perm, index, used, &block)
    # Recursively compute permutations of r elements of the set [0..n-1].
    # When we have a complete permutation of array indexes, copy the values
    # at those indexes into a new array and yield that array.
    #
    # num: the number of elements in each permutation
    # perm: the array (of size num) that we're filling in
    # index: what index we're filling in now
    # used: an array of booleans: whether a given index is already used
    #
    # Note: not as efficient as could be for big num.
    @total.times do |i|
      unless used[i]
        perm[index] = i
        if index < num-1
          used[i] = true
          __permute__(num, perm, index+1, used, &block)
          used[i] = false
        else
          yield values_at(*perm)
        end
      end
    end
  end
  private :__permute__

  def <=>(other)
    other = Rubinius::Type.check_convert_type other, Array, :to_ary
    return 0 if equal? other
    return nil if other.nil?

    total = Rubinius::Mirror::Array.reflect(other).total

    Thread.detect_recursion self, other do
      i = 0
      count = total < @total ? total : @total

      while i < count
        order = self[i] <=> other[i]
        return order unless order == 0

        i += 1
      end
    end

    # subtle: if we are recursing on that pair, then let's
    # no go any further down into that pair;
    # any difference will be found elsewhere if need be
    @total <=> total
  end

  def -(other)
    other = Rubinius::Type.coerce_to other, Array, :to_ary

    array = []
    im = Rubinius::IdentityMap.from other

    each { |x| array << x unless im.include? x }

    array
  end

  def |(other)
    other = Rubinius::Type.coerce_to other, Array, :to_ary

    im = Rubinius::IdentityMap.from self, other
    im.to_array
  end

  def ==(other)
    return true if equal?(other)
    unless other.kind_of? Array
      return false unless other.respond_to? :to_ary
      return other == self
    end

    return false unless size == other.size

    Thread.detect_recursion self, other do
      m = Rubinius::Mirror::Array.reflect other

      md = @tuple
      od = m.tuple

      i = @start
      j = m.start

      total = i + @total

      while i < total
        return false unless md[i] == od[j]
        i += 1
        j += 1
      end
    end

    true
  end

  def eql?(other)
    return true if equal? other
    return false unless other.kind_of?(Array)
    return false if @total != other.size

    Thread.detect_recursion self, other do
      i = 0
      each do |x|
        return false unless x.eql? other[i]
        i += 1
      end
    end

    true
  end

  def empty?
    @total == 0
  end

  # Helper to recurse through flattening since the method
  # is not allowed to recurse itself. Detects recursive structures.
  def recursively_flatten(array, out, max_levels = -1)
    modified = false

    # Strict equality since < 0 means 'infinite'
    if max_levels == 0
      out.concat(array)
      return false
    end

    max_levels -= 1
    recursion = Thread.detect_recursion(array) do
      m = Rubinius::Mirror::Array.reflect array

      i = m.start
      total = i + m.total
      tuple = m.tuple

      while i < total
        o = tuple.at i

        if ary = Rubinius::Type.check_convert_type(o, Array, :to_ary)
          modified = true
          recursively_flatten(ary, out, max_levels)
        else
          out << o
        end

        i += 1
      end
    end

    raise ArgumentError, "tried to flatten recursive array" if recursion
    modified
  end

  private :recursively_flatten

  def assoc(obj)
    each do |x|
      if x.kind_of? Array and x.first == obj
        return x
      end
    end

    nil
  end

  def bsearch
    return to_enum :bsearch unless block_given?

    m = Rubinius::Mirror::Array.reflect self

    tuple = m.tuple

    min = start = m.start
    max = total = start + m.total

    last_true = nil
    i = start + m.total / 2

    while max >= min and i >= start and i < total
      x = yield tuple.at(i)

      return tuple.at(i) if x == 0

      case x
        when Numeric
          if x > 0
            min = i + 1
          else
            max = i - 1
          end
        when true
          last_true = i
          max = i - 1
        when false, nil
          min = i + 1
        else
          raise TypeError, "Array#bsearch block must return Numeric or boolean"
      end

      i = min + (max - min) / 2
    end

    return tuple.at(i) if max > min
    return tuple.at(last_true) if last_true

    nil
  end


  def combination(num)
    num = Rubinius::Type.coerce_to_collection_index num
    return to_enum(:combination, num) unless block_given?

    if num == 0
      yield []
    elsif num == 1
      each do |i|
        yield [i]
      end
    elsif num == size
      yield self.dup
    elsif num >= 0 && num < size
      stack = Rubinius::Tuple.pattern num + 1, 0
      chosen = Rubinius::Tuple.new num
      lev = 0
      done = false
      stack[0] = -1
      until done
        chosen[lev] = self.at(stack[lev+1])
        while lev < num - 1
          lev += 1
          chosen[lev] = self.at(stack[lev+1] = stack[lev] + 1)
        end
        yield chosen.to_a
        lev += 1
        begin
          done = lev == 0
          stack[lev] += 1
          lev -= 1
        end while stack[lev+1] + num == size + lev + 1
      end
    end
    self
  end

  def cycle(n=nil)
    return to_enum(:cycle, n) unless block_given?
    return nil if empty?

    # Don't use nil? because, historically, lame code has overridden that method
    if n.equal? nil
      while true
        each { |x| yield x }
      end
    else
      n = Rubinius::Type.coerce_to_collection_index n
      n.times do
        each { |x| yield x }
      end
    end
    nil
  end

  def each_index
    return to_enum(:each_index) unless block_given?

    i = 0
    total = @total

    while i < total
      yield i
      i += 1
    end

    self
  end

  # WARNING: This method does no boundary checking. It is expected that
  # the caller handle that, eg #slice!
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
      # MODIFIED Can't -= the @total to modify length
      #@total -= del_length
      del_length.times do
        self.pop
      end

    end
  end

  private :delete_range

  def fill(a=undefined, b=undefined, c=undefined)
    Rubinius.check_frozen

    if block_given?
      unless undefined.equal?(c)
        raise ArgumentError, "wrong number of arguments"
      end
      one, two = a, b
    else
      if undefined.equal?(a)
        raise ArgumentError, "wrong number of arguments"
      end
      obj, one, two = a, b, c
    end

    if one.kind_of? Range
      raise TypeError, "length invalid with range" unless undefined.equal?(two)

      left = Rubinius::Type.coerce_to_collection_length one.begin
      left += size if left < 0
      raise RangeError, "#{one.inspect} out of range" if left < 0

      right = Rubinius::Type.coerce_to_collection_length one.end
      right += size if right < 0
      right += 1 unless one.exclude_end?
      return self if right <= left           # Nothing to modify

    elsif one and !undefined.equal?(one)
      left = Rubinius::Type.coerce_to_collection_length one
      left += size if left < 0
      left = 0 if left < 0

      if two and !undefined.equal?(two)
        begin
          right = Rubinius::Type.coerce_to_collection_length two
        rescue TypeError
          raise ArgumentError, "second argument must be a Fixnum"
        end

        return self if right == 0
        right += left
      else
        right = size
      end
    else
      left = 0
      right = size
    end

    total = @start + right

    if right > @total
      #reallocate total # I don't believe this is necessary since Tuple isn't used internally
      @total = right
    end

    # Must be after the potential call to reallocate, since
    # reallocate might change @tuple
    tuple = @tuple

    i = @start + left

    if block_given?
      while i < total
        tuple.put i, yield(i-@start)
        i += 1
      end
    else
      while i < total
        tuple.put i, obj
        i += 1
      end
    end

    self
  end

  def flatten(level=-1)
    level = Rubinius::Type.coerce_to_collection_index level
    return self.dup if level == 0

    out = new_reserved size
    recursively_flatten(self, out, level)
    Rubinius::Type.infect(out, self)
    out
  end

  def flatten!(level=-1)
    Rubinius.check_frozen

    level = Rubinius::Type.coerce_to_collection_index level
    return nil if level == 0

    out = new_reserved size
    if recursively_flatten(self, out, level)
      replace(out)
      return self
    end

    nil
  end


  def inspect
    return "[]".force_encoding(Encoding::US_ASCII) if @total == 0
    comma = ", "
    result = "["

    return "[...]" if Thread.detect_recursion self do
      each_with_index do |element, index|
        temp = element.inspect
        result.force_encoding(temp.encoding) if index == 0
        result << temp << comma
      end
    end

    Rubinius::Type.infect(result, self)
    result.shorten!(2)
    result << "]"
    result
  end

  alias_method :to_s, :inspect

  def join(sep=nil)
    return "".force_encoding(Encoding::US_ASCII) if @total == 0

    out = ""
    raise ArgumentError, "recursive array join" if Thread.detect_recursion self do
      sep = sep.nil? ? $, : StringValue(sep)

      # We've manually unwound the first loop entry for performance
      # reasons.
      x = @tuple[@start]

      if str = String.try_convert(x)
        x = str
      elsif ary = Array.try_convert(x)
        x = ary.join(sep)
      else
        x = x.to_s
      end

      out.force_encoding(x.encoding)
      out << x

      total = @start + size()
      i = @start + 1

      while i < total
        out << sep if sep

        x = @tuple[i]

        if str = String.try_convert(x)
          x = str
        elsif ary = Array.try_convert(x)
          x = ary.join(sep)
        else
          x = x.to_s
        end

        out << x
        i += 1
      end
    end

    Rubinius::Type.infect(out, self)
  end

  def keep_if(&block)
    return to_enum :keep_if unless block_given?

    Rubinius.check_frozen

    replace select(&block)
  end

  # Implementation notes: We build a block that will generate all the
  # combinations by building it up successively using "inject" and starting
  # with one responsible to append the values.
  def product(*args)
    args.map! { |x| Rubinius::Type.coerce_to(x, Array, :to_ary) }

    # Check the result size will fit in an Array.
    sum = args.inject(size) { |n, x| n * x.size }

    if sum > Fixnum::MAX
      raise RangeError, "product result is too large"
    end

    # TODO rewrite this to not use a tree of Proc objects.

    # to get the results in the same order as in MRI, vary the last argument first
    args.reverse!

    result = []
    args.push self

    outer_lambda = args.inject(result.method(:push)) do |trigger, values|
      lambda do |partial|
        values.each do |val|
          trigger.call(partial.dup << val)
        end
      end
    end

    outer_lambda.call([])

    if block_given?
      block_result = self
      result.each { |v| block_result << yield(v) }
      block_result
    else
      result
    end
  end

  def rassoc(obj)
    each do |elem|
      if elem.kind_of? Array and elem.at(1) == obj
        return elem
      end
    end

    nil
  end

  def repeated_combination(combination_size, &block)
    combination_size = combination_size.to_i
    unless block_given?
      return Enumerator.new(self, :repeated_combination, combination_size)
    end

    if combination_size < 0
      # yield nothing
    else
      Rubinius.privately do
        dup.compile_repeated_combinations(combination_size, [], 0, combination_size, &block)
      end
    end

    return self
  end

  def compile_repeated_combinations(combination_size, place, index, depth, &block)
    if depth > 0
      (length - index).times do |i|
        place[combination_size-depth] = index + i
        compile_repeated_combinations(combination_size,place,index + i,depth-1, &block)
      end
    else
      yield place.map { |element| self[element] }
    end
  end

  private :compile_repeated_combinations

  def repeated_permutation(combination_size, &block)
    combination_size = combination_size.to_i
    unless block_given?
      return Enumerator.new(self, :repeated_permutation, combination_size)
    end

    if combination_size < 0
      # yield nothing
    elsif combination_size == 0
      yield []
    else
      Rubinius.privately do
        dup.compile_repeated_permutations(combination_size, [], 0, &block)
      end
    end

    return self
  end

  def compile_repeated_permutations(combination_size, place, index, &block)
    length.times do |i|
      place[index] = i
      if index < (combination_size-1)
        compile_repeated_permutations(combination_size, place, index + 1, &block)
      else
        yield place.map { |element| self[element] }
      end
    end
  end

  private :compile_repeated_permutations

  def reverse
    Array.new dup.reverse!
  end

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

  def reverse_each
    return to_enum(:reverse_each) unless block_given?

    stop = @start - 1
    i = stop + @total
    tuple = @tuple

    while i > stop
      yield tuple.at(i)
      i -= 1
    end

    self
  end

  def rindex(obj=undefined)
    if undefined.equal?(obj)
      return to_enum(:rindex, obj) unless block_given?

      i = @total - 1
      while i >= 0
        return i if yield @tuple.at(@start + i)

        # Compensate for the array being modified by the block
        i = @total if i > @total

        i -= 1
      end
    else
      stop = @start - 1
      i = stop + @total
      tuple = @tuple

      while i > stop
        return i - @start if tuple.at(i) == obj
        i -= 1
      end
    end
    nil
  end

  def rotate(n=1)
    n = Rubinius::Type.coerce_to_collection_index n
    return Array.new(self) if length == 1
    return []       if empty?

    ary = Array.new(self)
    idx = n % ary.size

    ary[idx..-1].concat ary[0...idx]
  end

  def rotate!(cnt=1)
    Rubinius.check_frozen

    return self if length == 0 || length == 1

    ary = rotate(cnt)
    replace ary
  end

  def sample(count=undefined, options=undefined)
    return at Kernel.rand(size) if undefined.equal? count

    if undefined.equal? options
      if o = Rubinius::Type.check_convert_type(count, Hash, :to_hash)
        options = o
        count = nil
      else
        options = nil
        count = Rubinius::Type.coerce_to_collection_index count
      end
    else
      count = Rubinius::Type.coerce_to_collection_index count
      options = Rubinius::Type.coerce_to options, Hash, :to_hash
    end

    if count and count < 0
      raise ArgumentError, "count must be greater than 0"
    end

    rng = options[:random] if options
    rng = Kernel unless rng and rng.respond_to? :rand

    unless count
      random = Rubinius::Type.coerce_to_collection_index rng.rand(size)
      raise RangeError, "random value must be >= 0" if random < 0
      raise RangeError, "random value must be less than Array size" unless random < size

      return at random
    end

    count = size if count > size
    result = Array.new self
    tuple = Rubinius::Mirror::Array.reflect(result).tuple

    count.times do |i|
      random = Rubinius::Type.coerce_to_collection_index rng.rand(size)
      raise RangeError, "random value must be >= 0" if random < 0
      raise RangeError, "random value must be less than Array size" unless random < size

      tuple.swap i, random
    end

    return count == size ? result : result[0, count]
  end

  def find_index(obj=undefined)
    super
  end

  alias_method :index, :find_index

  def to_a
    if self.instance_of? Array
      self
    else
      Array.new(self)
    end
  end

  def fetch(idx, default=undefined)
    orig = idx
    idx = Rubinius::Type.coerce_to_collection_index idx

    idx += @total if idx < 0

    if idx < 0 or idx >= @total
      if block_given?
        return yield(orig)
      end

      return default unless undefined.equal?(default)

      raise IndexError, "index #{idx} out of bounds"
    end

    at(idx)
  end


  def shuffle(options = undefined)
    return dup.shuffle!(options) if instance_of? Array
    Array.new(self).shuffle!(options)
  end

  def shuffle!(options = undefined)
    Rubinius.check_frozen

    random_generator = Kernel

    unless undefined.equal? options
      options = Rubinius::Type.coerce_to options, Hash, :to_hash
      random_generator = options[:random] if options[:random].respond_to?(:rand)
    end

    size.times do |i|
      r = i + random_generator.rand(size - i).to_int
      raise RangeError, "random number too big #{r - i}" if r < 0 || r >= size
      @tuple.swap(@start + i, @start + r)
    end
    self
  end

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
          # @total -= 1 # MODIFIED Can't modify size using @total
          self.shift
          @start += 1
        else
          delete_range(start, 1)
        end
      end
    else
      start = Rubinius::Type.coerce_to_collection_index start
      length = Rubinius::Type.coerce_to_collection_index length

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

  def to_ary
    self
  end

  def transpose
    return [] if empty?

    out = []
    max = nil

    each do |ary|
      ary = Rubinius::Type.coerce_to ary, Array, :to_ary
      max ||= ary.size

      # Catches too-large as well as too-small (for which #fetch would suffice)
      raise IndexError, "All arrays must be same length" if ary.size != max

      ary.size.times do |i|
        entry = (out[i] ||= [])
        entry << ary.at(i)
      end
    end

    out
  end

  # Insertion sort in-place between the given indexes.
  def isort!(left, right)
    i = left + 1

    tup = @tuple

    while i < right
      j = i

      while j > left
        jp = j - 1
        el1 = tup.at(jp)
        el2 = tup.at(j)

        unless cmp = (el1 <=> el2)
          raise ArgumentError, "comparison of #{el1.inspect} with #{el2.inspect} failed (#{j})"
        end

        break unless cmp > 0

        tup.put(j, el1)
        tup.put(jp, el2)

        j = jp
      end

      i += 1
    end
  end
  private :isort!

  # Insertion sort in-place between the given indexes using a block.
  def isort_block!(left, right, block)
    i = left + 1

    while i < right
      j = i

      while j > left
        block_result = block.call(@tuple.at(j - 1), @tuple.at(j))

        if block_result.nil?
          raise ArgumentError, 'block returned nil'
        elsif block_result > 0
          @tuple.swap(j, (j - 1))
          j -= 1
        else
          break
        end
      end

      i += 1
    end
  end
  private :isort_block!

end
