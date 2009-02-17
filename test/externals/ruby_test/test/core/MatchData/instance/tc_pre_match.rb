###############################################################################
# tc_pre_match.rb
#
# Test case for the MatchData#pre_match instance method.
#
# TODO: Add more advanced tests.
###############################################################################
require 'test/unit'

class TC_MatchData_PreMatch_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138: The Movie'
      @regex  = /(.)(.)(\d+)(\d)/
      @match  = @regex.match(@string)
   end

   def test_pre_match_basic
      assert_respond_to(@match, :pre_match)
      assert_nothing_raised{ @match.pre_match }
      assert_kind_of(String, @match.pre_match)
   end

   def test_pre_match
      assert_equal('T', @match.pre_match)
   end

   # Verify that $` and pre_match are identical
   def test_pre_match_equals_dollar_backtick
      assert_nothing_raised{ @match = @regex.match(@string) }
      assert_equal($`, @match.pre_match)
   end

   def test_pre_match_expected_errors
      assert_raise(ArgumentError){ @match.pre_match(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @match  = nil
   end
end
