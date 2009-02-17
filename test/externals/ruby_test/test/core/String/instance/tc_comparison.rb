###############################################################################
# tc_comparison.rb
#
# Test case for the String#<=> instance method. Note that objects of custom
# non-String classes must declare both '<=>' and 'to_str' in order to work as
# expected.
#
# Note: the requirement that to_str be defined is curious, since it's not
# actually used internally when comparing strings against non-String objects.
#
# TODO: Do a binary compare by setting $= to false.
###############################################################################
require 'test/unit'

class TC_String_Comparison_InstanceMethod < Test::Unit::TestCase
   class StringCompare
      include Comparable
      attr :str

      def initialize(str)
         @str = str
      end

      def <=>(other)
         str.downcase <=> other.downcase
      end

      def to_str
         "123"
      end
   end

   def setup
      @long   = "abcdef"
      @short  = "abc"
      @caps   = "ABCDEF"
      @custom = StringCompare.new('ABC')
   end

   def test_comparison_basic
      assert_respond_to(@long, :<=>)
      assert_nothing_raised{ @long <=> @short }
      assert_kind_of(Fixnum, @long <=> @short)
   end

   def test_comparison
      assert_equal(0, @long <=> "abcdef")
      assert_equal(1, @long <=> @caps)
      assert_equal(1, @long <=> @short)
      assert_equal(-1, @short <=> @long)
   end

   def test_comparison_edge_cases
      assert_equal(0, '' <=> '')
      assert_equal(-1, '' <=> ' ')
      assert_equal(-1, ' ' <=> '       ')
   end

   def test_comparison_against_non_strings
      assert_nil(@long <=> 1)
      assert_nil(@long <=> [1,2,3])
      assert_nil(@long <=> nil)
      assert_nil(@long <=> true)
      assert_nil(@long <=> false)
   end

   # This is a caseless compare
   def test_comparison_against_custom_compare
      assert_equal(0, @short <=> @custom)
      assert_equal(1, @long <=> @custom)
      assert_equal(1, @caps <=> @custom)
   end

   def test_comparison_expected_errors
      assert_raise(ArgumentError){ @long.send(:<=>, 'hello', 'world') }
   end

   def teardown
      @long  = nil
      @short = nil
      @caps  = nil
   end
end
