require 'test/minirunit'
test_check "Test Blocks:"
values = []
5.times do |i|
   values.push i
end

test_ok([0,1,2,3,4] == values)
values = []
2.step 10, 2 do |i|
   values.push i
end

test_ok([2,4,6,8,10] == values)

values = []
[1,2,3].each {|v| values << v; break }
test_equal([1], values)

values = []
result = [1,2,3,4,5].collect {|v|
  if v > 2
    break
  end
  values << v
  v
}
test_equal([1,2], values)
test_ok(result.nil?)

def method1
  if id()   # Any non-toplevel method will do
    yield
  end
end
def method2
  method1 {
    yield
  }
end
method2 {
  test_ok(true)
}

class TestBlock_Foo
  def foo
    Proc.new { self }
  end
end
proc = TestBlock_Foo.new.foo
o = Object.new
test_equal(o, o.instance_eval(&proc))

test_equal(-1, Proc.new { 1 }.arity)
#test_equal(0, Proc.new{|| 1 }.arity)
#test_equal(2, Proc.new {|x,y| 1}.arity)
test_equal(-1, Proc.new{|*x| 1}.arity)

def f; yield; end; f {|*a| test_ok(a == []) }

# test passing blocks to super
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

test_equal("bar", B.new.foo { "bar" })

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

test_ok(Baz.new.foo { })

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

do_it {
}
test_equal(["C: b true", "D: c false", "C: a true"], $results)

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

block = Creator.new.create_block { "here" }
test_no_exception {Holder.new.call_block(&block)}
test_equal("here", Holder.new.call_block(&block))
