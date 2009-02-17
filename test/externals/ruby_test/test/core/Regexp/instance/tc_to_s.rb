###############################################################################
# tc_to_s.rb
#
# Test case for the Regexp#to_s instance method.
###############################################################################
require 'test/unit'

class TC_Regexp_ToS_InstanceMethod < Test::Unit::TestCase
   def setup
      @regexp = /abc\d+\s*/
      @nested = /(abc(\d+)\s*)/m
      @option = /abc\d+\s*/ix
   end

   def test_to_s_basic
      assert_respond_to(@regexp, :to_s)
      assert_nothing_raised{ @regexp.to_s }
      assert_kind_of(String, @regexp.to_s)
   end

   def test_to_s
      assert_equal('(?-mix:abc\d+\s*)', @regexp.to_s)
      assert_equal('(?m-ix:(abc(\d+)\s*))', @nested.to_s)
      assert_equal('(?ix-m:abc\d+\s*)', @option.to_s)
      assert_equal('(?-mix:[a-z][A-Z])', /[a-z][A-Z]/.to_s)
   end

   def test_to_s_edge_cases
      assert_equal('(?-mix:)', //.to_s)
      assert_equal('(?-mix: )', / /.to_s)
      assert_equal('(?-mix:[[:alpha:]])', /[[:alpha:]]/.to_s)
   end

   def test_to_s_expected_errors
      assert_raise(ArgumentError){ @regexp.to_s(/foo/) }
   end

   def teardown
      @regexp = nil
      @nested = nil
      @option = nil
   end
end
