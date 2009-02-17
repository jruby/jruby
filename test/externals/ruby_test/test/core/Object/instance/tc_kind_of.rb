########################################################################
# tc_kind_of.rb
#
# Test case for the Object#kind_of? instance method and the
# Object#is_a? alias.
########################################################################
require 'test/unit'

module ObjectKindOfM; end
class ClassKindOfA
   include ObjectKindOfM
end
class ClassKindOfB < ClassKindOfA; end
class ClassKindOfC < ClassKindOfB; end

class TC_Object_KindOf_InstanceMethod < Test::Unit::TestCase
   def setup
      @object   = Object.new
      @b_object = ClassKindOfB.new
   end

   def test_kind_of_basic
      assert_respond_to(@object, :kind_of?)
      assert_nothing_raised{ @object.kind_of?(Object) }
   end

   def test_is_a_alias_basic
      assert_respond_to(@object, :is_a?)
      assert_nothing_raised{ @object.is_a?(Object) }
   end

   def test_kind_of
      assert_equal(true, @b_object.kind_of?(ClassKindOfA))
      assert_equal(true, @b_object.kind_of?(ClassKindOfB))
      assert_equal(false, @b_object.kind_of?(ClassKindOfC))
      assert_equal(true, @b_object.kind_of?(ObjectKindOfM))
   end

   def test_is_a_alias
      assert_equal(true, @b_object.is_a?(ClassKindOfA))
      assert_equal(true, @b_object.is_a?(ClassKindOfB))
      assert_equal(false, @b_object.is_a?(ClassKindOfC))
      assert_equal(true, @b_object.is_a?(ObjectKindOfM))
   end

   def test_kind_of_object
      assert_equal(true, @object.kind_of?(Object))
      assert_equal(false, @object.kind_of?(Array))
      assert_equal(false, @object.kind_of?(Module))
      assert_equal(true, [].kind_of?(Object))
      assert_equal(true, 1.kind_of?(Object))
      assert_equal(true, nil.kind_of?(Object))
   end

   def test_is_a_object_alias
      assert_equal(true, @object.is_a?(Object))
      assert_equal(false, @object.is_a?(Array))
      assert_equal(false, @object.is_a?(Module))
      assert_equal(true, [].is_a?(Object))
      assert_equal(true, 1.is_a?(Object))
      assert_equal(true, nil.is_a?(Object))
   end

   def test_kind_of_expected_failures
      assert_raise(TypeError){ @object.kind_of?(1) }
      assert_raise(TypeError){ @object.kind_of?(@object) }
      assert_raise(ArgumentError){ @object.kind_of? }
   end

   def test_is_a_alias_expected_failures
      assert_raise(TypeError){ @object.is_a?(1) }
      assert_raise(TypeError){ @object.is_a?(@object) }
      assert_raise(ArgumentError){ @object.is_a? }
   end

   def teardown
      @object   = nil
      @b_object = nil
   end
end
