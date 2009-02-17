######################################################################
# tc_hash.rb
#
# Test case for the Range#hash instance method.
######################################################################
require 'test/unit'

class TC_Range_Hash_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(1, 100)
      @range2 = Range.new('a', 'z', true)
   end

   def test_hash_basic
      assert_respond_to(@range1, :hash)
      assert_nothing_raised{ @range1.hash }
      assert_kind_of(Fixnum, @range1.hash)
   end

   def test_hash_identical
      assert_equal(true, @range1.hash == Range.new(1, 100).hash)
      assert_equal(false, @range1.hash == Range.new(1, 99).hash)
      assert_equal(false, @range1.hash == Range.new(1.0, 100).hash)
   end

   def test_hash_identical_match_exclusive
      assert_equal(true, @range2.hash == Range.new('a', 'z', true).hash)
      assert_equal(false, @range2.hash == Range.new('a', 'z').hash)
   end

   def test_hash_expected_errors
      assert_raise(ArgumentError){ @range1.hash(1) }
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
