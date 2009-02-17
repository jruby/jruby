###############################################################################
# tc_captures.rb
#
# Test case for the MatchData#captures instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_Captures_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_captures_basic
      assert_respond_to(@match, :captures)
      assert_nothing_raised{ @match.captures }
      assert_kind_of(Array, @match.captures)
   end

   def test_captures
      assert_equal(['H', 'X', '113', '8'], @match.captures)
      assert_equal(['1abc2', 'abc', 'b'], @nested.match('1abc2').captures)
      assert_equal([], @plain.match('hello').captures)
   end

   def test_captures_expected_errors
      assert_raise(ArgumentError){ @match.captures(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
