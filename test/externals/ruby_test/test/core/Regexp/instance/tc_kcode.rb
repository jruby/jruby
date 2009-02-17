###############################################################################
# tc_kcode.rb
#
# Test case for the Regexp#kcode instance method.
###############################################################################
require 'test/unit'

class TC_Regexp_Kcode_InstanceMethod < Test::Unit::TestCase
   def setup
      @regex_plain = /abc/ix
      @regex_utf8  = /abc/u
      @regex_sjis  = /abc/s
      @regex_euc   = /abc/e
      @regex_ansi  = /abc/n
   end

   def test_kcode_basic
      assert_respond_to(@regex_plain, :kcode)
      assert_nothing_raised{ @regex_plain.kcode }
   end

   def test_kcode
      assert_nil(@regex_plain.kcode)
      assert_equal('utf8', @regex_utf8.kcode)
      assert_equal('sjis', @regex_sjis.kcode)
      assert_equal('euc', @regex_euc.kcode)
      assert_equal('none', @regex_ansi.kcode) # Eh?
   end

   def test_kcode_expected_errors
      assert_raise(ArgumentError){ @regex_plain.kcode('utf8') }
   end

   def teardown
      @regex_plain   = nil
      @regex_unicode = nil
      @regex_sjis    = nil
   end
end
