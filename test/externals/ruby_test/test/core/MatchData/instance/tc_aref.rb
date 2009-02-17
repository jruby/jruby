###############################################################################
# tc_aref.rb
#
# Test case for the MatchData#[] instance method.
###############################################################################
require 'test/unit'

class TC_MatchData_Aref_InstanceMethod < Test::Unit::TestCase
   def setup
      @regex = /(.)(.)(\d+)(\d)/
      @match = @regex.match('THX1138.')
   end

   def test_aref_basic
      assert_not_nil(@match)
      assert_kind_of(MatchData, @match)
      assert_respond_to(@match, :[])
   end

   def test_aref_index
      assert_equal('HX1138', @match[0])
      assert_equal('H', @match[1])
      assert_equal('8', @match[-1])
      assert_equal('X', @match[-3])
   end

   def test_aref_start_length
      assert_equal(['H', 'X'], @match[1, 2])
      assert_equal(['X', '113', '8'], @match[2, 4])
      assert_equal(['X', '113', '8'], @match[2, 5])
      assert_equal(['X', '113', '8'], @match[2, 99])
   end

   def test_aref_range
      assert_equal(['H', 'X', '113'], @match[1..3])
      assert_equal(['HX1138', 'H', 'X', '113', '8'], @match[0..-1])
   end

   def test_aref_edge_cases
      assert_equal(['X', '113', '8'], @match[-3..7])
   end

   def test_aref_expected_errors
      assert_raise(ArgumentError){ @match[] }
      assert_raise(ArgumentError){ @match[1,2,3] }
   end

   def teardown
      @regex = nil
      @match = nil
   end
end
