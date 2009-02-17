###############################################################################
# tc_end.rb
#
# Test case for the MatchData#end instance method.
#
# TODO: Add more advanced tests. These are pretty basic.
###############################################################################
require 'test/unit'

class TC_MatchData_End_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @match  = @regex.match(@string)
   end

   def test_end_basic
      assert_respond_to(@match, :end)
      assert_nothing_raised{ @match.end(0) }
      assert_kind_of(Fixnum, @match.end(0))
   end

   def test_end
      assert_equal(7, @match.end(0))
      assert_equal(3, @match.end(2))
   end

   def test_end_expected_errors
      assert_raise(ArgumentError){ @match.end(1,2) }
      assert_raise(TypeError){ @match.end(1..2) }
      assert_raise(TypeError){ @match.end('test') }
   end

   def teardown
      @string = nil
      @regex  = nil
      @match  = nil
   end
end
