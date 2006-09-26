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

# JRUBY-116 - Should not see warning
Array(:abc)
# JRUBY-117 - to_a should be public
test_equal(["to_a"], Object.public_instance_methods.grep(/to_a/))
# JRUBY-117 - remove_instance_variable should be private
test_equal(["remove_instance_variable"], Object.private_instance_methods.grep(/remove_instance_variable/))