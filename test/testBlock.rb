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
