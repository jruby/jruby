###############################################################################
# tc_begin.rb
#
# Test case for the MatchData#begin instance method.
#
# TODO: Add more advanced tests. These are pretty basic.
###############################################################################
require 'test/unit'

class TC_MatchData_Begin_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @match  = @regex.match(@string)
   end

   def test_begin_basic
      assert_respond_to(@match, :begin)
      assert_nothing_raised{ @match.begin(0) }
      assert_kind_of(Fixnum, @match.begin(0))
   end

   def test_begin
      assert_equal(1, @match.begin(0))
      assert_equal(2, @match.begin(2))
   end

   def test_begin_expected_errors
      assert_raise(ArgumentError){ @match.begin(1,2) }
      assert_raise(TypeError){ @match.begin(1..2) }
      assert_raise(TypeError){ @match.begin('test') }
   end

   def teardown
      @string = nil
      @regex  = nil
      @match  = nil
   end
end
