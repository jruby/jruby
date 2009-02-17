#######################################################################
# tc_clone.rb
#
# Test case for the Object#clone instance method.
#######################################################################
require 'test/unit'

class ObjectClone
   attr_accessor :str
end

class TC_Object_Clone_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
      @clone  = nil
      @string = 'Hello'

      @custom = ObjectClone.new
      @custom.str = @string
   end

   def test_clone_basic
      assert_respond_to(@object, :clone)
      assert_nothing_raised{ @object.clone }
   end

   def test_clone
      assert_nothing_raised{ @clone = @object.clone }
      assert_equal(true, @clone.object_id == @clone.object_id)
   end

   def test_clone_instance_variables
      assert_nothing_raised{ @clone = @custom.clone }
      assert_equal('Hello', @clone.str)
      assert_nothing_raised{ @custom.str[1,4] = 'i' }
      assert_equal('Hi', @custom.str)
      assert_equal('Hi', @clone.str)
   end

   def test_clone_keeps_frozen_state
      assert_nothing_raised{ @custom.freeze }
      assert_nothing_raised{ @clone = @custom.clone }
      assert_equal(true, @custom.frozen?)
      assert_equal(true, @clone.frozen?)
   end

   def test_clone_keeps_tainted_state
      assert_nothing_raised{ @custom.taint }
      assert_nothing_raised{ @clone = @custom.clone }
      assert_equal(true, @custom.tainted?)
      assert_equal(true, @clone.tainted?)
   end

   def test_clone_expected_errors
      assert_raise(ArgumentError){ @object.clone(true) }
      assert_raise(TypeError){ 7.clone }
      assert_raise(TypeError){ nil.clone }
      assert_raise(TypeError){ true.clone }
      assert_raise(TypeError){ false.clone }
      assert_raise(TypeError){ 'hello'.to_sym.clone }
   end

   def teardown
      @object = nil
      @clone  = nil
      @custom = nil
   end
end
