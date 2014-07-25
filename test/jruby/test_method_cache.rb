require 'test/unit'

class TestMethodCache < Test::Unit::TestCase
  def test_simple_hierarchy
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end
      class B < A; end
      class C < B; def bar; foo; end; end

      $test_simple_hierarchy1 = C.new.bar
      class B; def foo; 2; end; end
      $test_simple_hierarchy2 = C.new.bar
    end
    assert_equal(1, $test_simple_hierarchy1)
    assert_equal(2, $test_simple_hierarchy2)
  end

  def test_simple_include
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end
      module B; end
      class C < A; include B; def bar; foo; end; end
      $test_simple_include1 = C.new.bar

      # modules included into modules don't cause flush
      module X; def foo; 2; end; end
      module B; include X; end
      $test_simple_include2 = C.new.bar
      module X; def foo; 3; end; end
      $test_simple_include3 = C.new.bar

      # changes in included modules do cause flush
      module B; def foo; 4; end; end
      $test_simple_include4 = C.new.bar
    end

    assert_equal(1, $test_simple_include1)
    assert_equal(1, $test_simple_include2)
    assert_equal(1, $test_simple_include3)
    assert_equal(4, $test_simple_include4)
  end

  def test_simple_hierarchy_send
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end
      class B < A; end
      class C < B; def bar; foo; end; end

      $test_simple_hierarchy1 = C.new.send :bar
      class B; def foo; 2; end; end
      $test_simple_hierarchy2 = C.new.send :bar
    end
    assert_equal(1, $test_simple_hierarchy1)
    assert_equal(2, $test_simple_hierarchy2)
  end

  def test_simple_include_send
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end
      module B; end
      class C < A; include B; def bar; foo; end; end
      $test_simple_include1 = C.new.send :bar

      # modules included into modules don't cause flush
      module X; def foo; 2; end; end
      module B; include X; end
      $test_simple_include2 = C.new.send :bar
      module X; def foo; 3; end; end
      $test_simple_include3 = C.new.send :bar

      # changes in included modules do cause flush
      module B; def foo; 4; end; end
      $test_simple_include4 = C.new.send :bar
    end

    assert_equal(1, $test_simple_include1)
    assert_equal(1, $test_simple_include2)
    assert_equal(1, $test_simple_include3)
    assert_equal(4, $test_simple_include4)
  end

  def test_simple_alias
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end;
      class B < A; def bar; 2; end; end
      class C < B; end
      class D < C; def go; bar; end; end
      $test_simple_include1 = D.new.bar

      class A; alias bar foo; end
      $test_simple_include2 = D.new.bar

      class C; alias bar foo; end
      $test_simple_include3 = D.new.bar
    end

    assert_equal(2, $test_simple_include1)
    assert_equal(2, $test_simple_include2)
    assert_equal(1, $test_simple_include3)
  end

  def test_simple_alias_send
    obj = Object.new
    class << obj
      class A; def foo; 1; end; end;
      class B < A; def bar; 2; end; end
      class C < B; end
      class D < C; def go; bar; end; end
      $test_simple_include1 = D.new.send :bar

      class A; alias bar foo; end
      $test_simple_include2 = D.new.send :bar

      class C; alias bar foo; end
      $test_simple_include3 = D.new.send :bar
    end

    assert_equal(2, $test_simple_include1)
    assert_equal(2, $test_simple_include2)
    assert_equal(1, $test_simple_include3)
  end
end
