require 'test/minirunit'
test_check "Test Visibility:"

class VisibilityTest
  private
  def foo
    "foo"
  end

  def bar
    foo
  end

  public :bar
end

foo = VisibilityTest.new

test_equal(foo.bar, "foo")
test_exception(NameError) {
  foo.foo
}

$a = false
class A
  class << self
    protected
    def a=(b); $a = true;end

  end
  self.a=:whatever
end

test_equal(true, $a)

def foo
end
# top-level scope defaults to private
test_no_exception { foo }
test_exception { self.foo }

# top-level scope should allow setting visibility
public
def foo
end
test_no_exception { self.foo }

# module, class, struct bodies should default to public
private
s = "bar"
m = Module.new { def foo; end }
s.extend(m)
test_no_exception { s.foo }

s = Struct.new(:foo, :bar) {
  def foo; end
}
test_no_exception { s.new(0,0).foo }

c = Class.new { def foo; end }
test_no_exception { c.new.foo }

# blocks can't permanently modify containing frame's visibility
1.times { public }
def foo; end
test_exception { self.foo }