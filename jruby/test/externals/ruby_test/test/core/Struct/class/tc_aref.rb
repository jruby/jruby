######################################################################
# tc_aref.rb
#
# Foo case for the Struct::Class[] class method.
######################################################################
require 'test/unit'

class TC_Struct_Foo_ClassMethod < Test::Unit::TestCase
   def setup
      @struct = nil
      Struct.new('Foo', :name, :age) unless defined? Struct::Foo
   end

   def test_aref_basic
      assert_respond_to(Struct::Foo, :[])
      assert_nothing_raised{ @struct = Struct::Foo['Matz', 35] }
      assert_kind_of(Struct::Foo, @struct)
   end

   def test_aref_members_defined
      assert_nothing_raised{ @struct = Struct::Foo['Matz', 35] }
      assert_respond_to(@struct, :name)
      assert_respond_to(@struct, :name=)
      assert_respond_to(@struct, :age)
      assert_respond_to(@struct, :age=)
   end

   def test_aref_member_values
      assert_nothing_raised{ @struct = Struct::Foo['Matz', 35] }
      assert_equal('Matz', @struct.name)
      assert_equal(35, @struct.age)
   end

   def teardown
      Struct.send(:remove_const, 'Foo') if defined? Struct::Foo
      @struct = nil
   end
end
