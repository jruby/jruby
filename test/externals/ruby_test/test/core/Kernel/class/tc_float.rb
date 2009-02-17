###############################################################################
# tc_float.rb
#
# Test case for the Kernel.Float module method.
###############################################################################
require 'test/unit'

class TC_Kernel_Float_ModuleMethod < Test::Unit::TestCase
   def setup
      @int          = 99
      @float        = 123.45
      @string_int   = "123"
      @string_float = "123.45"
   end

   def test_float_basic
      assert_respond_to(Kernel, :Float)
      assert_nothing_raised{ Float(@int) }
      assert_kind_of(Float, Float(@int))
   end

   def test_float
      assert_equal(99.0, Float(@int))
      assert_equal(123.45, Float(@float))
      assert_equal(123.0, Float(@string_int))
      assert_equal(123.45, Float(@string_float))
   end

   def test_float_handles_underscores
      assert_equal(123.0, Float('12_3'))
      assert_equal(123.0, Float('1_2_3'))
      assert_equal(1.0, Float('0_1'))
      assert_equal(10.0, Float('0_1_0'))
      assert_equal(0.0, Float('0_0_0'))
   end

   def test_float_edge_cases
      assert_equal(0.0, Float(0))
      assert_equal(0.0, Float(0.0))
      assert_equal(234234.111111111222, Float(234234.111111111222))
      assert_kind_of(Float, Float(Time.new))
   end

   def test_float_expected_errors
      assert_raise(TypeError){ Float([]) }
      assert_raise(TypeError){ Float(nil) }
      assert_raise(TypeError){ Float(true) }
      assert_raise(TypeError){ Float(false) }
      assert_raise(ArgumentError){ Float("0x1a") }
      assert_raise(ArgumentError){ Float("1\0002") }
      assert_raise(ArgumentError){ Float("123_") }
   end

   def teardown
      @int = nil
      @float = nil
      @string_int = nil
      @string_float = nil
   end
end
