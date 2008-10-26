require 'test/unit'

class TestEvalWithBinding < Test::Unit::TestCase
  def test_proc_as_binding
    x = 1
    b = proc {}
    assert_equal(1, eval("x", b))
    eval("y = 2", b)
    assert_equal(2, eval("y", b))
    eval("z = 3")
    assert_equal(3, eval("z", b))

    b = proc {w = 4}
    assert_raises(NameError) { eval("w", b) }
  end

  def test_nested_bindings
    b = binding
    b2 = eval("binding", b)
    b3 = eval("binding", b2)
    
    eval("w = 1")
    eval("x = 2", b)
    eval("y = 3", b2)
    
    assert_equal(1, eval("w"))
    assert_equal(1, eval("w", b))
    assert_equal(1, eval("w", b2))
    
    assert_equal(2, eval("x"))
    assert_equal(2, eval("x", b))
    assert_equal(2, eval("x", b2))

    assert_equal(3, eval("y"))
    assert_equal(3, eval("y", b))
    assert_equal(3, eval("y", b2))
  end

  def test_proc_and_binding_nested
    b = binding
    b2 = eval("proc {l = 5; binding}.call", b)
    b3 = eval("proc {k = 6; binding}.call", b2)

    eval("w = 1")
    eval("x = 2", b)
    eval("y = 3", b2)
    eval("z = 4", b3)

    assert_equal(1, eval("w"))
    assert_equal(1, eval("w", b))
    assert_equal(1, eval("w", b2))
    assert_equal(1, eval("w", b3))

    assert_equal(2, eval("x"))
    assert_equal(2, eval("x", b))
    assert_equal(2, eval("x", b2))
    assert_equal(2, eval("x", b3))

    assert_raises(NameError) { eval("y") }
    assert_raises(NameError) { eval("y", b) }
    assert_equal(3, eval("y", b2))
    assert_equal(3, eval("y", b3))

    assert_raises(NameError) { eval("z") }
    assert_raises(NameError) { eval("z", b) }
    assert_raises(NameError) { eval("z", b2) }
    assert_equal(4, eval("z", b3))

    assert_raises(NameError) { eval("l") }
    assert_raises(NameError) { eval("l", b) }
    assert_equal(5, eval("l", b2))
    assert_equal(5, eval("l", b3))

    assert_raises(NameError) { eval("k") }
    assert_raises(NameError) { eval("k", b) }
    assert_raises(NameError) { eval("k", b2) }
    assert_equal(6, eval("k", b3))
  end

  def test_bound_eval_in_class
    cls = Class.new {
      eval "def foo; true; end", binding
      1.times {
        eval "def bar; true; end", binding
      }
      proc {
        eval "def baz; true; end", binding
      }.call
    }
    obj = cls.new
    assert obj.foo
    assert obj.bar
    assert obj.baz
  end
end

