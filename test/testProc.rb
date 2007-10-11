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

# procs aren't unecessarily cloned when passed to functions
def test_same_proc(obj_id, &p)
    test_equal(obj_id, p.__id__)
end

abc_proc = proc { :abc }
test_same_proc(abc_proc.__id__, &abc_proc)

test_exception(ArgumentError) {Proc.new(1) {}}

def foo(&block)
  block.call([1, 2, 3])
end

foo { |a, b, c| test_equal(1, a) }

p1 = Proc.new {}
p2 = p1.clone
test_equal(p1, p2)
test_ok(p1.to_s != nil)
