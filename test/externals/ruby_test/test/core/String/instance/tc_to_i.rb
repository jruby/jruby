###############################################################################
# tc_to_i.rb
#
# Test case for the String#to_i instance method.
###############################################################################
require "test/unit"

class TC_String_To_I_Instance < Test::Unit::TestCase
   def setup
      @string1 = "12345"
      @string2 = "0xC"
      @string3 = "0c"
      @string4 = "0b1"
      @string5 = "1100101"
   end

   def test_to_i_basic
      assert_respond_to(@string1, :to_i)
      assert_nothing_raised{ @string1.to_i }
      assert_kind_of(Integer, @string1.to_i)
   end

   def test_to_i
      assert_equal(12345, @string1.to_i)
      assert_equal(0, @string2.to_i)
      assert_equal(0, @string3.to_i)
      assert_equal(0, @string4.to_i)
      assert_equal(1100101, @string5.to_i)
   end

   def test_to_i_with_base
      assert_equal(12345, @string1.to_i(10))
      assert_equal(12, @string2.to_i(16))
      assert_equal(12, @string3.to_i(16))
      assert_equal(1, @string4.to_i(2))
      assert_equal(101, @string5.to_i(2))
      assert_equal(294977, @string5.to_i(8))
      assert_equal(1100101, @string5.to_i(10))
      assert_equal(17826049, @string5.to_i(16))
      assert_equal(199066177, @string5.to_i(24))
   end

   def test_to_i_edge_cases
      assert_equal(0, "0.0".to_i)
      assert_equal(0, "false".to_i)
      assert_equal(0, "true".to_i)
      assert_equal(0, "nil".to_i)
   end

   def test_to_i_expected_errors
      assert_raises(ArgumentError){ @string1.to_i(99) }
      assert_raises(ArgumentError){ @string1.to_i(-1) }
      assert_raises(ArgumentError){ @string1.to_i(10, 12) }
      assert_raises(TypeError){ @string1.to_i(nil) }
      assert_raises(TypeError){ @string1.to_i(false) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
      @string5 = nil
   end
end
