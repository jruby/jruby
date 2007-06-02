require 'test/unit'

class TestAssignment < Test::Unit::TestCase

  def testBasicAssignment
    a = nil;      assert_nil(a)
    a = 1;        assert_equal(1, a)
    a = [];       assert_equal([], a)
    a = [1];      assert_equal([1], a)
    a = [nil];    assert_equal([nil], a)
    a = [[]];     assert_equal([[]], a)
    a = [*[]];    assert_equal([], a)
    a = [*[1]];   assert_equal([1], a)
    a = [*[1,2]]; assert_equal([1,2], a)

    a = *nil;      assert_nil(a)
    a = *1;        assert_equal(1, a)
    *a = nil;      assert_equal([nil], a)
      a = *[];       assert_nil(a)
      a = *[1];      assert_equal(1, a)
      a = *[nil];    assert_nil(a)
      a = *[[]];     assert_equal([], a)
      a = *[*[]];    assert_nil(a)
      a = *[*[1]];   assert_equal(1, a)
    a = *[*[1,2]]; assert_equal([1,2], a)

    *a = 1;        assert_equal([1], a)
    *a = [];       assert_equal([[]], a)

      *a = [1];      assert_equal([[1]], a)
      *a = [nil];    assert_equal([[nil]], a)
      *a = [[]];     assert_equal([[[]]], a)
      *a = [*[]];    assert_equal([[]], a)
      *a = [*[1]];   assert_equal([[1]], a)
      *a = [*[1,2]]; assert_equal([[1,2]], a)

      *a = *nil;      assert_equal([nil], a)
      *a = *[nil];    assert_equal([nil], a)
      *a = *[[]];     assert_equal([[]], a)
    *a = *1;        assert_equal([1], a)
    *a = *[];       assert_equal([], a)
    *a = *[1];      assert_equal([1], a)
    *a = *[*[]];    assert_equal([], a)
    *a = *[*[1]];   assert_equal([1], a)
    *a = *[*[1,2]]; assert_equal([1,2], a)

    a,b,*c = nil;       assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = 1;         assert_equal([1, nil, []], [a,b,c])
    a,b,*c = [];        assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = [1];       assert_equal([1, nil, []], [a,b,c])
    a,b,*c = [nil];     assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = [[]];      assert_equal([[], nil, []], [a,b,c])
    a,b,*c = [*[]];     assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = [*[1]];    assert_equal([1, nil, []], [a,b,c])
    a,b,*c = [*[1,2]];  assert_equal([1, 2, []], [a,b,c])
    
    a,b,*c = *nil;      assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = *1;        assert_equal([1, nil, []], [a,b,c])
    a,b,*c = *[];       assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = *[1];      assert_equal([1, nil, []], [a,b,c])
    a,b,*c = *[nil];    assert_equal([nil, nil, []], [a,b,c])

    a,b,*c = *[[]];     assert_equal([[], nil, []], [a,b,c])

    a,b,*c = *[*[]];    assert_equal([nil, nil, []], [a,b,c])
    a,b,*c = *[*[1]];   assert_equal([1, nil, []], [a,b,c])
    a,b,*c = *[*[1,2]]; assert_equal([1, 2, []], [a,b,c])
  end

  def testExpansionWithNoConversion
	*x = (1..7).to_a
    assert_equal([[1, 2, 3, 4, 5, 6, 7]], x)
  end

  def testMultipleAssignment
    a, b = 1, 2
    assert_equal(1, a)
    assert_equal(2, b)
    
    a, b = b, a
    assert_equal(2, a)
    assert_equal(1, b)
    
    a, = 1,2
    assert_equal(1, a)
    
    a, *b = 1, 2, 3
    assert_equal(1, a)
    assert_equal([2, 3], b)
    
    a, (b, c), d = 1, [2, 3], 4
    assert_equal(1, a)
    assert_equal(2, b)
    assert_equal(3, c)
    assert_equal(4, d)
    
    *a = 1, 2, 3
    assert_equal([1, 2, 3], a)
    
    *a = 4
    assert_equal([4], a)
    
    *a = nil
    assert_equal([nil], a)
  end

  def testConditionalAssignment
    a=[]
    a[0] ||= "bar"
    assert_equal("bar", a[0])

    h={}
    h["foo"] ||= "bar"
    assert_equal("bar", h["foo"])
    
    aa = 5
    aa ||= 25
    assert_equal(5, aa)

    bb ||= 25
    assert_equal(25, bb)

    cc &&=33
    assert_nil(cc)

    cc = 5
    cc &&=44
    assert_equal(44, cc)
  end

end

