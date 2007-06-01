#####################################################################
# tc_id2ref.rb
#
# Test case for the ObjectSpace#_id2ref module method.
#
# TODO: Add tests for parameters to define_finalizer.
#####################################################################
require 'test/unit'

class TC_ObjectSpace_Id2ref_ClassMethod < Test::Unit::TestCase
   def setup
      @string = "hello"
      @fixnum = 77
   end

   def test_id2ref_basic
      assert_respond_to(ObjectSpace, :_id2ref) 
      assert_nothing_raised{ ObjectSpace._id2ref(@string.object_id) }
   end

   def test_id2ref
      assert_kind_of(String, ObjectSpace._id2ref(@string.object_id))
      assert_kind_of(Fixnum, ObjectSpace._id2ref(@fixnum.object_id))

      assert_equal(@string, ObjectSpace._id2ref(@string.object_id))
      assert_equal(@fixnum, ObjectSpace._id2ref(@fixnum.object_id))
   end

   def test_id2ref_edge_cases
      assert_kind_of(NilClass, ObjectSpace._id2ref(nil.object_id))
      assert_kind_of(TrueClass, ObjectSpace._id2ref(true.object_id))
      assert_kind_of(FalseClass, ObjectSpace._id2ref(false.object_id))
   end

   def test_id2ref_expected_errors
      assert_raises(ArgumentError){ ObjectSpace._id2ref }
   end

   def teardown
      @string = nil
      @fixnum = 77
   end
end
