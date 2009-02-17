###############################################################################
# tc_bref.rb
#
# Test case for the Fixnum#[] (bit ref) instance method.
###############################################################################
require 'test/unit'

class TC_Fixnum_Bref_InstanceMethod < Test::Unit::TestCase
   def setup
      @fixnum = 0b11001100101010
   end

   def test_bref_basic
      assert_respond_to(@fixnum, :[])
      assert_nothing_raised{ @fixnum[0] }
   end

   def test_bref
      assert_equal(0, @fixnum[0])
      assert_equal(1, @fixnum[1])
      assert_equal(0, @fixnum[2])
      assert_equal(0, @fixnum[10])
   end

   def test_bref_edge_cases
      assert_equal(0, @fixnum[-1])
      assert_equal(0, @fixnum[9999999])
   end

   def test_bref_expected_errors
      assert_raise(ArgumentError){ @fixnum[0,1] }
   end

   def teardown
      @fixnum = nil
   end
end
