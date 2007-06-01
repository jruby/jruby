###############################################################################
# tc_reverse.rb
#
# Test case for the String#reverse and String#reverse! instance methods.
###############################################################################
require "test/unit"

class TC_String_Reverse_Instance < Test::Unit::TestCase
   def setup
      @string = "stressed"
   end

   def test_reverse_basic
      assert_respond_to(@string, :reverse)
      assert_respond_to(@string, :reverse!)

      assert_nothing_raised{ @string.reverse }
      assert_nothing_raised{ @string.reverse! }
   end

   def test_reverse
      assert_equal("desserts", @string.reverse)
      assert_equal("stressed", @string)

      assert_equal('', ''.reverse)
      assert_equal('a', 'a'.reverse)
      assert_equal('\\oof', 'foo\\'.reverse)
      assert_equal('321', '123'.reverse)
   end

   def test_reverse_bang
      assert_equal("desserts", @string.reverse!)
      assert_equal("desserts", @string)

      assert_equal('', ''.reverse!)
      assert_equal('a', 'a'.reverse!)
      assert_equal('\\oof', 'foo\\'.reverse!)
      assert_equal('321', '123'.reverse!)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @string.reverse(1) }
   end

   def teardown
      @string = nil
   end
end
