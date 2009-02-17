###############################################################################
# tc_match.rb
#
# Test suite for the String#=~ instance method.
#
# Note that while String#match and String#=~ are very similar, there are
# just enough differences between them to warrant a separate test case.
#
# One curious note. It seems that the RHS object must have a "=~" method
# defined as well. Normally this isn't a problem because most classes
# inherit Object#=~. However, if you undef that method, you'll get a
# NoMethodError.
###############################################################################
require "test/unit"

class TC_String_MatchTilde_Instance < Test::Unit::TestCase
   def setup
      @simple  = "hello"
      @complex = "p1031'/> <b><c n='field'/><c n='fl"
   end

   def test_match_tilde_basic
      assert_respond_to(@simple, :=~)
      assert_nothing_raised{ @simple =~ /\w+/ }
      assert_kind_of(Fixnum, @complex =~ /\d/) # Note, not a MatchData object
   end

   def test_match_tilde
      assert_equal(0, @simple =~ /h/)
      assert_equal(9, @complex =~ /<b><c n='field'\/><c n='fl/)
      assert_nil(@simple =~ /\d/)
      assert_nil(@complex =~ /z/)
   end

   def test_match_tilde_edge_cases
      assert_equal(0, @simple =~ //)
   end

   # Resorts to Object#=~ default
   def test_match_tilde_against_non_regex
      assert_equal(false, @simple =~ 0)
      assert_equal(false, @complex =~ 0)
      assert_equal(false, @simple =~ {1,2,3,4})
   end

   def test_match_tilde_expected_errors
      assert_raises(TypeError){ @simple =~ "hello" }
   end

   def teardown
      @simple  = nil
      @complex = nil
   end
end
