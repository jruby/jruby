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
