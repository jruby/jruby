require File.dirname(__FILE__) + "/../spec_helper"

describe "Collection Ruby extensions" do
  before(:each) do
    @data = ['foo', 'baz', 'bar', '']
  end

  it 'iterates using each' do
    set = java.util.LinkedHashSet.new
    @data.each { |elem| set.add elem }
    data = []
    set.each { |elem| data << elem }
    expect(data).to eq @data

    data = []
    ret = java.util.concurrent.LinkedBlockingQueue.new(@data).each { |elem| data << elem }
    expect(data).to eq @data
    expect(ret).to be_a java.util.concurrent.LinkedBlockingQueue
  end

  it 'iterates with an Enumerator on #each' do
    enum = java.util.concurrent.LinkedBlockingQueue.new(@data).each
    expect( enum.next ).to eq 'foo'
    expect( enum.next ).to eq 'baz'
    expect( enum.next ).to eq 'bar'
    enum.next
    expect { enum.next }.to raise_error(StopIteration)
  end

  it 'iterates with index' do
    set = java.util.LinkedHashSet.new
    @data.each { |elem| set.add elem }
    data = []; idx = []
    ret = set.each_with_index { |elem, i| data << elem; idx << i }
    expect(data).to eq @data
    expect(idx).to eq [0, 1, 2, 3]
    expect(ret).to be set

    data = []
    java.util.concurrent.LinkedBlockingQueue.new.each_with_index { |elem| data << elem }
    expect(data).to eq []
  end

  it 'iterates with an Enumerator on #each_with_index' do
    enum = java.util.LinkedHashSet.new(@data).each_with_index
    expect( enum.next[0] ).to eq 'foo'
    expect( enum.next[1] ).to eq 1
    expect( enum.next[0] ).to eq 'bar'
    expect( enum.next[1] ).to eq 3
    expect { enum.next }.to raise_error(StopIteration)
  end

  it 'supports (Enumerable\'s) first' do
    set = java.util.LinkedHashSet.new [ 'foo', 'bar', 'baz' ]
    expect( set.first ).to eq 'foo'
    expect( set.first(2) ).to eq ['foo', 'bar']
    expect( set.first(1) ).to eq ['foo']
    expect( set.first(8) ).to eq ['foo', 'bar', 'baz']
    expect( set.first(0) ).to eq []
    set.clear
    expect( set.first ).to be nil

    # java.util.Queue conflicts since it has getFirst :
    que = java.util.ArrayDeque.new [1, 2, 3]
    expect( que.first ).to eq 1
    expect( que.ruby_first(2).to_a ).to eq [1, 2]
    expect( que.ruby_first(0).to_a ).to eq []
    que.clear
    expect( que.ruby_first ).to be nil
  end

  it 'handles << as add' do
    set = java.util.HashSet.new
    set << 'ZZZ'
    expect( set.iterator.next ).to eq 'ZZZ'
    set << 'ZZZ'; set << 'AAA'; set << 'ZZZ'
    expect( set.size ).to eq 2
  end

  it 'converts to_a' do
    coll = java.util.ArrayDeque.new(@data)
    expect(coll.to_a).to eq(@data.to_a)

    coll = java.util.LinkedHashSet.new(@data)
    expect(coll.entries).to eq(@data.to_a)

    coll = java.util.HashSet.new
    expect(coll.to_a).to eq([])
  end

  it 'reports list size/length' do
    list = java.util.TreeSet.new(@data)
    expect(list.length).to eq(@data.size)
    expect(list.size).to eq(list.length)
    expect( java.util.Collections.emptySet.length ).to eq 0
  end

  it 'adds collections' do
    set = java.util.LinkedHashSet.new ['foo', 'baz', 'bar', '']
    vec = java.util.Vector.new ['', 'baz', 'zzz', 1]
    expect( coll = set + vec ).to be_a java.util.LinkedHashSet
    expect( coll.to_a ).to eql ['foo', 'baz', 'bar', '', 'zzz', 1]

    vec = java.util.ArrayList.new ['', 'baz', 'zzz', 1]
    expect( coll = vec + set ).to be_a java.util.ArrayList
    expect( coll.to_a ).to eql ['', 'baz', 'zzz', 1, 'foo', 'baz', 'bar', '']

    expect( vec.to_a ).to eql ['', 'baz', 'zzz', 1] # not affected!
    expect( set.to_a ).to eql ['foo', 'baz', 'bar', ''] # not affected!
  end

  it 'distracts collections' do
    set = java.util.LinkedHashSet.new ['foo', 'baz', 'bar', '']
    vec = java.util.ArrayList.new ['', 'baz', 'zzz', 1]
    expect( coll = set - vec ).to be_a java.util.LinkedHashSet
    expect( coll.to_a ).to eql ['foo', 'bar']

    vec = java.util.Vector.new ['', 'baz', 'zzz', 1]
    expect( coll = vec - set ).to be_a java.util.Vector
    expect( coll.to_a ).to eql ['zzz', 1]

    expect( vec.to_a ).to eql ['', 'baz', 'zzz', 1] # not affected!
    expect( set.to_a ).to eql ['foo', 'baz', 'bar', ''] # not affected!
  end

  it 'dups' do
    set = java.util.HashSet.new ['0']
    expect( set.dup ).to be_a java.util.HashSet
    set.dup.add '1'
    expect( set.to_a ).to eql ['0']

    arr = java.util.ArrayList.new [0]
    expect( arr.dup ).to be_a java.util.ArrayList
    arr.dup.set(0, 1)
    expect( arr.to_a ).to eql [0]

    # a non java.lang.Cloneable collection :
    arr = java.util.concurrent.LinkedBlockingQueue.new; arr.add 42
    expect( arr.dup ).to be_a java.util.concurrent.LinkedBlockingQueue
    expect( arr.dup.poll ).to eq 42
    expect( arr.to_a ).to eql [42]

    # immutable and Cloneable
    arr = Java::java_integration::fixtures::coll::CloneableImmutableList.new
    expect( arr.dup ).to equal arr

    # non-Cloneable with public <init>()
    arr = Java::java_integration::fixtures::coll::NonCloneableList.new; arr.add(42)
    expect( arr.dup ).to eql arr
    expect( arr.dup ).to_not equal arr

    # immutable and non-Cloneable
    arr = Java::java_integration::fixtures::coll::NonCloneableImmutableList.new
    expect { arr.dup }.to raise_error(Java::JavaLang::UnsupportedOperationException)

    arr = Java::java_integration::fixtures::coll::NonCloneableImmutableList2::INSTANCE
    expect { arr.dup }.to raise_error(Java::JavaLang::IllegalStateException) # since 9.2 (swallowed previously)

    arr = Java::java_integration::fixtures::coll::NonCloneableImmutableList3.new
    expect { arr.dup }.to raise_error(Java::JavaLang::UnsupportedOperationException) # not CloneNotSupportedException
  end

  it 'clones' do
    arr = java.util.concurrent.CopyOnWriteArrayList.new ['0']
    expect( arr.clone ).to be_a java.util.concurrent.CopyOnWriteArrayList
    arr.clone.add '1'
    expect( arr.to_a ).to eql ['0']

    arr = java.util.LinkedList.new [0]
    expect( arr.clone ).to be_a java.util.LinkedList
    arr.clone.set(0, 1)
    expect( arr.to_a ).to eql [0]

    # a non java.lang.Cloneable collection :
    set = java.util.concurrent.CopyOnWriteArraySet.new ['0']
    expect( set.clone ).to be_a java.util.concurrent.CopyOnWriteArraySet
    set.clone.add '1'
    expect( set.to_a ).to eql ['0']

    # immutable and Cloneable
    arr = Java::java_integration::fixtures::coll::CloneableImmutableList.new
    expect( arr.clone ).to equal arr

    # non-Cloneable with public <init>()
    arr = Java::java_integration::fixtures::coll::NonCloneableList.new; arr.add(42)
    expect( arr.clone ).to eql arr
    expect( arr.clone ).to_not equal arr

    # immutable and non-Cloneable
    arr = Java::java_integration::fixtures::coll::NonCloneableImmutableList.new
    expect { arr.clone }.to raise_error(java.lang.UnsupportedOperationException)
  end

  it '#include?' do
    set = java.util.LinkedHashSet.new [1, 2, 3]
    expect( set.include? 1 ).to be true
    expect( set.member? 2 ).to be true
    expect( set.contains 3 ).to be true
    set.add 4; set.add 5.to_java
    expect( set.include? 4 ).to be true
    expect( set.include? 5 ).to be true
    expect( set.contains 4 ).to be true
    expect( set.contains 5 ).to be true
    expect( set.contains 4.to_java ).to be true
    expect( set.contains 5.to_java ).to be true
    expect( set.contains 5.to_java(:byte) ).to be false
    expect( set.contains 5.to_java(:short) ).to be false
    expect( set.contains 6 ).to be false
    expect( set.include? 6 ).to be false
    expect( set.include? 4.to_java(:byte) ).to be false
    expect( set.include? 4.to_java(:short) ).to be false
    expect( set.include? 2.to_java ).to be true
    expect( set.include? 2.to_java(:short) ).to be false
  end

  it '#include? (specific)' do
    pending 'due Java numeric conversion can not add to_java(:xxx) to collection'

    set = java.util.LinkedHashSet.new [2]
    set << 1.to_java(:short)
    expect( set.include? 1.to_java ).to be true
    expect( set.include? 1.to_java(:short) ).to be true

    set.add 3.to_java(:short)
    expect( set.contains 3.to_java ).to be true
    expect( set.contains 3.to_java(:short) ).to be true
  end

  it 'counts' do
    vec = java.util.Vector.new [1, 2, 2, 3, 2, 4, 2, 5, 2]
    expect( vec.count ).to eql 9
    expect( java.util.Collections::EMPTY_SET.count ).to eql 0

    expect( vec.count { |i| i > 2 } ).to eq 3
    expect( vec.count(2) ).to eq 5

    expect( java.util.HashSet.new(vec).count(2) ).to eq 1
    expect( java.util.LinkedHashSet.new(vec).count(0) ).to eq 0
  end

  it "should respect to_ary objects defined on iteration" do
    class Pair
      def initialize(a, b)
        @a = a ; @b = b
      end

      def to_ary
        [@a, @b]
      end
    end

    java.util.HashSet.new([Pair.new(:x, :y)]).each do |car, cdr|
      expect(car).to eq(:x)
      expect(cdr).to eq(:y)
    end
  end

  describe 'Ruby class' do

    require 'delegate'

    class RubyCollectionWrapper < Delegator
      include java.util.Collection

      def initialize(coll) @coll = coll end

      def size; @coll.empty? ? -1 : @coll.size end

      def __getobj__ ; @coll  end
    end

    it 'reports expected size' do
      obj = Object.new
      def obj.empty?; true end

      wrapper = RubyCollectionWrapper.new(obj)
      expect( wrapper.size ).to eql -1

      wrapper = RubyCollectionWrapper.new java.util.Collections::EMPTY_SET
      expect( wrapper.size ).to eql -1

      wrapper = RubyCollectionWrapper.new coll = java.util.LinkedList.new
      coll.add 1; coll.add 2
      wrapper.add 3
      expect( wrapper.size ).to eql 3
    end

    it 'is usable as a Collection' do
      wrapper = RubyCollectionWrapper.new coll = java.util.LinkedHashSet.new([1, 2])
      java.util.Collections.addAll wrapper, 1, 2, 3
      expect( wrapper.size ).to eql 3
      expect( wrapper.toString ).to eql '[1, 2, 3]'
    end

    it 'iterates as an Enumerable' do
      wrapper = RubyCollectionWrapper.new coll = java.util.LinkedHashSet.new([1, 2])
      elems = [] ; wrapper.map { |el| elems << el * 3 }
      expect( elems ).to eql [3, 6]

      elems = [] ; wrapper.each_with_index { |el, i| elems << [el, i] }
      expect( elems ).to eql [[1, 0], [2, 1]]
    end

  end

end
