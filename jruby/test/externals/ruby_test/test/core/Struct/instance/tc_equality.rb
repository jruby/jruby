######################################################################
# tc_equality.rb
#
# Test case for the Struct::Class#== method.
######################################################################
require 'test/unit'

class TC_Struct_Equality_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('Foo', :name, :age) unless defined? Struct::Foo
      Struct.new('Bar', :name, :age) unless defined? Struct::Bar

      @struct1 = Struct::Foo.new('Matz', 25)
      @struct2 = Struct::Foo.new('Matz', 25)
      @struct3 = Struct::Foo.new('Larry', 35)
      @struct4 = Struct::Bar.new('Matz', 25)
   end

   def test_equality_basic
      assert_respond_to(@struct1, :==)
      assert_nothing_raised{ @struct1 == @struct2 }
   end

   def test_equality
      assert_equal(true, @struct1 == @struct2)
      assert_equal(false, @struct1 == @struct3)
      assert_equal(false, @struct1 == @struct4)
      assert_equal(false, @struct1 == 7)
   end

   def teardown
      Struct.send(:remove_const, 'Foo') if defined? Struct::Foo
      Struct.send(:remove_const, 'Bar') if defined? Struct::Bar
      @struct1 = nil
      @struct2 = nil
      @struct3 = nil
      @struct4 = nil
   end
end
