########################################################################
# tc_instance_of.rb
#
# Test case for the Object#instance_of? instance method.
########################################################################
require 'test/unit'

class TC_Object_InstanceOf_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
   end

   def test_instance_of_basic
      assert_respond_to(@object, :instance_of?)
      assert_nothing_raised{ @object.instance_of?(Object) }
   end

   def test_instance_of
      assert_equal(true, @object.instance_of?(Object))
      assert_equal(false, @object.instance_of?(Array))
      assert_equal(false, @object.instance_of?(Module))
      assert_equal(false, [].instance_of?(Object))
      assert_equal(false, 1.instance_of?(Object))
      assert_equal(false, nil.instance_of?(Object))
   end

   def test_instance_of_expected_failures
      assert_raise(TypeError){ @object.instance_of?(1) }
      assert_raise(TypeError){ @object.instance_of?(@object) }
      assert_raise(ArgumentError){ @object.instance_of? }
   end

   def teardown
      @object = nil
   end
end
