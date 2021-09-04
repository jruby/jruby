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

  # JRUBY-2921

  class MySuper
    def self.meth1
      'MySuper::meth1'
    end
    def self.meth2
      'MySuper::meth2'
    end
  end

  class MySub < MySuper
  end

  def test_jruby_2921
    meth1 = :meth1
    meth2 = :meth2
    assert_equal 1, MySub.methods.select {|m| m == meth1}.size
    assert_equal 1, MySub.methods.select {|m| m == meth2}.size

    assert_equal('MySuper::meth1', MySub::meth1)
    assert_equal('MySuper::meth1', calling_meth1)
    assert_equal('MySuper::meth2', MySub::meth2)
    # Note: calling_meth2 is not called here

    self.class.class_eval "
    class MySub
      def self.meth1
        'MySub::meth1'
      end
      def self.meth2
        'MySub::meth2'
      end
    end
  "

    assert_equal 1, MySub.methods.select {|m| m == meth1}.size
    assert_equal 1, MySub.methods.select {|m| m == meth2}.size

    assert_equal('MySub::meth1', MySub::meth1)
    assert_equal('MySub::meth1', calling_meth1)
    assert_equal('MySub::meth2', MySub::meth2)
    assert_equal('MySub::meth2', calling_meth2)
  end

  private

  def calling_meth1
    MySub.meth1
  end

  def calling_meth2
    MySub.meth2
  end

end
