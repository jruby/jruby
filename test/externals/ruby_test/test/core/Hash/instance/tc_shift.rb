############################################################
# tc_shift.rb
#
# Test suite for the Hash#shift instance method.
############################################################
require "test/unit"

class TC_Hash_Shift_Instance < Test::Unit::TestCase
   def setup
      @hash1 = {"a",1}
      @hash2 = Hash.new(99)      
      @hash3 = Hash.new{ |h,k| h[k] = "test" }
   end

   def test_shift_basic
      assert_respond_to(@hash1, :shift)
      assert_nothing_raised{ @hash1.shift }
   end

   def test_shift
      assert_equal(["a",1], @hash1.shift)
      assert_equal(nil, @hash1.shift)
   end

   def test_shift_default_value
      assert_equal(99, @hash2.shift)
      assert_equal(99, @hash2.shift)
   end

   def test_shift_default_proc
      assert_equal("test", @hash3.shift)
      assert_equal([nil,"test"], @hash3.shift)
   end

   def test_shift_expected_errors
      assert_raises(ArgumentError){ @hash1.shift(2) }
   end

   def teardown
      @hash1 = nil
   end
end
