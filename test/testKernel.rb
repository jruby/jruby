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

# JRUBY-117 - to_a should be public
test_equal(["to_a"], Object.public_instance_methods.grep(/to_a/))
# JRUBY-117 - remove_instance_variable should be private
test_equal(["remove_instance_variable"], Object.private_instance_methods.grep(/remove_instance_variable/))

# JRUBY-116 (Array())
class A1; def to_ary; [1]; end; end
class A2; def to_a  ; [2]; end; end
class A3; def to_ary;   3; end; end
class A4; def to_a  ;   4; end; end
class A5; def to_ary; [5]; end; def to_a  ; [:no]; end; end
class A6; def to_ary; :no; end; def to_a  ;   [6]; end; end
class A7; end
class A8; def to_ary; nil; end; end
class A9; def to_a  ; nil; end; end


test_equal([], Array(nil))
# No warning for this first case either
test_equal([1], Array(1))
test_equal([1], Array(A1.new))
test_equal([2], Array(A2.new))
test_exception(TypeError) { Array(A3.new) }
test_exception(TypeError) { Array(A4.new) }
test_equal([5], Array(A5.new))
test_exception(TypeError) { Array(A6.new) }
a = A7.new
test_equal([a], Array(a))
a = A8.new
test_equal([a], Array(a))
test_exception(TypeError) { Array(A9.new) }
