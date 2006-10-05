require 'test/minirunit'

class InspectInner
  def initialize(); @a = 1; end
end

class InspectOuter
  def initialize(foo); @b = foo; end
end

IRE = /(#<([^:]+):\S+(?:|\s@([^=]+)=([^#\s>]+))*>)/

def test_inspect_meat(inspect_string, *expected)
  match = IRE.match(inspect_string)

  test_equal(expected[0], match[2])
  test_equal(expected[1], match[3])
  test_equal(expected[2], match[4])
end

b = InspectOuter.new(InspectInner.new)

outer = b.inspect
outer.sub!(IRE, '___')
inner = $1

test_inspect_meat(outer, "InspectOuter", "b", "___")
test_inspect_meat(inner, "InspectInner", "a", "1")
