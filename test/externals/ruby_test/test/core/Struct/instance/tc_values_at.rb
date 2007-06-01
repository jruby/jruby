######################################################################
# tc_values_at.rb
#
# Test case for the Struct#values_at instance method.
######################################################################
require 'test/unit'

class TC_Struct_ValuesAt_InstanceMethod < Test::Unit::TestCase
   def setup
      unless defined? Struct::ValuesAt
         Struct.new('ValuesAt', :a, :b, :c, :d, :e, :f)
      end
      @struct = Struct::ValuesAt.new(11, 22, 33, 44, 55, 66)
   end

   def test_values_at_basic
      assert_respond_to(@struct, :values_at)
   end

   def test_values_at_index
      assert_equal([11], @struct.values_at(0))
      assert_equal([11, 22], @struct.values_at(0,1))
      assert_equal([11, 33, 66], @struct.values_at(0,2,5))
      assert_equal([66], @struct.values_at(-1))
   end

   def test_values_at_range
      assert_equal([11], @struct.values_at(0..0))
      assert_equal([11, 22], @struct.values_at(0..1))
      assert_equal([11, 22, 33, 44], @struct.values_at(0..3))
      assert_equal([55, 66], @struct.values_at(-2..-1))
   end

   def test_values_at_range_exclusive
      assert_equal([], @struct.values_at(0...0))
      assert_equal([11], @struct.values_at(0...1))
      assert_equal([11, 22, 33], @struct.values_at(0...3))
      assert_equal([55], @struct.values_at(-2...-1))
   end

   def test_values_at_expected_errors
      assert_raise(TypeError){ @struct.values_at(nil) }
      assert_raise(TypeError){ @struct.values_at('test') }
      assert_raise(IndexError){ @struct.values_at(99) }
      assert_raise(IndexError){ @struct.values_at(0..99) }
   end

   def teardown
      Struct.send(:remove_const, 'ValuesAt') if defined? Struct::ValuesAt
      @struct = nil
   end
end
