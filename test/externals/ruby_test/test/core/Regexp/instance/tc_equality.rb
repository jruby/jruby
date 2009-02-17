###############################################################################
# tc_equality.rb
#
# Test case for the Regexp#== instance method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Regexp_Equality_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @regex    = /abc/
      @regex_x  = /abc/x
      @regex_i  = /abc/i
      @regex_ix = /abc/ix
   end

   def test_equality_basic
      assert_respond_to(@regex, :==)
      assert_nothing_raised{ @regex == @regex_x }
      assert_kind_of(Boolean, @regex == @regex_x)
   end

   def test_equality_true
      assert_equal(true, @regex == /abc/)
      assert_equal(true, @regex_x == /abc/x)
      assert_equal(true, @regex_i == /abc/i)
      assert_equal(true, @regex_ix == /abc/xi)
   end

   def test_equality_false
      assert_equal(false, @regex == /abcd/)
      assert_equal(false, @regex == @regex_x)
      assert_equal(false, @regex == @regex_i)
      assert_equal(false, @regex == @regex_ix)
      assert_equal(false, @regex_x == @regex_ix)
      assert_equal(false, @regex_i == @regex_ix)
   end

   def test_equality_edge_cases
      assert_equal(true, // == //)
      assert_equal(false, // == //i)
   end

   def test_equality_against_non_regexp_objects
      assert_equal(false, @regex == [])
      assert_equal(false, @regex == true)
      assert_equal(false, @regex == false)
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @regex.send(:==, @regex_i, @regex_x) }
   end

   def teardown
      @regex    = nil
      @regex_x  = nil
      @regex_i  = nil
      @regex_ix = nil
   end
end
