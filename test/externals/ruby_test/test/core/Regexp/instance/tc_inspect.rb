###############################################################################
# tc_inspect.rb
#
# Test case for the Regexp#inspect instance method.
###############################################################################
require 'test/unit'

class TC_Regexp_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @regexp = /abc\d+\s*/
      @nested = /(abc(\d+)\s*)/
      @option = /abc\d+\s*/ix
   end

   def test_inspect_basic
      assert_respond_to(@regexp, :inspect)
      assert_nothing_raised{ @regexp.inspect }
      assert_kind_of(String, @regexp.inspect)
   end

   def test_inspect
      assert_equal('/abc\d+\s*/', @regexp.inspect)
      assert_equal('/(abc(\d+)\s*)/', @nested.inspect)
      assert_equal('/abc\d+\s*/ix', @option.inspect)
      assert_equal('/[a-z][A-Z]/', /[a-z][A-Z]/.inspect)
   end

   def test_inspect_edge_cases
      assert_equal('//', //.inspect)
      assert_equal('/ /', / /.inspect)
      assert_equal('/[[:alpha:]]/', /[[:alpha:]]/.inspect)
   end

   def test_inspect_expected_errors
      assert_raise(ArgumentError){ @regexp.inspect(/foo/) }
   end

   def teardown
      @regexp = nil
      @nested = nil
      @option = nil
   end
end
