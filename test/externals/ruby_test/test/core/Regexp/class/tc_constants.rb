###############################################################################
# tc_constants.rb
#
# Test case to verify the constants associated with Regexp.
###############################################################################
require 'test/unit'

class TC_Regexp_Constants < Test::Unit::TestCase
   def test_extended
      assert_not_nil(Regexp::EXTENDED)
      assert_equal(2, Regexp::EXTENDED)
   end

   def test_ignorecase
      assert_not_nil(Regexp::IGNORECASE)
      assert_equal(1, Regexp::IGNORECASE)
   end

   def test_multiline
      assert_not_nil(Regexp::MULTILINE)
      assert_equal(4, Regexp::MULTILINE)
   end
end
