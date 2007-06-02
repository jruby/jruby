######################################################################
# tc_each.rb
#
# Test for the Range#each instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Range_Each_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @range_int   = Range.new(-1, 1)
      @range_float = Range.new(-1.5, 1.5)
      @range_char  = Range.new('a', 'c')
   end

   def test_each_basic
      assert_respond_to(@range_int, :each)
      assert_nothing_raised{ @range_int.each{} }
   end

   def test_each_int
      array = []
      assert_nothing_raised{ @range_int.each{ |e| array << e } }
      assert_equal(-1, array[0])
      assert_equal(0, array[1])
      assert_equal(1, array[2])
      assert_equal(nil, array[3])
   end

   def test_each_char
      array = []
      assert_nothing_raised{ @range_char.each{ |e| array << e } }
      assert_equal('a', array[0])
      assert_equal('b', array[1])
      assert_equal('c', array[2])
      assert_equal(nil, array[3])
   end

   # Previous releases will go into an infinite loop here.
   if RELEASE >= 6
      def test_each_edge_cases
         assert_nothing_raised{ Range.new("", "").each{} }
         assert_nothing_raised{ Range.new(0, 0).each{} }
      end
   end

   def test_each_expected_errors
      assert_raises(TypeError){ @range_float.each{} }
   end

   def teardown
      @range_int   = nil  
      @range_float = nil  
      @range_char  = nil  
   end
end
