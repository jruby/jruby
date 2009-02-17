###############################################################################
# tc_values_at.rb
#
# Test case for the MatchData#values_at instance method.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_MatchData_ValuesAt_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @string = 'THX1138'
      @regex  = /(.)(.)(\d+)(\d)/
      @nested = /(\d(\w(\w)\w)\d)/
      @plain  = /\w+/
      @match  = @regex.match(@string)
   end

   def test_values_at_basic
      assert_respond_to(@match, :values_at)
      assert_nothing_raised{ @match.values_at(0) }
      assert_nothing_raised{ @match.values_at(0, 1, 2) }
      assert_kind_of(Array, @match.values_at(0))
   end

   def test_values_at
      assert_equal(['HX1138'], @match.values_at(0))
      assert_equal(['HX1138', 'X', '8'], @match.values_at(0, 2, 4))
      assert_equal(['1abc2', '1abc2'], @nested.match('1abc2').values_at(0, 1))
      assert_equal(['hello'], @plain.match('hello').values_at(0))
   end

   def test_values_at_with_range
      assert_equal(['HX1138'], @match.values_at(0..0))
      assert_equal(['HX1138', 'H', 'X', '8'], @match.values_at(0..2, 4))
      assert_equal(['1abc2', '1abc2'], @nested.match('1abc2').values_at(0..1))
      assert_equal(['hello', nil], @plain.match('hello').values_at(0..1))
   end

   # I consider JRuby's behavior proper, MRI's behavior broken
   def test_values_at_edge_cases
      assert_equal([], @match.values_at) unless JRUBY
      assert_equal([nil, nil], @match.values_at(99, 100))
   end

   def test_values_at_expected_errors
      assert_raise(TypeError){ @match.values_at(/\d/) }
      assert_raise(ArgumentError){ @match.values_at } if JRUBY
   end

   def teardown
      @string = nil
      @regex  = nil
      @nested = nil
      @plain  = nil
      @match  = nil
   end
end
