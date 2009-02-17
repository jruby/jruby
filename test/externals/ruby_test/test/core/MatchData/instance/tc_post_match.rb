###############################################################################
# tc_post_match.rb
#
# Test case for the MatchData#post_match instance method.
#
# TODO: Add more advanced tests.
###############################################################################
require 'test/unit'

class TC_MatchData_PostMatch_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138: The Movie'
      @regex  = /(.)(.)(\d+)(\d)/
      @match  = @regex.match(@string)
   end

   def test_post_match_basic
      assert_respond_to(@match, :post_match)
      assert_nothing_raised{ @match.post_match }
      assert_kind_of(String, @match.post_match)
   end

   def test_post_match
      assert_equal(': The Movie', @match.post_match)
   end

   # Verify that $' and post_match are identical
   def test_post_match_equals_dollar_tick
      assert_nothing_raised{ @match = @regex.match(@string) }
      assert_equal($', @match.post_match)
   end

   def test_post_match_expected_errors
      assert_raise(ArgumentError){ @match.post_match(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @match  = nil
   end
end
