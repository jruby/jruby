########################################################################
# tc_object_id.rb
#
# Test case for the Object#object_id instance method and the
# Object#__id__ alias.
########################################################################
require 'test/unit'

class TC_Object_ObjectId_InstanceMethod < Test::Unit::TestCase
   def setup
      @object = Object.new
   end

   def test_object_id_basic
      assert_respond_to(@object, :object_id)
      assert_nothing_raised{ @object.object_id }
      assert_kind_of(Fixnum, @object.object_id)
   end

   def test_id_alias_basic
      assert_respond_to(@object, :__id__)
      assert_nothing_raised{ @object.__id__ }
      assert_kind_of(Fixnum, @object.__id__)
   end

   def teardown
      @object = nil
   end
end
