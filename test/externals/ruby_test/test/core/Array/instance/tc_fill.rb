#######################################################################
# tc_fill.rb
#
# Test suite for the Array#fill instance method.
#######################################################################
require 'test/unit'
require 'test/helper'

class TC_Array_Fill_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @array = %w/a b c d/
   end

   def test_fill_basic
      assert_respond_to(@array, :fill)
      assert_nothing_raised{ @array.fill('test') }
      assert_nothing_raised{ @array.fill('test', 1) }
      assert_nothing_raised{ @array.fill('test', 1, 2) }
      assert_nothing_raised{ @array.fill('test', 1..2) }
   end

   def test_fill_basic_block
      assert_nothing_raised{ @array.fill{ 'test' } }
      assert_nothing_raised{ @array.fill(1){ 'test' } }
      assert_nothing_raised{ @array.fill(1, 2){ 'test' } }
   end

   def test_fill_no_start
      assert_equal(['x','x','x','x'], @array.fill('x'))
      assert_equal(['z','z','z','z'], @array.fill('z'))
      assert_equal([nil,nil,nil,nil], @array.fill(nil))
   end

   def test_fill_with_start
      assert_equal(['a','b','z','z'], @array.fill('z', 2))
      assert_equal(['a','t','t','t'], @array.fill('t', -3))
      assert_equal(['a','t','t','t'], @array.fill('b', 99))
   end

   def test_fill_with_start_and_end
      assert_equal(['y','y','c','d'], @array.fill('y', 0, 2))
      assert_equal(['x','y','c','d'], @array.fill('x', 0, 1))
      assert_equal(['x','x','c','d'], @array.fill('x', 0..1))
      assert_equal(['x','x','c','c','c','c'], @array.fill('c', -1, 3))
   end

   def test_fill_with_float_start_and_end
      assert_equal(['y','y','c','d'], @array.fill('y', 0.0, 2.7))
      assert_equal(['x','y','c','d'], @array.fill('x', 0.9, 1.2))
      assert_equal(['x','y','c','c','c','c'], @array.fill('c', -1.9, 3.5))
   end

   def test_fill_with_block
      assert_equal(['x','x','x','x'], @array.fill{ 'x' })
      assert_equal(['x','x','z','z'], @array.fill(2){ 'z' })
      assert_equal(['y','y','z','z'], @array.fill(0,2){ 'y' })
      assert_equal(['v','v','z','z'], @array.fill(0..1){ 'v' })
   end

   def test_fill_explicit_nil_start_same_as_zero
      assert_equal(['x','x','x','x'], @array.fill('x', nil))
      assert_equal(['z','z','z','z'], @array.fill('z', nil))
   end

   def test_fill_explicit_nil_end_same_as_all
      assert_equal(['a','b','x','x'], @array.fill('x', 2, nil))
      assert_equal(['z','z','z','z'], @array.fill('z', nil, nil))
   end

   def test_fill_edge_cases
      assert_equal([], [].fill('x'))
      assert_equal([], [].fill([]))

      unless JRUBY || (RELEASE > 5 && RUBY_PATCHLEVEL >= 36)
         assert_equal([1,2,3], [1,2,3].fill('a', 0, -2))
      end
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.fill }
      unless JRUBY || (RELEASE > 5 && RUBY_PATCHLEVEL >= 36)
         assert_raise(ArgumentError){ [1,2,3].fill('a', 0, -2) }
      end
   end

   def teardown
      @array = nil
   end
end
