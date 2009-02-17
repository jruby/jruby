######################################################################
# tc_between.rb
#
# Test case for the Comparable#between? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Comparable_Between_Instance < Test::Unit::TestCase
   include Test::Helper

   def test_between_basic
      assert_respond_to(1, :between?)
      assert_nothing_raised{ 1.between?(1,2) }
   end

   def test_between_numbers
      assert_equal(true, 1.between?(0,2))
      assert_equal(true, 1.between?(1,2))
      assert_equal(true, 1.between?(1,1))
      assert_equal(true, 1.between?(0,1))
      assert_equal(false, 1.between?(1,0))
   end

   def test_between_letters
      assert_equal(true, 'm'.between?('a', 'z'))
      assert_equal(true, 'm'.between?('m', 'z'))
      assert_equal(true, 'm'.between?('m', 'm'))
      assert_equal(false, 'm'.between?('a','l'))
      assert_equal(false, 'm'.between?('z','a'))
      assert_equal(false, 'm'.between?('z','m'))
   end

   def test_between_edge_cases
      assert_equal(true, 1.0.between?(0.99999, 1.00001))
      assert_equal(true, 'm'.between?("\000", 'z'))
      assert_equal(false, 'm'.between?("\000", "\000"))
      assert_equal(true, 'm'.between?('', 'z'))
      assert_equal(false, 'm'.between?('m', ''))
      assert_equal(false, 'm'.between?('', ''))
   end

   def test_between_expected_errors
      assert_raises(ArgumentError){ 1.between? }
      assert_raises(ArgumentError){ 1.between?(1) }
      assert_raises(ArgumentError){ 1.between?(1, nil) }
      assert_raises(ArgumentError){ 1.between?(1, 2, 3) }
      assert_raises(ArgumentError){ 'm'.between?('a', 1) }
   end
end
