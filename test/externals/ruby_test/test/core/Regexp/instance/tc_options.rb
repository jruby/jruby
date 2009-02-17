###############################################################################
# tc_options.rb
#
# Test case for the Regexp#options instance method.
###############################################################################
require 'test/unit'

class TC_Regexp_Options_InstanceMethod < Test::Unit::TestCase
   def setup
      @regex_plain  = /abc/
      @regex_extend = /abc/x
      @regex_imulti = /abc/im
      @regex_utf8   = /abc/uix
      @regex_sjis   = /abc/sim
   end

   def test_options_basic
      assert_respond_to(@regex_plain, :options)
      assert_nothing_raised{ @regex_plain.options }
   end

   # The values expected here are the bitwise-or'd values of Regexp
   # constants or other internal values.
   #
   def test_options
      assert_equal(0, @regex_plain.options)
      assert_equal(2, @regex_extend.options)
      assert_equal(5, @regex_imulti.options)
      assert_equal(67, @regex_utf8.options)
      assert_equal(53, @regex_sjis.options)
   end

   def test_options_expected_errors
      assert_raise(ArgumentError){ @regex_plain.options('x') }
   end

   def teardown
      @regex_plain  = nil
      @regex_extend = nil
      @regex_imulti = nil
      @regex_utf8   = nil
      @regex_sjis   = nil
   end
end
