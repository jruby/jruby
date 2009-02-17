###############################################################################
# tc_succ.rb
#
# Test case for the String#succ instance method, and the String#next alias.
# The tests for String#succ! method are in the tc_succ_bang.rb file.
###############################################################################
require 'test/unit'

class TC_String_Succ_InstanceMethod < Test::Unit::TestCase
   def setup
      @string1 = 'abcd'
      @string2 = 'THX1138'
      @string3 = '<<koala>>'
      @string4 = 'ZZZ9999'
      @string5 = '***'
   end

   def test_succ_basic
      assert_respond_to(@string1, :succ)
      assert_nothing_raised{ @string1.succ }
      assert_kind_of(String, @string1.succ)
   end

   def test_succ
      assert_equal('abce', @string1.succ)
      assert_equal('THX1139', @string2.succ)
      assert_equal('<<koalb>>', @string3.succ)
      assert_equal('AAAA0000', @string4.succ)
      assert_equal('**+', @string5.succ)
   end

   def test_alias_next
      assert_equal('abce', @string1.next)
      assert_equal('THX1139', @string2.next)
      assert_equal('<<koalb>>', @string3.next)
      assert_equal('AAAA0000', @string4.next)
      assert_equal('**+', @string5.next)
   end

   def test_succ_unmodified
      assert_nothing_raised{ @string1.succ }
      assert_equal('abcd', @string1)
   end

   def test_succ_frozen_string
      assert_equal('abce', @string1.freeze.succ)
   end

   def test_succ_edge_cases
      assert_equal('', ''.succ)
      assert_equal('nim', 'nil'.succ)
      assert_equal('truf', 'true'.succ)
      assert_equal('falsf', 'false'.succ)
      assert_equal('9223372036854775809', (2**63).to_s.succ)
   end

   def test_expected_errors
      assert_raise(ArgumentError){ @string1.succ(1) }
   end

   def test_next_alias_expected_errors
      assert_raise(ArgumentError){ @string1.next(1) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
      @string3 = nil
      @string4 = nil
      @string5 = nil
   end
end
