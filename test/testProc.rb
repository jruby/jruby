require 'test/minirunit'

test_check "Test Procs"

test_equal(-1, Proc.new {}.arity)
test_equal(0, Proc.new {||}.arity)
test_equal(1, Proc.new {|a|}.arity)
test_equal(2, Proc.new {|a, b|}.arity)
test_equal(3, Proc.new {|a, b, c|}.arity)
test_equal(-1, Proc.new {|*a|}.arity)
test_equal(-2, Proc.new {|a, *b|}.arity)
test_equal(-3, Proc.new {|a, b, *c|}.arity)

# when adding arity to blocks, I broke the for loop
j = 0; for i in 1..3 do j += i end; test_equal(6, j)
j = 0; for $i in 1..3 do j += $i end; test_equal(6, j)
j = 0; for i, k in {1=>2, 3=>4} do j += i + k end; test_equal(10, j)

def argument_test(state, proc, *args)
  x = state
  begin
    proc.call(*args)
  rescue ArgumentError
    x = !x
  end
  test_ok(x)
end

argument_test(true, lambda{||})
argument_test(false, lambda{||}, 1)
argument_test(true, lambda{|a,|}, 1)
argument_test(false, lambda{|a,|})
argument_test(false, lambda{|a,|}, 1,2)

l = lambda{1}
test_ok(l.clone.call == 1)

##### Proc.binding #####
c = "a"
def fred(param)
  lambda {}
end

b = fred(99)
test_equal(99, eval("param", b.binding))
test_equal(99, eval("param", b))