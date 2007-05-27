require File.dirname(__FILE__) + '/../spec_helper'
#require File.dirname(__FILE__) + '/../../kernel/bootstrap/array'
#require File.dirname(__FILE__) + '/../../kernel/core/array'

# &, *, +, -, <<, <=>, ==, [], []=, assoc, at, clear,
# collect, collect!, compact, compact!, concat, delete, delete_at,
# delete_if, each, each_index, empty?, eql?, fetch, fill, first,
# flatten, flatten!, frozen?, hash, include?, index, indexes,
# indices, initialize_copy, insert, inspect, join, last, length, map,
# map!, nitems, pack, pop, push, rassoc, reject, reject!, replace,
# reverse, reverse!, reverse_each, rindex, select, shift, size,
# slice, slice!, sort, sort!, to_a, to_ary, to_s, transpose, uniq,
# uniq!, unshift, values_at, zip, |

context "Array class methods" do
  specify "new without arguments should return a new array" do
    a = Array.new
    a.class.should == Array
  end
  
  specify "new with size should return a new array of size with nil elements" do
    Array.new(5).should == [nil, nil, nil, nil, nil]
  end
  
  specify "new with size and default object should return a new array of size objects" do
    Array.new(4, true).should == [true, true, true, true]
  end
  
  specify "new with array-like argument should return a new array by calling to_ary on argument" do
    class A; def to_ary(); [:foo]; end; end

    Array.new(A.new).should == [:foo]
  end
  
  specify "new with size and block should return an array of size elements from the result of passing each index to block" do
    Array.new(5) { |i| i + 1 }.should == [1, 2, 3, 4, 5]
  end

# Unnecessary, block spec stuff. --rue
#  specify "new with size and block should have proper side effects from passing each index to block" do
#      a = []
#      Array.new(5) { |i| a << i + 1 }
#      a.should == [1, 2, 3, 4, 5]
#  end
  
  specify ".[] should return a new array populated with the given elements" do
    Array.[](5, true, nil, 'a', "Ruby").should == [5, true, nil, "a", "Ruby"]
  end

  specify "[] should be a synonym for .[]" do
    Array[5, true, nil, 'a', "Ruby"].should == [5, true, nil, "a", "Ruby"]
  end
end

context "Array instance methods" do
  
  specify "& should create an array with elements common to both arrays (intersection)" do
    ([ 1, 1, 3, 5 ] & [ 1, 2, 3 ]).should == [1, 3]
  end
  
  specify "& should create an array with no duplicates" do
    ([ 1, 1, 3, 5 ] & [ 1, 2, 3 ]).uniq!.should == nil
  end

  specify "| should return an array of elements that appear in either array (union) without duplicates" do
    ([1, 2, 3] | [1, 2, 3, 4, 5]).should == [1, 2, 3, 4, 5]
  end
  
  specify "* with a string should be equivalent to self.join(str)" do
    ([ 1, 2, 3 ] * ",").should == [1, 2, 3].join(",")
  end
  
  specify "* with an int should concatenate n copies of the array" do
    ([ 1, 2, 3 ] * 3).should == [1, 2, 3, 1, 2, 3, 1, 2, 3]
  end
  
  specify "+ should concatenate two arrays" do
    ([ 1, 2, 3 ] + [ 4, 5 ]).should == [1, 2, 3, 4, 5]
  end

  specify "- should create an array minus any items from other array" do
    ([ 1, 1, 2, 2, 3, 3, 4, 5 ] - [ 1, 2, 4 ]).should == [3, 3, 5]
  end
  
  specify "<< should push the object onto the end of the array" do
    ([ 1, 2 ] << "c" << "d" << [ 3, 4 ]).should == [1, 2, "c", "d", [3, 4]]
  end
  
  specify "<=> should be 1 if the array is greater than the other array" do
    ([ 1, 2, 3, 4, 5, 6 ] <=> [ 1, 2 ]).should == 1
  end
  
  specify "<=> should be 0 if the arrays are equal" do
    ([] <=> []).should == 0
  end
  
  specify "<=> should be -1 if the array is less than the other array" do
    ([ "a", "a", "c" ] <=> [ "a", "b", "c" ]).should == -1
  end
  
  specify "== should be true if each element is == to the corresponding element in the other array" do
    ([ "a", "c", 7 ] == [ "a", "c", 7 ]).should == true
  end
  
  specify "== should be false if any element is not == to the corresponding element in the other the array" do
    ([ "a", "c" ] == [ "a", "c", 7 ]).should == false
  end
  
  
  specify "assoc should return the first contained array the first element of which is obj" do
    s1 = [ "colors", "red", "blue", "green" ] 
    s2 = [ "letters", "a", "b", "c" ] 
    s3 = "foo" 
    a = [ s1, s2, s3 ] 
    a.assoc("letters").should == %w{letters a b c}
    a.assoc("foo").should == nil
  end
  
  specify "at should return the element at index" do
    a = [1, 2, 3, 4, 5, 6]
    a.at(0).should  == 1
    a.at(-2).should == 5
    a.at(10).should == nil
  end
  
  specify "clear should remove all elements" do
    a = [1, 2, 3, 4]
    a.clear
    a.should == []
  end
  
  specify "collect should return a copy of array with each element replaced by the value returned by block" do
    a = ['a', 'b', 'c', 'd']
    b = a.collect { |i| i + '!' }
    b.should == ["a!", "b!", "c!", "d!"]
  end
  
  specify "collect! should replace each element with the value returned by block" do
    a = [7, 9, 3, 5]
    a.collect! { |i| i - 1 }
    a.should == [6, 8, 2, 4]
  end
  
  specify "compact should return a copy of array with all nil elements removed" do
    a = [1, nil, 2, nil, 4, nil]
    a.compact.should == [1, 2, 4]
  end
  
  specify "compact! should remove all nil elements" do
    a = ['a', nil, 'b', nil, nil, 'c']
    a.compact!.should == ["a", "b", "c"]
  end
  
  # FIX: Should fix and go to incompatible/ as [result, status]? --rue
  specify "compact! should return nil if there are no nil elements to remove" do
    [1, 2, 3].compact!.should == nil
  end
  
  specify "concat should append the elements in the other array" do
    [1, 2, 3].concat([9, 10, 11]).should == [1, 2, 3, 9, 10, 11]
  end
  
  specify "delete removes elements that are #== to object" do
    class B; def ==(other); (3 == other) || super; end; end

    x = B.new

    a = [1, 2, 3, x, 4, 3, 5, x]

    a.delete Object.new
    a.should == [1, 2, 3, x, 4, 3, 5, x]

    a.delete 3
    a.should == [1, 2, 4, 5]
  end

  specify "delete should return object or nil if no elements match object" do
    [1, 2, 4, 5].delete(1).should == 1
    [1, 2, 4, 5].delete(3).should == nil
  end

  specify 'delete may be given a block that is executed if no element matches object' do
    [].delete('a') {:not_found}.should == :not_found
  end
  
  specify "delete_at should remove the element at the specified index" do
    a = [1, 2, 3, 4]
    a.delete_at(2)
    a.should == [1, 2, 4]
  end

  specify "delete_at should return the removed element at the specified index" do
    a = [1, 2, 3, 4]
    a.delete_at(2).should == 3
  end
  
  specify "delete_at should return nil if the index is out of range" do
    a = [1, 2]
    a.delete_at(3).should == nil
  end
  
  specify "delete_if should remove each element for which block returns true" do
    a = [ "a", "b", "c" ] 
    a.delete_if { |x| x >= "b" }
    a.should == ["a"]
  end
  
  specify "each should yield each element to the block" do
    a = []
    [1, 2, 3].each { |item| a << item }
    a.should == [1, 2, 3]
  end
  
  specify "each_index should pass the index of each element to the block" do
    a = []
    ['a', 'b', 'c', 'd'].each_index { |i| a << i }
    a.should == [0, 1, 2, 3]
  end
  
  specify "empty? should return true if the array has no elements" do
    [].empty?.should == true
    [1].empty?.should == false
    [1, 2].empty?.should == false
  end
  
  specify "eql? should return true if other is the same array" do
    a, b = [1], [2]

    a.eql?(b).should == false
    a.eql?(a).should == true
  end
  
  specify "eql? should return true if other has the same length and elements (except empty)" do
    a = [1, 2, 3, 4]
    b = [1, 2, 3, 4]
    c = [1, 2]
    d = ['a', 'b', 'c', 'd']

    a.eql?(b).should == true
    a.eql?(c).should == false
    a.eql?(d).should == false
  end

  specify "fetch should return the element at index" do
    [[1, 2, 3].fetch(1), [1, 2, 3, 4].fetch(-1)].should == [2, 4]
  end
  
  specify "fetch should raise if there is no element at index" do
    should_raise(IndexError) { [1, 2, 3].fetch(3) }
  end
  
  specify "fetch with default should return default if there is no element at index" do
    [1, 2, 3].fetch(5, :not_found).should == :not_found
  end

  specify "fetch with block should return the value of block if there is no element at index" do
    [1, 2, 3].fetch(9) { |i| i * i }.should == 81
  end
  
  specify "fill should replace all elements in the array with object" do
    ['a', 'b', 'c', 'duh'].fill(8).should == [8, 8, 8, 8]
  end
  
  specify "fill with start, length should replace length elements beginning with start with object" do
    [1, 2, 3, 4, 5].fill('a', 2, 2).should == [1, 2, "a", "a", 5]
  end
  
  specify "fill with range should replace elements in range with object" do
    [1, 2, 3, 4, 5, 6].fill(8, 0..3).should == [8, 8, 8, 8, 5, 6]
  end
  
  specify "fill with block should replace all elements with the value of block (index given to block)" do
    [nil, nil, nil, nil].fill { |i| i * 2 }.should == [0, 2, 4, 6]
  end
  
  specify "fill with start, length and block should replace length elements beginning with start with the value of block" do
    [true, false, true, false, true, false, true].fill(1, 4) { |i| i + 3 }.should == [true, 4, 5, 6, 7, false, true]
  end
  
  specify "fill with range and block should replace all elements in range with the value of block" do
    [1, 1, 1, 1, 1, 1].fill(1..6) { |i| i + 1 }.should == [1, 2, 3, 4, 5, 6, 7]
  end
  
  specify "first should return the first element" do
    %w{a b c}.first.should == 'a'
    [nil].first.should == nil
  end
  
  specify "first should return nil if self is empty" do
    [].first.should == nil
  end
  
  specify "first with count should return the first count elements" do
    [true, false, true, nil, false].first(2).should == [true, false]
  end
  
  specify "first with count == 0 should return an empty array" do
    [1, 2, 3, 4, 5].first(0).should == []
  end
  
  specify "first with count == 1 should return an array containing the first element" do
    [1, 2, 3, 4, 5].first(1).should == [1]
  end
  
  specify "first should raise ArgumentError when count is negative" do
    should_raise(ArgumentError) { [1, 2].first(-1) }
  end
  
  specify "first should return the entire array when count > length " do
    [1, 2, 3, 4, 5, 9].first(10).should == [1, 2, 3, 4, 5, 9]
  end
  
  specify "flatten should return a one-dimensional flattening recursively" do
    [[[1, [2, 3]],[2, 3, [4, [4, [5, 5]], [1, 2, 3]]], [4]]].flatten.should == [1, 2, 3, 2, 3, 4, 4, 5, 5, 1, 2, 3, 4]
  end
  
  specify "flatten! should modify array to produce a one-dimensional flattening recursively" do
    a = [[[1, [2, 3]],[2, 3, [4, [4, [5, 5]], [1, 2, 3]]], [4]]]
    a.flatten!
    a.should == [1, 2, 3, 2, 3, 4, 4, 5, 5, 1, 2, 3, 4]
  end
  
  specify "frozen? should return true if array is frozen" do
    a = [1, 2, 3]
    a.frozen?.should == false
    a.freeze
    a.frozen?.should == true
  end

#  FIX: I have no idea why this should be the case. 
#       Move to incompatible/ if we want this --rue
#  specify "frozen? should return true if array is temporarily frozen while being sorted" do
#    a = [1, 2, 3]
#    a.sort! { |x,y| a.frozen?.should == true; x <=> y }
#  end
  
  specify "provides #hash" do
    [].respond_to?(:hash).should == true
  end
  
  specify "include? should return true if object is present, false otherwise" do
    [1, 2, "a", "b"].include?("c").should == false
    [1, 2, "a", "b"].include?("a").should == true
  end
  
  specify "index returns the index of the first element == to object" do
    class X; def ==(obj); 3 == obj; end; end

    x = X.new

    [2, x, 3, 1, 3, 1].index(3).should == 1
  end

  specify "index returns 0 if first element == to object" do
    [2, 1, 3, 2, 5].index(2).should == 0
  end

  specify "index returns size-1 if only last element == to object" do
    [2, 1, 3, 1, 5].index(5).should == 4
  end

  specify "index returns nil if no element == to object" do
    [2, 1, 1, 1, 1].index(3).should == nil
  end
  
  # If we test in terms of something then TEST IN TERMS OF IT :) --rue
  specify "indexes and #indices with integer indices are DEPRECATED synonyms for values_at" do
    array = [1, 2, 3, 4, 5]

    params = [1, 0, 5, -1, -8, 10]
    array.indexes(*params).should == array.values_at(*params)
    array.indices(*params).should == array.values_at(*params)
  end

  specify 'indexes and indices can be given ranges which are returned as nested arrays (DEPRECATED)' do
    warn "DEPRECATED but #values_at does not use the same interface!"

    array = [1, 2, 3, 4, 5]

    params = [0..2, 1...3, 4..6]
    array.indexes(*params).should == [[1, 2, 3], [2, 3], [5]]
    array.indices(*params).should == [[1, 2, 3], [2, 3], [5]]
  end
  
  specify "initialize_copy should be a synonym for replace" do
    [1, 2, 3, 4, 5].send(:initialize_copy, ['a', 'b', 'c']).should == [1, 2, 3, 4, 5].replace(%w{a b c})
  end
  
  specify "insert with non-negative index should insert object before the element at index" do
    [1, 2, 3].insert(1, 'a').should == [1, "a", 2, 3]
  end

  specify "insert with index -1 should append object to the end" do
    [1, 3, 3].insert(-1, 2).should == [1, 3, 3, 2]
  end

  specify "insert with negative index should insert object after the element at index" do
    [1, 2, 3].insert(-2, -3).should == [1, 2, -3, 3]
    [1, 2, 3].insert(-1, -3).should == [1, 2, 3, -3]
  end
  
  specify "inspect should create a printable representation of some kind" do
    [1, 2, 3].inspect.class.should == String
  end

  # FIX: compatibility? --rue
  specify 'currently insert should produce a string equivalent to evaluated source code representation' do
    [1, 2, 3].inspect.should == '[1, 2, 3]'
    [1, 2, 3 + 4].inspect.should == '[1, 2, 7]'
  end
  
  specify "join should return a string formed by concatentating each element.to_s separated by separator without trailing separator" do
    class C; def to_s; 'foo'; end; end

    [1, 2, 3, 4, C.new].join(' | ').should == '1 | 2 | 3 | 4 | foo'
  end

  specify 'The separator to #join defaults to $, (which defaults to empty)' do
    [1, 2, 3].join.should == '123'
    old, $, = $,, '-'
    [1, 2, 3].join.should == '1-2-3'
    $, = old
  end
  
  specify "last returns the last element" do
    [1, 1, 1, 1, 2].last.should == 2
  end
  
  specify "last returns nil if self is empty" do
    [].last.should == nil
  end
  
  specify "last returns the last count elements" do
    [1, 2, 3, 4, 5, 9].last(3).should == [4, 5, 9]
  end
  
  specify "last returns an empty array when count == 0" do
    [1, 2, 3, 4, 5].last(0).should == []
  end
  
  specify "last raises ArgumentError when count is negative" do
    should_raise(ArgumentError) { [1, 2].last(-1) }
  end
  
  specify "last returns the entire array when count > length" do
    [1, 2, 3, 4, 5, 9].last(10).should == [1, 2, 3, 4, 5, 9]
  end

  specify "length should return the number of elements" do
    [1, 2, 3].length.should == 3
  end
  
  specify "map should be a synonym for collect" do
    a = ['a', 'b', 'c', 'd']
    a.map { |i| i + '!'}.should == a.collect { |i| i + '!' }
  end
  
  specify "map! should be a synonym for collect!" do
    a, b = [7, 9, 3, 5], [7, 9, 3, 5]
    a.map! { |i| i - 1 }
    b.collect! { |i| i - 1 }

    a.should == b
  end
  
  specify "nitems should return the number of non-nil elements" do
    [1, nil, 2, 3, nil, nil, 4].nitems.should == 4
  end
  
  # FIX: fill in a proper spec
  specify "pack should return a binary representation of the elements according to template" do
    true.should == false
  end
  
  specify "partition should return two arrays" do
    [].partition.should == [[], []]
  end
  
  specify "partition should return in the left array values for which the block evaluates to true" do
    [ 0,1,2,3,4,5 ].partition { |i| i % 2 == 0 }.should == [[0, 2, 4], [1, 3, 5]]
  end
  
  specify "pop should remove and return the last element of the array" do
    a = ["a", 1, nil, true]
    i = a.pop

    i.should == true
    a.should == ['a', 1, nil]
  end
  
  specify "pop should return nil if there are no more elements" do
    [].pop.should == nil
  end
  
  specify "push should append the arguments to the array" do
    a = [ "a", "b", "c" ]
    a.push("d", "e", "f")
    a.should == ["a", "b", "c", "d", "e", "f"]
  end
  
  specify "rassoc should return the first contained array whose second element is == object" do
    [[1, "a"], [2, "b"], [3, "b"], [4, "c"]].rassoc("b").should == [2, "b"]
  end
  
  specify "reject should return a new array without elements for which block is true" do
    [1, 2, 3, 4, 5].reject { |i| i < 3 }.should == [3, 4, 5]
  end
  
  specify "reject! should remove elements for which block is true" do
    a = [3, 4, 5, 6, 7, 8, 9, 10, 11]
    a.reject! { |i| i % 2 == 0 }
    a.should == [3, 5, 7, 9, 11]
  end
  
  specify "reject! should return nil if no changes are made" do
    [1, 2, 3].reject! { |i| i < 0 }.should == nil
  end
  
  specify "replace should replace the elements with elements from other array" do
    [1, 2, 3, 4, 5].replace(['a', 'b', 'c']).should == ["a", "b", "c"]
  end
  
  specify "reverse should return a new array with the elements in reverse order" do
    [1, 3, 5, 2].reverse.should == [2, 5, 3, 1]
  end
  
  specify "reverse! should reverse the elements in place" do
    a = [6, 3, 4, 2, 1]
    a.reverse!
    a.should == [1, 2, 4, 3, 6]
  end
  
  specify "reverse_each should traverse array in reverse order and pass each element to block" do
    a = []
    [1, 3, 4, 6].reverse_each { |i| a << i }
    a.should == [6, 4, 3, 1]
  end
  
  specify "rindex returns the first index backwards from the end where element == to object" do
    class X; def ==(obj); 3 == obj; end; end

    [2, 3, 3, 1, X.new, 1].rindex(3).should == 4
  end

  specify "rindex returns size-1 if last element == to object" do
    [2, 1, 3, 2, 5].rindex(5).should == 4
  end

  specify "rindex returns 0 if only first element == to object" do
    [2, 1, 3, 1, 5].rindex(2).should == 0
  end

  specify "rindex returns nil if no element == to object" do
    [1, 1, 3, 2, 1, 3].rindex(4).should == nil
  end
  
  specify "select should return a new array of elements for which block is true" do
    [1, 3, 4, 5, 6, 9].select { |i| i % ((i + 1) / 2) == 0}.should == [1, 4, 6]
  end
  
  specify "shift should remove and return the first element" do
    a = [5, 1, 1, 5, 4]
    a.shift.should == 5
    a.should == [1, 1, 5, 4]
  end
  
  specify "shift should return nil when the array is empty" do
    [].shift.should == nil
  end
  
  specify "size should be a synonym for length" do
    [1, 2, 3].size.should == [1, 2, 3].length
  end
  
  # FIX: test in terms of #slice or #[]
  specify "slice! with index should remove and return the element at index" do
    a = [1, 2, 3, 4]

    a.slice!(2).should == 3
    a.should == [1, 2, 4]
  end
  
  specify "slice! with start, length should remove and return length elements beginning at start" do
    a = [1, 2, 3, 4, 5, 6]
    a.slice!(2, 3).should == [3, 4, 5]
    a.should == [1, 2, 6]
  end

  specify "slice! with range should remove and return elements in range" do
    a = [1, 2, 3, 4, 5, 6, 7]
    a.slice!(1..4).should == [2, 3, 4, 5]
    a.should == [1, 6, 7]
  end

  specify "sort should return a new array from sorting elements using first their class and then <=>" do
#    foo = Class.new { def <=>(obj); 4 <=> obj; end}.new
    class D; def <=>(obj); 4 <=> obj; end; end
    ass = D.new

    [1, 1, 5, -5, 2, -10, 14, 6].sort.should == [-10, -5, 1, 1, 2, 5, 6, 14]
    should_raise {[1, 1, 5, -5, 2, -10, 14, 6, ass].sort.should == [-10, -5, 1, 1, 2, ass, 5, 6, 14]}
  end
  
  specify "sort may take a block which is used to determine the order of objects a and b described as -1, 0 or +1" do
    a = [5, 1, 4, 3, 2]
    a.sort.should == [1, 2, 3, 4, 5]
    a.sort {|x, y| y <=> x}.should == [5, 4, 3, 2, 1]
  end
  
  specify "sort! should sort array in place using <=>" do
    a = [1, 9, 7, 11, -1, -4]
    a.sort!
    a.should == [-4, -1, 1, 7, 9, 11]
  end
  
  specify "sort! should sort array in place using block value" do
    a = [1, 3, 2, 5, 4]
    a.sort! { |x, y| y <=> x }
    a.should == [5, 4, 3, 2, 1]
  end
  
  specify "to_a returns self" do
    a = [1, 2, 3]
    a.to_a.should == [1, 2, 3]
    a.equal?(a.to_a).should == true 
  end
  
  specify "to_a called on a subclass of Array should return an instance of Array" do
    class E < Array; end
    
    e = E.new
    e << 1
    p e, e[0..-1]
#    should_raise { F.new.to_a.class.should == Array }
  end
  
  specify "to_ary returns self" do
    a = [1, 2, 3]
    a.equal?(a.to_ary).should == true
  end
  
  specify "to_s is equivalent to #joining without a separator string" do
    a = [1, 2, 3, 4]
    a.to_s.should == a.join
    $, = '-'
    a.to_s.should == a.join
    $, = ''
  end
  
  specify "transpose assumes an array of arrays and should return the result of transposing rows and columns" do
    [[1, 'a'], [2, 'b'], [3, 'c']].transpose.should == [[1, 2, 3], ["a", "b", "c"]]
  end

  specify 'transpose raises if the elements of the array are not Arrays or respond to to_ary' do
    class G; def to_a(); [1, 2]; end; end
    class H; def to_ary(); [1, 2]; end; end

    should_raise { [G.new, [:a, :b]].transpose } 
    [H.new, [:a, :b]].transpose.should == [[1, :a], [2, :b]]
  end

  specify 'transpose raises if the arrays are not of the same length' do
    should_raise(IndexError) { [[1, 2], [:a]].transpose }
  end
  
  specify "uniq should return an array with no duplicates" do
    [ "a", "a", "b", "b", "c" ].uniq.should == ["a", "b", "c"]
  end
  
  specify "uniq! modifies the array in place" do
    a = [ "a", "a", "b", "b", "c" ]
    a.uniq!
    a.should == ["a", "b", "c"]
  end
  
  specify "uniq! should return self" do
    a = [ "a", "a", "b", "b", "c" ]
    a.equal?(a.uniq!).should == true
  end
  
  specify "uniq! should return nil if no changes are made to the array" do
    [ "a", "b", "c" ].uniq!.should == nil
  end
  
  specify "unshift should prepend object to the original array" do
    a = [1, 2, 3]
    a.unshift("a").should == ["a", 1, 2, 3]
    a.should == ['a', 1, 2, 3]
  end
  
  specify "values_at with indexes should return an array of elements at the indexes" do
    [1, 2, 3, 4, 5].values_at(1, 0, 5, -1, -8, 10).should == [2, 1, nil, 5, nil, nil]
  end
  
  specify "values_at with ranges should return an array of elements in the ranges" do
    [1, 2, 3, 4, 5].values_at(0..2, 1...3, 4..6).should == [1, 2, 3, 2, 3, 5, nil]
  end
  
  specify "zip should return an array of arrays containing cooresponding elements of each array" do
    [1, 2, 3, 4].zip(["a", "b", "c", "d"]).should == [[1, "a"], [2, "b"], [3, "c"], [4, "d"]]
  end
  
  specify 'zip fills in missing values with nil' do
    [1, 2, 3, 4, 5].zip(["a", "b", "c", "d"]).should == [[1, "a"], [2, "b"], [3, "c"], [4, "d"], [5, nil]]
  end
end

describe 'Array substringing using #[] and #slice' do
  # These two must be synonymous
  %w|[] slice|.each do |cmd|

    it "provides the element at the specified index with [x] (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 1).should == "b"
    end
    
    it "counts backwards for negative indices for [x] (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, -2).should == "d"
    end
    
    it "[x, y] returns subarray of length counting from end for negative index (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, -2, 2).should == ["d", "e"]
    end
    
    it "[x, y] should provide a subarray from x containing length elements (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 2, 3).should == ["c", "d", "e"]
    end
    
    it "[0, x] should provide a subarray from start containing length elements (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 0, 3).should == ["a", "b", "c"]
    end
    
    it "[m..n] should provide a subarray specified by range (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 1..3).should == ["b", "c", "d"]
      [ "a", "b", "c", "d", "e" ].send(cmd, 4..-1).should == ['e']
      [ "a", "b", "c", "d", "e" ].send(cmd, 3..3).should == ['d']
      [ "a", "b", "c", "d", "e" ].send(cmd, 3..-2).should == ['d']
      ['a'].send(cmd, 0..-1).should == ['a']
    end
    
    it "[m...n] should provide a subarray specified by range sans last as per normal Ranges (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 1...3).should == ["b", "c"]
    end
    
    it "[m..n] should return existing requested items if range start is in the array but range end is not (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 4..7).should == ["e"]
    end
    
    it "[x] returns nil for a requested index not in the array (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 5).should == nil
    end
    
    it "[x, y] returns [] if the index is valid but count is zero (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 0, 0).should == []
      [ "a", "b", "c", "d", "e" ].send(cmd, 2, 0).should == []
    end

    it "[x, y] returns [] if index == array.size (##{cmd})" do
      %w|a b c d e|.send(cmd, 5, 2).should == []
    end

    it "[x, y] returns nil if index > array.size (##{cmd})" do
      %w|a b c d e|.send(cmd, 6, 2).should == nil
    end

    it "[x, y] returns nil if count is negative (##{cmd})" do
      %w|a b c d e|.send(cmd, 3, -1).should == nil
      %w|a b c d e|.send(cmd, 2, -2).should == nil
      %w|a b c d e|.send(cmd, 1, -100).should == nil
    end
    
    it "[x..y] returns nil if no requested index is in the array (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, 6..10).should == nil
    end
    
    it "[m..n] returns nil if range start is not in the array (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, -10..2).should == nil
      [ "a", "b", "c", "d", "e" ].send(cmd, 10..12).should == nil
    end
    
    it "[m...n] should return an empty array when m == n (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 1...1).should == []
    end
    
    it "[0...0] should return an empty array (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 0...0).should == []
    end
    
    it "[m..n] should provide a subarray where m, n negatives and m < n (##{cmd})" do
      [ "a", "b", "c", "d", "e" ].send(cmd, -3..-2).should == ["c", "d"]
    end
    
    it "[0..0] should return an array containing the first element (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 0..0).should == [1]
    end
    
    it "[0..-1] should return the entire array (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 0..-1).should == [1, 2, 3, 4, 5]
    end
    
    it "[0...-1] should return all but the last element (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 0...-1).should == [1, 2, 3, 4]
    end

    it "returns [3] for [2..-1] out of [1, 2, 3]  (##{cmd}) <Resolves bug found by brixen, Defiler, mae>" do
      [1,2,3].send(cmd, 2..-1).should == [3]
    end
    
    it "[m..n] should return an empty array when m > n and m, n are positive (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, 3..2).should == []
    end
    
    it "[m..n] should return an empty array when m > n and m, n are negative (##{cmd})" do
      [1, 2, 3, 4, 5].send(cmd, -2..-3).should == []
    end

  end                         # slice, [] each
end

describe 'Array splicing using #[]=' do
  specify "[]= should set the value of the element at index" do
    a = [1, 2, 3, 4]
    a[2] = 5
    a[-1] = 6
    a[5] = 3
    a.should == [1, 2, 5, 6, nil, 3]
  end
  
  specify "[]= should remove the section defined by start, length when set to nil" do
    a = ['a', 'b', 'c', 'd', 'e']
    a[1, 3] = nil
    a.should == ["a", "e"]
  end
  
  specify "[]= should set the section defined by start, length to other" do
    a = [1, 2, 3, 4, 5, 6]
    a[0, 1] = 2
    a[3, 2] = ['a', 'b', 'c', 'd']
    a.should == [2, 2, 3, "a", "b", "c", "d", 6]
  end
  
  specify "[]= should remove the section defined by range when set to nil" do
    a = [1, 2, 3, 4, 5]
    a[0..1] = nil
    a.should == [3, 4, 5]
  end
  
  specify "[]= should set the section defined by range to other" do
    a = [6, 5, 4, 3, 2, 1]
    a[1...2] = 9
    a[3..6] = [6, 6, 6]
    a.should == [6, 9, 4, 6, 6, 6]
  end

  specify "[0]= returns value assigned" do
    a = [1, 2, 3, 4, 5]
    (a[0] = 6).should == 6
  end
  
  specify "[idx]= returns value assigned if idx is inside array" do
    a = [1, 2, 3, 4, 5]
    (a[3] = 6).should == 6
  end
  
  specify "[idx]= returns value assigned if idx is right beyond right array boundary" do
    a = [1, 2, 3, 4, 5]
    (a[5] = 6).should == 6
  end
  
  specify "[idx]= returns value assigned if idx far beyond right array boundary" do
    a = [1, 2, 3, 4, 5]
    (a[10] = 6).should == 6
  end

  specify "[idx, cnt]= returns non-array value if non-array value assigned" do
    a = [1, 2, 3, 4, 5]
    (a[2, 3] = 10).should == 10
  end

  specify "[idx, cnt]= returns array if array assigned" do
    a = [1, 2, 3, 4, 5]
    (a[2, 3] = [4, 5]).should == [4, 5]
  end

  specify "[m..n]= returns non-array value if non-array value assigned" do
    a = [1, 2, 3, 4, 5]
    (a[2..4] = 10).should == 10
  end
  
  specify "[m..n]= returns array if array assigned" do
    a = [1, 2, 3, 4, 5]
    (a[2..4] = [7, 8]).should == [7, 8]
  end
  
  specify "[idx]= sets the value of the element at index" do
      a = [1, 2, 3, 4]
      a[2] = 5
      a[-1] = 6
      a[5] = 3
      a.should == [1, 2, 5, 6, nil, 3]
    end

  specify "[idx]= with idx right beyond array boundary should set the value of the element" do
    a = [1, 2, 3, 4]
    a[4] = 8
    a.should == [1, 2, 3, 4, 8]
  end
    
  specify "[idx, cnt]= removes the section defined by start, length when set to nil" do
      a = ['a', 'b', 'c', 'd', 'e']
      a[1, 3] = nil
      a.should == ["a", "e"]
    end
    
  specify "[idx, cnt]= removes the section when set to nil if negative index within bounds and cnt > 0" do
    a = ['a', 'b', 'c', 'd', 'e']
    a[-3, 2] = nil
    a.should == ["a", "b", "e"]
  end
  
  specify "[idx, cnt]= replaces the section defined by start, length to other" do
      a = [1, 2, 3, 4, 5, 6]
      a[0, 1] = 2
      a[3, 2] = ['a', 'b', 'c', 'd']
      a.should == [2, 2, 3, "a", "b", "c", "d", 6]
    end

  specify "[idx, cnt]= replaces the section to other if idx < 0 and cnt > 0" do
    a = [1, 2, 3, 4, 5, 6]
    a[-3, 2] = ["x", "y", "z"]
    a.should == [1, 2, 3, "x", "y", "z", 6]
  end

  specify "[idx, cnt]= replaces the section to other even if cnt spanning beyond the array boundary" do
    a = [1, 2, 3, 4, 5]
    a[-1, 3] = [7, 8]
    a.should == [1, 2, 3, 4, 7, 8]
  end

  specify "[idx, 0]= inserts other section in place defined by idx" do
    a = [1, 2, 3, 4, 5]
    a[3, 0] = [7, 8]
    a.should == [1, 2, 3, 7, 8, 4, 5]
  end
    
  specify "[idx, cnt]= raises IndexError if cnt < 0" do
    begin
    should_raise(IndexError) { [1, 2, 3, 4,  5][2, -1] = [7, 8] }
    rescue 
    end
  end

  specify "[m..n]= removes the section defined by range when set to nil" do
      a = [1, 2, 3, 4, 5]
      a[0..1] = nil
      a.should == [3, 4, 5]
    end

  specify "[m..n]= removes the section when set to nil if m and n < 0" do
    a = [1, 2, 3, 4, 5]
    a[-3..-2] = nil
    a.should == [1, 2, 5]
  end
    
  specify "[m..n]= replaces the section defined by range" do
      a = [6, 5, 4, 3, 2, 1]
      a[1...2] = 9
      a[3..6] = [6, 6, 6]
      a.should == [6, 9, 4, 6, 6, 6]
    end

  specify "[m..n]= replaces the section if m and n < 0" do
    a = [1, 2, 3, 4, 5]
    a[-3..-2] = [7, 8, 9]
    a.should == [1, 2, 7, 8, 9, 5]
  end

  specify "[m..n]= replaces the section if m < 0 and n > 0" do
    a = [1, 2, 3, 4, 5]
    a[-4..3] = [8]
    a.should == [1, 8, 5]
  end

  specify "[m..n]= inserts the other section at m if m > n" do
    a = [1, 2, 3, 4, 5]
    a[3..1] = [8]
    a.should == [1, 2, 3, 8, 4, 5]
  end

end

# Redundant, should be in Object --rue
context "Array inherited instance method" do
 specify "instance_variable_get should return the value of the instance variable" do
   a = []
   a.instance_variable_set(:@c, 1)
   a.instance_variable_get(:@c).should == 1
 end
 
 specify "instance_variable_get should return nil if the instance variable does not exist" do
   [].instance_variable_get(:@c).should == nil
 end
 
 specify "instance_variable_get should raise NameError if the argument is not of form '@x'" do
   should_raise(NameError) { [].instance_variable_get(:c) }
 end
end

