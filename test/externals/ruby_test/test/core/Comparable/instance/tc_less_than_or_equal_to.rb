######################################################################
# tc_less_than_or_equal_to.rb
#
# Test case for the Comparable#<= instance method.  For testing
# purposes we setup a custom class that mixes in Comparable and
# defines a basic <=> method.
######################################################################
require 'test/unit'

class TC_Comparable_LessThanOrEqualTo_Instance < Test::Unit::TestCase

   class Foo
      include Comparable
      attr :val
      def initialize(val)
         @val = val
      end
      def <=>(other)
         @val <=> other.val
      end
   end

   def setup
      @f1 = Foo.new(1)
      @f2 = Foo.new(2)
      @f3 = Foo.new(nil)
   end

   def test_less_than_or_equal_to_basic
      assert_respond_to(@f1, :<=)
   end

   def test_less_than_or_equal_to
      assert_equal(true, @f1 <= @f2)
      assert_equal(true, @f1 <= @f1)
      assert_equal(false, @f2 <= @f1)
   end

   def test_less_than_or_equal_to_expected_errors
      assert_raises(ArgumentError){ @f1 <= @f3 }
      assert_raises(NoMethodError){ @f3 <= @f1 }
   end

   def teardown
      @f1 = nil
      @f2 = nil
      @f3 = nil
   end
end
