######################################################################
# tc_members.rb
#
# Test case for the Struct::Class.members class method.
######################################################################
require 'test/unit'

class TC_Struct_Members_ClassMethod < Test::Unit::TestCase
   def setup
      Struct.new('Members', :name, :age) unless defined? Struct::Members
   end

   def test_members_basic
      assert_respond_to(Struct::Members, :members)
      assert_nothing_raised{ Struct::Members.members }
      assert_kind_of(Array, Struct::Members.members)
   end

   def test_members
      assert_equal(['name', 'age'], Struct::Members.members)
   end

   def test_members_expected_errors
      assert_raises(ArgumentError){ Struct::Members.members(1) }
   end
end
