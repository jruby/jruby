require File.dirname(__FILE__) + '/../spec_helper'

# ==, [], []=, clear, default, default=, default_proc, delete,
# delete_if, each, each_key, each_pair, each_value, empty?, fetch,
# has_key?, has_value?, include?, index, indexes, indices,
# initialize_copy, inspect, invert, key?, keys, length, member?,
# merge, merge!, rehash, reject, reject!, replace, select, shift,
# size, sort, store, to_a, to_hash, to_s, update, value?, values,
# values_at

context 'Creating a Hash' do
  specify 'Hash[] is a shorthand for creating a Hash, values can be provided as  key => value, .. OR key, value, ...' do
    Hash[:a => 1, :b => 2].should == {:a => 1, :b => 2} 
    Hash[:a, 1, :b, 2].should == {:a => 1, :b => 2}
  end
 
  specify 'Using the parameter list format for Hash[], an even number of arguments must be provided' do
    should_raise(ArgumentError) { Hash[1, 2, 3] }
  end
 
  specify 'The two argument styles to Hash[] cannot be mixed' do
    should_raise(ArgumentError) { Hash[1, 2, {3 => 4}] }
  end
end

context "Hash class methods" do
  specify "new with object creates a new Hash with default object" do
    Hash.new(5)[:a].should == 5
  end

  specify 'new with object will return the *same* object' do
    class HashSpecNew; end

    h = Hash.new(HashSpecNew.new)
    h[:foo].object_id.should == h[:bar].object_id
  end
  
  specify "new with block creates a new Hash calling block with key for the default object" do
    h = Hash.new { |hash, k| k.kind_of?(Numeric) ? hash[k] = k + 2 : hash[k] = k }
    h[1].should == 3
    h['this'].should == 'this'
    h.should == {1 => 3, 'this' => 'this'}
  end
end

context "Hash instance methods" do
  specify "== is true if they have the same number of keys and each key-value pair matches" do
    Hash.new(5).should == Hash.new(1)
    Hash.new {|h, k| 1}.should == Hash.new {}
    Hash.new {|h, k| 1}.should == Hash.new(2)
    
    a = {:a => 5}
    b = {}

    a.should_not == b

    b[:a] = 5

    a.should == b

    c = Hash.new {|h, k| 1}
    d = Hash.new {}
    c[1] = 2
    d[1] = 2
    c.should == d
  end
  
  specify "[] should return the default (immediate) value" do
    Hash.new(7)[1234].should == 7
  end

  specify "[] should return the default (dynamic) value" do
    Hash.new {|hsh, key| hsh[key] = (key.to_s + 'awesome')}[1234].should == '1234awesome'
  end
  
  specify "[]= should associate the key with the value and return the value" do
    h = { :a => 1 }
    (h[:b] = 2).should == 2
    h.should == {:b=>2, :a=>1}
  end
  
  specify "clear should remove all key, value pairs" do
    {:a=>2,:b=>1,:c=>1}.clear.should == {}
  end

  specify "default should return the default value" do
    h = Hash.new(5)
    h.default.should == 5 
    {}.default.should == nil
  end
  
  specify "default= should set the default value" do
    h = Hash.new
    h.default = 99
    h.default.should == 99
  end
  
  specify "default_proc should return the block passed to Hash.new" do
    h = Hash.new { |i| 'Paris' }
    p = h.default_proc
    p.call(1).should == 'Paris'
  end
  
  specify "default_proc should return nil if no block was passed to proc" do
    Hash.new.default_proc.should == nil
  end
  
  specify "delete should delete the entry whose key is == key and return the deleted value" do
    h = {:a => 5, :b => 2}
    h.delete(:b).should == 2
    h.should == {:a => 5}
  end
  
  specify "delete should return nil if the key is not found" do
    {:a => 1, :b => 10, :c => 100 }.delete(:d).should == nil
  end
  
  specify "delete_if should remove every entry for which block is true and returns self" do
    h = {:a => 1, :b => 2, :c => 3, :d => 4}
    h.delete_if { |k,v| v % 2 == 1 }.should == {:b => 2, :d => 4} 
    h.should == {:b => 2, :d => 4}
  end
  
  specify "each should call block once for each entry, passing key, value" do
    h = {}
    {:a, 1, :b, 2, :c, 3, :d, 5}.each { |k,v| h[k.to_s] = v.to_s }
    h.should == {"a" => "1", "b" => "2", "c" => "3", "d" => "5" }
  end
  
  specify "each_key should call block once for each key, passing key" do
    h = {}
    {1 => -1, 2 => -2, 3 => -3, 4 => -4 }.each_key { |k| h[k] = k }
    h.should == { 1 => 1, 2 => 2, 3 => 3, 4 => 4 }
  end
  
  specify "each_pair should be a synonym for each" do
    a, b = [], []
    {:a, 1, :b, 2, :c, 3, :d, 5}.each_pair { |k,v| a << "#{k} => #{v}" }
    {:a, 1, :b, 2, :c, 3, :d, 5}.each { |k,v| b << "#{k} => #{v}" }

    a.should == b
  end
  
  specify "each_value should should call block once for each key, passing value" do
    h = {}
    { :a => -5, :b => -3, :c => -2, :d => -1 }.each_value { |v| h[v.abs] = v }
    h.should == { 5 => -5, 1 => -1, 2 => -2, 3 => -3 }
  end
  
  specify "empty? should return true if block has not entries" do
    {}.empty?.should == true
    {1 => 1}.empty?.should == false
  end
  
  specify "fetch should return the value for key" do
    { :a => 1, :b => -1 }.fetch(:b).should == -1
  end
  
  specify "fetch should raise IndexError if key is not found" do
    should_raise(IndexError) { {}.fetch(:a) }
  end
  
  specify "fetch with default should return default if key is not found" do
    {}.fetch(:a, 'not here!').should == "not here!"
  end
  
  specify "fetch with block should return value of block if key is not found" do
    {}.fetch('a') { |k| k + '!' }.should == "a!"
  end
  
  specify "has_key? should be a synonym for key?" do
    h = {:a => 1, :b => 2, :c => 3}
    h.has_key?(:a).should == h.key?(:a)
    h.has_key?(:b).should == h.key?(:b) 
    h.has_key?('b').should == h.key?('b') 
    h.has_key?(2).should == h.key?(2)
  end
  
  specify "has_value? should be a synonym for value?" do
    {:a => :b}.has_value?(:a).should == {:a => :b}.value?(:a)
    {1 => 2}.has_value?(2).should == {1 => 2}.value?(2)
  end
  
  specify "include? should be a synonym for key?" do
    h = {:a => 1, :b => 2, :c => 3}
    h.include?(:a).should   == h.key?(:a) 
    h.include?(:b).should   == h.key?(:b) 
    h.include?('b').should  == h.key?('b')
    h.include?(2).should    == h.key?(2)
  end

  specify "index should return the corresponding key for value" do
    {2 => 'a', 1 => 'b'}.index('b').should == 1
  end
  
  specify "index should return nil if the value is not found" do
    {:a => -1, :b => 3.14, :c => 2.718}.index(1).should == nil
  end
  
  specify "indexes and indices should be DEPRECATED synonyms for values_at" do
    h = {:a => 9, :b => 'a', :c => -10, :d => nil}
    h.indexes(:a, :d, :b).should == h.values_at(:a, :d, :b)
    h.indices(:a, :d, :b).should == h.values_at(:a, :d, :b)
  end
  
  specify "initialize_copy should be a synonym for replace" do
    Hash.new.send(:initialize_copy, { :a => 1, :b => 2 }).should == Hash.new.send(:replace, { :a => 1, :b => 2 })
  end
  
  specify "inspect should return a string representation of some kind" do
    {:a => 0, :b => -2, :c => 4, :d => -6}.inspect.class.should == String
  end
  
  specify "invert should return a new hash where keys are values and vice versa" do
    { 1 => 'a', 2 => 'b', 3 => 'c' }.invert.should == { 'a' => 1, 'b' => 2, 'c' => 3 }
  end
  
  specify "key? should return true if argument is a key" do
    h = { :a => 1, :b => 2, :c => 3 }
    h.key?(:a).should == true
    h.key?(:b).should == true
    h.key?('b').should == false
    h.key?(2).should == false
  end
  
  specify "key? should return false if the key was not found" do
    { 5 => 7 }.key?(7).should == false
  end

  specify "key? should return true if the key's matching value was nil" do
    { :xyz => nil }.key?(:xyz).should == true
  end

  specify "key? should return true if the key's matching value was false" do
    { :xyz => false }.key?(:xyz).should == true
  end

  specify "key? should return true if the key was found" do
    { :xyz => 9 }.key?(:xyz).should == true
  end

  specify "keys should return an array populated with keys" do
    { 1 => 2, 2 => 4, 4 => 8 }.keys.should == [1, 2, 4]
  end

  specify "length should return the number of entries" do
    {:a => 1, :b => 'c'}.length.should == 2
    {}.length.should == 0
  end
  
  specify "member? should be a synonym for key?" do
    h = {:a => 1, :b => 2, :c => 3}
    h.member?(:a).should == h.key?(:a)
    h.member?(:b).should == h.key?(:b)
    h.member?('b').should == h.key?('b')
    h.member?(2).should == h.key?(2)
  end
  
  specify "merge should return a new hash by combining self with the contents of other" do
    { 1, :a, 2, :b, 3, :c }.merge(:a => 1, :c => 2).should == { :c=> 2, 1 => :a, 2 => :b, :a => 1, 3 => :c }
  end
  
  specify "merge with block sets any duplicate key to the value of block" do
    { :a => 2, :b => 1 }.merge(:a => -2, :c => -3) { |k,v| -9 }.should == { :c => -3, :b => 1, :a => -9 }
  end
  
  specify "merge! should adds the entries from other, overwriting duplicate keys. Returns self" do
    h = { :_1 => 'a', :_2 => '3' }
    h.merge!(:_1 => '9', :_9 => 2).should == { :_1 => "9", :_2 => "3", :_9 => 2 }
    h.should == {:_1 => "9", :_2 => "3", :_9 => 2}
  end
  
  specify "merge! with block sets any duplicate key to the value of block" do
    h = { :a => 2, :b => -1 }
    h.merge!(:a => -2, :c => 1) { |k,v| 3.14 }.should == {:c => 1, :b => -1, :a => 3.14}
    h.should == {:c => 1, :b => -1, :a => 3.14}
  end
  
  specify "rehash should be provided" do
    Hash.new.respond_to?(:rehash).should == true
  end
  
  specify "reject should be equivalent to hsh.dup.delete_if" do
    h = { :a => 'a', :b => 'b', :c => 'd' }
    h.reject { |k,v| k == 'd' }.should == (h.dup.delete_if { |k, v| k == 'd' })
  end
  
  specify "reject! is equivalent to delete_if if changes are made" do
    {:a => 2}.reject! { |k,v| v > 1 }.should == ({:a => 2}.delete_if { |k, v| v > 1 })
  end
  
  specify "reject! should return nil if no changes were made" do
    { :a => 1 }.reject! { |k,v| v > 1 }.should == nil
  end
  
  specify "replace should replace the contents of self with other" do
    h = { :a => 1, :b => 2 }
    h.replace(:c => -1, :d => -2).should == { :c => -1, :d => -2 }
  end
  
  specify "select should return an array of entries for which block is true" do
    a = { :a => 9, :c => 4, :b => 5, :d => 2 }.select { |k,v| v % 2 == 0 }
    a.sort { |a,b| a.to_s <=> b.to_s }.should == [[:c, 4], [:d, 2]]
  end
  
  specify "shift should remove an entry from hash and return it in a two-element array" do
    h = { :a => 2 }
    h.shift.should == [:a, 2]
    h.should == {}
  end
  
  specify "size should be a synonym for length" do
    { :a => 1, :b => 'c' }.size.should == {:a => 1, :b => 'c'}.length 
    {}.size.should == {}.length
  end
  
  specify "sort should convert self to a nested array of [key, value] arrays and sort with Array#sort" do
    { 'a' => 'b', '1' => '2', 'b' => 'a' }.sort.should == [["1", "2"], ["a", "b"], ["b", "a"]]
  end
  
  specify "sort with block should use block to sort array" do
    { 1 => 2, 2 => 9, 3 => 4 }.sort { |a,b| b <=> a }.should == [[3, 4], [2, 9], [1, 2]]
  end

  specify "store should be a synonym for []=" do
    h1, h2 = {:a => 1}, {:a => 1}
    h1.store(:b, 2).should == (h2[:b] = 2)
    h1.should == h2
  end
  
  specify "to_a should return a nested array of [key, value] arrays" do
    {:a => 1, 1 => :a, 3 => :b, :b => 5}.to_a.sort { |a,b| a.to_s <=> b.to_s }.should == [[1, :a], [3, :b], [:a, 1], [:b, 5]]
  end
  
  specify "to_hash should should return self" do
    h = {}
    h.to_hash.equal?(h).should == true
  end
  
  specify "to_s should return a string by calling Hash#to_a and using Array#join with default separator" do
    { :fun => 'fun', 'fun' => :fun }.to_s.should == 'funfunfunfun'
  end
  
  specify "update should be a synonym for merge!" do
    h1 = { :_1 => 'a', :_2 => '3' }
    h2 = h1.dup

    h1.update(:_1 => '9', :_9 => 2).should == h2.merge!(:_1 => '9', :_9 => 2)
    h1.should == h2
  end
  
  specify "update with block should be a synonym for merge!" do
    h1 = { :a => 2, :b => -1 }
    h2 = h1.dup

    h1.update(:a => -2, :c => 1) { |k,v| 3.14 }.should == h2.update(:a => -2, :c => 1) { |k,v| 3.14 }
    h1.should == h2
  end
  
  specify "value? returns true if the value exists in the hash" do
    {:a => :b}.value?(:a).should == false
    {1 => 2}.value?(2).should == true
  end
  
  specify "values should return an array of values" do
    { 1 => :a, 'a' => :a, 'the' => 'lang'}.values.sort {|a, b| a.to_s <=> b.to_s}.should == [:a, :a, 'lang']
  end
  
  specify "values_at should return an array of values for the given keys" do
    {:a => 9, :b => 'a', :c => -10, :d => nil}.values_at(:a, :d, :b).should == [9, nil, 'a']
  end
end
