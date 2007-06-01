######################################################################
# tc_new.rb
#
# Test case for the Struct.new class method. Note that this test
# case tests both Struct.new and Struct::Class.new (where 'Class'
# is whatever name we choose).
######################################################################
require 'test/unit'

class TC_Struct_New_ClassMethod < Test::Unit::TestCase
   def setup
      @struct = nil
   end

   def test_new_basic
      assert_respond_to(Struct, :new)
      assert_nothing_raised{ @struct = Struct.new('New') }
      assert_kind_of(Class, @struct)
   end

   def test_new_no_accessors
      assert_nothing_raised{ @struct = Struct.new('New2') }
      assert_kind_of(Struct::New2, @struct.new)
   end

   def test_new_with_accessors
      assert_nothing_raised{ Struct.new('Newb', :name, :age) }
      assert_nothing_raised{ @struct = Struct::Newb.new }
      assert_kind_of(Struct::Newb, @struct)

      assert_respond_to(@struct, :name)
      assert_respond_to(@struct, :name=)
      assert_respond_to(@struct, :age)
      assert_respond_to(@struct, :age=)
   end

   def teardown
      @struct = nil
   end
end
