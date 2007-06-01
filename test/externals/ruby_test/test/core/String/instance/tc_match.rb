###########################################################
# tc_match.rb
#
# Test suite for the String#match instance method.
###########################################################
require "test/unit"

class TC_String_Match_Instance < Test::Unit::TestCase
   def setup
      @simple  = "hello"
      @complex = "p1031'/> <b><c n='field'/><c n='fl"
   end

   def test_match_basic
      assert_respond_to(@simple, :match)
      assert_nothing_raised{ @simple.match("h") }
      assert_nothing_raised{ @simple.match(/\w+/) }
      assert_kind_of(MatchData, @simple.match("h"))
   end

   def test_match_string_complex
      assert_equal("<b><c n='field'/><c n='fl", @complex.match("<b><c n='field'/><c n='fl")[0])
   end

   def test_match_string_with_string
      assert_equal("h", @simple.match("h")[0])
      assert_equal("ell", @simple.match("ell")[0])
      assert_nil(@simple.match("z"))
   end

   def test_match_string_with_regex
      assert_equal("h", @simple.match(/h/)[0])
      assert_equal(["ll"], @simple.match(/l+/)[0..1])
      assert_equal("h", @simple.match(/h/)[0])
   end

   def test_match_expected_errors
      assert_raises(TypeError){ @simple.match(0) }
      assert_raises(ArgumentError){ @simple.match(1,2,3) }
   end

   def teardown
      @simple  = nil
      @complex = nil
   end
end
