######################################################################
# tc_aset.rb
#
# Test case for the Struct#[] instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Struct_Aset_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('Foo', :name, :age) unless defined? Struct::Foo
      @struct = Struct::Foo.new('Matz', 25)
   end

   def test_aset_basic
      assert_respond_to(@struct, :[]=)
   end

   def test_aset_by_name
      assert_equal('Dan', @struct[:name] = 'Dan')
      assert_equal(35, @struct[:age] = 35)
   end

   def test_aset_by_position
      assert_equal('Charles', @struct[0] = 'Charles')
      assert_equal(28, @struct[1] = 28)
      assert_equal('Charlie', @struct[-1] = 'Charlie')
   end

   def test_aset_by_method_name
      assert_equal('Dan', @struct.name = 'Dan')
      assert_equal(35, @struct.age = 35)
   end

   def test_aset_expected_errors
      assert_raises(NameError){ @struct[:foo] = 1 }
      assert_raises(ArgumentError){ @struct[:foo, :bar] = 1 }
      assert_raises(IndexError){ @struct[99] = 1 }
   end

   def teardown
      @struct = nil
      Struct.send(:remove_const, 'Foo') if defined? Struct::Foo
   end
end
