###############################################################################
# tc_source.rb
#
# Test case for the Regexp#source instance method.
###############################################################################
require 'test/unit'

class TC_Regexp_Source_InstanceMethod < Test::Unit::TestCase
   def setup
      @regexp = /abc\d+\s*/
      @nested = /(abc(\d+)\s*)/
      @option = /abc\d+\s*/ix
   end

   def test_source_basic
      assert_respond_to(@regexp, :source)
      assert_nothing_raised{ @regexp.source }
      assert_kind_of(String, @regexp.source)
   end

   def test_source
      assert_equal('abc\d+\s*', @regexp.source)
      assert_equal('(abc(\d+)\s*)', @nested.source)
      assert_equal('abc\d+\s*', @option.source)
      assert_equal('[a-z][A-Z]', /[a-z][A-Z]/.source)
   end

   def test_source_edge_cases
      assert_equal('', //.source)
      assert_equal(' ', / /.source)
      assert_equal('[[:alpha:]]', /[[:alpha:]]/.source)
   end

   def test_source_expected_errors
      assert_raise(ArgumentError){ @regexp.source(/foo/) }
   end

   def teardown
      @regexp = nil
      @nested = nil
      @option = nil
   end
end
