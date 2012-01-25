require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.util.ArrayList"

describe "List Ruby extensions" do 
  before(:each) do 
    @data = ["foo", "quux", "bar", "aa"]
    @list = ArrayList.new(@data)
  end
  
  it "should be sortable with sort() without block" do 
    @list.sort.to_a.should == @data.sort
  end

  it "should be sortable with sort() with block" do 
    result = @list.sort do |a, b|
      a.length <=> b.length
    end

    expected = @data.sort do |a, b|
      a.length <=> b.length
    end

    result.to_a.should == expected
  end

  it "should be sortable with sort!() without block" do 
    list = ArrayList.new(@data)
    list.sort!
    list.to_a.should == @data.sort
  end

  it "should be sortable with sort!() with block" do 
    list = ArrayList.new(@data)
    list.sort! do |a, b|
      a.length <=> b.length
    end

    expected = @data.sort do |a, b|
      a.length <=> b.length
    end

    list.to_a.should == expected
  end

  it "should support slicing with 2 arguments" do
    @list[0,3].to_a.should == @data[0,3]
  end

  it "should support slicing with inclusive ranges" do
    @list[0..3].to_a.should == @data[0..3]
  end

   it "should support slicing with exclusive ranges" do
    @list[0...2].to_a.should == @data[0...2]
  end

  it "should respect to_ary objects defined on iteration" do
    class Pair
      def initialize(a, b)
        @a = a
        @b = b
      end

      def to_ary
        [@a, @b]
      end
    end

    ArrayList.new([Pair.new(:x, :y)]).each do |car, cdr|
      car.should == :x
      cdr.should == :y
    end
  end
end
