###############################################################################
# tc_each_byte.rb
#
# Test case for the String#each_byte instance method.
###############################################################################
require 'test/unit'

class TC_String_EachByte_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = "hello\n"
      @array  = []
   end

   def test_each_byte_string
      assert_respond_to(@string, :each_byte)
      assert_nothing_raised{ @string.each_byte{ } }
      assert_kind_of(String, @string.each_byte{ })
   end

   def test_each_byte
      assert_nothing_raised{ @string.each_byte{ |b| @array << b } }
      assert_equal([104, 101, 108, 108, 111, 10], @array)
   end

   def test_each_byte_edge_cases
      assert_nothing_raised{ ''.each_byte{ |b| @array << b } }
      assert_equal([], @array)
   end

   def test_each_byte_expected_errors
      assert_raise(ArgumentError){ @string.each_byte('x'){ } }
   end

   def teardown
      @string = nil
      @array  = nil
   end
end
