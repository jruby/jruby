###############################################################################
# tc_concatenate.rb
#
# Test case for the String#+ instance method.
###############################################################################
require 'test/unit'

class TC_String_Concatenate_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = "hello "
   end

   def test_concatenate_basic
      assert_respond_to(@string, :+)
      assert_nothing_raised{ @string + 'world' }
      assert_kind_of(String, @string + 'world')
   end

   def test_concatenate
      assert_equal('hello world', @string + 'world')
      assert_equal('hello world 2', @string + 'world' + ' 2')
   end

   def test_concatenate_original_string_unmodified
      assert_nothing_raised{ @string + 'world' }
      assert_equal('hello ', @string)
   end

   def test_concatenate_edge_cases
      assert_equal('hello ', @string + '')
      assert_equal('hello ', '' + 'hello ')
      assert_equal('', '' + '')
   end

   def test_concatenate_expected_errors
      assert_raise(TypeError){ @string + 1 }
      assert_raise(ArgumentError){ @string.send(:+, 'test1', 'test2') }
   end

   def teardown
      @string = nil
   end
end
