require 'test/unit'

class TestArraySubclassBehavior < Test::Unit::TestCase
  class MyArray < Array
  end
  
  def setup
    @arr = MyArray.new([1,2,3])
    @arr2 = MyArray.new([[1,2],[2,3],[3,3]])
  end
  
  def test_array_instance_methods_on_subclass
    assert_equal(Array, @arr2.transpose.class)
    assert_equal(MyArray, @arr.compact.class)
    assert_equal(MyArray, @arr.reverse.class)
    assert_equal(MyArray, @arr2.flatten.class)
    assert_equal(MyArray, @arr.uniq.class)
    assert_equal(MyArray, @arr.sort.class)
    assert_equal(MyArray, @arr[1,2].class)
    assert_equal(MyArray, @arr[1..2].class)
    assert_equal(Array, @arr.to_a.class)
    assert_equal(MyArray, @arr.to_ary.class)
    assert_equal(MyArray, @arr.slice(1,2).class)
    assert_equal(MyArray, @arr.slice!(1,2).class)
    assert_equal(MyArray, (@arr*0).class)
    assert_equal(MyArray, (@arr*2).class)
    assert_equal(MyArray, @arr.replace([1,2,3]).class)
    assert_equal(Array, @arr.last(2).class)
    assert_equal(Array, @arr.first(2).class)
    assert_equal(Array, @arr.collect.class)
    assert_equal(Array, @arr.collect{true}.class)
    assert_equal(Array, @arr.zip([1,2,3]).class)
    assert_equal(MyArray, @arr.dup.class)
  end
end
