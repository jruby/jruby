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
