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

  if RUBY_VERSION >= "1.9"
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
  end

  def test_proc_and_binding_nested
    b = TOPLEVEL_BINDING.dup
    b2 = eval("proc {l1 = 5; binding}.call", b)
    b3 = eval("proc {k1 = 6; binding}.call", b2)
    b4 = eval("proc {j1 = 7; binding}.call", b3)

    eval("w1 = 1", b)
    eval("x1 = 2", b2)
    eval("y1 = 3", b3)
    eval("z1 = 4", b4)

    assert_equal(1, eval("w1", b))
    assert_equal(1, eval("w1", b2))
    assert_equal(1, eval("w1", b3))
    assert_equal(1, eval("w1", b4))

    assert_raises(NameError) { eval("x1", b) }
    assert_equal(2, eval("x1", b2))
    assert_equal(2, eval("x1", b3))
    assert_equal(2, eval("x1", b4))

    assert_raises(NameError) { eval("y1", b) }
    assert_raises(NameError) { eval("y1", b2) }
    assert_equal(3, eval("y1", b3))
    assert_equal(3, eval("y1", b4))

    assert_raises(NameError) { eval("z1", b) }
    assert_raises(NameError) { eval("z1", b2) }
    assert_raises(NameError) { eval("z1", b3) }
    assert_equal(4, eval("z1", b4))

    assert_raises(NameError) { eval("l1", b) }
    assert_equal(5, eval("l1", b2))
    assert_equal(5, eval("l1", b3))
    assert_equal(5, eval("l1", b4))

    assert_raises(NameError) { eval("k1", b) }
    assert_raises(NameError) { eval("k1", b2) }
    assert_equal(6, eval("k1", b3))
    assert_equal(6, eval("k1", b4))

    assert_raises(NameError) { eval("j1", b) }
    assert_raises(NameError) { eval("j1", b2) }
    assert_raises(NameError) { eval("j1", b3) }
    assert_equal(7, eval("j1", b4))
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

