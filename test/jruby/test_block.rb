require 'test/unit'

class TestBlock < Test::Unit::TestCase
  def test_block_variable_closure
    values = []
    5.times do |i|
      values.push i
    end
    assert_equal([0,1,2,3,4], values)

    values = []
    2.step 10, 2 do |i|
      values.push i
    end
    assert_equal([2,4,6,8,10], values)
  end

  def test_block_break
    values = []
    [1,2,3].each {|v| values << v; break }
    assert_equal([1], values)

    values = []
    result = [1,2,3,4,5].collect {|v|
      if v > 2
        break
      end
      values << v
      v
    }
    assert_equal([1,2], values)
    assert(result.nil?)
  end

  def method1
    if object_id   # Any non-toplevel method will do
      yield
    end
  end
  def method2
    method1 {
      yield
    }
  end

  def test_block_yield
    flag = false
    method2 {
      flag = true
    }
    assert(flag)
  end

  class TestBlock_Foo
    def foo
      Proc.new { self }
    end
  end

  def test_proc_as_block_arg
    proc = TestBlock_Foo.new.foo
    o = Object.new
    assert_equal(o, o.instance_eval(&proc))
  end

  def test_proc_arity
    assert_equal(0, Proc.new { 1 }.arity)
    assert_equal(0, Proc.new{|| 1 }.arity)
    assert_equal(2, Proc.new {|x,y| 1}.arity)
    assert_equal(-1, Proc.new{|*x| 1}.arity)
  end

  def f; yield; end
  def test_yield_with_zero_arity
    f {|*a| assert(a == []) }
  end

  class A
    def foo
      yield
    end
  end

  class B < A
    def foo
      super
    end
  end

  def test_block_passed_to_super
    assert_equal("bar", B.new.foo { "bar" })
  end

  # test blocks being available to procs (JRUBY-91)
  class Baz
    def foo
      bar do
        qux
      end
    end

    def bar(&block)
      block.call
    end

    def qux
      if block_given?
        return false
      end
      return true
    end
  end

  def test_block_available_to_proc
    assert(Baz.new.foo { })
  end

  # test instance_evaling with more complicated block passing (JRUBY-88)
  $results = []
  class C
    def t(&block)
      if block
        instance_eval &block
      end
    end
    def method_missing(sym, *args, &block)
      $results << "C: #{sym} #{!block}"
      if sym == :b
        return D.new { |block|
          t(&block)
        }
      end
      t(&block)
    end
  end

  class D
    def initialize(&blk)
      @blk = blk
    end

    def method_missing(sym, *args, &block)
      $results << "D: #{sym} #{!block}"
      @blk.call(block)
    end
  end
  def do_it(&blk)
    C.new.b.c {
      a 'hello'
    }
  end

  def test_block_passing_with_instance_eval
    do_it {
    }
    assert_equal(["C: b true", "D: c false", "C: a true"], $results)
  end

  def test_instance_exec_self
    o = Object.new
    assert_equal(o, o.instance_exec { self })
  end

  def test_instance_exec_self_args
    o = Object.new
    assert_equal(o, o.instance_exec(1) { self })
  end

  def test_instance_exec_args_result
    o = Object.new
    assert_equal(2, o.instance_exec(1) { |x| x + 1 })
  end

  def test_instance_exec_args_multiple_result
    o = Object.new
    assert_equal([1, 4], o.instance_exec(1, 2, 3, 4) { |a, b, c, d| [a, d] })
  end

  def test_instance_exec_no_block
    o = Object.new
    assert_raise(LocalJumpError) { o.instance_exec }
  end

  def test_instance_exec_no_block_args
    o = Object.new
    assert_raise(LocalJumpError) { o.instance_exec(1) }
  end
  
  # ensure proc-ified blocks can be yielded to when no block arg is specified in declaration
  class Holder
    def call_block
      yield
    end
  end

  class Creator
    def create_block
      proc do
        yield
      end
    end
  end

  def test_block_converted_to_proc_yields
    block = Creator.new.create_block { "here" }
    assert_nothing_raised {Holder.new.call_block(&block)}
    assert_equal("here", Holder.new.call_block(&block))
  end

  def proc_call(&b)
    b.call
  end

  def proc_return1
    proc_call{return 42}+1
  end

  def proc_return2
    puts proc_call{return 42}+1
  end

  def test_proc_or_block_return
    assert_nothing_raised { assert_equal 42, proc_return1 }
    assert_nothing_raised { assert_equal 42, proc_return2 }
  end

  def bar(a, b)
    yield a, b
  end

  def block_arg_that_breaks_while(&block)
    while true
      block.call
    end
  end
  
  def block_that_breaks_while
    while true
      yield
    end
  end

  def test_block_arg_that_breaks_while
    assert_nothing_raised { block_arg_that_breaks_while { break }}
  end
  
  def test_block_that_breaks_while
    assert_nothing_raised { block_that_breaks_while { break }}
  end
  
  def yield_arg(arg)
    yield arg
  end
  
  def block_call_arg(arg,&block)
    block.call arg
  end
  
  def test_yield_arg_expansion
    assert_equal 1, yield_arg([1,2]) { |a,b| a }
    assert_equal 1, block_call_arg([1,2]) { |a,b| a }
  end
end


class TestVarArgBlock < Test::Unit::TestCase
  def blockyield(arg)
    yield arg
  end

  def blockarg(arg, &b)
    b.call(arg)
  end

  def test_vararg_blocks
    Proc.new { |*element| assert_equal [["a"]], element }.call( ["a"] )
    Proc.new { |*element| assert_equal ["a"], element }.call( "a" )
    proc { |*element| assert_equal [["a"]], element }.call( ["a"] )
    proc { |*element| assert_equal ["a"], element }.call( "a" )
    lambda { |*element| assert_equal [["a"]], element }.call( ["a"] )
    lambda { |*element| assert_equal ["a"], element }.call( "a" )
    blockyield(["a"]) { |*element| assert_equal [["a"]], element }
    blockyield("a") { |*element| assert_equal ["a"], element }
    blockyield(["a"], &Proc.new { |*element| assert_equal [["a"]], element })
    blockyield("a", &Proc.new { |*element| assert_equal ["a"], element })
    blockyield(["a"], &proc { |*element| assert_equal [["a"]], element })
    blockyield("a", &proc { |*element| assert_equal ["a"], element })
    blockyield(["a"], &lambda { |*element| assert_equal [["a"]], element })
    blockyield("a", &lambda { |*element| assert_equal ["a"], element })
    blockarg(["a"]) { |*element| assert_equal [["a"]], element }
    blockarg("a") { |*element| assert_equal ["a"], element }
    blockarg(["a"], &Proc.new { |*element| assert_equal [["a"]], element })
    blockarg("a", &Proc.new { |*element| assert_equal ["a"], element })
    blockarg(["a"], &proc { |*element| assert_equal [["a"]], element })
    blockarg("a", &proc { |*element| assert_equal ["a"], element })
    blockarg(["a"], &lambda { |*element| assert_equal [["a"]], element })
    blockarg("a", &lambda { |*element| assert_equal ["a"], element })
  end

  def test_requiredarg_blocks
    Proc.new { |element| assert_equal ["a"], element }.call( ["a"] )
    Proc.new { |element| assert_equal "a", element }.call( "a" )
    proc { |element| assert_equal ["a"], element }.call( ["a"] )
    proc { |element| assert_equal "a", element }.call( "a" )
    lambda { |element| assert_equal ["a"], element }.call( ["a"] )
    lambda { |element| assert_equal "a", element }.call( "a" )
    blockyield(["a"]) { |element| assert_equal ["a"], element }
    blockyield("a") { |element| assert_equal "a", element }
    blockyield(["a"], &Proc.new { |element| assert_equal ["a"], element })
    blockyield("a", &Proc.new { |element| assert_equal "a", element })
    blockyield(["a"], &proc { |element| assert_equal ["a"], element })
    blockyield("a", &proc { |element| assert_equal "a", element })
    blockyield(["a"], &lambda { |element| assert_equal ["a"], element })
    blockyield("a", &lambda { |element| assert_equal "a", element })
    blockarg(["a"]) { |element| assert_equal ["a"], element }
    blockarg("a") { |element| assert_equal "a", element }
    blockarg(["a"], &Proc.new { |element| assert_equal ["a"], element })
    blockarg("a", &Proc.new { |element| assert_equal "a", element })
    blockarg(["a"], &proc { |element| assert_equal ["a"], element })
    blockarg("a", &proc { |element| assert_equal "a", element })
    blockarg(["a"], &lambda { |element| assert_equal ["a"], element })
    blockarg("a", &lambda { |element| assert_equal "a", element })
  end

  def test_requiredargs_blocks
    Proc.new { |element, a| assert_equal "a", element }.call( ["a"] )
    Proc.new { |element, a| assert_equal "a", element }.call( "a" )
      proc { |element, a| assert_equal "a", element }.call( ["a"] )
      proc { |element, a| assert_equal "a", element }.call( "a" )
    assert_raises(ArgumentError) {
      lambda { |element, a| assert_equal ["a"], element }.call( ["a"] )
    }
    assert_raises(ArgumentError) {
      lambda { |element, a| assert_equal "a", element }.call( "a" )
    }
    blockyield(["a"]) { |element, a| assert_equal "a", element }
    blockyield("a") { |element, a| assert_equal "a", element }
    blockyield(["a"], &Proc.new { |element, a| assert_equal "a", element })
    blockyield("a", &Proc.new { |element, a| assert_equal "a", element })
    blockyield(["a"], &proc { |element, a| assert_equal "a", element })
    blockyield("a", &proc { |element, a| assert_equal "a", element })
    assert_raises(ArgumentError) {
      blockyield(["a"], &lambda { |element, a| assert_equal "a", element })
    }
    assert_raises(ArgumentError) {
      blockyield("a", &lambda { |element, a| assert_equal "a", element })
    }
      blockarg(["a"]) { |element, a| assert_equal "a", element }
      blockarg("a") { |element, a| assert_equal "a", element }
      blockarg(["a"], &Proc.new { |element, a| assert_equal "a", element })
      blockarg("a", &Proc.new { |element, a| assert_equal "a", element })
      blockarg(["a"], &proc { |element, a| assert_equal "a", element })
      blockarg("a", &proc { |element, a| assert_equal "a", element })
    assert_raises(ArgumentError) {
      blockarg(["a"], &lambda { |element, a| assert_equal ["a"], element })
    }
    assert_raises(ArgumentError) {
      blockarg("a", &lambda { |element, a| assert_equal "a", element })
    }
  end

  def check_all_definemethods(obj)
    results = obj.foo1 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo2 "a"
    assert_equal(results[0], results[1])
    results = obj.foo3 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo4 "a"
    assert_equal(results[0], results[1])
    results = obj.foo5 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo6 "a"
    assert_equal(results[0], results[1])
    results = obj.foo7 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo8 "a"
    assert_equal(results[0], results[1])
    results = obj.foo9 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo10 "a"
    assert_equal(results[0], results[1])
    results = obj.foo11 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo12 "a"
    assert_equal(results[0], results[1])
    results = obj.foo13 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo14 "a"
    assert_equal(results[0], results[1])
  end

  def check_requiredargs_definemethods(obj)
    assert_raises(ArgumentError) { results = obj.foo1 ["a"] }
    # assert_equal(results[0], results[1])
    assert_raises(ArgumentError) { results = obj.foo2 "a" }
    # assert_equal(results[0], results[1])
    assert_raises(ArgumentError) { results = obj.foo3 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo4 "a" }
    assert_raises(ArgumentError) { results = obj.foo5 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo6 "a" }
    assert_raises(ArgumentError) { results = obj.foo7 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo8 "a" }
    assert_raises(ArgumentError) { results = obj.foo9 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo10 "a" }
    assert_raises(ArgumentError) { results = obj.foo11 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo12 "a" }
    assert_raises(ArgumentError) { results = obj.foo13 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo14 "a" }
  end

  def test_definemethods
    obj = Object.new

    class << obj
      define_method :foo1, Proc.new { |*element| [[["a"]], element] }
      define_method :foo2, Proc.new { |*element| [["a"], element] }
      define_method :foo3, proc { |*element| [[["a"]], element] }
      define_method :foo4, proc { |*element| [["a"], element] }
      define_method :foo5, lambda { |*element| [[["a"]], element] }
      define_method :foo6, lambda { |*element| [["a"], element] }
      define_method(:foo7) { |*element| [[["a"]], element] }
      define_method(:foo8) { |*element| [["a"], element] }
      define_method :foo9, &Proc.new { |*element| [[["a"]], element] }
      define_method :foo10, &Proc.new { |*element| [["a"], element] }
      define_method :foo11, &proc { |*element| [[["a"]], element] }
      define_method :foo12, &proc { |*element| [["a"], element] }
      define_method :foo13, &lambda { |*element| [[["a"]], element] }
      define_method :foo14, &lambda { |*element| [["a"], element] }
    end

    check_all_definemethods(obj)

    class << obj
      define_method :foo1, Proc.new { |element| [["a"], element] }
      define_method :foo2, Proc.new { |element| ["a", element] }
      define_method :foo3, proc { |element| [["a"], element] }
      define_method :foo4, proc { |element| ["a", element] }
      define_method :foo5, lambda { |element| [["a"], element] }
      define_method :foo6, lambda { |element| ["a", element] }
      define_method(:foo7) { |element| [["a"], element] }
      define_method(:foo8) { |element| ["a", element] }
      define_method :foo9, &Proc.new { |element| [["a"], element] }
      define_method :foo10, &Proc.new { |element| ["a", element] }
      define_method :foo11, &proc { |element| [["a"], element] }
      define_method :foo12, &proc { |element| ["a", element] }
      define_method :foo13, &lambda { |element| [["a"], element] }
      define_method :foo14, &lambda { |element| ["a", element] }
    end

    check_all_definemethods(obj)

    class << obj
      define_method :foo1, Proc.new { |element, a| ["a", element] }
      define_method :foo2, Proc.new { |element, a| ["a", element] }
      define_method :foo3, proc { |element, a| [["a"], element] }
      define_method :foo4, proc { |element, a| ["a", element] }
      define_method :foo5, lambda { |element, a| [["a"], element] }
      define_method :foo6, lambda { |element, a| ["a", element] }
      define_method(:foo7) { |element, a| [["a"], element] }
      define_method(:foo8) { |element, a| ["a", element] }
      define_method :foo9, &Proc.new { |element, a| [["a"], element] }
      define_method :foo10, &Proc.new { |element, a| ["a", element] }
      define_method :foo11, &proc { |element, a| [["a"], element] }
      define_method :foo12, &proc { |element, a| ["a", element] }
      define_method :foo13, &lambda { |element, a| [["a"], element] }
      define_method :foo14, &lambda { |element, a| ["a", element] }
    end

    check_requiredargs_definemethods(obj)
  end
end


class TestCrazyBlocks < Test::Unit::TestCase
  def foo(a)
    p = proc { a.each {|x| yield x } }
    1.times { p.call }
  end

  def bar(&b)
    [[1,2],[3,4]].each(&b)
  end

  def baz
    bar {|a| foo(a) { |x| yield x } }
  end

  def test_crazy
    a = []
    p = proc {|y| a << y}
    baz {|x| p.call x}
    assert_equal [1,2,3,4], a
  end

  def hello(&b)
    1.times { b.call }
  end

  def test_crazy2
    a = []
    hello {
      p = proc {|y| a << y}
      baz{|x| p.call x}
    }
    assert_equal [1,2,3,4], a
  end

  def test_crazy3
    a = []
    p = proc {|y| a << y}
    self.class.send(:define_method, :goodbye) {
      hello {
        baz {|x| p.call(x)}
      }
    }
    goodbye
    assert_equal [1,2,3,4], a
  end

  def test_crazy4
    a = []
    p = proc {|x| a << x}
    hello {
      p2 = proc { |x| eval "p.call x" }
      baz { |x| eval "p2.call x" }
    }
    assert_equal [1,2,3,4], a
  end

end
