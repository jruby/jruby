require 'test/minirunit'
test_check "kernel"

test_ok(! eval("defined? some_unknown_variable"))
x = 1
test_equal(1, eval("x"))
eval("x = 2")
test_equal(2, x)
eval("unknown = 3")
test_equal(2, x)     # Make sure eval() didn't destroy locals
test_ok(! defined? unknown)
test_equal(nil, true && defined?(Bogus))