######################################################################
# tc_eql.rb
#
# Test case for the Numeric#eql? instance method.
######################################################################
require 'test/unit'

class TC_Numeric_Eql_InstanceMethod < Test::Unit::TestCase
   def setup
      @integer  = 1
      @float    = 1.0
      @bignum   = 4294967296
      @bigfloat = 4294967296.0
   end

   def test_is_eql_basic
      assert_respond_to(@integer, :eql?)
      assert_nothing_raised{ @integer.eql?(@float) }
   end

   def test_is_eql
      assert_equal(true, @integer.eql?(@integer))
      assert_equal(false, @integer.eql?(@float))
      assert_equal(true, @float.eql?(@float))
      assert_equal(false, @float.eql?(@integer))
   end

   def test_is_eql_big
      assert_equal(true, @bignum.eql?(@bignum))
      assert_equal(false, @bignum.eql?(@bigfloat))
   end

   def test_is_eql_edge_cases
      assert_equal(true, 0.eql?(0))
      assert_equal(true, 1.0.eql?(1.00000000000000000000))
      assert_equal(false, 0.eql?(0.0))
      assert_equal(false, 000000.eql?(0.0))
      assert_equal(true, 000000.eql?(000))
   end

   def teardown
      @integer  = nil
      @float    = nil
      @bignum   = nil
      @bigfloat = nil
   end
end
