###############################################################################
# tc_string.rb
#
# Test case for the MatchData#string instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_String_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_string_basic
      assert_respond_to(@match, :string)
      assert_nothing_raised{ @match.string }
      assert_kind_of(String, @match.string)
   end

   def test_string
      assert_equal('THX1138', @match.string)
      assert_equal('1abc2xyz', @nested.match('1abc2xyz').string)
      assert_equal('99hello99', @plain.match('99hello99').string)
      assert(@match.string.frozen?)
   end

   def test_string_expected_errors
      assert_raise(ArgumentError){ @match.string(1) }
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
