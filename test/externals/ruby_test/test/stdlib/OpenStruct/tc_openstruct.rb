########################################################################
# tc_openstruct.rb
#
# Test case for the OpenStruct library.
########################################################################
require 'test/unit'
require 'ostruct'

class TC_OStruct < Test::Unit::TestCase
   def setup
      @ostruct1 = OpenStruct.new
      @ostruct2 = OpenStruct.new
   end

   def test_constructor
      assert_respond_to(OpenStruct, :new)
      assert_nothing_raised{ OpenStruct.new }
      assert_kind_of(OpenStruct, OpenStruct.new)
   end

   def test_constructor_with_initial_values
      assert_nothing_raised{
         @ostruct1 = OpenStruct.new(:foo => 1, 'bar' => 'hello')
      }
      assert_equal(1, @ostruct1.foo)
      assert_equal('hello', @ostruct1.bar)
   end

   def test_constructor_expected_errors
      assert_raise(ArgumentError, NoMethodError){ OpenStruct.new(1) }
      assert_raise(ArgumentError, NoMethodError){ OpenStruct.new(true) }
      assert_raise(ArgumentError, NoMethodError){ OpenStruct.new(:foo) }
      assert_raise(ArgumentError){ OpenStruct.new(1, 2) }
   end

   # Anything that responds to 'each' will work. Nil and false are ignored.
   def test_constructor_edge_cases
      assert_nothing_raised{ OpenStruct.new('foo') }
      assert_nothing_raised{ OpenStruct.new([]) }
      assert_nothing_raised{ OpenStruct.new({}) }
      assert_nothing_raised{ OpenStruct.new(false) }
      assert_nothing_raised{ OpenStruct.new(nil) }
   end

   def test_dynamic_getters
      assert_nil(@ostruct1.foo)
      assert_nil(@ostruct1.random_method)
      assert_nil(@ostruct1.UpperCase)
   end

   def test_dynamic_setters
      assert_equal('hello', @ostruct1.foo = 'hello')
      assert_equal('hello', @ostruct1.foo)
   end

   def test_equality
      assert_equal(true, @ostruct1 == @ostruct2)
      assert_nothing_raised{ @ostruct1.foo = 'hello' }
      assert_equal(false, @ostruct1 == @ostruct2)
      assert_nothing_raised{ @ostruct2.foo = 'hello' }
      assert_equal(true, @ostruct1 == @ostruct2)
   end

   def teardown
      @ostruct1 = nil
      @ostruct2 = nil
   end
end
