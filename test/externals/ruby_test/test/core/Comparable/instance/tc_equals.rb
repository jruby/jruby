######################################################################
# tc_equals.rb
#
# Test case for the Comparable#< instance method.  For testing
# purposes we setup a custom class that mixes in Comparable and
# defines a basic <=> method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Comparable_Equals_Instance < Test::Unit::TestCase

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

   def test_equals_basic
      assert_respond_to(@f1, :==)
      assert_kind_of(Boolean, @f1 == @f2)
   end

   def test_equals
      assert_equal(false, @f1 == @f2)
      assert_equal(true,  @f1 == @f1)
      assert_equal(false, @f2 == @f1)
   end

   def test_equals_against_different_object_types
      msg = "=> Possibly a bug. See ruby-core: 13448 and following"
      # Fails, perhaps correctly matching MRI?
#      assert_equal(false, @f1 == 0, msg)
#      assert_equal(false, @f1 == 'hello', msg)
   end

   def teardown
      @f1 = nil
      @f2 = nil
      @f3 = nil
   end
end
