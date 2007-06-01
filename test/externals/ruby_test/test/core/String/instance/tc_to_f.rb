###############################################################################
# tc_to_f.rb
#
# Test case for the String#to_f instance method.
###############################################################################
require "test/unit"

class TC_String_To_F_Instance < Test::Unit::TestCase
   def setup
      @string1 = "123.45e1"
      @string2 = "45.67 degrees"
      @string3 = "thx1138"
   end

   def test_to_f_basic
      assert_respond_to(@string1, :to_f)
      assert_nothing_raised{ @string1.to_f }
      assert_kind_of(Float, @string1.to_f)
   end

   def test_to_f
      assert_equal(1234.5, @string1.to_f)
      assert_equal(45.67, @string2.to_f)
      assert_equal(0.0, @string3.to_f)
   end

   def test_to_f_edge_cases
      # assert_equal(1.0e+55, "1e55".to_f) # This doesn't work in Test::Unit
      assert_equal(0.0, "0.0".to_f)
      assert_equal(0.0, "false".to_f)
      assert_equal(0.0, "true".to_f)
      assert_equal(0.0, "nil".to_f)
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
   end
end
