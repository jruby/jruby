#######################################################################
# tc_aset.rb
#
# Test case for the Array[] class method.
#######################################################################
require 'test/unit'

class TC_Array_Aset_ClassMethod < Test::Unit::TestCase
   def test_empty
      assert_equal([], Array[])
   end
   
   def test_nil
      assert_equal([nil], Array[nil])
      assert_equal([nil, nil, nil], Array[nil, nil, nil])
   end
   
   def test_one_object_type
      assert_equal(['foo', 'bar'], Array['foo', 'bar'])
      assert_equal(['aaa'], Array['a'*3])
      assert_equal([0], Array[0])
   end
   
   def test_multiple_object_types
      assert_equal([1, 'foo', /^$/], Array[1, 'foo', /^$/])
      assert_equal([1, nil, ['a']], Array[1, nil, ['a']])
   end
   
   def test_nested_arrays
      assert_equal([[],[],[]], Array[[],[],[]])
   end
   
   def test_empty_string
      assert_equal([''], Array[''])
   end
end
