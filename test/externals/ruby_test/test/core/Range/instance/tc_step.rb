######################################################################
# tc_step.rb
#
# Test case for the Range#step instance method.
######################################################################
require 'test/unit'

class TC_Range_Step_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(0, 9)
      @range2 = Range.new('a', 'd')
      @range3 = Range.new(2**63, 2**63 + 1)
      @range4 = Range.new(File.stat(Dir.pwd), File.stat(Dir.pwd)) # '<=>' but no 'succ'
   end

   def test_step_basic
      assert_respond_to(@range1, :step)
      assert_nothing_raised{ @range1.step{} }
      assert_nothing_raised{ @range1.step(1){} }
   end

   def test_step_alpha_default_value
      array = []
      assert_nothing_raised{ @range2.step{ |e| array << e }}
      assert_equal(['a', 'b', 'c', 'd'], array)
   end

   def test_step_alpha_non_default_value
      array = []
      assert_nothing_raised{ @range2.step(2){ |e| array << e }}
      assert_equal(['a', 'c'], array)
   end

   def test_step_fixnum_default_value
      array = []
      assert_nothing_raised{ @range1.step{ |e| array << e }}
      assert_equal([0,1,2,3,4,5,6,7,8,9], array)
   end

   def test_step_fixnum_non_default_value
      array = []
      assert_nothing_raised{ @range1.step(3){ |e| array << e }}
      assert_equal([0,3,6,9], array)
   end

   def test_step_bignum_default_value
      array = []
      assert_nothing_raised{ @range3.step{ |e| array << e }}
      assert_equal([9223372036854775808, 9223372036854775809], array)
   end

   def test_step_edge_cases
      array = []
      assert_nothing_raised{ @range1.step(100){ |e| array << e }}
      assert_equal([0], array)

      array = []
      assert_nothing_raised{ @range2.step(100){ |e| array << e }}
      assert_equal(['a'], array)
   end

   # No longer a valid test in 1.8.7
=begin
   def test_step_expected_failures
      assert_raises(TypeError){ @range1.step(nil) }
      assert_raises(TypeError){ @range4.step{} }
      assert_raises(ArgumentError){ @range1.step(0) }
      assert_raises(ArgumentError){ @range1.step(-1) }
      assert_raises(LocalJumpError){ @range1.step }
   end
=end

   def teardown
      @range1 = nil
      @range2 = nil
      @range3 = nil
      @range4 = nil
   end
end
