######################################################################
# tc_to_a.rb
#
# Test case for the Struct#to_a instance method and the Struct#values
# instance method.
######################################################################
require 'test/unit'

class TC_Struct_ToA_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('TO_A', :name, :age) unless defined? Struct::TO_A
      @struct = Struct::TO_A.new('Dan', 37)
   end

   def test_to_a_basic
      assert_respond_to(@struct, :to_a)
      assert_respond_to(@struct, :values)
      assert_nothing_raised{ @struct.to_a }
      assert_kind_of(Array, @struct.to_a)
   end

   def test_to_a
      assert_equal(['Dan', 37], @struct.to_a)
      assert_equal(['Dan', 37], @struct.values)
   end

   def test_to_a_expected_errors
      assert_raises(ArgumentError){ @struct.to_a(1) }
   end

   def teardown
      Struct.send(:remove_const, 'TO_A') if defined? Struct::TO_A
      @struct = nil
   end
end
