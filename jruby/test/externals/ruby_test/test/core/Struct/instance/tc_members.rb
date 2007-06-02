######################################################################
# tc_members.rb
#
# Test case for the Struct#members instance method.
######################################################################
require 'test/unit'

class TC_Struct_Members_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('IMembers', :name, :age) unless defined? Struct::IMembers
      @struct = Struct::IMembers.new('Dan', 37)
   end

   def test_members_basic
      assert_respond_to(@struct, :members)
      assert_nothing_raised{ @struct.members }
      assert_kind_of(Array, @struct.members)
   end

   def test_members
      assert_equal(['name', 'age'], @struct.members)
   end

   def test_members_expected_errors
      assert_raises(ArgumentError){ @struct.members(1) }
   end

   def teardown
      Struct.send(:remove_const, 'IMembers') if defined? Struct::IMembers
      @struct = nil
   end
end
