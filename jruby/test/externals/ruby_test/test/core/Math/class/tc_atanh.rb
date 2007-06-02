#####################################################################
# tc_atanh.rb
#
# Test cases for the Math.atanh method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Math_Atanh_Class < Test::Unit::TestCase
   include Test::Helper

   def test_atanh_basic
      assert_respond_to(Math, :atanh)
      assert_nothing_raised{ Math.atanh(0.5) }
      assert_kind_of(Float, Math.atanh(0.5))
   end
   
   def test_atanh_zero
      assert_nothing_raised{ Math.atanh(0) }
      assert_equal(0.0, Math.atanh(0))
   end
   
   def test_atanh_positive_float
      assert_nothing_raised{ Math.atanh(0.345) }
      assert_in_delta(0.359, Math.atanh(0.345), 0.01)
   end
   
   def test_atanh_negative_float
      assert_nothing_raised{ Math.atanh(-0.345) }
      assert_in_delta(-0.359, Math.atanh(-0.345), 0.01)
   end

   # This is arguably a platform bug. See ruby-core:10174.
   def test_atanh_edge_cases
      if WINDOWS || OSX
         assert_nothing_raised{ Math.atanh(1) }
         assert_nothing_raised{ Math.atanh(-1) }
         assert_equal(1, Math.atanh(1).infinite?)
         assert_equal(-1, Math.atanh(-1).infinite?)
      else
         assert_raises(Errno::ERANGE, Errno::EDOM){ Math.atanh(1) }
         assert_raises(Errno::ERANGE, Errno::EDOM){ Math.atanh(-1) }
      end
   end

   def test_atanh_expected_errors
      assert_raises(Errno::EDOM){ Math.atanh(2) }
      assert_raises(Errno::EDOM){ Math.atanh(-2) }
   end
end
