require 'test/minirunit'

# ensure binding is setting self correctly
def x
  "foo"
end

Z = binding

class A
  def x
    "bar"
  end

  def y
    eval("x", Z)
  end
end

test_equal(A.new.y, "foo")