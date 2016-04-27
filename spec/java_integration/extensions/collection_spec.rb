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
    java.util.concurrent.LinkedBlockingQueue.new(@data).each { |elem| data << elem }
    expect(data).to eq @data
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
    set.each_with_index { |elem, i| data << elem; idx << i }
    expect(data).to eq @data
    expect(idx).to eq [0, 1, 2, 3]

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
end
