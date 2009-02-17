###############################################################################
# tc_to_a.rb
#
# Test case for the MatchData#to_a instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_ToA_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_to_a_basic
      assert_respond_to(@match, :to_a)
      assert_nothing_raised{ @match.to_a }
      assert_kind_of(Array, @match.to_a)
   end

   def test_to_a
      assert_equal(['HX1138', 'H', 'X', '113', '8'], @match.to_a)
      assert_equal(['1abc2', '1abc2', 'abc', 'b'], @nested.match('1abc2').to_a)
      assert_equal(['hello'], @plain.match('hello').to_a)
   end

   def test_to_a_expected_errors
      assert_raise(ArgumentError){ @match.to_a(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
