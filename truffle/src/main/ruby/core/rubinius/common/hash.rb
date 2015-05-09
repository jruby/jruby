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

unless Rubinius::Config['hash.hamt']
class Hash
  include Enumerable

  attr_reader :size
  attr_reader :capacity
  attr_reader :max_entries

  alias_method :length, :size

  Entries = Rubinius::Tuple

  # Initial size of Hash. MUST be a power of 2.
  MIN_SIZE = 16

  # Allocate more storage when this full. This value grows with
  # the size of the Hash so that the max load factor is 0.75.
  MAX_ENTRIES = 12

  class State
    attr_accessor :head
    attr_accessor :tail

    def self.from(state)
      new_state = new
      new_state.compare_by_identity if state and state.compare_by_identity?
      new_state
    end

    def initialize
      @compare_by_identity = false
      @head = nil
      @tail = nil
    end

    def compare_by_identity?
      @compare_by_identity
    end

    def compare_by_identity
      @compare_by_identity = true

      class << self
        def match?(this_key, this_hash, other_key, other_hash)
          Rubinius::Type.object_equal other_key, this_key
        end
      end

      self
    end

    def match?(this_key, this_hash, other_key, other_hash)
      other_hash == this_hash and (Rubinius::Type::object_equal(other_key, this_key) or other_key.eql?(this_key))
    end
  end

  # Bucket stores key, value pairs in Hash. The key's hash
  # is also cached in the item and recalculated when the
  # Hash#rehash method is called.

  class Bucket
    attr_accessor :key
    attr_accessor :key_hash
    attr_accessor :value
    attr_accessor :link
    attr_accessor :previous
    attr_accessor :next
    attr_accessor :state

    def initialize(key, key_hash, value, state)
      @key      = key
      @key_hash = key_hash
      @value    = value
      @link     = nil
      @state    = state

      if tail = state.tail
        @previous = tail
        state.tail = tail.next = self
      else
        state.head = state.tail = self
      end
    end

    def delete(key, key_hash)
      if @state.match? @key, @key_hash, key, key_hash
        remove
        self
      end
    end

    def remove
      if @previous
        @previous.next = @next
      else
        @state.head = @next
      end

      if @next
        @next.previous = @previous
      else
        @state.tail = @previous
      end
    end
  end

  # An external iterator that returns entries in insertion order.  While
  # somewhat following the API of Enumerator, it is named Iterator because it
  # does not provide <code>#each</code> and should not conflict with
  # +Enumerator+ in MRI 1.8.7+. Returned by <code>Hash#to_iter</code>.

  class Iterator
    def initialize(state)
      @state = state
    end

    # Returns the next object or +nil+.
    def next(item)
      if item
        return item if item = item.next
      else
        return @state.head
      end
    end
  end

  def self.new_from_literal(size)
    allocate
  end

  # Creates a fully-formed instance of Hash.
  def self.allocate
    hash = super()
    Rubinius.privately { hash.__setup__ }
    hash
  end

  def self.new_from_associate_array(associate_array)
    hash = new
    associate_array.each do |array|
      next unless array.respond_to? :to_ary
      array = array.to_ary
      unless (1..2).cover? array.size
        raise ArgumentError, "invalid number of elements (#{array.size} for 1..2)"
      end
      hash[array.at(0)] = array.at(1)
    end
    hash
  end
  private_class_method :new_from_associate_array

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, Hash, :to_hash
  end

  # #entries is a method provided by Enumerable which calls #to_a,
  # so we have to not collide with that.
  attr_reader_specific :entries, :__entries__

  def self.[](*args)
    if args.size == 1
      obj = args.first
      if hash = Rubinius::Type.check_convert_type(obj, Hash, :to_hash)
        new_hash = allocate.replace(hash)
        new_hash.default = nil
        return new_hash
      elsif associate_array = Rubinius::Type.check_convert_type(obj, Array, :to_ary)
        return new_from_associate_array(associate_array)
      end
    end

    return new if args.empty?

    if args.size & 1 == 1
      raise ArgumentError, "Expected an even number, got #{args.length}"
    end

    hash = new
    i = 0
    total = args.size

    while i < total
      hash[args[i]] = args[i+1]
      i += 2
    end

    hash
  end

  def []=(key, value)
    Rubinius.check_frozen

    redistribute @entries if @size > @max_entries

    key_hash = Rubinius.privately { key.hash }
    index = key_hash & @mask

    item = @entries[index]
    unless item
      @entries[index] = new_bucket key, key_hash, value
      return value
    end

    if @state.match? item.key, item.key_hash, key, key_hash
      return item.value = value
    end

    last = item
    item = item.link
    while item
      if @state.match? item.key, item.key_hash, key, key_hash
        return item.value = value
      end

      last = item
      item = item.link
    end
    last.link = new_bucket key, key_hash, value

    value
  end

  alias_method :store, :[]=

  # Used internally to get around subclasses redefining #[]=
  alias_method :__store__, :[]=

  def ==(other)
    return true if self.equal? other
    unless other.kind_of? Hash
      return false unless other.respond_to? :to_hash
      return other == self
    end

    return false unless other.size == size

    Thread.detect_recursion self, other do
      each_item do |item|
        other_item = other.find_item(item.key)

        # Other doesn't even have this key
        return false unless other_item

        # Order of the comparison matters! We must compare our value with
        # the other Hash's value and not the other way around.
        unless Rubinius::Type::object_equal(item.value, other_item.value) or
               item.value == other_item.value
          return false
        end
      end
    end
    true
  end

  def assoc(key)
    each_item { |e| return e.key, e.value if key == e.key }
  end

  def compare_by_identity
    Rubinius.check_frozen

    @state = State.new unless @state
    @state.compare_by_identity
    self
  end

  def compare_by_identity?
    return false unless @state
    @state.compare_by_identity?
  end

  def default(key=undefined)
    if @default_proc and !undefined.equal?(key)
      @default_proc.call(self, key)
    else
      @default
    end
  end

  def default_proc
    @default_proc
  end

  # Sets the default proc to be executed on each key lookup
  def default_proc=(prc)
    Rubinius.check_frozen
    unless prc.nil?
      prc = Rubinius::Type.coerce_to prc, Proc, :to_proc

      if prc.lambda? and prc.arity != 2
        raise TypeError, "default proc must have arity 2"
      end
    end

    @default = nil
    @default_proc = prc
  end

  def delete(key)
    Rubinius.check_frozen
    key_hash = Rubinius.privately { key.hash }

    index = key_index key_hash
    if item = @entries[index]
      if item.delete key, key_hash
        @entries[index] = item.link
        @size -= 1
        return item.value
      end

      last = item
      while item = item.link
        if item.delete key, key_hash
          last.link = item.link
          @size -= 1
          return item.value
        end
        last = item
      end
    end

    return yield(key) if block_given?
  end

  def each_item
    return unless @state

    item = @state.head
    while item
      yield item
      item = item.next
    end
  end

  def each
    return to_enum(:each) unless block_given?

    return unless @state

    item = @state.head
    while item
      yield [item.key, item.value]
      item = item.next
    end

    self
  end

  alias_method :each_pair, :each

  def fetch(key, default=undefined)
    if item = find_item(key)
      return item.value
    end

    return yield(key) if block_given?
    return default unless undefined.equal?(default)
    raise KeyError, "key #{key} not found"
  end

  # Searches for an item matching +key+. Returns the item
  # if found. Otherwise returns +nil+.
  def find_item(key)
    key_hash = Rubinius.privately { key.hash }

    item = @entries[key_index(key_hash)]
    while item
      if @state.match? item.key, item.key_hash, key, key_hash
        return item
      end
      item = item.link
    end
  end

  def flatten(level=1)
    to_a.flatten(level)
  end

  def keep_if
    return to_enum(:keep_if) unless block_given?

    Rubinius.check_frozen

    each_item { |e| delete e.key unless yield(e.key, e.value) }

    self
  end

  def initialize(default=undefined, &block)
    Rubinius.check_frozen

    if !undefined.equal?(default) and block
      raise ArgumentError, "Specify a default or a block, not both"
    end

    if block
      @default = nil
      @default_proc = block
    elsif !undefined.equal?(default)
      @default = default
      @default_proc = nil
    end

    self
  end
  private :initialize

  def merge!(other)
    Rubinius.check_frozen

    other = Rubinius::Type.coerce_to other, Hash, :to_hash

    if block_given?
      other.each_item do |item|
        key = item.key
        if key? key
          __store__ key, yield(key, self[key], item.value)
        else
          __store__ key, item.value
        end
      end
    else
      other.each_item do |item|
        __store__ item.key, item.value
      end
    end
    self
  end

  alias_method :update, :merge!

  # Returns a new +Bucket+ instance having +key+, +key_hash+,
  # and +value+. If +key+ is a kind of +String+, +key+ is
  # duped and frozen.
  def new_bucket(key, key_hash, value)
    if key.kind_of?(String) and !key.frozen? and !compare_by_identity?
      key = key.dup
      key.freeze
    end

    @size += 1
    Bucket.new key, key_hash, value, @state
  end
  private :new_bucket

  # Adjusts the hash storage and redistributes the entries among
  # the new bins. Any Iterator instance will be invalid after a
  # call to #redistribute. Does not recalculate the cached key_hash
  # values. See +#rehash+.
  def redistribute(entries)
    capacity = @capacity

    # Rather than using __setup__, initialize the specific values we need to
    # change so we don't eg overwrite @state.
    @capacity    = capacity * 2
    @entries     = Entries.new @capacity
    @mask        = @capacity - 1
    @max_entries = @max_entries * 2

    i = -1
    while (i += 1) < capacity
      next unless old = entries[i]
      while old
        old.link = nil if nxt = old.link

        index = key_index old.key_hash
        if item = @entries[index]
          old.link = item
        end
        @entries[index] = old

        old = nxt
      end
    end
  end

  def rassoc(value)
    each_item { |e| return e.key, e.value if value == e.value }
  end

  def replace(other)
    Rubinius.check_frozen

    other = Rubinius::Type.coerce_to other, Hash, :to_hash
    return self if self.equal? other

    # Normally this would be a call to __setup__, but that will create a new
    # unused Tuple that we would wind up replacing anyways.
    @capacity = other.capacity
    @entries  = Entries.new @capacity
    @mask     = @capacity - 1
    @size     = 0
    @max_entries = other.max_entries

    @state = State.new
    @state.compare_by_identity if other.compare_by_identity?

    other.each_item do |item|
      __store__ item.key, item.value
    end

    @default = other.default
    @default_proc = other.default_proc

    self
  end

  alias_method :initialize_copy, :replace
  private :initialize_copy

  def select
    return to_enum(:select) unless block_given?

    selected = Hash.allocate

    each_item do |item|
      if yield(item.key, item.value)
        selected[item.key] = item.value
      end
    end

    selected
  end

  def select!
    return to_enum(:select!) unless block_given?

    Rubinius.check_frozen

    return nil if empty?

    size = @size
    each_item { |e| delete e.key unless yield(e.key, e.value) }
    return nil if size == @size

    self
  end

  def shift
    Rubinius.check_frozen

    return default(nil) if empty?

    item = @state.head
    delete item.key

    return item.key, item.value
  end

  # Sets the underlying data structures.
  #
  # @capacity is the maximum number of +@entries+.
  # @max_entries is the maximum number of entries before redistributing.
  # @size is the number of pairs, equivalent to <code>hsh.size</code>.
  # @entrien is the vector of storage for the item chains.
  def __setup__(capacity=MIN_SIZE, max=MAX_ENTRIES, size=0)
    @capacity = capacity
    @mask     = capacity - 1
    @max_entries = max
    @size     = size
    @entries  = Entries.new capacity
    @state    = State.new
  end
  private :__setup__

  def to_h
    if instance_of? Hash
      self
    else
      Hash.allocate.replace(to_hash)
    end
  end

  # Returns an external iterator for the bins. See +Iterator+
  def to_iter
    Iterator.new @state
  end
  def eql?(other)
    # Just like ==, but uses eql? to compare values.
    return true if self.equal? other
    unless other.kind_of? Hash
      return false unless other.respond_to? :to_hash
      return other.eql?(self)
    end

    return false unless other.size == size

    Thread.detect_recursion self, other do
      each_item do |item|
        other_item = other.find_item(item.key)

        # Other doesn't even have this key
        return false unless other_item

        # Order of the comparison matters! We must compare our value with
        # the other Hash's value and not the other way around.
        unless Rubinius::Type::object_equal(item.value, other_item.value) or
               item.value.eql?(other_item.value)
          return false
        end
      end
    end
    true
  end

  def hash
    val = size
    Thread.detect_outermost_recursion self do
      each_item do |item|
        val ^= item.key.hash
        val ^= item.value.hash
      end
    end

    val
  end

  def [](key)
    if item = find_item(key)
      item.value
    else
      default key
    end
  end

  def clear
    Rubinius.check_frozen
    __setup__
    self
  end

  def default=(value)
    @default_proc = nil
    @default = value
  end

  def delete_if(&block)
    return to_enum(:delete_if) unless block_given?

    Rubinius.check_frozen

    select(&block).each { |k, v| delete k }
    self
  end

  def each_key
    return to_enum(:each_key) unless block_given?

    each_item { |item| yield item.key }
    self
  end

  def each_value
    return to_enum(:each_value) unless block_given?

    each_item { |item| yield item.value }
    self
  end

  # Returns true if there are no entries.
  def empty?
    @size == 0
  end

  def index(value)
    each_item do |item|
      return item.key if item.value == value
    end
  end

  alias_method :key, :index

  def inspect
    out = []
    return '{...}' if Thread.detect_recursion self do
      each_item do |item|
        str =  item.key.inspect
        str << '=>'
        str << item.value.inspect
        out << str
      end
    end

    ret = "{#{out.join ', '}}"
    Rubinius::Type.infect(ret, self) unless empty?
    ret
  end

  alias_method :to_s, :inspect

  def invert
    inverted = {}
    each_item do |item|
      inverted[item.value] = item.key
    end
    inverted
  end

  def key?(key)
    find_item(key) != nil
  end

  alias_method :has_key?, :key?
  alias_method :include?, :key?
  alias_method :member?, :key?

  # Calculates the +@entries+ slot given a key_hash value.
  def key_index(key_hash)
    key_hash & @mask
  end
  private :key_index

  def keys
    ary = []
    each_item do |item|
      ary << item.key
    end
    ary
  end

  def merge(other, &block)
    dup.merge!(other, &block)
  end

  # Recalculates the cached key_hash values and reorders the entries
  # into a new +@entries+ vector. Does NOT change the size of the
  # hash. See +#redistribute+.
  def rehash
    capacity = @capacity
    entries  = @entries

    @entries = Entries.new @capacity

    i = -1
    while (i += 1) < capacity
      next unless old = entries[i]
      while old
        old.link = nil if nxt = old.link

        index = key_index(old.key_hash = old.key.hash)
        if item = @entries[index]
          old.link = item
        end
        @entries[index] = old

        old = nxt
      end
    end

    self
  end

  def reject(&block)
    return to_enum(:reject) unless block_given?

    hsh = dup.delete_if(&block)
    hsh.taint if tainted?
    hsh
  end

  def reject!(&block)
    return to_enum(:reject!) unless block_given?

    Rubinius.check_frozen

    unless empty?
      size = @size
      delete_if(&block)
      return self if size != @size
    end

    nil
  end

  def sort(&block)
    to_a.sort(&block)
  end

  def to_a
    ary = []

    each_item do |item|
      ary << [item.key, item.value]
    end

    Rubinius::Type.infect ary, self
    ary
  end

  def to_hash
    self
  end

  def value?(value)
    each_item do |item|
      return true if item.value == value
    end
    false
  end

  alias_method :has_value?, :value?

  def values
    ary = []

    each_item do |item|
      ary << item.value
    end

    ary
  end

  def values_at(*args)
    args.map do |key|
      if item = find_item(key)
        item.value
      else
        default key
      end
    end
  end

  alias_method :indices, :values_at
  alias_method :indexes, :values_at
end
end

class Hash

  # Renamed version of the Rubinius Hash#[] method
  #def self.[](*args)
  def self._constructor_fallback(*args)
    if args.size == 1
      obj = args.first
      if hash = Rubinius::Type.check_convert_type(obj, Hash, :to_hash)
        new_hash = allocate.replace(hash)
        new_hash.default = nil
        return new_hash
      elsif associate_array = Rubinius::Type.check_convert_type(obj, Array, :to_ary)
        return new_from_associate_array(associate_array)
      end
    end

    return new if args.empty?

    if args.size & 1 == 1
      raise ArgumentError, "Expected an even number, got #{args.length}"
    end

    hash = new
    i = 0
    total = args.size

    while i < total
      hash[args[i]] = args[i+1]
      i += 2
    end

    hash
  end

end
