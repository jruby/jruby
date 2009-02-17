###################################################################
# tc_sort.rb
#
# Test suite for the Array#sort and Array#sort! instance methods.
###################################################################
require "test/unit"

class TC_Array_Sort_InstanceMethod < Test::Unit::TestCase
   def setup
      @array = %w/b e a d c/
   end

   def test_basic
      assert_respond_to(@array, :sort)
      assert_respond_to(@array, :sort!)
      assert_nothing_raised{ @array.sort }
      assert_nothing_raised{ @array.sort{ |x,y| x <=> y } }
      assert_nothing_raised{ @array.sort! }
      assert_nothing_raised{ @array.sort!{ |x,y| x <=> y } }
   end

   def test_sort
      assert_equal(["a","b","c","d","e"], @array.sort)
      assert_equal(["e","d","c","b","a"], @array.sort{ |x,y| y <=> x })
      assert_equal(["b","e","a","d","c"], @array)
   end

   def test_sort_bang
      assert_equal(["a","b","c","d","e"], @array.sort!)
      assert_equal(["a","b","c","d","e"], @array)
      assert_equal(["e","d","c","b","a"], @array.sort!{ |x,y| y <=> x })
      assert_equal(["e","d","c","b","a"], @array)
   end

   def test_expected_errors
      assert_raise(ArgumentError){ @array.sort(1) }
      assert_raise(ArgumentError){ @array.sort{} }
   end

   def teardown
      @array = nil
   end
end
