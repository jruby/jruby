###############################################################################
# tc_length.rb
#
# Test case for the MatchData#length instance method, and the MatchData#size
# alias.
###############################################################################
require 'test/unit'

class TC_MatchData_Length_InstanceMethod < Test::Unit::TestCase
   def setup
      @regex  = /(.)(.)(.)(\d+)/
      @string = 'THX1138'
      @match  = @regex.match(@string)
   end

   def test_length_basic
      assert_respond_to(@match, :length)
      assert_nothing_raised{ @match.length }
      assert_kind_of(Fixnum, @match.length)
   end

   def test_size_alias
      assert_respond_to(@match, :size)
      assert_nothing_raised{ @match.size }
      assert_kind_of(Fixnum, @match.size)
   end

   # The length includes the full match, i.e. index 0, as well as the
   # invidual matches.
   #
   def test_length
      assert_equal(5, @match.length)
      assert_equal(2, /\w\w(\d*)/.match('hello').length)     # 2nd elem. empty
      assert_equal(3, /(\w+)(\d*)/.match('hello1234').length)
   end

   def test_length_expected_errors
      assert_raise(ArgumentError){ @match.length(1) }
   end

   def teardown
      @regex  = nil
      @string = nil
      @match  = nil
   end
end
