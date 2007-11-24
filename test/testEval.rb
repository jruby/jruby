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
test_equal("foo", A.new.y)
test_equal("foo", x)

#### ensure self is back to pre bound eval
test_equal(old_self, self)

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

# JRUBY-386 tests
# need at least one arg
test_exception(ArgumentError) { eval }
test_exception(ArgumentError) {self.class.module_eval}
test_exception(ArgumentError) {self.class.class_eval}
test_exception(ArgumentError) {3.instance_eval}

# args must respond to #to_str
test_exception(TypeError) {eval 3}
test_exception(TypeError) {self.class.module_eval 3}
test_exception(TypeError) {self.class.class_eval 4}
test_exception(TypeError) {3.instance_eval 4}

begin
  eval 'return'
rescue LocalJumpError => e
  test_ok(e.message =~ /unexpected return$/)
end

begin
  eval 'break'
rescue LocalJumpError => e
  test_ok(e.message =~ /unexpected break$/)
end

begin
  "".instance_eval 'break'
rescue LocalJumpError => e
  test_ok(e.message =~ /unexpected break$/)
end

begin
  "".instance_eval 'return'
rescue LocalJumpError => e
  test_ok(e.message =~ /unexpected return$/)
end

# If getBindingRubyClass isn't used, this test case will fail,
# since when eval gets called, Kernel will get pushed on the
# parent-stack, and this will always become the RubyClass for
# the evaled string, which is incorrect.
class AbcTestFooAbc
  eval <<-ENDT
  def foofoo_foofoo
  end
ENDT
end

test_equal ["foofoo_foofoo"], AbcTestFooAbc.instance_methods.grep(/foofoo_foofoo/)
test_equal [], Object.instance_methods.grep(/foofoo_foofoo/)

# test Binding.of_caller
def foo
  x = 1
  bar
end

def bar
  eval "x + 1", Binding.of_caller
end

test_equal(2, foo)

# test returns within an eval
def foo
  eval 'return 1'
  return 2
end
def foo2
  x = "blah"
  x.instance_eval "return 1"
  return 2
end

test_equal(1, foo)
# this case is still broken
test_equal(1, foo2)

$a = 1
eval 'BEGIN { $a = 2 }'
test_equal(1, $a)

$b = nil
class Foo
  $b = binding
end

a = Object.new
main = self
b = binding
a.instance_eval { 
  eval("test_equal(a, self)") 
  eval("test_equal(main,self)", b)
  eval("test_equal(Foo, self)", $b)
}

module EvalScope
  eval "class Bar; def self.foo; 'foo'; end; end"
end

test_equal("foo", EvalScope::Bar.foo)
