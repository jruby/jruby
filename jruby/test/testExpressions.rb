require 'test/minirunit'
test_check "Test Expressions:"

test_exception(NameError) { @@a }
@@a ||= 'one'
test_equal('one', @@a)
@@a ||= 'two'
test_equal('one', @@a)

@b ||= 'one'
test_equal('one', @b)
@b ||= 'two'
test_equal('one', @b)

def foo=(arg)
  "bar"
end

test_equal("baz", self.foo = "baz")