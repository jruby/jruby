###############################################################################
# tc_reverse.rb
#
# Test case for the String#reverse instance method. The String#reverse! tests
# can be found in the tc_reverse_bang.rb file.
###############################################################################
require 'test/unit'

class TC_String_Reverse_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = 'stressed'
   end

   def test_reverse_basic
      assert_respond_to(@string, :reverse)
      assert_nothing_raised{ @string.reverse }
      assert_kind_of(String, @string.reverse)
   end

   def test_reverse
      assert_equal('desserts', @string.reverse)
      assert_equal('stressed', @string) # Check original not modified
      assert_equal('\\oof', 'foo\\'.reverse)
      assert_equal('321', '123'.reverse)
   end

   def test_reverse_tainted_string
      assert_equal(false, 'hello'.reverse.tainted?)
      assert_equal(true, 'hello'.taint.reverse.tainted?)
   end

   def test_reverse_edge_cases
      assert_equal('', ''.reverse)
      assert_equal(' ', ' '.reverse)
      assert_equal('a', 'a'.reverse)
      assert_equal("\001\000\000\000", "\000\000\000\001".reverse)
   end

   def test_reverse_expected_errors
      assert_raises(ArgumentError){ @string.reverse(1) }
   end

   def teardown
      @string = nil
   end
end
