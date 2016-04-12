# TODO java.util.Comparator support?

# *java.util.Collection* is enhanced (not just) to act like Ruby's `Enumerable`.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Collection.html
module Java::java::util::Collection
  include ::Enumerable

  def each
    i = iterator
    while i.hasNext
      yield i.next
    end
  end

  def <<(a); add(a); self end

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
    size
  end

  def join(*args)
    to_a.join(*args)
  end

  def to_a
    # JRUBY-3910: conversion is faster by going directly to java array
    # first
    toArray.to_a
  end

end

# A *java.util.Enumeration* instance might be iterated Ruby style.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Enumeration.html
module Java::java::util::Enumeration
  include ::Enumerable

  def each
    while has_more_elements
      yield next_element
    end
  end
end

# A *java.util.Iterator* acts like an `Enumerable`.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Iterator.html
module Java::java::util::Iterator
  include ::Enumerable

  def each
    while has_next
      yield self.next
    end
  end
end

# Ruby extensions for *java.util.List* instances.
# @see Java::java::util::Collection
# @see http://docs.oracle.com/javase/8/docs/api/java/util/List.html
module Java::java::util::List
  # @private
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
  private_constant :RubyComparators

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

  def index(obj = (no_args = true))
    if !no_args
      ix = 0
      iter = iterator
      while (iter.has_next)
        return ix if obj == iter.next
        ix +=1
      end
      return nil
    elsif block_given?
      ix = 0
      iter = iterator
      while (iter.has_next)
        return ix if yield iter.next
        ix +=1
      end
      return nil
    else
      Enumerator.new(self, :index)
    end
  end

  def rindex(obj = (no_args = true))
    if !no_args
      i = size
      while (i -= 1) >= 0
        return i if obj == get(i)

        # blocks can modify the list, don't go past bounds
        i = size if i > size
      end
      return nil
    elsif block_given?
      i = size
      while (i -= 1) >= 0
        return i if yield get(i)

        # blocks can modify the list, don't go past bounds
        i = size if i > size
      end
      return nil
    else
      Enumerator.new(self, :rindex)
    end
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

  alias_method :to_ary, :to_a

end