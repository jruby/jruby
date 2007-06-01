######################################################################
# tc_length.rb
#
# Test case for the Struct#length instance method and the
# Struct#size alias.
######################################################################
require 'test/unit'

class TC_Struct_Length_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('Length', :name, :age, :tall) unless defined? Struct::Length
      @struct1 = Struct::Length.new('Dan', 37, true)
      @struct2 = Struct::Length.new
   end

   def test_length
      assert_respond_to(@struct1, :length)
      assert_equal(3, @struct1.length)
      assert_equal(3, @struct2.length)
   end

   def test_size_alias
      assert_respond_to(@struct1, :size)
      assert_equal(3, @struct1.size)
      assert_equal(3, @struct2.size)
   end

   def test_length_expected_errors
      assert_raise(ArgumentError){ @struct1.length(1) }
   end

   def teardown
      Struct.send(:remove_const, 'Length') if defined? Struct::Length
      @struct1 = nil
      @struct2 = nil
   end
end
