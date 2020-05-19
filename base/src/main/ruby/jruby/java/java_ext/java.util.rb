# NOTE: these Ruby extensions were moved to native code!
# - **org.jruby.javasupport.ext.JavaUtil.java**
# this file is no longer loaded but is kept to provide doc stubs

# *java.util.Collection* is enhanced (not just) to act like Ruby's `Enumerable`.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Collection.html
module Java::java::util::Collection
  include ::Enumerable

  # @see Java::java::lang::Iterable#each
  def each(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # @see Java::java::lang::Iterable#each_with_index
  def each_with_index(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Re-implemented for efficiency, so that we do not (`#each`) loop over the collection
  # for types where its not necessary (e.g. *java.util.Set* instances), uses (native) `java.util.Collection#contains`.
  # @see Java::java::lang::Iterable#include?
  # @return [true, false]
  # @since 9.1.3
  def include?(obj)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias member? include?

  # `Enumerable#first`
  # @note Might collide with *java.util.Deque#getFirst* in which case you want to alias its ruby_ name
  #       so that the Ruby version is used e.g. `java.util.ArrayDeque.class_eval { alias first ruby_first }`
  # @since 9.1
  def first(count = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias ruby_first first

  # Pushes (adds) and element into this collection.
  # @example
  #    coll = java.util.ArrayDeque.new
  #    coll << 111
  # @return [Java::java::util::Collection]
  def <<(a)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # add(a)
    # self
  end

  # Join this collection with another (adding all elements).
  # @example
  #    coll1 = java.util.ArrayList.new [1, 2]
  #    coll2 = java.util.LinkedHashSet.new [2, 3]
  #    coll1 + coll2 # [ 1, 2, 2, 3 ] (java.util.ArrayList)
  #    coll2 + coll1 # [ 2, 3, 1 ] (java.util.LinkedHashSet)
  # @return [Java::java::util::Collection] a new collection
  def +(oth)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # nw = self.dup
    # nw.addAll(oth)
    # nw
  end

  # Subtract all elements in the provided collection from this one.
  # @example
  #    coll1= java.util.HashSet.new [2, 3]
  #    coll2 = java.util.LinkedList.new [1, 2]
  #    coll1 - coll2 # [ 3 ] (java.util.HashSet)
  # @return [Java::java::util::Collection] a new collection
  def -(oth)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # nw = self.dup
    # nw.removeAll(oth)
    # nw
  end

  # @return [Integer] the collection size
  def size
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias length size

  # @private Not sure if this makes sense to have.
  def join(*args)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Converts a collection instance to an array.
  # @return [Array]
  def to_a
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  # @since 9.1.3
  alias entries to_a

  # Return a dup-ed collection (if possible).
  # @example
  #    arr = java.util.concurrent.CopyOnWriteArrayList.new ['0']
  #    arr.dup # ['0'] a new CopyOnWriteArrayList instance
  # @return [Java::java::util::Collection] of the same type as target.
  # @since 9.1
  def dup
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

end if false

# A *java.util.Enumeration* instance might be iterated Ruby style.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Enumeration.html
module Java::java::util::Enumeration
  include ::Enumerable

  def each
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # while hasMoreElements
    #   yield nextElement
    # end
  end
end if false

# A *java.util.Iterator* acts like an `Enumerable`.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Iterator.html
module Java::java::util::Iterator
  include ::Enumerable

  def each
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # while hasNext
    #   yield next
    # end
  end
end if false

# Ruby extensions for *java.util.List* instances.
# All of the {@link Java::java::util::Collection} methods are available.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
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

  # Element retrieval (slicing) similar to `Array#[]`.
  # @example
  #    list = java.util.ArrayList.new ['foo', 'bar', 'baz']
  #    list[0] # 'foo'
  #    list[-2] # 'bar'
  #    list[10] # nil
  #    list[1, 2] # ['bar','baz'] sub-list
  #    list[0..2] # ['foo','bar'] sub-list
  # @return [Java::java::util::List, Object, nil] sub-list, list element or nil
  # @note Like with an `Array` will return *nil* for "out-of-bound" indexes.
  def [](i1, i2 = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Set an element or multiple elements like `Array#[]=`.
  # @example
  #    list = java.util.ArrayList.new ['foo', 'bar', 'baz']
  #    list[-3] = 1 # [1, 'bar', 'baz']
  #    list[4] = 4 # [1, 'bar', 'baz', nil, 4]
  #    list[1..2] = 3 # [1, nil, 3, 4]
  # @return [Object] set value
  def []=(i, val)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Return an index for the first occurrence of the matched element (similar to `Array#index`).
  # @example
  #    list = java.util.LinkedList.new [ 'foo', 'bar', 'foo' ]
  #    list.index 'foo' # 0
  #    list.index 'baz' # nil
  #    list.index { |e| e.start_with?('ba') } # 1
  # @return [Integer, nil, Enumerator]
  def index(obj = nil, &block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Return an index for the last occurrence of the matched element (similar to `Array#rindex`).
  # @example
  #    list = java.util.LinkedList.new [ 'foo', 'bar', 'foo' ]
  #    list.rindex 'foo' # 2
  #    list.rindex 'baz' # nil
  #    list.rindex { |e| e.start_with?('ba') } # 1
  # @return [Integer, nil, Enumerator]
  def rindex(obj = nil, &block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end

  # Sort a (mutable) list, returning a new instance of same type.
  # @example
  #    list = java.util.ArrayList.new [ 22, 1, 333 ]
  #    list.sort # [ 1, 22, 333 ] (java.util.ArrayList) ... only works on Java 7 by default!
  # @note Since Java 8 this method collides with the built-in *java.util.List#sort* in which case you want to alias its
  #       ruby_ name for list types where you want to have the Ruby version available
  #       e.g. `java.util.ArrayList.class_eval { alias sort ruby_sort }`
  # @return [Java::java::util::List] dup-ed list with element sorted
  def sort(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # comparator = block ? RubyComparators::BlockComparator.new(block) : RubyComparators::SpaceshipComparator.new
    # list = self.dup
    # java::util::Collections.sort(list, comparator)
    # list
  end
  alias ruby_sort sort

  # Sort a (mutable) list, in place.
  # @example
  #    list = java.util.Vector.new [ '22', '1', '333' ]
  #    list.sort! { |a, b| a.length <=> b.length } # [ '1', '22', '333' ]
  # @return [Java::java::util::List] self
  def sort!(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
    # comparator = block ? RubyComparators::BlockComparator.new(block) : RubyComparators::SpaceshipComparator.new
    # java::util::Collections.sort(self, comparator)
    # self
  end

  # @see Java::java::util::Collection#to_a
  # @return [Array]
  def to_a
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias_method :to_ary, :to_a

  # `Enumerable#first`
  # @example
  #    list = java.util.ArrayList.new [1, 2, 3]
  #    expect( list.first ).to eq 1
  #    expect( list.first(2).to_a ).to eq [1, 2]
  #    list = java.util.LinkedList.new
  #    expect( list.ruby_first ).to be nil
  # @note Might collide with *#getFirst* on some list impls in which case you want to alias its ruby_ name
  #       so that the Ruby version is used e.g. `java.util.LinkedList.class_eval { alias first ruby_first }`
  # @since 9.1
  def first(count = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias ruby_first first

  # `Array#last`
  # @example
  #    list = java.util.Vector.new [1, 2, 3]
  #    expect( list.last ).to eq 3
  #    expect( list.last(2).to_a ).to eq [2, 3]
  # @note Might collide with *#getLast* on some list impls in which case you want to alias its ruby_ name
  #       so that the Ruby version is used e.g. `java.util.LinkedList.class_eval { alias last ruby_last }`
  # @since 9.1
  def last(count = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaUtil.java
  end
  alias ruby_last last

end if false

# Ruby extensions for *java.util.Map* instances.
# Generally maps behave like Ruby's `Hash` objects.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/Map.html
module Java::java::util::Map

  def default(arg = nil)
    # stub
  end

  def default=(value)
    # stub
  end

  def default_proc()
    # stub
  end

  def default_proc=(proc)
    # stub
  end

  alias size length

  def empty?
    # stub
  end

  # @return [Array]
  def to_a
    # stub
  end

  # @return [Proc]
  def to_proc
    # stub
  end

  # @return [Hash]
  def to_h
    # stub
  end
  alias to_hash to_h

  def [](key)
    # stub
  end

  def []=(key, value)
    # stub
  end
  alias store []

  def fetch(key, default = nil, &block)
    # stub
  end

  def key?(key)
    # stub
  end
  alias has_key? key?
  alias include? key?
  alias member? key?

  def value?(value)
    # stub
  end
  alias has_value? value?

  def each(&block)
    # stub
  end
  alias each_pair each

  def each_key(&block)
    # stub
  end

  def each_value(&block)
    # stub
  end

  def ==(other)
    # stub
  end

  def <(other)
    # stub
  end

  def <=(other)
    # stub
  end

  def >(other)
    # stub
  end

  def >=(other)
    # stub
  end

  def select(&block)
    # stub
  end

  def select!(&block)
    # stub
  end

  def keep_if(&block)
    # stub
  end

  def sort(&block)
    # stub
  end

  def delete(key, &block)
    # stub
  end

  def delete_if(&block)
    # stub
  end

  def reject(&block)
    # stub
  end

  def reject!(&block)
    # stub
  end

  def invert
    # stub
  end

  def key(value)
    # stub
  end

  def keys
    # stub
  end

  def values
    # stub
  end
  alias ruby_values values

  def values_at(*args)
    # stub
  end

  def fetch_values(*args)
    # stub
  end

  def clear
    # stub
  end
  alias ruby_clear clear

  # `Hash#merge`
  # @example
  #    map = java.util.HashMap.new({ 1 => '1', 2 => '2' })
  #    map.merge 1 => 'one' # { 1 => "one", 2 => "2" } (java.util.HashMap)
  # @note Since Java 8 this method collides with the built-in *java.util.Map#merge* in which case you want to alias its
  #       ruby_ name for map types where you want to have the Ruby version available
  #       e.g. `java.util.HashMap.class_eval { alias merge ruby_merge }`
  # @return [Java::java::util::Map] merged map instance
  def merge(other, &block)
    # stub
  end
  alias ruby_merge merge

  def merge!(other, &block)
    # stub
  end

  # `Hash#replace`
  # @example
  #    map1 = java.util.Hashtable.new({ 1 => '1', 2 => '2' })
  #    map2 = java.util.LinkedHashMap.new; map2[1] = 'one'
  #    map1.replace map2 # { 1 => "one" } (java.util.Hashtable)
  # @note Since Java 8 this method collides with the built-in *java.util.Map#replace* in which case you want to alias its
  #       ruby_ name for map types where you want to have the Ruby version available
  #       e.g. `java.util.Hashtable.class_eval { alias replace ruby_replace }`
  # @return [Java::java::util::Map] replaced map instance
  def replace(other)
    # stub
  end
  alias ruby_replace replace

  def flatten(level = nil)
    # stub
  end

  def assoc(obj)
    # stub
  end

  def rassoc(obj)
    # stub
  end

  def any?(&block)
    # stub
  end

  def dig(*args)
    # stub
  end

end if false