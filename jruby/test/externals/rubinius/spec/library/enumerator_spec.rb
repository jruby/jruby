require File.dirname(__FILE__) + '/../spec_helper'

# each_cons, each_slice, enum_cons, enum_slice, enum_with_index  

describe "Enumerable::Enumerator.new" do
  require 'enumerator'

  it "should create a new custom enumerator with the given object, iterator and arguments" do
    enum = Enumerable::Enumerator.new(1, :upto, 3)
    enum.kind_of?(Enumerable::Enumerator).should == true
  end

  it "should create a new custom enumerator that responds to #each" do
    enum = Enumerable::Enumerator.new(1, :upto, 3)
    enum.respond_to?(:each).should == true
  end

  it "should create a new custom enumerator that runs correctly" do
    Enumerable::Enumerator.new(1, :upto, 3).map{|x|x}.should == [1,2,3]
  end
end

context "A class with Enumerable::Enumerator mixin" do
  require 'enumerator'
  
  class Numerous
    include Enumerable
    
    def initialize(*list)
      @list = list.empty? ? [2, 5, 3, 6, 1, 4] : list
    end
    
    def each
      @list.each { |i| yield i }
    end
    
  end

  specify "each_cons should iterate the block for each array of n consecutive elements" do
    a = []
    Numerous.new.each_cons(4) { |e| a << e }
    a.should == [[2, 5, 3, 6], [5, 3, 6, 1], [3, 6, 1, 4]]
  end
  
  specify "each_slice should iterate the block for each slice of n elements" do
    a = []
    Numerous.new.each_slice(3) { |e| a << e }
    a.should == [[2, 5, 3], [6, 1, 4]]
  end
  
  specify "enum_cons should return an enumerator of the receiver with iteration of each_cons for each array of n concecutive elements" do
    a = []
    enum = Numerous.new.enum_cons(3)
    enum.each {|x| a << x}
    enum.kind_of?(Enumerable::Enumerator).should == true
    a.should == [[2, 5, 3], [5, 3, 6], [3, 6, 1], [6, 1, 4]]
  end
  
  specify "enum_slice should return an enumerator of the receiver with iteration of each_slice for each slice of n elements" do
    a = []
    enum = Numerous.new.enum_slice(4)
    enum.kind_of?(Enumerable::Enumerator).should == true
    enum.each { |e| a << e }
    a.should == [[2, 5, 3, 6], [1, 4]]
  end
  
  specify "enum_with_index should return an enumerator of the receiver with an iteration of each_with_index" do
    a = []
    enum = Numerous.new.enum_with_index
    enum.kind_of?(Enumerable::Enumerator).should == true
    enum.each { |e| a << e }
    a.should == [[2, 0], [5, 1], [3, 2], [6, 3], [1, 4], [4, 5]]
  end
end
