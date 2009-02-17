#######################################################################
# tc_new.rb
#
# Test case for the Array.new class method.
#######################################################################
require 'test/unit'

class TC_Array_New_ClassMethod < Test::Unit::TestCase
   def test_empty
      assert_equal([], Array.new)
   end
   
   def test_size
      assert_equal([], Array.new(0))
      assert_equal([nil], Array.new(1))
      assert_equal([nil,nil], Array.new(2, nil))
      assert_equal([false,false], Array.new(2, false))
   end
   
   def test_expected_size_errors     
      assert_raise(ArgumentError){ Array.new(-1) }
      assert_raise(ArgumentError){ Array.new(-1.5) }
      assert_raise(ArgumentError){ Array.new(-999999999) }
      assert_raise(TypeError){ Array.new(nil) }
   end

   def test_size_with_floats
      assert_equal([nil], Array.new(1.5))
      assert_equal(['a', 'a'], Array.new(2.5, 'a'))
   end
   
   def test_size_plus_object
      assert_equal([], Array.new(0, 'test'))
      assert_equal(['foo'], Array.new(1, 'foo'))
      assert_equal([2], Array.new(1,2))
      assert_equal(['bar', 'bar', 'bar'], Array.new(3, 'bar'))
   end
   
   def test_expected_size_plus_object_errors   
      assert_raise(ArgumentError){ Array.new(-1, 'baz') }
   end
   
   def test_array
      assert_equal([], Array.new([]))
      assert_equal([1,2,3], Array.new([1,2,3]))
   end
   
   def test_size_with_block
      assert_equal([], Array.new(0){ Hash.new })
      assert_equal([{}, {}], Array.new(2){ Hash.new })
      assert_equal([0,1,4,9], Array.new(4){ |i| i * i })
   end
   
   def test_expected_size_with_block_errors   
      assert_raise(ArgumentError){ Array.new(-1){ Hash.new } }
   end
end
