####################################################################
# tc_uniq.rb
#
# Test suite for the Array#uniq and Array#unique instance methods.
####################################################################
require "test/unit"
require 'digest/md5'

class TestObject
  attr_reader :a, :b
  def initialize(a, b)
    @a = a
    @b = b
  end
  def eql?(other_test_object)
    [@a, @b].eql?([other_test_object.a, other_test_object.b])
  end
  def hash;
    Digest::MD5.hexdigest("#{@a}_#{@b}").to_i(16)
  end
end

class TC_Array_Uniq_Instance < Test::Unit::TestCase
   def setup   
      @array = ["a","b","b","c","c","c",nil,nil,false,false,true,true]
      t1 = TestObject.new("abc", "123")
      t2 = TestObject.new("abc", "123")
      @array2 = [t1, t2]
   end

   def test_basic
      assert_respond_to(@array, :uniq)
      assert_respond_to(@array, :uniq!)
      assert_nothing_raised{ @array.uniq }
      assert_nothing_raised{ @array.uniq! }
   end

   def test_uniq
      assert_equal(["a","b","c",nil,false,true], @array.uniq)
      assert_equal([1,2,3], [1,2,3].uniq)
      assert_equal([nil], [nil].uniq)
      assert_equal(
         ["a","b","b","c","c","c",nil,nil,false,false,true,true], @array
      )
   end
   
   def test_uniq_objects
     assert_equal(1, @array2.uniq.length)
   end

   def test_uniq_bang
      assert_equal(["a","b","c",nil,false,true], @array.uniq!)
      assert_equal(["a","b","c",nil,false,true], @array)
      assert_equal(nil, [1,2,3].uniq!)
      assert_equal(nil, [nil].uniq!)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.uniq(1) }
      assert_raises(ArgumentError){ @array.uniq!(1) }
   end

   def teardown
      @array = nil
   end
end
