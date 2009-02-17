###############################################################################
# tc_offset.rb
#
# Test case for the MatchData#offset instance method.
#
# TODO: Add more advanced tests. These are pretty basic.
###############################################################################
require 'test/unit'

class TC_MatchData_Offset_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @match  = @regex.match(@string)
   end

   def test_offset_basic
      assert_respond_to(@match, :offset)
      assert_nothing_raised{ @match.offset(0) }
      assert_kind_of(Array, @match.offset(0))
   end

   def test_offset
      assert_equal([1,7], @match.offset(0))
      assert_equal([6,7], @match.offset(4))
   end

   def test_offset_expected_errors
      assert_raise(ArgumentError){ @match.offset(1,2) }
      assert_raise(TypeError){ @match.offset(1..2) }
      assert_raise(TypeError){ @match.offset('test') }
   end

   def teardown
      @string = nil
      @regex  = nil
      @match  = nil
   end
end
