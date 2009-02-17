###########################################################################
# tc_squeeze.rb
#
# Test case for the String#squeeze and String#squeeze! instance methods.
###########################################################################
require 'test/unit'

class TC_String_Squeeze_InstanceMethod < Test::Unit::TestCase
   def setup
      @string = "yellow   moon"
   end

   def test_squeeze_basic
      assert_respond_to(@string, :squeeze)
      assert_respond_to(@string, :squeeze!)
      assert_nothing_raised{ @string.squeeze }
      assert_nothing_raised{ @string.squeeze! }
   end

   def test_squeeze
      assert_equal("yelow mon", @string.squeeze)
      assert_equal("yellow   moon", @string)
      assert_equal("'", "''''''''".squeeze)
      assert_equal('', ''.squeeze)
      assert_equal('\\', '\\\\\\\\\\'.squeeze)
   end

   def test_squeeze_with_args
      assert_equal("yelow   moon", @string.squeeze("l"))
      assert_equal("yelow moon", @string.squeeze("l "))
      assert_equal("yelow   mon", @string.squeeze("l-p"))
      assert_equal('\\', '\\\\\\\\\\'.squeeze('\\'))
   end

   def test_squeeze_bang
      assert_equal("yelow mon", @string.squeeze!)
      assert_equal(nil, @string.squeeze!)
      assert_equal("yelow mon", @string)
   end

   def test_squeeze_bang_with_args
      assert_equal("yelow   moon", @string.squeeze!("l"))
      assert_equal("yelow   moon", @string)

      assert_equal("yelow moon", @string.squeeze!("l "))
      assert_equal("yelow moon", @string)

      assert_equal("yelow mon", @string.squeeze!("l-p"))
      assert_equal("yelow mon", @string)

      assert_equal(nil, @string.squeeze!("a-z?!\\"))
   end

   def test_squeeze_expected_errors
      assert_raise(TypeError){ @string.squeeze(1) }
      assert_raise(TypeError){ @string.freeze.squeeze!('l') }
   end

   def teardown
      @string = nil
   end
end
