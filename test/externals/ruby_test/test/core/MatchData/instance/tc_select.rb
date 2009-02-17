###############################################################################
# tc_select.rb
#
# Test case for the MatchData#select instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_Select_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_select_basic
      assert_respond_to(@match, :select)
      assert_nothing_raised{ @match.select{} }
      assert_kind_of(Array, @match.select{})
   end

   def test_select
      assert_equal(['HX1138', '113', '8'], @match.select{ |x| x =~ /\d/ })
      assert_equal(['1abc2', '1abc2'], @nested.match('1abc2').select{ |x| x =~ /\d/ })
      assert_equal([], @plain.match('hello').select{ |x| x =~ /\d/ })
   end

   def test_select_expected_errors
      assert_raise(ArgumentError){ @match.select(1) }
      assert_raise(LocalJumpError){ @match.select }
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
