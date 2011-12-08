# TODO java.util.Comparator support?
module java::util::Collection
  include Enumerable
  def each(&block)
    iter = iterator
    while iter.hasNext
      block.call(iter.next)
    end
  end
  def <<(a); add(a); self; end
  def +(oth)
    nw = self.dup
    nw.addAll(oth)
    nw
  end
  def -(oth)
    nw = self.dup
    nw.removeAll(oth)
    nw
  end
  def length
    self.size
  end
  def join(*args)
    self.to_a.join(*args)
  end
  def to_a
    # JRUBY-3910: conversion is faster by going directly to java array
    # first
    toArray.to_a
  end
end

module java::util::Enumeration
  include Enumerable

  def each
    while (has_more_elements)
      yield next_element
    end
  end
end

module java::util::Iterator
  include Enumerable

  def each
    while (has_next)
      yield self.next
    end
  end
end

module java::util::List
  module RubyComparators
    class BlockComparator
      include java::util::Comparator

      def initialize(block)
        @block = block
      end

      def compare(o1, o2)
        @block.call(o1, o2)
      end
    end

    class SpaceshipComparator
      include java::util::Comparator
      def compare(o1, o2)
        o1 <=> o2
      end
    end
  end

  def [](ix1, ix2 = nil)
    if (ix2)
      sub_list(ix1, ix1 + ix2)
    elsif (ix1.is_a?(Range))
      sub_list(ix1.first, ix1.exclude_end? ? ix1.last : ix1.last + 1)
    elsif ix1 < size
      get(ix1)
    else
      nil
    end
  end

  def []=(ix,val)
    if (ix.is_a?(Range))
      ix.each { |i| remove(i) }
      add_all(ix.first, val)
    elsif size < ix
      ((ix-size)+1).times { self << nil }
    end
    set(ix,val)
    val
  end

  def sort(&block)
    comparator = block ? RubyComparators::BlockComparator.new(block) : RubyComparators::SpaceshipComparator.new
    list = java::util::ArrayList.new
    list.addAll(self)
    java::util::Collections.sort(list, comparator)
    list
  end

  def sort!(&block)
    comparator = block ? RubyComparators::BlockComparator.new(block) : RubyComparators::SpaceshipComparator.new
    java::util::Collections.sort(self, comparator)
    self
  end
end
