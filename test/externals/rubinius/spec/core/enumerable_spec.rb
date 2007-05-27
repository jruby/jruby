require File.dirname(__FILE__) + '/../spec_helper'

# all?, any?, collect, detect, 
# each_with_index, entries,
# find, find_all, grep, include?, inject, map, max, member?, min,
# partition, reject, select, sort, sort_by, to_a, zip

context "A class with Enumerable mixin, method" do
  class Numerous
    include Enumerable
    
    def initialize(*list)
      @list = list.empty? ? [2, 5, 3, 6, 1, 4] : list
    end
    
    def each
      @list.each { |i| yield i }
    end
    
  end
  
  specify "each should be provided" do
    Numerous.new.respond_to?(:each).should == true
  end
  
  specify "each should provide each element to the block" do
    @a = []
    Numerous.new.each { |i| @a << i }
    @a.should == [2, 5, 3, 6, 1, 4]
  end
  
  specify "all? with no block should return true if no elements are false or nil" do
    Numerous.new.all?.should == true 
    Numerous.new(1, nil, 2).all?.should == false 
    Numerous.new(false).all?.should == false
  end
  
  specify "all? should return true if the block never returns false or nil" do
    Numerous.new.all? { true }.should == true
  end
  
  specify "all? should return false if the block ever returns false or nil" do
    Numerous.new.all? { |i| i > 5 }.should == false
    Numerous.new.all? { |i| i == 3 ? nil : true }.should == false
  end
  
  specify "any? with no block should return true if any element is not false or nil" do
    Numerous.new.any?.should == true 
    Numerous.new(1, nil, 2).any?.should == true 
    Numerous.new(false).any?.should == false
  end
  
  specify "any? should return true if the block ever returns other than false or nil" do
    Numerous.new.any? { |i| i == 5 }.should == true
  end
  
  specify "any? should return false if the block never returns other than false or nil" do
    Numerous.new.any? { false }.should == false
    Numerous.new.any? { nil }.should == false
  end
  
  specify "collect should return a new array with the results of passing each element to block" do
    Numerous.new.collect { |i| i % 2 }.should == [0, 1, 1, 0, 1, 0]
  end
  
  specify "detect should return the first element for which block is not false" do
    Numerous.new.detect { |i| i > 3 }.should == 5
  end
  
  specify "each_with_index should pass each element and its index to block" do
    @a = []
    Numerous.new.each_with_index { |o, i| @a << [o, i] }
    @a.should == [[2, 0], [5, 1], [3, 2], [6, 3], [1, 4], [4, 5]]
  end
  
  specify "entries should return an array containing the elements" do
    Numerous.new(1, nil, 'a', 2, false, true).entries.should == [1, nil, "a", 2, false, true]
  end
  
  specify "find should be a synonym for detect" do
    Numerous.new.find { |i| i > 3 }.should == Numerous.new.detect { |i| i > 3 }
  end
  
  specify "find_all should be a synonym for select" do
    Numerous.new.find_all { |i| i % 2 == 0 }.should == Numerous.new.select { |i| i % 2 == 0 }
  end
  
  specify "grep without a block should return an array of all elements === pattern" do
    class EnumerableSpecGrep; def ===(obj); obj == '2'; end; end

    Numerous.new('2', 'a', 'nil', '3', false).grep(EnumerableSpecGrep.new).should == ['2']
  end
  
  specify "grep with a block should return an array of elements === pattern passed through block" do
    class EnumerableSpecGrep2; def ===(obj); /^ca/ =~ obj; end; end

    Numerous.new("cat", "coat", "car", "cadr", "cost").grep(EnumerableSpecGrep2.new) { |i| i.upcase }.should == ["CAT", "CAR", "CADR"]
  end
  
  specify "include? should return true if any element == argument" do
    class EnumerableSpecIncludeP; def ==(obj) obj == 5; end; end

    Numerous.new.include?(5).should == true 
    Numerous.new.include?(10).should == false
    Numerous.new.include?(EnumerableSpecIncludeP.new).should == true
  end
  
  specify "inject with argument takes a block with an accumulator (with argument as initial value) and the current element. Value of block becomes new accumulator" do
    a = []
    Numerous.new.inject(0) { |memo, i| a << [memo, i]; i }
    a.should == [[0, 2], [2, 5], [5, 3], [3, 6], [6, 1], [1, 4]]
  end
    
  specify "inject without argument takes a block with an accumulator (with first element as initial value) and the current element. Value of block becomes new accumulator" do
    a = []
    Numerous.new.inject { |memo, i| a << [memo, i]; i }
    a.should == [[2, 5], [5, 3], [3, 6], [6, 1], [1, 4]]
  end
  
  specify "map should be a synonym for collect" do
    Numerous.new.map { |i| i % 2 }.should == Numerous.new.collect { |i| i % 2 }
  end
  
  specify "member? should be a synonym for include?" do
    Numerous.new.include?(5).should ==  Numerous.new.member?(5)
    Numerous.new.include?(10).should ==  Numerous.new.member?(10)
  end
  
  specify "partition should return two arrays, the first containing elements for which the block is true, the second containing the rest" do
    Numerous.new.partition { |i| i % 2 == 0 }.should == [[2, 6, 4], [5, 3, 1]]
  end
  
  specify "reject should return an array of the elements for which block is false" do
    Numerous.new.reject { |i| i > 3 }.should == [2, 3, 1]
  end
  
  specify "select should return an array of all elements for which block is not false" do
    Numerous.new.find_all { |i| i % 2 == 0 }.should == [2, 6, 4]
  end
  
  specify "to_a should should be a synonym for entries" do
    Numerous.new(1, nil, 'a', 2, false, true).to_a.should == Numerous.new(1, nil, 'a', 2, false, true).entries
  end
  
  specify "zip without a block should merge elements with corresponding indexes from each #to_a argument" do
    class EnumerableSpecZip; def to_a(); [1, 2, 3, 4, 5, 6]; end; end

    a, b, c = ['a', 'b', 'c', 'c', 'd'], [false, nil, 1, true, 2, 0], EnumerableSpecZip.new

    Numerous.new.zip(a, b).should == [[2, "a", false],[5, "b", nil],[3, "c", 1],[6, "c", true],[1, "d", 2],[4, nil, 0]]
    Numerous.new.zip(c).should == [[2, 1], [5, 2], [3, 3], [6, 4], [1, 5], [4, 6]]
  end
  
  specify "zip with a block merges corresponding indexes and passes these to the block" do
    @a = []
    Numerous.new.zip(1..6) { |a| @a << a }
    @a.should == [[2, 1], [5, 2], [3, 3], [6, 4], [1, 5], [4, 6]]
  end
end

context "A class with Enumerable mixin and <=> defined, method" do
#    class Numerous
#      include Enumerable
#      
#      def initialize(*list)
#        @list = list.empty? ? [2, 5, 3, 6, 1, 4] : list
#      end
#      
#      def each
#        @list.each { |i| yield i }
#      end
#      
#      def <=>(other)
#        not self <=> other
#      end
#    end
#    CODE
#  end
  
  specify "max should return the maximum element" do
    Numerous.new.max.should == 6
  end
  
  specify "min should return the minimum element" do
    Numerous.new.min.should == 1
  end
  
  specify "sort without a block should should return an array of elements ordered lowest to highest" do
    Numerous.new.sort.should == [1, 2, 3, 4, 5, 6]
  end
  
  specify "sort with a block should return an array of elements ordered by the result of block" do
    Numerous.new.sort { |a, b| b <=> a }.should == [6, 5, 4, 3, 2, 1]
  end
  
  specify "sort_by should return an array of elements ordered by the result of block" do
    Numerous.new("once", "upon", "a", "time").sort_by { |i| i[0] }.should == ["a", "once", "time", "upon"]
  end
end
