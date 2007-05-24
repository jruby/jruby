require 'test/unit'


class TestMethod < Test::Unit::TestCase

  def setup
    @m1 = 12.method("+")
  end

  def test_AREF # '[]'
    assert_equal(15, @m1[3])
    assert_equal(9,  @m1[-3])
  end

  def dummy1(a, b=0, c=1)
  end

  def test_arity
    assert_equal(1, @m1.arity)
    assert_equal(0, self.method("test_arity").arity)
    assert_equal(-2, self.method("dummy1").arity)
    assert_equal(-1, @m1.method("call").arity)
    assert_equal(-1, @m1.method("respond_to?").arity)
  end

  def test_call
    assert_equal(15, @m1.call(3))
    assert_equal(9,  @m1.call(-3))
  end

  def test_to_proc
    p = @m1.to_proc
    assert_instance_of(Proc, p)
    assert_equal(15, p.call(3))
  end
  
  def optarg_assigns_variable(a, b=(c=1; 0))
    [a, b, c]
  end
  
  def test_optarg_that_assigns_variable
    assert_equal([2, 0, 1], optarg_assigns_variable(2))
    assert_equal([2, 1, nil], optarg_assigns_variable(2, 1))
  end

end
