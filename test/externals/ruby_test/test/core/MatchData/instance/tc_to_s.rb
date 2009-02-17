###############################################################################
# tc_to_s.rb
#
# Test case for the MatchData#to_s instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_ToS_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_to_s_basic
      assert_respond_to(@match, :to_s)
      assert_nothing_raised{ @match.to_s }
      assert_kind_of(String, @match.to_s)
   end

   def test_to_s
      assert_equal('HX1138', @match.to_s)
      assert_equal('1abc2', @nested.match('1abc2').to_s)
      assert_equal('hello', @plain.match('hello').to_s)
   end

   def test_to_s_expected_errors
      assert_raise(ArgumentError){ @match.to_s(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
