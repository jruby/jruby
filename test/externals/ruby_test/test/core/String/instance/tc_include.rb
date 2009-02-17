######################################################################
# tc_include.rb
#
# Test case for the String#include? instance method.
######################################################################
require "test/unit"

class TC_String_Include_Instance < Test::Unit::TestCase
   def setup
      @string = "a\/\t\n\r\"789"
   end

   def test_include_basic
      assert_respond_to(@string, :include?)
      assert_nothing_raised{ @string.include?("a") }
   end

   def test_include_string_expected_true
      assert_equal(true, @string.include?("a"))
      assert_equal(true, @string.include?("/"))
      assert_equal(true, @string.include?("\t"))
      assert_equal(true, @string.include?("\n"))
      assert_equal(true, @string.include?("\r"))
      assert_equal(true, @string.include?('"'))
      assert_equal(true, @string.include?(97))
   end

   def test_include_string_expected_false
      assert_equal(false, @string.include?("b"))
      assert_equal(false, @string.include?("\\"))
      assert_equal(false, @string.include?(789))
   end

   def test_include_int
      assert_equal(true, @string.include?(?a))
      assert_equal(false, @string.include?(?b))
   end

   def test_include_expected_errors
      assert_raises(ArgumentError){ @string.include? }
      assert_raises(ArgumentError){ @string.include?(1,2) }
   end

   def teardown
      @string = nil
   end
end
