#######################################################################
# tc_dup.rb
#
# Test case for the Object#dup instance method.
#######################################################################
require 'test/unit'

class ObjectDup
   attr_accessor :str
end

class TC_Object_Dup_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
      @dup    = nil
      @string = 'Hello'

      @custom = ObjectDup.new
      @custom.str = @string
   end

   def test_dup_basic
      assert_respond_to(@object, :dup)
      assert_nothing_raised{ @object.dup }
   end

   def test_dup
      assert_nothing_raised{ @dup = @object.clone }
      assert_equal(false, @dup.object_id == @object.object_id)
   end

   def test_dup_instance_variables
      assert_nothing_raised{ @dup = @custom.clone }
      assert_equal('Hello', @dup.str)
      assert_nothing_raised{ @custom.str[1,4] = 'i' }
      assert_equal('Hi', @custom.str)
      assert_equal('Hi', @dup.str)
   end

   def test_dup_keeps_frozen_state
      assert_nothing_raised{ @custom.freeze }
      assert_nothing_raised{ @dup = @custom.clone }
      assert_equal(true, @custom.frozen?)
      assert_equal(true, @dup.frozen?)
   end

   def test_dup_keeps_tainted_state
      assert_nothing_raised{ @custom.taint }
      assert_nothing_raised{ @dup = @custom.clone }
      assert_equal(true, @custom.tainted?)
      assert_equal(true, @dup.tainted?)
   end

   def test_dup_expected_errors
      assert_raise(ArgumentError){ @object.dup(true) }
      assert_raise(TypeError){ 7.dup }
      assert_raise(TypeError){ nil.dup }
      assert_raise(TypeError){ true.dup }
      assert_raise(TypeError){ false.dup }
      assert_raise(TypeError){ 'hello'.to_sym.dup }
   end

   def teardown
      @object = nil
      @dup  = nil
      @custom = nil
   end
end
