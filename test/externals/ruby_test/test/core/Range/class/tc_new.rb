######################################################################
# tc_new.rb
#
# Test case for the Range.new class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Range_New_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @high  = 9223372036854775808
      @low   = -9223372036854775808
      @range = nil

      # Atypical range objects
      @file1 = 'test1.txt'
      @file2 = 'test2.txt'
      @time1 = Time.now
      @time2 = @time1 + 1000
      touch(@file1)
      touch(@file2)
   end

   def test_new_basic
      assert_respond_to(Range, :new)
      assert_nothing_raised{ Range.new(0, 1) }
      assert_nothing_raised{ Range.new(-100, 100) }
      assert_nothing_raised{ Range.new('a', 'z') }
   end

   def test_new_numeric
      assert_nothing_raised{ Range.new(0, 1) }
      assert_nothing_raised{ Range.new(0, 100) }
      assert_nothing_raised{ Range.new(-100, 100) }
      assert_nothing_raised{ Range.new(1.7, 23.5) }
      assert_nothing_raised{ Range.new(@low, @high) }
   end

   def test_new_numeric_with_exclusive
      assert_nothing_raised{ Range.new(0, 1, true) }
      assert_nothing_raised{ Range.new(0, 100, false) }
      assert_nothing_raised{ Range.new(-100, 100, true) }
      assert_nothing_raised{ Range.new(@low, @high, false) }
   end

   def test_new_alphabetic
      assert_nothing_raised{ Range.new('a', 'z') }
      assert_nothing_raised{ Range.new('a', 'a') }
      assert_nothing_raised{ Range.new('.', '"') }
   end

   def test_new_alphabetic_with_exclusive
      assert_nothing_raised{ Range.new('a', 'z', true) }
      assert_nothing_raised{ Range.new('a', 'a', false) }
      assert_nothing_raised{ Range.new('.', '"', true) }
   end

   # Support <=> but not .succ
   def test_new_with_nonstandard_objects
      assert_nothing_raised{ Range.new([], []) }
      assert_nothing_raised{ Range.new(@file1, @file2) }
      assert_nothing_raised{ Range.new(@time1, @time2) }
   end

   def test_new_edge_cases
      assert_nothing_raised{ Range.new(0, 0) }
      assert_nothing_raised{ Range.new('', '', true) }
      assert_nothing_raised{ Range.new(1, 0) }
      assert_nothing_raised{ Range.new(0.5, 7) }
   end

   def test_new_expected_errors
      assert_raises(ArgumentError){ Range.new(nil, nil) }
      assert_raises(ArgumentError){ Range.new(true, true) }
      assert_raises(ArgumentError){ Range.new({}, {}) }
      assert_raises(ArgumentError){ Range.new('z', 0) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)

      @high  = nil
      @low   = nil
      @range = nil
      @file1 = nil
      @file2 = nil
      @time1 = nil
      @time2 = nil
   end
end
