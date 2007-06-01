######################################################################
# tc_upto.rb
#
# Test case for the String#upto instance method.
######################################################################
require 'test/unit'

class TC_String_Upto_Instance < Test::Unit::TestCase
   def setup
      @string1 = "a1"
      @string2 = "hello"
      @string3 = "\220"
      @array   = []
   end

   def test_upto_basic
      assert_respond_to(@string1, :upto)
      assert_nothing_raised{ @string1.upto("a9"){} }
   end

   def test_upto
      @string1.upto("a3"){ |str| @array << str }
      assert_equal(['a1','a2','a3'], @array)
      @array = []

      @string2.upto("hellq"){ |str| @array << str }
      assert_equal(['hello', 'hellp', 'hellq'], @array)
      @array = []

      @string3.upto("\222"){ |str| @array << str }
      assert_equal(["\220", "\221", "\222"], @array)
      @array = []
   end

   def test_upto_edge_cases
      assert_nothing_raised{ @string1.upto(""){} }
      assert_nothing_raised{ " ".upto("xxx"){} }
      assert_nothing_raised{ "x".upto("a"){} }
   end

   def test_upto_expected_errors
      assert_raises(TypeError){ @string1.upto(nil) }
      assert_raises(LocalJumpError){ @string1.upto("a9") } # No block provided
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @array   = nil
   end
end
