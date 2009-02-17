#######################################################################
# tc_fetch.rb
#
# Test suite for the Array#fetch instance method.
#######################################################################
require 'test/unit'

class TC_Array_Fetch_InstanceMethod < Test::Unit::TestCase
   def setup
      @array = %w/a b c d/
   end

   def test_fetch_basic
      assert_respond_to(@array, :fetch)
      assert_nothing_raised{ @array.fetch(0) }
      assert_nothing_raised{ @array.fetch(0, 'a') }
      assert_nothing_raised{ @array.fetch(0){ } }
   end

   def test_fetch
      assert_equal('a', @array.fetch(0))
      assert_equal('c', @array.fetch(2))
      assert_equal('d', @array.fetch(-1))
      assert_equal('c', @array.fetch(-2))
   end

   def test_fetch_with_float
      assert_equal('a', @array.fetch(0.0))
      assert_equal('c', @array.fetch(2.9))
      assert_equal('d', @array.fetch(-1.1))
      assert_equal('c', @array.fetch(-2.5))
   end

   def test_fetch_with_default
      assert_equal('test', @array.fetch(99, 'test'))
      assert_equal('test', @array.fetch(-23, 'test'))
   end

   def test_fetch_with_block
      assert_equal(8, @array.fetch(4){ |i| i * 2 })
      assert_equal(-10, @array.fetch(-5){ |i| i * 2 })
      assert_equal('test', @array.fetch(99){ 'test' })
   end

   def test_fetch_expected_errors
      assert_raises(ArgumentError){ @array.fetch }
      assert_raises(ArgumentError){ @array.fetch{ 'test' } }
      assert_raises(IndexError){ @array.fetch(4) }
      assert_raises(IndexError){ @array.fetch(-5) }
   end

   def teardown
      @array = nil
   end
end
