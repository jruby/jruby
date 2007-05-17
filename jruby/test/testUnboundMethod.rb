require 'test/minirunit'
test_check "Test UnboundMethod"


class Square
  def area; @side * @side; end
  def initialize(side); @side = side; end
end

area_unbound = Square.instance_method(:area)

s = Square.new(12)
area = area_unbound.bind(s)
test_equal(144, area.call)

class Test
  def test; :original; end
  def arity_method(a,b,c); end
end

um = Test.instance_method(:test)
class Test
  def test; :modified; end
end
t = Test.new
test_equal(:modified, t.test)
test_equal(:original, um.bind(t).call)

##### arity #####
test_equal(0, area_unbound.arity)
a = Test.instance_method(:arity_method)
test_equal(3, a.arity)

##### bind #####
class A
  def test; self.class; end
end
class B < A; end
class C < B; end
um = B.instance_method(:test)
test_equal(C, um.bind(C.new).call)
test_equal(B, um.bind(B.new).call)
test_exception(TypeError) { um.bind(A.new).call }
