######################################################################
# tc_aref.rb
#
# Test case for the Struct#[] instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Struct_Aref_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('Aref', :name, :age) unless defined? Struct::Aref
      @struct = Struct::Aref.new('Matz', 25)
   end

   def test_aref_basic
      assert_respond_to(@struct, :[])
   end

   def test_aref_by_name
      assert_equal('Matz', @struct[:name])
      assert_equal('Matz', @struct['name'])
      assert_equal(25, @struct[:age])
      assert_equal(25, @struct['age'])
   end

   def test_aref_by_position
      assert_equal('Matz', @struct[0])
      assert_equal(25, @struct[1])
      assert_equal(25, @struct[-1])
   end

   def test_aref_by_method
      assert_equal('Matz', @struct.name)
      assert_equal(25, @struct.age)
   end

   def test_aref_expected_errors
      assert_raise(ArgumentError){ @struct[] }
      assert_raise(ArgumentError){ @struct[:foo, :bar] }
      assert_raise(NameError){ @struct[:foo] }
      assert_raise(NoMethodError){ @struct.foo }
      assert_raise(IndexError){ @struct[99] }
   end

   def teardown
      @struct = nil
      Struct.send(:remove_const, 'Aref') if defined? Struct::Aref
   end
end
