# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

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

unless Rubinius::Config['hash.hamt']
class Hash
  include Enumerable

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

  def default(key=undefined)
    if default_proc and !undefined.equal?(key)
      default_proc.call(self, key)
    else
      internal_default_value
    end
  end

  # Sets the default proc to be executed on each key lookup
  def default_proc=(prc)
    Truffle.check_frozen
    unless prc.nil?
      prc = Rubinius::Type.coerce_to prc, Proc, :to_proc

      if prc.lambda? and prc.arity != 2
        raise TypeError, "default proc must have arity 2"
      end
    end

    @default = nil
    @default_proc = prc
  end

  def dig(key, *more)
    result = self[key]
    if result.nil? || more.empty?
      result
    else
      raise TypeError, "#{result.class} does not have #dig method" unless result.respond_to?(:dig)
      result.dig(*more)
    end
  end

  def fetch(key, default=undefined)
    if item = find_item(key)
      return item.value
    end

    return yield(key) if block_given?
    return default unless undefined.equal?(default)
    raise KeyError, "key #{key} not found"
  end

  def fetch_values(*keys, &block)
    keys.map do |key|
      self.fetch(key, &block)
    end
  end

  def flatten(level=1)
    to_a.flatten(level)
  end

  def keep_if
    return to_enum(:keep_if) { size } unless block_given?

    Truffle.check_frozen

    each_item { |e| delete e.key unless yield(e.key, e.value) }

    self
  end

  def merge!(other)
    Truffle.check_frozen

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

  def rassoc(value)
    each_item { |e| return e.key, e.value if value == e.value }
  end

  def select
    return to_enum(:select) { size } unless block_given?

    selected = Hash.allocate

    each_item do |item|
      if yield(item.key, item.value)
        selected[item.key] = item.value
      end
    end

    selected
  end

  def select!
    return to_enum(:select!) { size } unless block_given?

    Truffle.check_frozen

    return nil if empty?

    previous_size = size
    each_item { |e| delete e.key unless yield(e.key, e.value) }
    return nil if previous_size == size

    self
  end

  def to_h
    if instance_of? Hash
      self
    else
      Hash.allocate.replace(to_hash)
    end
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

  def delete_if(&block)
    return to_enum(:delete_if) { size } unless block_given?

    Truffle.check_frozen

    select(&block).each { |k, v| delete k }
    self
  end

  def each_key
    return to_enum(:each_key) { size } unless block_given?

    each_item { |item| yield item.key }
    self
  end

  def each_value
    return to_enum(:each_value) { size } unless block_given?

    each_item { |item| yield item.value }
    self
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

  def key?(key)
    find_item(key) != nil
  end

  alias_method :has_key?, :key?
  alias_method :include?, :key?
  alias_method :member?, :key?

  def keys
    ary = []
    each_item do |item|
      ary << item.key
    end
    ary
  end

  def reject!(&block)
    return to_enum(:reject!) { size } unless block_given?

    Truffle.check_frozen

    unless empty?
      previous_size = size
      delete_if(&block)
      return self if previous_size != size
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

  def invert
    inverted = {}
    each_pair { |key, value|
      inverted[value] = key
    }
    inverted
  end

  def to_hash
    self
  end

  # Implementation of a fundamental Rubinius method that allows their Hash
  # implementation to work. We probably want to remove uses of this in the long
  # term, as it creates objects which already exist for them and we have to
  # create, but for now it allows us to use more Rubinius code unmodified.

  class KeyValue

    attr_reader :key
    attr_reader :value

    def initialize(key, value)
      @key = key
      @value = value
    end

  end

  alias_method :each_original, :each

  def each_item
    # use aliased each to protect against overriding #each
    each_original do |key, value|
      yield KeyValue.new(key, value)
    end
    nil
  end

  def find_item(key)
    value = _get_or_undefined(key)
    if undefined.equal?(value)
      nil
    else
      # TODO CS 7-Mar-15 maybe we should return the stored key?
      KeyValue.new(key, value)
    end
  end

  def merge_fallback(other, &block)
    merge(Rubinius::Type.coerce_to other, Hash, :to_hash, &block)
  end

  # Rubinius' Hash#reject taints but we don't want this

  def reject(&block)
    return to_enum(:reject) { size } unless block_given?
    copy = dup
    copy.untaint
    copy.delete_if(&block)
    copy
  end

end
end
