######################################################################
# tc_oct.rb
#
# Test case for the String#oct instance method.
######################################################################
require "test/unit"

class TC_String_Oct_InstanceMethod < Test::Unit::TestCase
   def setup
      @pos_num = "123"
      @neg_num = "-377"
      @bad_str = "hello"
      @oct_str = "0700"
   end

   def test_oct_basic
      assert_respond_to(@pos_num, :oct)
      assert_nothing_raised{ @pos_num.oct }
      assert_kind_of(Fixnum, @pos_num.oct)
   end

   def test_oct
      assert_equal(83, @pos_num.oct)
      assert_equal(-255, @neg_num.oct)
      assert_equal(0, @bad_str.oct)
      assert_equal(448, @oct_str.oct)
   end

   def test_oct_d_format
      assert_equal(11, '0d11'.oct)
      assert_equal(500, '0d500'.oct)
   end

   def test_oct_o_format
      assert_equal(9, '0o11'.oct)
      assert_equal(320, '0o500'.oct)
   end

   def test_oct_expected_errors
      assert_raises(ArgumentError){ @pos_num.oct(1) }
   end

   def teardown
      @pos_num = nil
      @neg_num = nil
      @bad_str = nil
      @oct_str = nil
   end
end
