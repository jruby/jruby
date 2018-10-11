require File.dirname(__FILE__) + "/../spec_helper"

describe "List Ruby extensions" do
  before(:each) do
    @data = ["foo", "quux", "bar", "aa"]
    @list = java.util.ArrayList.new(@data)
  end

  it "should support index() with one argument" do
    expect(@list.index("quux")).to eq(1)
  end

  it "should return nil on index() with one argument that does not exist" do
    expect(@list.index(451)).to eq(nil)
  end

  it "should return nil on rindex() with one argument that does not exist" do
    expect(@list.rindex(-24)).to eq(nil)
    list = java.util.Stack.new
    expect(list.rindex('foo')).to eq(nil)
    expect(list.rindex(nil)).to eq(nil)
  end

  it "should support index() with a block" do
    expect(@list.index{ |x| x == 'bar' }).to eq(2)
  end

  it "should support rindex() with a block" do
    list = java.util.Vector.new @data; list << 'bar'; list << 'aa'
    expect(list.rindex{ |x| x == 'bar' }).to eq(4)
  end

  it "should support index() with a block that does not exist" do
    expect(@list.index{ |x| x == :nazgul }).to eq(nil)
  end

  it "should support index() with nil (not found)" do
    expect(@list.index(nil)).to eq(nil)
  end

  it "should support rindex() with nil (not found)" do
    expect(@list.rindex(nil)).to eq(nil)
  end

  it "should support index() with nil (found)" do
    expect(java.util.LinkedList.new(["foo", "quux", nil, "bar", "aa"]).index(nil)).to eq(2)
  end

  it "should support rindex() with nil (found)" do
    expect(java.util.ArrayList.new(["foo", "quux", nil, "bar", nil, "aa"]).rindex(nil)).to eq(4)
  end

  it "should support index() with no arguments" do
    expect( @list.index.each { |x| x == "foo" } ).to eq(0)
  end

  it "should support index() with no arguments (not existing)" do
    expect( @list.index.each { |x| x == ":-(" } ).to eq(nil)
  end

  it "should support rindex() with no arguments" do
    list = java.util.Stack.new
    list << 1; list << 2; list << 3; list << 1; list << 2; list << 3
    expect( list.rindex.each { |x| x < 2 } ).to eq(3)
  end

  # Java 8 adds a single-parameter sort method to List that sorts in-place
  # if ENV_JAVA['java.specification.version'] < '1.8'
  #   it "should be sortable with sort() without block" do
  #     expect(@list.sort.to_a).to eq(@data.sort)
  #   end
  #
  #   it "should be sortable with sort() with block" do
  #     result = @list.sort do |a, b|
  #       a.length <=> b.length
  #     end
  #
  #     expected = @data.sort do |a, b|
  #       a.length <=> b.length
  #     end
  #
  #     expect(result.to_a).to eq(expected)
  #   end
  # end

  it "should be sortable with sort!() without block" do
    list = java.util.LinkedList.new(@data)
    list.sort!
    expect(list.to_a).to eq(@data.sort)
  end

  it "should be sortable with sort!() with block" do
    list = java.util.ArrayList.new(@data)
    list.sort! do |a, b|
      a.length <=> b.length
    end

    expected = @data.sort do |a, b|
      a.length <=> b.length
    end

    expect(list.to_a).to eq(expected)
  end

  it 'returns same collection type as target on sort' do
    list = java.util.Vector.new ['b','a']

    # NOTE: collides with Java 8's sort :
    java.util.Vector.class_eval { alias sort ruby_sort }

    list = list.sort { |a, b| a.length <=> b.length }
    expect( list ).to be_a java.util.Vector
    expect( list.to_a ).to eq ['b','a']

    list = java.util.LinkedList.new ['b','a','c']
    # list = list.sort { |a, b| a <=> b }
    sorted = list.ruby_sort { |a, b| a <=> b }
    expect( sorted ).to be_a java.util.LinkedList
    expect( sorted.to_a ).to eq ['a','b','c']
    expect( sorted ).to_not be list

    expect( java.util.ArrayList.new.ruby_sort ).to be_a java.util.ArrayList
  end

  it 'converts to_ary' do
    list = java.util.LinkedList.new(@data)
    expect(list.to_ary).to eq(@data)

    list = java.util.ArrayList.new(@data)
    list.sort!
    expect(list.to_ary).to eq(@data.sort)
  end

  it 'reports list size/length' do
    list = java.util.Vector.new(@data)
    expect(list.length).to eq(@data.size)
    expect(list.size).to eq(list.length)
    expect( java.util.Collections.emptyList.length ).to eq 0
  end

  it '[] with 1 arg returns element like an Array' do
    expect(@list[0]).to eq(@data[0])
    expect(@list[3]).to eq(@data[3])
    expect(@list[-1]).to eq(@data[-1])
    expect(@list[11]).to eq(@data[11]) # nil
    expect(@list[-9]).to eq(@data[-9]) # nil
  end

  it 'slices with 2 arguments like an Array' do
    expect(@list[0,3].to_a).to eq(@data[0,3])
    expect(@list[2,3].to_a).to eq(@data[2,3])
    expect(@list[3,8].to_a).to eq(@data[3,8])
    expect(@list[4,8].to_a).to eq(@data[4,8])
    expect(@list[5,8]).to be nil
    expect(@list[-1,1].to_a).to eq(@data[-1,1])
    expect(@list[-2,3].to_a).to eq(@data[-2,3])
    expect(@list[-5,3]).to be nil
    expect(@list[0,0].empty?).to be true
  end

  it 'slices with a range argument like an Array' do
    expect(@list[0..3].to_a).to eq(@data[0..3])
    expect(@list[0...2].to_a).to eq(@data[0...2])
    expect(@list[0..0].to_a).to eq(@data[0..0])
    expect(@list[0...0].to_a).to eq(@data[0...0])
    expect(@list[1...-1].to_a).to eq(@data[1...-1])
    expect(@list[-2..-1].to_a).to eq(@data[-2..-1])
    expect(@list[-2...-1].to_a).to eq(@data[-2...-1])
    expect(@list[-4..-2].to_a).to eq(@data[-4..-2])
    expect(@list[-4...8].to_a).to eq(@data[-4...8])
    expect(@list[-5..-1]).to be nil
    expect(@list[-5...-1]).to be nil
    expect(@list[7..8]).to be nil
    expect(@list[-2...-2].empty?).to be true
    expect(@list[1...1].empty?).to be true
  end

  it 'slice returns a subList' do
    expect(@list[0,3]).to be_a java.util.List
    expect(@list[0,0]).to be_a java.util.List # empty

    expect(@list[0..3]).to be_a java.util.List
    expect(@list[-2...-2]).to be_a java.util.List # empty
  end

  it '[]= with 1 arg sets an element' do
    list = java.util.Vector.new(@data)
    list[0] = nil
    expect( list.get(0) ).to be nil
    list[-1] = -1
    expect( list.get(list.size() - 1) ).to eq -1

    list = java.util.LinkedList.new(@data)
    list[0] = nil
    expect( list.get(0) ).to be nil
    list[-1] = -1
    expect( list.get(list.size() - 1) ).to eq -1
  end

  it '[]= with arg gt than size adds and sets like an Array' do
    list = java.util.LinkedList.new @data
    list[5] = 5 # list.size == 4
    expect( list.get(4) ).to be nil
    expect( list.get(5) ).to eq 5
    expect( list.size ).to eq 6

    list = java.util.Stack.new
    list[3] = 3; list[0] = 0
    expect( list.to_a ).to eq [0, nil, nil, 3]
  end

  it 'supports (Array-like) first/last' do
    expect( @list.first ).to eq 'foo'
    list = java.util.ArrayList.new [1, 2, 3]
    expect( list.first(2).to_a ).to eq [1, 2]
    expect( list.first(1).to_a ).to eq [1]
    expect( list.first(0).to_a ).to eq []
    expect( list.first(5).to_a ).to eq [1, 2, 3]

    # LinkedList does getList, unless we alias first ruby_first
    expect( java.util.LinkedList.new.ruby_first ).to be nil
    expect( java.util.LinkedList.new.ruby_last ).to be nil

    list = java.util.LinkedList.new [1, 2, 3]
    expect( list.ruby_last ).to eq 3
    expect( java.util.Vector.new.last ).to be nil
    expect( list.ruby_last(1).to_a ).to eq [3]
    expect( list.ruby_last(2) ).to be_a java.util.List
    expect( list.ruby_last(2).to_a ).to eq [2, 3]
    expect( list.ruby_last(5).to_a ).to eq [1, 2, 3]
    expect( list.ruby_last(0) ).to be_a java.util.List
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

    java.util.ArrayList.new([Pair.new(:x, :y)]).each do |car, cdr|
      expect(car).to eq(:x)
      expect(cdr).to eq(:y)
    end
  end
end
