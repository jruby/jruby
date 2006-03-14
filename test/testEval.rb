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

old_self = self
test_equal(A.new.y, "foo")
test_equal(x, "foo")

#### ensure self is back to pre bound eval
test_equal(self, old_self)

#### ensure returns within ensures that cross evalstates during an eval are handled properly (whew!)
def inContext &proc 
   begin
     proc.call
   ensure
   end
end

def x2
  inContext do
     return "foo"
  end
end

test_equal(x2, "foo")