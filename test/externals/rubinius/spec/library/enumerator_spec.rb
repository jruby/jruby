require File.dirname(__FILE__) + '/../spec_helper'

# each_cons, each_slice, enum_cons, enum_slice, enum_with_index  

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
    a.should == "[2, 5, 3, 6]\n[5, 3, 6, 1]\n[3, 6, 1, 4]"
  end
  
  specify "each_slice should " do
    a = []
    Numerous.new.each_slice { |e| a << e }
    a.should == []
  end
  
  specify "enum_cons should " do
    a = []
    Numerous.new.enum_cons { |e| a << e }
    a.should == []
  end
  
  specify "enum_slice should " do
    a = []
    Numerous.new.enum_slice { |e| a << e }
    a.should == []
  end
  
  specify "enum_with_index should " do
    a = []
    Numerous.new.enum_with_index { |e| a << e }
    a.should == []
  end
end
