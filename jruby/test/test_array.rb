require 'test/unit'

class TestArray < Test::Unit::TestCase

  def test_unshift_and_leftshift_op
    arr = ["zero", "first"]
    arr.unshift "second", "third"
    assert_equal(["second", "third", "zero", "first"], arr)
    assert_equal(["first"], arr[-1..-1])
    assert_equal(["first"], arr[3..3])
    assert_equal([], arr[3..2])
    assert_equal([], arr[3..1])
    assert(["third", "zero", "first"] == arr[1..4])
    assert('["third", "zero", "first"]' == arr[1..4].inspect)
  
    arr << "fourth"

    assert("fourth" == arr.pop());
    assert("second" == arr.shift());
  end
  
  class MyArray < Array
    def [](arg)
      arg
    end
  end

  def test_aref
    assert_equal(nil, [].slice(-1..1))
    # test that overriding in child works correctly
    assert_equal(2, MyArray.new[2])
  end

  def test_class
    assert(Array == ["zero", "first"].class)
    assert("Array" == Array.to_s)
  end

  def test_dup_and_reverse
    arr = [1, 2, 3]
    arr2 = arr.dup
    arr2.reverse!
    assert_equal([1,2,3], arr)
    assert_equal([3,2,1], arr2)

    assert_equal([1,2,3], [1,2,3,1,2,3,1,1,1,2,3,2,1].uniq)

    assert_equal([1,2,3,4], [[[1], 2], [3, [4]]].flatten)
    assert_equal(nil, [].flatten!)
  end
  
  def test_delete
    arr = [1, 2, 3]
    arr2 = []
    arr.each { |x|
      arr2 << x
      arr.delete(x) if x == 2
    }
    assert_equal([1,2], arr2)
  end
  
  def test_fill
    arr = [1,2,3,4]
    arr.fill(1,10)
    assert_equal([1,2,3,4], arr)
    arr.fill(1,0)
    assert_equal([1,1,1,1], arr)
  end
  
  def test_flatten
    arr = []
    arr << [[[arr]]]
    assert_raises(ArgumentError) {
      arr.flatten
    }
  end

  # To test int coersion for indicies
  class IntClass
    def initialize(num); @num = num; end
    def to_int; @num; end; 
  end

  def test_conversion
    arr = [1, 2, 3]

    index = IntClass.new(1)
    arr[index] = 4
    assert_equal(4, arr[index])
    eindex = IntClass.new(2)
    arr[index, eindex] = 5
    assert_equal([1,5], arr)
    arr.delete_at(index)
    assert_equal([1], arr)
    arr = arr * eindex
    assert_equal([1, 1], arr)
  end

  def test_unshift_nothing
    assert_nothing_raised { [].unshift(*[]) }
    assert_nothing_raised { [].unshift() }
  end

  ##### Array#[] #####

  def test_indexing
    assert_equal([1], Array[1])
    assert_equal([], Array[])
    assert_equal([1,2], Array[1,2])
  end

  ##### insert ####

  def test_insert
    a = [10, 11]
    a.insert(1, 12)
    assert_equal([10, 12, 11], a)
    a = []
    a.insert(-1, 10)
    assert_equal([10], a)
    a.insert(-2, 11)
    assert_equal([11, 10], a)
    a = [10]
    a.insert(-1, 11)
    assert_equal([10, 11], a)
  end

  ##### == #####
  
  def test_ary
    o = Object.new
    def o.to_ary; end
    def o.==(o); true; end
    assert_equal(true, [].==(o))
  end
  
  # test that extensions of the base classes are typed correctly
  class ArrayExt < Array
  end

  def test_array_extension
    assert_equal(ArrayExt, ArrayExt.new.class)
    assert_equal(ArrayExt, ArrayExt[:foo, :bar].class)
  end

  ##### flatten #####
  def test_flatten
    a = [2,[3,[4]]]
    assert_equal([1,2,3,4],[1,a].flatten)
    assert_equal([2,[3,[4]]],a)
    a = [[1,2,[3,[4],[5]],6,[7,[8]]],9]
    assert_equal([1,2,3,4,5,6,7,8,9],a.flatten)
    assert(a.flatten!,"We did flatten")
    assert(!a.flatten!,"We didn't flatten")
  end

  ##### splat test #####
  class ATest
    def to_a; 1; end
  end

  def test_splatting
    proc { |a| assert_equal(1, a) }.call(*1)
    assert_raises(TypeError) { proc { |a| }.call(*ATest.new) }
  end

  #### index test ####
  class AlwaysEqual
    def ==(arg)
      true
    end
  end

  def test_index
    array_of_alwaysequal = [AlwaysEqual.new]
    # this should pass because index should call AlwaysEqual#== when searching
    assert_equal(0, array_of_alwaysequal.index("foo"))
    assert_equal(0, array_of_alwaysequal.rindex("foo"))
  end

  def test_spaceship
    assert_equal(0, [] <=> [])
    assert_equal(0, [1] <=> [1])
    assert_equal(-1, [1] <=> [2])
    assert_equal(1, [2] <=> [1])
    assert_equal(1, [1] <=> [])
    assert_equal(-1, [] <=> [1])

    assert_equal(0, [1, 1] <=> [1, 1])
    assert_equal(-1, [1, 1] <=> [1, 2])

    assert_equal(1, [1,6,1] <=> [1,5,0,1])
    assert_equal(-1, [1,5,0,1] <=> [1,6,1])
  end
  
  class BadComparator
    def <=>(other)
      "hello"
    end
  end

  def test_bad_comparator
    assert_equal("hello", [BadComparator.new] <=> [BadComparator.new])
  end

  def test_raises_stack_exception
    assert_raises(SystemStackError) { a = []; a << a; a <=> a }
  end
  
  def test_multiline_array_not_really_add
    assert_raises(NoMethodError) do
  	  [1,2,3]
  	  +[2,3]
  	end
  end
end
