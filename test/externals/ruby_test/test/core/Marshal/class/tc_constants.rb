###############################################################################
# tc_constants.rb
#
# Test case to verify the definition of Marshal constants.
###############################################################################
require 'test/unit'

class TC_Marshal_Constants < Test::Unit::TestCase
   def test_major_version
      assert_not_nil(Marshal::MAJOR_VERSION)
      assert_equal(4, Marshal::MAJOR_VERSION)
   end

   def test_minor_version
      assert_not_nil(Marshal::MINOR_VERSION)
      assert_equal(8, Marshal::MINOR_VERSION)
   end
end
