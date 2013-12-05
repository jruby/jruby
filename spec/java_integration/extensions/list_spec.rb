require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.util.ArrayList"

describe "List Ruby extensions" do
  before(:each) do
    @data = ["foo", "quux", "bar", "aa"]
    @list = ArrayList.new(@data)
  end

  it "should support index() with one argument" do
    @list.index("quux").should == 1
  end

  it "should return nil on index() with one argument that does not exist" do
    @list.index(451).should == nil
  end

  it "should support index() with a block" do
    @list.index{|x| x == "bar" }.should == 2
  end

  it "should support index() with a block that does not exist" do
    @list.index{|x| x == :nazgul }.should == nil
  end

  it "should support index() with nil (not found)" do
    @list.index(nil).should == nil
  end

  it "should support index() with nil (found)" do
    ArrayList.new(["foo", "quux", nil, "bar", "aa"]).index(nil).should == 2
  end

  it "should support index() with no arguments" do
    @list.index.each {|x| x == "foo" }.should == 0
  end

  it "should support index() with no arguments (not existing)" do
    @list.index.each {|x| x == ":-(" }.should == nil
  end

  # Java 8 adds a single-parameter sort method to List that sorts in-place
  if ENV_JAVA['java.specification.version'] < '1.8'
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
