#######################################################################
# tc_class.rb
#
# Test case for the Object#class instance method.
#######################################################################
require 'test/unit'

class TC_Object_Class_InstanceMethod < Test::Unit::TestCase
   def setup
      @obj = Object.new
   end

   def test_class_basic
      assert_respond_to(@obj, :class)
      assert_nothing_raised{ @obj.class }
   end

   def test_class
      assert_equal(Object, @obj.class)
      assert_equal(Fixnum, 1.class)
      assert_equal(Float, 1.0.class)
      assert_equal(Array, [].class)
   end

   def test_class_expected_errors
      assert_raise(ArgumentError){ @obj.class(true) }
   end

   def teardown
      @obj = nil
   end
end
