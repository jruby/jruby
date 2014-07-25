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
    assert_equal(-1, Proc.new { 1 }.arity)
    #assert_equal(0, Proc.new{|| 1 }.arity)
    #assert_equal(2, Proc.new {|x,y| 1}.arity)
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

  if defined? instance_exec
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
    assert_raise(ArgumentError) { o.instance_exec }
  end

  def test_instance_exec_no_block_args
    o = Object.new
    assert_raise(ArgumentError) { o.instance_exec(1) }
  end
  end # if defined? instance_exec
  
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
  
  def test_block_hash_args
    h = Hash.new
    bar(1, 2) { |h[:v], h[:u]| }
    puts h[:v], h[:u]
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
