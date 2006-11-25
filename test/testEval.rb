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

# test that evaling a proc doesn't goof up the module nesting for a binding
proc_binding = eval("proc{binding}.call", TOPLEVEL_BINDING)
nesting = eval("$nesting = nil; class A; $nesting = Module.nesting; end; $nesting", TOPLEVEL_BINDING)
test_equal("A", nesting.to_s)

class Foo
  def initialize(p)
    @prefix = p
  end

  def result(val)
    redefine_result
    result val
  end
  
  def redefine_result
    method_decl = "def result(val); \"#{@prefix}: \#\{val\}\"; end"
    instance_eval method_decl, "generated code (#{__FILE__}:#{__LINE__})"
  end
end

f = Foo.new("foo")
test_equal "foo: hi", f.result("hi")

g = Foo.new("bar")
test_equal "bar: hi", g.result("hi")

test_equal "foo: bye", f.result("bye")
test_equal "bar: bye", g.result("bye")

# JRUBY-214 - eval should call to_str on arg 0
class Bar
  def to_str
    "magic_number"
  end
end
magic_number = 1
test_equal(magic_number, eval(Bar.new))

test_exception(TypeError) { eval(Object.new) }
